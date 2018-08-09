package mrriegel.stackable.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TileAnyPile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TESRAnyPile extends TileEntitySpecialRenderer<TileAnyPile> {

	@Override
	public void render(TileAnyPile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		List<ItemStack> stacks = te.itemList();
		List<AxisAlignedBB> aabbs = te.itemBoxes();
		int size = Math.min(stacks.size(), aabbs.size());
		float f = 1f / Stackable.size;
		float ff = f * .9f;
		float add = f - ff;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		for (int i = 0; i < size; i++) {
			ItemStack s = stacks.get(i);
			AxisAlignedBB aabb = aabbs.get(i);
			if (s.getItem() instanceof ItemBlock) {
				TileEntity t = getTile(((ItemBlock) s.getItem()).getBlock().getStateFromMeta(s.getMetadata()), te.getWorld());
				if (t != null) {
					TileEntitySpecialRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(t.getClass());
					if (tesr != null && !invalids.contains(tesr)) {
						GlStateManager.scale(ff, ff, ff);
						int stackDepthMatrix = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
						int stackDepthAttrib = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
						try {
							tesr.render(t, (aabb.minX + add / 2) / ff, (aabb.minY + add / 2) / ff, (aabb.minZ + add / 2) / ff, partialTicks, destroyStage, alpha);
						} catch (Exception e) {
							e.printStackTrace();
							invalids.add(tesr);
							int newStackDepthMatrix = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
							for (int j = 0; j < newStackDepthMatrix - stackDepthMatrix; j++)
								GlStateManager.popMatrix();
							int newStackDepthAttrib = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
							for (int j = 0; j < newStackDepthAttrib - stackDepthAttrib; j++)
								GlStateManager.popAttrib();
						}
						GlStateManager.scale(1 / ff, 1 / ff, 1 / ff);
					}
				}
			}
		}
		GlStateManager.popMatrix();
	}

	private Map<IBlockState, TileEntity> cache = new IdentityHashMap<>();
	private Set<TileEntitySpecialRenderer<TileEntity>> invalids = Collections.newSetFromMap(new IdentityHashMap<>());

	private TileEntity getTile(IBlockState state, World world) {
		if (!state.getBlock().hasTileEntity(state) || state.getBlock().getRenderType(state) != EnumBlockRenderType.ENTITYBLOCK_ANIMATED)
			return null;
		TileEntity t = cache.get(state);
		if (t == null) {
			t = state.getBlock().createTileEntity(world, state);
			t.setPos(BlockPos.ORIGIN);
			cache.put(state, t);
		}
		t.setWorld(world);
		return t;

	}

}
