package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.property.IExtendedBlockState;

public class IngotModel implements IBakedModel {

	private static final Map<TileIngots, List<BakedQuad>> cachedQuads = new WeakHashMap<>();
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
		TileIngots tile = ((IExtendedBlockState) state).getValue(BlockIngots.prop);
		List<BakedQuad> quads = new ArrayList<>();
		if (tile != null) {
			final Item[] ar= {Items.DIAMOND_AXE,Items.DIAMOND_HOE,Items.DIAMOND_PICKAXE,Items.DIAMOND_SHOVEL,Items.DIAMOND_SWORD};
			if(true) {
				Item i=ar[new Random(tile.getPos().toLong()).nextInt(ar.length)];
				return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(new ItemStack(i)).getQuads(null, null, 0);
			}
			//cachedQuads.clear();
			if (!tile.changedClient && cachedQuads.containsKey(tile))
				return cachedQuads.get(tile);
			List<ItemStack> stacks = tile.ingotList();
			List<AxisAlignedBB> aabbs = tile.ingotBoxes();
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
