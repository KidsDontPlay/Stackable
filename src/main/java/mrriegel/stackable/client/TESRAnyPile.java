package mrriegel.stackable.client;

import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TileAnyPile;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;

public class TESRAnyPile extends TileEntitySpecialRenderer<TileAnyPile> {

	@Override
	public void render(TileAnyPile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		List<ItemStack> stacks = te.itemList();
		List<AxisAlignedBB> aabbs = te.itemBoxes();
		int size = Math.min(stacks.size(), aabbs.size());
		float f = 1f / Stackable.size;
		float ff = f * Stackable.scaleA;
		float add = f - ff;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		for (int i = 0; i < size; i++) {
			ItemStack s = stacks.get(i);
			IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(s);
			if (!model.isBuiltInRenderer() || invalids.contains(s))
				continue;
			AxisAlignedBB aabb = aabbs.get(i);
			GlStateManager.scale(ff, ff, ff);
			GlStateManager.translate((aabb.minX + add / 2) / ff, (aabb.minY + add / 2) / ff, (aabb.minZ + add / 2) / ff);
			GlStateManager.translate(0.5F, 0.5F, 0.5F);
			int stackDepthMatrix = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
			int stackDepthAttrib = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
			try {
				Minecraft.getMinecraft().getRenderItem().renderItem(s, model);
			} catch (Exception e) {
				e.printStackTrace();
				invalids.add(s);
				int newStackDepthMatrix = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
				for (int j = 0; j < newStackDepthMatrix - stackDepthMatrix; j++)
					GlStateManager.popMatrix();
				int newStackDepthAttrib = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
				for (int j = 0; j < newStackDepthAttrib - stackDepthAttrib; j++)
					GlStateManager.popAttrib();
			}
			GlStateManager.translate(-0.5F, -0.5F, -0.5F);
			GlStateManager.translate(-(aabb.minX + add / 2) / ff, -(aabb.minY + add / 2) / ff, -(aabb.minZ + add / 2) / ff);
			GlStateManager.scale(1 / ff, 1 / ff, 1 / ff);
		}
		GlStateManager.popMatrix();
	}

	private Set<ItemStack> invalids = new ObjectOpenCustomHashSet<>(TilePile.strategyFuzzy);
}
