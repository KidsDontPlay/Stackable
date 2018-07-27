package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public BlockPos masterPos;
	public final IngotInventory inv = new IngotInventory(this);
	public AxisAlignedBB box = null;
	public List<AxisAlignedBB> positions = null;
	public boolean changedClient = true;

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
	}

	public void change() {
		inv.items = null;
		changedClient = true;
		box = null;
		positions = null;
		if (world != null)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		NBTTagCompound n = compound.getCompoundTag("handler");
		n.removeTag("Size");
		isMaster = compound.getBoolean("isMaster");
		masterPos = compound.hasKey("master") ? BlockPos.fromLong(compound.getLong("master")) : null;
		inv.deserializeNBT(compound.getCompoundTag("inv"));
		super.readFromNBT(compound);
		change();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setBoolean("isMaster", isMaster);
		if (masterPos != null)
			compound.setLong("master", masterPos.toLong());
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

	public AxisAlignedBB getBox() {
		if (box != null)
			return box;
		List<ItemStack> ingotList = ingotList();
		double yy = 1d / Stackable.perY;
		int count = 0;
		boolean an = false;
		for (int i = 0; i < ingotList.size(); i++) {
			if (ingotList.get(i).isEmpty()) {
				count = i;
				an = true;
				break;
			}
		}
		if (!an)
			count = ingotList.size();
		int heigh = (int) Math.ceil((double) count / (Stackable.perX * Stackable.perZ));
		if (heigh == 0)
			heigh = 1;
		return box = new AxisAlignedBB(0, 0, 0, 1, heigh * yy, 1);
	}

	/** without offset */
	public List<AxisAlignedBB> ingotBoxes() {
		if (positions != null)
			return positions;
		List<ItemStack> ingotList = ingotList();
		int count = 0;
		double xs = 1. / Stackable.perX, ys = 1. / Stackable.perY, zs = 1. / Stackable.perZ;
		Vec3d vecSize = new Vec3d(xs, ys, zs);
		Vec3d vecSizeI = new Vec3d(zs, ys, xs);
		List<AxisAlignedBB> lis = new ArrayList<>();
		for (int y = 0; y < Stackable.perY; y++) {
			for (int z = 0; z < Stackable.perZ; z++) {
				for (int x = 0; x < Stackable.perX; x++) {
					ItemStack s = ingotList.get(count);
					if (s.isEmpty())
						break;
					boolean uneven = y % 2 == 0;
					Vec3d v = new Vec3d(uneven ? x * xs : z * zs, y * ys, uneven ? z * zs : x * xs);
					Vec3d vv = v.add(uneven ? vecSize : vecSizeI);
					AxisAlignedBB a = new AxisAlignedBB(v.x, v.y, v.z, vv.x, vv.y, vv.z);
					lis.add(a);
					count++;
				}
			}
		}
		return positions = lis;
	}

	public List<ItemStack> ingotList() {
		List<ItemStack> ingotList = new ArrayList<>();
		for (Object2IntMap.Entry<ItemStack> e : inv.inventory.object2IntEntrySet()) {
			int max = Math.min(e.getKey().getMaxStackSize(), Stackable.itemsPerIngot);
			int value = e.getIntValue();
			while (value > 0) {
				if (value > max) {
					ingotList.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), max));
					value -= max;
				} else {
					ingotList.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), value));
					break;
				}
			}
		}
		while (ingotList.size() > maxIngotAmount)
			ingotList.remove(ingotList.size() - 1);
		while (ingotList.size() < maxIngotAmount)
			ingotList.add(ItemStack.EMPTY);
		return ingotList;

	}

	public ItemStack lookingStack(EntityPlayer player) {
		Vec3i v = lookingPos(player).getLeft();
		if (v == null)
			return ItemStack.EMPTY;
		return ingotList().get(TileIngots.coordMap.inverse().get(v));
	}

	public Pair<Vec3i, AxisAlignedBB> lookingPos(EntityPlayer player) {
		double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
		Vec3d p1 = player.getPositionEyes(1);
		Vec3d look = player.getLook(1);
		Vec3d p2 = p1.add(look.scale(reach));
		HashMap<AxisAlignedBB, Pair<Integer, RayTraceResult>> hitMap = new HashMap<>();
		List<AxisAlignedBB> l = ingotBoxes();
		for (int i = 0; i < l.size(); i++) {
			AxisAlignedBB pp = l.get(i);
			AxisAlignedBB aabb = pp.offset(pos);
			RayTraceResult rtr2 = null;
			if ((rtr2 = aabb.calculateIntercept(p1, p2)) != null) {
				hitMap.put(pp, Pair.of(i, rtr2));
			}
		}
		if (hitMap.isEmpty())
			return Pair.of(null, null);
		AxisAlignedBB fin = null;
		RayTraceResult r1 = null;
		for (Map.Entry<AxisAlignedBB, Pair<Integer, RayTraceResult>> e : hitMap.entrySet()) {
			AxisAlignedBB pp = e.getKey();
			if (fin == null) {
				fin = pp;
				r1 = hitMap.get(pp).getRight();
				continue;
			}
			RayTraceResult r2 = e.getValue().getRight();
			Vec3d v1 = e.getValue().getRight().hitVec, v2 = r1.hitVec;
			if (v1.distanceTo(p1) < v2.distanceTo(p1)) {
				fin = pp;
				r1 = r2;
			}
		}
		//		AxisAlignedBB aabb = new AxisAlignedBB(fin.getLeft().x, fin.getLeft().y, fin.getLeft().z, fin.getRight().x, fin.getRight().y, fin.getRight().z);
		return Pair.of(TileIngots.coordMap.get(hitMap.get(fin).getLeft()), fin);
	}

}
