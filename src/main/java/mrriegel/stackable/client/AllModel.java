package mrriegel.stackable.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import mrriegel.stackable.Stackable;
import mrriegel.stackable.block.BlockIngots;
import mrriegel.stackable.block.BlockStackable;
import mrriegel.stackable.tile.TileAll;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.property.IExtendedBlockState;

public class AllModel implements IBakedModel {

	private final Map<TileAll, List<BakedQuad>> cachedQuads = new WeakHashMap<>();

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (side != null)
			return Collections.emptyList();
		StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
		if ("getDamageModel".equals(ste.getMethodName())) {
			return IngotModel.brokenQuads;
		}
		TileAll tile = (TileAll) ((IExtendedBlockState) state).getValue(BlockStackable.TILE_PROP);
		List<BakedQuad> quads = new ArrayList<>();
		if (tile != null) {
			//cachedQuads.clear();
			if (!tile.changedClient && cachedQuads.containsKey(tile))
				return cachedQuads.get(tile);
			List<ItemStack> stacks = tile.itemList();
			List<AxisAlignedBB> aabbs = tile.itemBoxes();
			int size = Math.min(stacks.size(), aabbs.size());
			float f = 1f / Stackable.allSize;
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
			tile.changedClient = false;
			cachedQuads.put(tile, quads);
		} else
			quads.addAll(IngotModel.fallBack);
		return quads;
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(Blocks.PLANKS.getDefaultState()).getParticleTexture();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

}
