package mrriegel.stackable.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import mrriegel.stackable.block.BlockPile;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;

public abstract class PileModel implements IBakedModel {

	private static List<BakedQuad> brokenQuads, fallBack;

	public static void init() {
		IBlockState cobble = Blocks.COBBLESTONE.getDefaultState();
		IBakedModel m = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(cobble);
		brokenQuads = Arrays.stream(EnumFacing.VALUES).flatMap(f -> m.getQuads(cobble, f, 0).stream()).collect(Collectors.toList());
		fallBack = new ArrayList<>();
		Block[] blocks = new Block[] { Blocks.PURPUR_BLOCK, Blocks.BEDROCK, Blocks.CLAY, Blocks.PACKED_ICE, Blocks.MOSSY_COBBLESTONE, Blocks.LAPIS_BLOCK };
		for (int i = 0; i < 6; i++) {
			IBlockState ss = blocks[i].getDefaultState();
			IBakedModel mm = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(ss);
			List<BakedQuad> qs = mm.getQuads(ss, EnumFacing.VALUES[i], 0);
			if (!qs.isEmpty())
				fallBack.add(qs.get(0));
		}
	}

	private final Map<TilePile, List<BakedQuad>> cachedQuads = new WeakHashMap<>();

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (side != null)
			return Collections.emptyList();
		StackTraceElement ste = new Exception().getStackTrace()[3];
		if ("getDamageModel".equals(ste.getMethodName())) {
			return brokenQuads;
		}
		TilePile tile = (TilePile) ((IExtendedBlockState) state).getValue(BlockPile.TILE_PROP);
		List<BakedQuad> quads = new ArrayList<>();
		if (tile != null) {
			//cachedQuads.clear();
			if (!tile.changedClient && cachedQuads.containsKey(tile))
				return cachedQuads.get(tile);
			addQuads(quads, tile);
			tile.changedClient = false;
			cachedQuads.put(tile, quads);
		} else
			quads.addAll(fallBack);
		return quads;
	}

	protected abstract void addQuads(List<BakedQuad> quads, TilePile tile);

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
		return ClientUtils.defaultTas;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

}
