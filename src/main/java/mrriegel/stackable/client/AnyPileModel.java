package mrriegel.stackable.client;

import java.util.List;

import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;

public class AnyPileModel extends PileModel {

	@Override
	protected void addQuads(List<BakedQuad> quads, TilePile tile) {
		List<ItemStack> stacks = tile.itemList();
		List<AxisAlignedBB> aabbs = tile.itemBoxes();
		int size = Math.min(stacks.size(), aabbs.size());
		float f = 1f / Stackable.size;
		float ff = f * .9f;
		float add = f - ff;
		for (int i = 0; i < size; i++) {
			ItemStack s = stacks.get(i);
			List<BakedQuad> m = ClientUtils.getBakedQuads(s);
			AxisAlignedBB aabb = aabbs.get(i);
			for (BakedQuad q : m) {
				q = ClientUtils.scale(q, ff, ff, ff);
				q = ClientUtils.translate(q, (float) aabb.minX + add / 2, (float) aabb.minY + add / 2, (float) aabb.minZ + add / 2);
				quads.add(q);
			}
		}
	}

}
