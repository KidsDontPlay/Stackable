package mrriegel.stackable.client;

import java.util.List;

import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TileStackable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;

public class IngotPileModel extends PileModel {

	@Override
	protected void addQuads(List<BakedQuad> quads, TileStackable tile) {
		List<ItemStack> stacks = tile.itemList();
		List<AxisAlignedBB> aabbs = tile.itemBoxes();
		int size = Math.min(stacks.size(), aabbs.size());
		for (int i = 0; i < size; i++) {
			ItemStack s = stacks.get(i);
			ClientUtils.createIngot(quads, s, aabbs.get(i), Stackable.useBlockTexture ? ClientUtils.sprite(s) : null);
		}
	}

}
