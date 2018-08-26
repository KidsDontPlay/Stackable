package mrriegel.stackable.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mrriegel.stackable.PileInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class TileStackable extends TileEntity {
	public static final Strategy<ItemStack> strategy = new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			if (o == null)
				return 0;
			int hash = 31;
			hash ^= o.getItem().getRegistryName().hashCode();
			hash ^= o.getMetadata();
			hash ^= Objects.hashCode(o.getTagCompound());
			return hash;
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			if (a == null || b == null)
				return false;
			if (a.getItem() != b.getItem())
				return false;
			if (a.getMetadata() != b.getMetadata())
				return false;
			return Objects.equals(a.getTagCompound(), b.getTagCompound());
		}
	};

	public static String getOverlayText(ItemStack s, TileStackable t) {
		TileStackable m = t.getMaster();
		return m.inv.inventory.getInt(s) + "x " + s.getDisplayName();
	}

	public PileInventory inv = new PileInventory(this);

	public boolean needSync = true;
	public boolean isMaster;
	public boolean changedClient = true;
	public BlockPos masterPos;
	public long lastTake;

	//cache
	public AxisAlignedBB box;
	public List<AxisAlignedBB> positions;
	public List<ItemStack> items;
	public Pair<Vec3i, AxisAlignedBB> raytrace;
	private Vec3d eye, front;

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
		return new SPacketUpdateTileEntity(pos, 1337, serializeNBT());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void onLoad() {
		needSync = true;
		if (!world.isRemote) {
			new Thread(() -> {
				try {
					Thread.sleep(world.getMinecraftServer().getTickCounter() < 100 ? 4000 : 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				world.getMinecraftServer().addScheduledTask(() -> {
					if (itemList().stream().allMatch(ItemStack::isEmpty)) {
						world.setBlockToAir(pos);
						world.removeTileEntity(pos);
					}
				});
			}).start();
		}
	}

	public void change() {
		if (world == null)
			return;
		for (TileStackable t : getAllPileBlocks()) {
			if (t.inv != null)
				t.inv.items = null;
			t.changedClient = true;
			t.box = null;
			t.positions = null;
			t.items = null;
			t.raytrace = null;
			t.world.markBlockRangeForRenderUpdate(t.pos, t.pos);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		isMaster = compound.getBoolean("isMaster");
		masterPos = compound.hasKey("master") ? BlockPos.fromLong(compound.getLong("master")) : null;
		if (inv != null)
			inv.deserializeNBT(compound.getCompoundTag("inv"));
		super.readFromNBT(compound);
		if (!isMaster && inv != null)
			inv = null;
		change();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setBoolean("isMaster", isMaster);
		if (masterPos != null)
			compound.setLong("master", masterPos.toLong());
		if (inv != null)
			compound.setTag("inv", inv.serializeNBT());
		return super.writeToNBT(compound);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (isMaster && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (isMaster && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return (T) inv;
		return super.getCapability(capability, facing);
	}

	public abstract int maxVisualItems();

	public abstract List<AxisAlignedBB> uncachedItemBoxes(List<ItemStack> itemList);

	public abstract BiMap<Integer, Vec3i> getCoordMap();

	public abstract boolean validItem(ItemStack stack);

	public abstract int itemsPerVisualItem();

	public abstract SoundEvent placeSound(ItemStack stack);

	public abstract Vec3i getDimension();

	public abstract int maxPileHeight();

	public List<TileStackable> getAllPileBlocks() {
		List<TileStackable> l = new ArrayList<>();
		TileStackable master = getMaster();
		Class<?> masterClass = master.getClass();
		Validate.isTrue(masterClass == getClass());
		l.add(master);
		BlockPos p = master.pos;
		while (true) {
			p = p.up();
			if (p.equals(pos)) {
				l.add(this);
				continue;
			}
			TileEntity t = world.getTileEntity(p);
			if (t != null && t.getClass() == masterClass) {
				TileStackable tile = (TileStackable) t;
				if (!tile.isMaster && master.getPos().equals(tile.masterPos))
					l.add(tile);
				else
					break;
			} else
				break;
		}
		return l;
	}

	public int getLevel() {
		return pos.getY() - getMaster().pos.getY();
	}

	public TileStackable getMaster() {
		return isMaster ? this : masterPos == null ? null : (TileStackable) world.getTileEntity(masterPos);
	}

	public AxisAlignedBB getBox() {
		if (box != null)
			return box;
		AxisAlignedBB aabb = new AxisAlignedBB(0, 0, 0, 1 / 16., 1 / 16., 1 / 16.);
		for (AxisAlignedBB ab : itemBoxes())
			aabb = aabb.union(ab);
		return box = aabb;
	}

	/** without offset */
	public List<AxisAlignedBB> itemBoxes() {
		if (positions != null)
			return positions;
		List<ItemStack> itemList = itemList();
		if (itemList.isEmpty())
			return Collections.emptyList();
		List<AxisAlignedBB> lis = uncachedItemBoxes(itemList);
		return positions = lis;
	}

	public List<ItemStack> itemList() {
		if (items != null)
			return items;
		TileStackable master = getMaster();
		if (master == null)
			return Collections.emptyList();
		List<ItemStack> itemList = new ArrayList<>();
		for (Object2IntMap.Entry<ItemStack> e : master.inv.inventory.object2IntEntrySet()) {
			int max = Math.min(e.getKey().getMaxStackSize(), itemsPerVisualItem());
			int value = e.getIntValue();
			while (value > 0) {
				int size = Math.min(max, value);
				itemList.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), size));
				value -= size;
			}
		}
		int max = maxVisualItems() * getAllPileBlocks().size();
		while (itemList.size() > max)
			itemList.remove(itemList.size() - 1);
		while (itemList.size() < max)
			itemList.add(ItemStack.EMPTY);
		int start = getLevel() * maxVisualItems();
		return items = itemList.subList(start, start + maxVisualItems());
	}

	public ItemStack lookingStack(EntityPlayer player) {
		Vec3i v = lookingPos(player).getLeft();
		if (v == null)
			return ItemStack.EMPTY;
		return itemList().get(getCoordMap().inverse().get(v));
	}

	public Pair<Vec3i, AxisAlignedBB> lookingPos(EntityPlayer player) {
		double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
		Vec3d p1 = player.getPositionEyes(1);
		Vec3d look = player.getLook(1);
		Vec3d p2 = p1.add(look.scale(reach + 1));
		if (p1.equals(eye) && p2.equals(front) && raytrace != null)
			return raytrace;
		eye = p1;
		front = p2;
		int index = -1;
		AxisAlignedBB ab = null;
		RayTraceResult rtr = null;
		List<AxisAlignedBB> l = itemBoxes();
		for (int i = 0; i < l.size(); i++) {
			AxisAlignedBB pp = l.get(i);
			RayTraceResult rtr2 = null;
			if ((rtr2 = pp.offset(pos).calculateIntercept(p1, p2)) != null && //
					(rtr == null || rtr2.hitVec.distanceTo(p1) < rtr.hitVec.distanceTo(p1))) {
				rtr = rtr2;
				index = i;
				ab = pp;
			}
		}
		Pair<Vec3i, AxisAlignedBB> fin = null;
		if (rtr == null)
			fin = Pair.of(null, null);
		else
			fin = Pair.of(getCoordMap().get(index), ab);
		return raytrace = fin;
	}
}
