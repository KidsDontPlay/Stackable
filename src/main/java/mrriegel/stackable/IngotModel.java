package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
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

public class IngotModel implements IBakedModel {

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
	private final Map<TileIngots, List<BakedQuad>> cachedQuads = new WeakHashMap<>();

	@Override
	public boolean isGui3d() {
		return false;
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
		if (side != null)
			return Collections.emptyList();
		StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
		if ("getDamageModel".equals(ste.getMethodName())) {
			return brokenQuads;
		}
		if (!true) {
			IBakedModel ironModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(Blocks.IRON_ORE.getDefaultState());
			List<BakedQuad> iron = Arrays.stream(EnumFacing.VALUES).flatMap(f -> ironModel.getQuads(Blocks.IRON_ORE.getDefaultState(), f, 0).stream()).collect(Collectors.toList());
//			iron=Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("immersiveengineering", "chemthrower")))).getQuads(null, null, 0);
			final int size = 4;
			float f = 1f / size;
			float ff=f*.9f;
			List<BakedQuad> quads = new ArrayList<>();
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					for (int x = 0; x < size; x++) {
						for (BakedQuad q : iron) {
							BakedQuad b = q;
							b = ClientUtils.scale(b, ff, ff, ff);
							b = ClientUtils.translate(b, x * f, y * f, z * f);
//							b=ClientUtils.rotate(b, 2, 0, 1, 0);
							quads.add(b);
						}
					}
				}
			}
			return quads;
		}
		TileIngots tile = (TileIngots) ((IExtendedBlockState) state).getValue(BlockIngots.TILE_PROP);
		List<BakedQuad> quads = new ArrayList<>();
		if (tile != null) {
			//cachedQuads.clear();
			if (!tile.changedClient && cachedQuads.containsKey(tile))
				return cachedQuads.get(tile);
			List<ItemStack> stacks = tile.itemList();
			List<AxisAlignedBB> aabbs = tile.itemBoxes();
			int size = Math.min(stacks.size(), aabbs.size());
			for (int i = 0; i < size; i++) {
				ItemStack s = stacks.get(i);
				ClientUtils.createIngot(quads, s, aabbs.get(i), Stackable.useBlockTexture ? ClientUtils.sprite(s) : null);
			}
			tile.changedClient = false;
			cachedQuads.put(tile, quads);
		} else
			quads.addAll(fallBack);
		return quads;
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
