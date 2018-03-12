package mrriegel.stackable;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

public class BakedModel implements IBakedModel {
	//	private static final Map<TileIngots, List<BakedQuad>> cachedQuads = new WeakHashMap<>();
	TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/bone_block_side");

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		List<BakedQuad> quads = new ArrayList<>();
		//		TileIngots tile = ((IExtendedBlockState) state).getValue(BlockIngots.prop);
		//		if (tile != null) {
		//			cachedQuads.clear();
		//			if (!tile.changed && cachedQuads.containsKey(tile))
		//				return cachedQuads.get(tile);
		//			IItemHandler handler = tile.handler;
		//			int count = 0;
		//			for (int y = 0; y < perY; y++) {
		//				for (int z = 0; z < perZ; z++) {
		//					for (int x = 0; x < perX; x++) {
		//						ItemStack s = handler.getStackInSlot(count);
		//						if (!s.isEmpty())
		//							createIngot(quads, s, x, y, z);
		//						count++;
		//					}
		//				}
		//			}
		//		}
		//		tile.changed = false;
		//		cachedQuads.put(tile, quads);
		return quads;
	}

	//	private void createIngot(List<BakedQuad> quads, ItemStack stack, int x, int y, int z) {
	//		int color = color(stack);
	//		float r = ColorHelper.getRed(color) / 255f, g = ColorHelper.getGreen(color) / 255f, b = ColorHelper.getBlue(color) / 255f;
	//		float xs = 1f / perX, ys = 1f / perY, zs = 1f / perZ;
	//		if (y % 2 != 0) {
	//			int k = x;
	//			x = z;
	//			z = k;
	//			float ks = xs;
	//			xs = zs;
	//			zs = ks;
	//		}
	//		float diffX = xs * .1f, diffZ = zs * .1f;
	//		Vector3f va = new Vector3f(0 + (diffX * .2f) + xs * x, 0 + ys * y, 0 + (diffZ * .2f) + zs * z), //
	//				vb = new Vector3f(0 + (diffX * .2f) + xs * x, 0 + ys * y, zs - (diffZ * .2f) + zs * z), //
	//				vc = new Vector3f(0 + diffX + xs * x, ys + ys * y, zs - diffZ + zs * z), //
	//				vd = new Vector3f(0 + diffX + xs * x, ys + ys * y, 0 + diffZ + zs * z), //
	//				ve = new Vector3f(xs - (diffX * .2f) + xs * x, 0 + ys * y, 0 + (diffZ * .2f) + zs * z), //
	//				vf = new Vector3f(xs - (diffX * .2f) + xs * x, 0 + ys * y, zs - (diffZ * .2f) + zs * z), //
	//				vg = new Vector3f(xs - diffX + xs * x, ys + ys * y, zs - diffZ + zs * z), //
	//				vh = new Vector3f(xs - diffX + xs * x, ys + ys * y, 0 + diffZ + zs * z);
	//		//bottom
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, va, ve, vf, vb, r, g, b));
	//		//top
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, vh, vd, vc, vg, r, g, b));
	//		//sides
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, vh, ve, va, vd, r, g, b));
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, vd, va, vb, vc, r, g, b));
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, vc, vb, vf, vg, r, g, b));
	//		quads.add(createQuad(DefaultVertexFormats.ITEM, vg, vf, ve, vh, r, g, b));
	//	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return tex;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}
}
