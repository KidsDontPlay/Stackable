package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

public class TileIngots extends TileEntity {

	public static final Strategy<ItemStack> strategy = new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			return o == null ? 0 : (o.getItemDamage() + "" + o.getItem().getRegistryName()).hashCode();
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a != null && b != null && a.getItemDamage() == b.getItemDamage() && a.getItem() == b.getItem();
		}
	};
	public static int maxIngotAmount = 0;
	public static BiMap<Integer, Vec3i> coordMap = null;
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

	public boolean needSync = true;
	public boolean isMaster;
	public boolean changedClient = true;
	public BlockPos masterPos;
	public IngotInventory inv = new IngotInventory(this);
	//cache
	public AxisAlignedBB box = null;
	public List<AxisAlignedBB> positions = null;
	public List<ItemStack> ingots = null;
	public Pair<Vec3i, AxisAlignedBB> raytrace;

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

	public void change() {
		if (world == null)
			return;
		for (TileIngots t : getAllIngotBlocks()) {
			if (t.inv != null)
				t.inv.items = null;
			t.changedClient = true;
			t.box = null;
			t.positions = null;
			t.ingots = null;
			t.raytrace = null;
			t.world.markBlockRangeForRenderUpdate(t.pos, t.pos);
		}
	}

	@Override
	public void onLoad() {
		if (!world.isRemote) {
			new Thread(() -> {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				world.getMinecraftServer().addScheduledTask(() -> {
					if (ingotList().stream().allMatch(ItemStack::isEmpty)) {
						world.setBlockToAir(pos);
						world.removeTileEntity(pos);
					}
				});
			}).start();
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

	public List<TileIngots> getAllIngotBlocks() {
		List<TileIngots> l = new ArrayList<>();
		TileIngots master = getMaster();
		l.add(master);
		BlockPos p = master.pos;
		while (true) {
			p = p.up();
			if (p.equals(pos)) {
				l.add(this);
				continue;
			}
			TileEntity t = world.getTileEntity(p);
			if (t instanceof TileIngots) {
				TileIngots tile = (TileIngots) t;
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

	public TileIngots getMaster() {
		return isMaster ? this : masterPos == null ? null : (TileIngots) world.getTileEntity(masterPos);
	}

	public AxisAlignedBB getBox() {
		if (box != null)
			return box;
		AxisAlignedBB aabb = new AxisAlignedBB(0, 0, 0, 1, 1 / 16., 1);
		for (AxisAlignedBB ab : ingotBoxes())
			aabb = aabb.union(ab);
		return box = aabb;
	}

	/** without offset */
	public List<AxisAlignedBB> ingotBoxes() {
		if (positions != null)
			return positions;
		List<ItemStack> ingotList = ingotList();
		if (ingotList.isEmpty())
			return Collections.emptyList();
		int count = 0;
		double xs = 1. / Stackable.perX, ys = 1. / Stackable.perY, zs = 1. / Stackable.perZ;
		Vec3d vecEven = new Vec3d(xs, ys, zs), vecUneven = new Vec3d(zs, ys, xs);
		List<AxisAlignedBB> lis = new ArrayList<>();
		mian: for (int y = 0; y < Stackable.perY; y++) {
			for (int z = 0; z < Stackable.perZ; z++) {
				for (int x = 0; x < Stackable.perX; x++) {
					ItemStack s = ingotList.get(count);
					if (s.isEmpty())
						break mian;
					boolean even = y % 2 == 0;
					Vec3d v = new Vec3d(even ? x * xs : z * zs, y * ys, even ? z * zs : x * xs);
					Vec3d vv = v.add(even ? vecEven : vecUneven);
					AxisAlignedBB a = new AxisAlignedBB(v.x, v.y, v.z, vv.x, vv.y, vv.z);
					lis.add(a);
					count++;
				}
			}
		}
		return positions = lis;
	}

	public List<ItemStack> ingotList() {
		if (ingots != null)
			return ingots;
		TileIngots master = getMaster();
		if (master == null)
			return Collections.emptyList();
		List<ItemStack> ingotList = new ArrayList<>();
		for (Object2IntMap.Entry<ItemStack> e : master.inv.inventory.object2IntEntrySet()) {
			int max = Math.min(e.getKey().getMaxStackSize(), Stackable.itemsPerIngot);
			int value = e.getIntValue();
			while (value > 0) {
				int size = Math.min(max, value);
				ingotList.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), size));
				value -= size;
			}
		}
		int max = maxIngotAmount * getAllIngotBlocks().size();
		while (ingotList.size() > max)
			ingotList.remove(ingotList.size() - 1);
		while (ingotList.size() < max)
			ingotList.add(ItemStack.EMPTY);
		int start = getLevel() * maxIngotAmount;
		return ingots = ingotList.subList(start, start + maxIngotAmount);
	}

	public ItemStack lookingStack(EntityPlayer player) {
		Vec3i v = lookingPos(player).getLeft();
		if (v == null)
			return ItemStack.EMPTY;
		return ingotList().get(TileIngots.coordMap.inverse().get(v));
	}

	private Vec3d eye, front;

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
		List<AxisAlignedBB> l = ingotBoxes();
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
			fin = Pair.of(TileIngots.coordMap.get(index), ab);
		return raytrace = fin;
	}

}
