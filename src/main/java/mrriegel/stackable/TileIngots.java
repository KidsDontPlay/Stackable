package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
	public List<Pair<Vec3d, Vec3d>> positions = null;
	public boolean changedClient = true;
	public IngotObject io = new IngotObject(this);

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
		inv.items = null;
		//		handler.items=null;
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
		//		handler.back.deserializeNBT(n);
		isMaster = compound.getBoolean("isMaster");
		masterPos = compound.hasKey("master") ? BlockPos.fromLong(compound.getLong("master")) : null;
		inv.deserializeNBT(compound.getCompoundTag("inv"));
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		//		compound.setTag("handler", handler.back.serializeNBT());
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
			//			return (T) handler;
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

	public List<Pair<Vec3d, Vec3d>> ingotPositions() {
		if (positions != null)
			return positions;
		List<ItemStack> ingotList = ingotList();
		int count = 0;
		double xs = 1. / Stackable.perX, ys = 1. / Stackable.perY, zs = 1. / Stackable.perZ;
		Vec3d vecSize = new Vec3d(xs, ys, zs);
		Vec3d vecSizeI = new Vec3d(zs, ys, xs);
		List<Pair<Vec3d, Vec3d>> lis = new ArrayList<>();
		for (int y = 0; y < Stackable.perY; y++) {
			for (int z = 0; z < Stackable.perZ; z++) {
				for (int x = 0; x < Stackable.perX; x++) {
					ItemStack s = ingotList.get(count);
					if (s.isEmpty())
						break;
					boolean uneven = y % 2 == 0;
					Vec3d v = new Vec3d(uneven ? x * xs : z * zs, y * ys, uneven ? z * zs : x * xs);
					Pair<Vec3d, Vec3d> p = Pair.of(v, v.add(uneven ? vecSize : vecSizeI));
					lis.add(p);
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

}
