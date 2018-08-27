package mrriegel.stackable.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import mrriegel.stackable.Stackable;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.oredict.OreDictionary;

public class TileIngotPile extends TilePile {

	public static int maxIngotAmount = 0;
	public static BiMap<Integer, Vec3i> coordMap = null;
	private static Object2BooleanOpenCustomHashMap<ItemStack> validCache = new Object2BooleanOpenCustomHashMap<>(strategy);

	public static boolean validItem1(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		if (validCache.containsKey(stack))
			return validCache.getBoolean(stack);
		boolean res = Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).anyMatch(s -> s.startsWith("ingot")) || Stackable.allowedIngots.contains(stack.getItem().getRegistryName());
		validCache.put(stack, res);
		return res;
	}

	@Override
	public int maxVisualItems() {
		return maxIngotAmount;
	}

	@Override
	public List<AxisAlignedBB> uncachedItemBoxes(List<ItemStack> itemList) {
		List<AxisAlignedBB> lis = new ArrayList<>();
		int count = 0;
		double xs = 1. / Stackable.sizeX, ys = 1. / Stackable.sizeY, zs = 1. / Stackable.sizeZ;
		Vec3d vecEven = new Vec3d(xs, ys, zs), vecUneven = new Vec3d(zs, ys, xs);
		mian: for (int y = 0; y < Stackable.sizeY; y++) {
			for (int z = 0; z < Stackable.sizeZ; z++) {
				for (int x = 0; x < Stackable.sizeX; x++) {
					ItemStack s = itemList.get(count);
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
		return lis;
	}

	@Override
	public BiMap<Integer, Vec3i> getCoordMap() {
		return coordMap;
	}

	@Override
	public boolean validItem(ItemStack stack) {
		return validItem1(stack);
	}

	@Override
	public int itemsPerVisualItem() {
		return Stackable.itemsPerItemI;
	}

	@Override
	public SoundEvent placeSound(ItemStack stack) {
		return SoundEvents.BLOCK_METAL_PLACE;
	}

	@Override
	public int maxPileHeight() {
		return Stackable.maxPileHeightI;
	}

}
