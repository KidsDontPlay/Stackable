package mrriegel.stackable.tile;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;

import mrriegel.stackable.Stackable;
import net.minecraft.block.SoundType;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class TileAnyPile extends TileStackable {
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
		double d = 1. / Stackable.size;
		Vec3d vec = new Vec3d(d, d, d);
		mian: for (int y = 0; y < Stackable.size; y++) {
			for (int z = 0; z < Stackable.size; z++) {
				for (int x = 0; x < Stackable.size; x++) {
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
		return Stackable.itemsPerItemA;
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

	@Override
	public int maxPileHeight() {
		return Stackable.maxPileHeightA;
	}

}
