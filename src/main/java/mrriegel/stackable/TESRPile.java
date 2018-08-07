package mrriegel.stackable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TESRPile extends TileEntitySpecialRenderer<TileAll> {

	@Override
	public void render(TileAll te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		List<ItemStack> stacks = te.itemList();
		List<AxisAlignedBB> aabbs = te.itemBoxes();
		int size = Math.min(stacks.size(), aabbs.size());
		float f = 1f / Stackable.allSize;
		float ff = f * .9f;
		float add = f - ff;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		for (int i = 0; i < size; i++) {
			ItemStack s = stacks.get(i);
			AxisAlignedBB aabb = aabbs.get(i);
			if (s.getItem() instanceof ItemBlock) {
				TileEntity t = getTile(((ItemBlock) s.getItem()).getBlock().getDefaultState(), te.getWorld());
				if (t != null) {
					TileEntitySpecialRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(t.getClass());
					if (tesr != null) {
						GlStateManager.scale(ff, ff, ff);
						tesr.render(t, (aabb.minX + add / 2) / ff, (aabb.minY + add / 2) / ff, (aabb.minZ + add / 2) / ff, partialTicks, destroyStage, alpha);
						GlStateManager.scale(1 / ff, 1 / ff, 1 / ff);
					}
				}
			}
		}
		GlStateManager.popMatrix();
	}

	private Map<IBlockState, TileEntity> cache = new IdentityHashMap<>();

	private TileEntity getTile(IBlockState state, World world) {
		if (!state.getBlock().hasTileEntity(state))
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
