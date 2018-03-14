package mrriegel.stackable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = Stackable.MODID)
public class TileIngots extends TileEntity {

	public static Set<TileIngots> tiles = Collections.newSetFromMap(new WeakHashMap<>());

	public boolean needSync = true;
	public boolean isMaster;
	public BlockPos masterPos;

	ItemHandler handler = new ItemHandler(this);

	private AxisAlignedBB box = null;
	private boolean toRemove = false;
	/** client only */
	public boolean changed = true;

	public TileIngots() {
		if (FMLCommonHandler.instance().getEffectiveSide().isServer())
			tiles.add(this);
	}

	public static boolean validItem(ItemStack stack) {
		return !stack.isEmpty() && Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).anyMatch(s -> s.startsWith("ingot"));
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return serializeNBT();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = writeToNBT(new NBTTagCompound());
		return new SPacketUpdateTileEntity(this.pos, 1337, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound tag = pkt.getNbtCompound();
		readFromNBT(tag);
		changed = true;
		box = null;
		if (world != null && world.isRemote)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		NBTTagCompound n = compound.getCompoundTag("handler");
		n.removeTag("Size");
		handler.back.deserializeNBT(n);
		isMaster = compound.getBoolean("isMaster");
		masterPos = compound.hasKey("master") ? BlockPos.fromLong(compound.getLong("master")) : null;
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("handler", handler.back.serializeNBT());
		compound.setBoolean("isMaster", isMaster);
		if (masterPos != null)
			compound.setLong("master", masterPos.toLong());
		return super.writeToNBT(compound);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (isMaster && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (isMaster && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return (T) handler;
		return super.getCapability(capability, facing);
	}

	public AxisAlignedBB getBox() {
		if (box != null)
			return box;
		double yy = 1d / Stackable.perY;
		int count = 0;
		boolean an = false;
		for (int i = 0; i < handler.getSlots(); i++) {
			if (handler.getStackInSlot(i).isEmpty()) {
				count = i;
				an = true;
				break;
			}
		}
		if (!an)
			count = handler.getSlots();
		int heigh = (int) Math.ceil((double) count / (Stackable.perX * Stackable.perZ));
		if (heigh == 0)
			heigh = 1;
		return box = new AxisAlignedBB(0, 0, 0, 1, heigh * yy, 1);
	}

	@SubscribeEvent
	public static void tick(WorldTickEvent event) {
		if (event.phase == Phase.END && !event.world.isRemote && event.world.getTotalWorldTime() % 5 == 0) {
			tiles.stream().filter(t -> t.needSync && !t.toRemove && t.getWorld() == event.world).forEach(t -> {
				t.markDirty();
				for (EntityPlayerMP player : event.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(t.pos.add(-11, -11, -11), t.pos.add(11, 11, 11)))) {
					if (player.ticksExisted > 20) {
						t.needSync = false;
						Packet<?> p = t.getUpdatePacket();
						if (p != null)
							player.connection.sendPacket(p);
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void tick(PlayerLoggedOutEvent event) {
		tiles.clear();
	}

	private static class ItemHandler implements IItemHandler {

		TileIngots tile;

		public ItemHandler(TileIngots tile) {
			super();
			this.tile = tile;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!validItem(stack))
				return stack;
			return back.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return back.getStackInSlot(slot);
		}

		@Override
		public int getSlots() {
			return back.getSlots();
		}

		@Override
		public int getSlotLimit(int slot) {
			return back.getSlotLimit(slot);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			//			if (!getStackInSlot(Math.min(slot + 1, getSlots() - 1)).isEmpty() && slot != getSlots() - 1)
			//				return ItemStack.EMPTY;
			ItemStack ret = back.extractItem(slot, amount, simulate);
			if (slot == 0 && getStackInSlot(0).isEmpty() && !tile.world.isRemote) {
				tile.toRemove = true;
				tile.world.getMinecraftServer().addScheduledTask(() -> tile.world.setBlockToAir(tile.pos));
			}
			return ret;
		}

		private ItemStackHandler back = new ItemStackHandler(Stackable.perX * Stackable.perY * Stackable.perZ) {
			@Override
			protected void onContentsChanged(int slot) {
				tile.needSync = true;
				tile.markDirty();
				tile.box = null;
			}

			@Override
			public int getSlotLimit(int slot) {
				return Stackable.itemsPerIngot;
			}

			@Override
			public void onLoad() {
				List<ItemStack> l = stacks.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
				for (int i = 0; i < stacks.size(); i++)
					stacks.set(i, ItemStack.EMPTY);
				for (int j = 0; j < l.size(); j++)
					stacks.set(j, l.get(j));
			}

		};
	}
}
