package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

public class TileIngots extends TileEntity {

	public static final Strategy<ItemStack> strategy = new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			return o == null ? 0 : (o.getItemDamage() + "" + o.getItem().getRegistryName()).hashCode();
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == null || b == null ? false : a.getItemDamage() == b.getItemDamage() && a.getItem() == b.getItem();
		}
	};

	public boolean needSync = true;
	public boolean isMaster;
	public BlockPos masterPos;
	//	private final Vector3f ingotSize = new Vector3f(1f / Stackable.perX, 1f / Stackable.perY, 1f / Stackable.perZ);
	public final ItemStack[][][] ingots = new ItemStack[Stackable.perX][Stackable.perY][Stackable.perZ];
	public final int maxIngotAmount = Stackable.perX * Stackable.perY * Stackable.perZ;

	ItemHandler handler = new ItemHandler(this);

	private AxisAlignedBB box = null;
	public boolean changedClient = true;

	public TileIngots() {
		fillArray();
	}

	private static Object2BooleanOpenCustomHashMap<ItemStack> validCache = new Object2BooleanOpenCustomHashMap<>(strategy);

	public static boolean validItem(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		if (validCache.containsKey(stack))
			return validCache.getBoolean(stack);
		boolean res = Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).anyMatch(s -> s.startsWith("ingot")) || Stackable.allowedIngots.contains(stack.getItem().getRegistryName());
		validCache.put(stack, res);
		return res;
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
		fillArray();
		changedClient = true;
		box = null;
		if (world != null)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		NBTTagCompound n = compound.getCompoundTag("handler");
		n.removeTag("Size");
		handler.back.deserializeNBT(n);
		isMaster = compound.getBoolean("isMaster");
		masterPos = compound.hasKey("master") ? BlockPos.fromLong(compound.getLong("master")) : null;
		NBTTagList list1 = compound.getTagList("list1", 10);
		int[] list2 = compound.getIntArray("list2");
		Validate.isTrue(list1.tagCount() == list2.length);
		inventory.clear();
		for (int i = 0; i < list1.tagCount(); i++)
			inventory.put(new ItemStack(list1.getCompoundTagAt(i)), list2[i]);
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("handler", handler.back.serializeNBT());
		compound.setBoolean("isMaster", isMaster);
		if (masterPos != null)
			compound.setLong("master", masterPos.toLong());
		NBTTagList list1 = new NBTTagList();
		IntArrayList list2 = new IntArrayList();
		for (Object2IntMap.Entry<ItemStack> e : inventory.object2IntEntrySet()) {
			list1.appendTag(e.getKey().writeToNBT(new NBTTagCompound()));
			list2.add(e.getIntValue());
		}
		compound.setTag("list1", list1);
		compound.setIntArray("list2", list2.toIntArray());
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

	public ItemStack extractItem(ItemStack stack, int amount, boolean simulate) {
		int i = inventory.getInt(stack);
		if (i <= 0)
			return ItemStack.EMPTY;
		i = Math.min(amount, i);
		if (!simulate) {
			inventory.addTo(stack, -i);
			if (inventory.getInt(stack) == 0) {
				inventory.removeInt(stack);
				if (inventory.isEmpty())
					new Thread(() -> world.getMinecraftServer().addScheduledTask(() -> world.setBlockToAir(pos))).start();
			}
		}
		return ItemHandlerHelper.copyStackWithSize(stack, i);
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		if (!validItem(stack))
			return stack;
		return null;
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

	private void fillArray() {
		int count = 0;
		for (int y = 0; y < Stackable.perY; y++) {
			for (int z = 0; z < Stackable.perZ; z++) {
				for (int x = 0; x < Stackable.perX; x++) {
					ItemStack s = handler.getStackInSlot(count);
					count++;
					ingots[x][y][z] = s;
				}
			}
		}
	}

	public final Object2IntLinkedOpenCustomHashMap<ItemStack> inventory = new Object2IntLinkedOpenCustomHashMap<>(strategy);

	private static class II implements IItemHandler {
		TileIngots tile;
		private int slots = -1;
		private List<ItemStack> items = null;
		boolean threadStarted = false;

		public II(TileIngots tile) {
			super();
			this.tile = tile;
		}

		@Override
		public int getSlots() {
			return slots != -1 ? slots : (slots = tile.inventory.object2IntEntrySet().stream().mapToInt(e -> (int) Math.ceil(e.getIntValue() / (double) e.getKey().getMaxStackSize())).sum() + 1);
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			List<ItemStack> l = getItems();
			return slot >= 0 && slot < l.size() ? l.get(slot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack s = tile.insertItem(stack, simulate);
			if (!simulate && s.getCount() != stack.getCount())
				onChange();
			return s;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			amount = Math.min(amount, Stackable.itemsPerIngot);
			ItemStack s = tile.extractItem(getStackInSlot(slot), amount, simulate);
			if (!simulate && !s.isEmpty())
				onChange();
			return s;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		private void onChange() {
			tile.needSync = true;
			tile.markDirty();
			tile.box = null;
			if (!threadStarted) {
				threadStarted = true;
				new Thread(() -> tile.world.getMinecraftServer().addScheduledTask(() -> {
					items = null;
					threadStarted = false;
				})).start();
			}
		}

		private List<ItemStack> getItems() {
			if (items != null)
				return items;
			items = new ArrayList<>();
			for (Object2IntMap.Entry<ItemStack> e : tile.inventory.object2IntEntrySet()) {
				int stacks = (int) Math.ceil(e.getIntValue() / (double) e.getKey().getMaxStackSize());
				for (int i = 0; i < stacks - 1; i++)
					items.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), e.getKey().getMaxStackSize()));
				int lastSize = e.getIntValue() % e.getKey().getMaxStackSize();
				if (lastSize > 0)
					items.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), lastSize));
			}
			return items;
		}
	}

	private static class ItemHandler implements IItemHandler {

		TileIngots tile;
		boolean empty = true;
		final int size = Stackable.perX * Stackable.perY * Stackable.perZ;
		ItemStackHandlerBack back = new ItemStackHandlerBack(size);

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
			if (slot == 0 && getStackInSlot(0).isEmpty() && !tile.world.isRemote && false) {
				new Thread(() -> tile.world.getMinecraftServer().addScheduledTask(() -> tile.world.setBlockToAir(tile.pos))).start();
			}
			if (!simulate && !ret.isEmpty() && false)
				back.onLoad();
			return ret;
		}

		private class ItemStackHandlerBack extends ItemStackHandler {

			public ItemStackHandlerBack(int size) {
				super(size);
			}

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
				empty = stacks.stream().allMatch(ItemStack::isEmpty);
				if (true)
					return;
				List<ItemStack> l = stacks.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
				for (int i = 0; i < stacks.size(); i++)
					stacks.set(i, ItemStack.EMPTY);
				for (int j = 0; j < l.size(); j++)
					stacks.set(j, l.get(j));
				System.out.println("load");
			}

		}

	}
}
