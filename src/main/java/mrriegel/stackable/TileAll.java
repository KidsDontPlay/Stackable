package mrriegel.stackable;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;

import net.minecraft.block.SoundType;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class TileAll extends TileStackable {
	public static int maxItemAmount = 0;
	public static BiMap<Integer, Vec3i> coordMap = null;

	@Override
	public int maxVisualItems() {
		return maxItemAmount;
	}

	@Override
	public List<AxisAlignedBB> uncachedItemBoxes(List<ItemStack> itemList) {
		List<AxisAlignedBB> lis = new ArrayList<>();
		int count = 0;
		double d = 1. / Stackable.allSize;
		Vec3d vec = new Vec3d(d, d, d);
		mian: for (int y = 0; y < Stackable.allSize; y++) {
			for (int z = 0; z < Stackable.allSize; z++) {
				for (int x = 0; x < Stackable.allSize; x++) {
					ItemStack s = itemList.get(count);
					if (s.isEmpty())
						break mian;
					Vec3d v = new Vec3d(x * d, y * d, z * d);
					Vec3d vv = v.add(vec);
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
		return !stack.isEmpty();
	}

	@Override
	public int itemsPerVisualItem() {
		return 1;
	}

	@Override
	public SoundEvent placeSound(ItemStack stack) {
		if (stack.getItem() instanceof ItemBlock) {
			SoundType se = ((ItemBlock) stack.getItem()).getBlock().getSoundType();
			if (se != null && se.getPlaceSound() != null)
				return se.getPlaceSound();
		}
		return SoundEvents.BLOCK_CLOTH_PLACE;
	}

}
