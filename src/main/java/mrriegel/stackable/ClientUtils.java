package mrriegel.stackable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.vecmath.Point2f;

import org.lwjgl.util.vector.Vector3f;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.IItemHandler;

@EventBusSubscriber(modid = Stackable.MODID, value = { Side.CLIENT })
public class ClientUtils {

	private static final Object2IntMap<ItemStack> colors = new Object2IntOpenCustomHashMap<>(new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			return o == null ? 0 : (o.getItemDamage() + "" + o.getItem().getRegistryName()).hashCode();
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == null || b == null ? false : a.getItemDamage() == b.getItemDamage() && a.getItem() == b.getItem();
		}
	});
	private static final Map<TileIngots, List<BakedQuad>> cachedQuads = new WeakHashMap<>();

	public static int color(ItemStack s) {
		if (colors.containsKey(s))
			return colors.getInt(s);
		Minecraft mc = Minecraft.getMinecraft();
		if (s.isEmpty())
			return 0xffffff;
		TextureAtlasSprite tas = mc.getRenderItem().getItemModelMesher().getItemModel(s).getParticleTexture();
		if (tas == mc.getTextureMapBlocks().getMissingSprite() || tas.getIconHeight() <= 0 || tas.getIconWidth() <= 0 || tas.getFrameCount() <= 0)
			return 0xffffff;
		BufferedImage img = new BufferedImage(tas.getIconWidth(), tas.getIconHeight() * tas.getFrameCount(), BufferedImage.TYPE_4BYTE_ABGR);
		for (int i = 0; i < tas.getFrameCount(); i++) {
			int[][] frameTextureData = tas.getFrameTextureData(i);
			int[] largestMipMapTextureData = frameTextureData[0];
			img.setRGB(0, i * tas.getIconHeight(), tas.getIconWidth(), tas.getIconHeight(), largestMipMapTextureData, 0, tas.getIconWidth());
		}
		int red = 0, green = 0, blue = 0, count = 0;
		for (int x = 0; x < img.getWidth(); x++)
			for (int y = 0; y < img.getHeight(); y++) {
				int rgb = img.getRGB(x, y);
				Color col = new Color(rgb, true);
				if (col.getAlpha() == 255) {
					red += col.getRed();
					green += col.getGreen();
					blue += col.getBlue();
					count++;
				}
			}
		int c = new Color((red / count), (green / count), (blue / count)).getRGB();
		//		c = ColorHelper.brighter(c, .15);
		colors.put(s, c);
		return c;
	}

	public static void init() {
		if (false)
			ClientRegistry.bindTileEntitySpecialRenderer(TileIngots.class, new TileEntitySpecialRenderer<TileIngots>() {

				ModelRenderer mr = null;
				{
					ModelBase b = new ModelBase() {
					};
					b.textureHeight = b.textureWidth = 64;
					mr = new ModelRenderer(b, 0, 0);
					mr.setTextureOffset(12, 12);
					//					mr.setRotationPoint(-8, 8, -8);
					mr.addBox(0, 0, 0, 16, 16, 16, 0);
				}

				@Override
				public void render(TileIngots te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
					if (destroyStage >= 0 && false) {
						this.bindTexture(DESTROY_STAGES[destroyStage]);
						//					this.bindTexture(new ResourceLocation("textures/blocks/gold_block.png"));
						GlStateManager.matrixMode(5890);
						GlStateManager.pushMatrix();
						GlStateManager.scale(4.0F, 4.0F, 1.0F);
						GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
						GlStateManager.matrixMode(5888);
						GlStateManager.pushMatrix();
						GlStateManager.enableRescaleNormal();
						GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
						GlStateManager.translate((float) x, (float) y + 1.0F, (float) z + 1.0F);
						GlStateManager.scale(1.0F, -1.0F, -1.0F);

						mr.render(.0625f);
						GlStateManager.disableRescaleNormal();
						GlStateManager.popMatrix();
						GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

						GlStateManager.matrixMode(5890);
						GlStateManager.popMatrix();
						GlStateManager.matrixMode(5888);
					}

				}
			});
	}

	@SubscribeEvent
	public static void bake(ModelBakeEvent event) {
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.ingots.getRegistryName().toString()), new IBakedModel() {
			TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/glass");
			List<BakedQuad> fullCube = null;

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
				StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
				if ("getDamageModel".equals(ste.getMethodName())) {
					if (fullCube != null && false)
						return fullCube;
					IBlockState cobble = Blocks.COBBLESTONE.getDefaultState();
					IBakedModel m = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(cobble);
					return fullCube = new SimpleBakedModel.Builder(cobble, m, ModelLoader.White.INSTANCE, BlockPos.ORIGIN).makeBakedModel().getQuads(cobble, side, rand);
				}
				TileIngots tile = ((IExtendedBlockState) state).getValue(BlockIngots.prop);
				List<BakedQuad> quads = new ArrayList<>();
				if (tile != null) {
					cachedQuads.clear();
					if (!tile.changed && cachedQuads.containsKey(tile))
						return cachedQuads.get(tile);
					IItemHandler handler = tile.handler;
					int count = 0;
					for (int y = 0; y < Stackable.perY; y++) {
						for (int z = 0; z < Stackable.perZ; z++) {
							for (int x = 0; x < Stackable.perX; x++) {
								ItemStack s = handler.getStackInSlot(count);
								if (!s.isEmpty())
									createIngot(quads, s, x, y, z);
								count++;
							}
						}
					}
					tile.changed = false;
				}
				cachedQuads.put(tile, quads);
				return quads;
			}

			private void createIngot(List<BakedQuad> quads, ItemStack stack, int x, int y, int z) {
				int color = color(stack);
				Color col = new Color(color);
				float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
				col = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + .15f));
				float r = col.getRed() / 255f, g = col.getGreen() / 255f, b = col.getBlue() / 255f;
				float xs = 1f / Stackable.perX, ys = 1f / Stackable.perY, zs = 1f / Stackable.perZ;
				if (y % 2 != 0) {
					int k = x;
					x = z;
					z = k;
					float ks = xs;
					xs = zs;
					zs = ks;
				}
				float diffX = xs * .1f, diffZ = zs * .1f;
				//				diffX = diffZ = .02f;
				if (diffX > diffZ)
					diffX = diffZ;
				else
					diffZ = diffX;
				Vector3f va = new Vector3f(0 + (diffX * .2f) + xs * x, 0 + ys * y, 0 + (diffZ * .2f) + zs * z), //
						vb = new Vector3f(0 + (diffX * .2f) + xs * x, 0 + ys * y, zs - (diffZ * .2f) + zs * z), //
						vc = new Vector3f(0 + diffX + xs * x, ys + ys * y, zs - diffZ + zs * z), //
						vd = new Vector3f(0 + diffX + xs * x, ys + ys * y, 0 + diffZ + zs * z), //
						ve = new Vector3f(xs - (diffX * .2f) + xs * x, 0 + ys * y, 0 + (diffZ * .2f) + zs * z), //
						vf = new Vector3f(xs - (diffX * .2f) + xs * x, 0 + ys * y, zs - (diffZ * .2f) + zs * z), //
						vg = new Vector3f(xs - diffX + xs * x, ys + ys * y, zs - diffZ + zs * z), //
						vh = new Vector3f(xs - diffX + xs * x, ys + ys * y, 0 + diffZ + zs * z);
				//bottom
				quads.add(createQuad(DefaultVertexFormats.ITEM, va, ve, vf, vb, r, g, b));
				//top
				quads.add(createQuad(DefaultVertexFormats.ITEM, vh, vd, vc, vg, r, g, b));
				//sides
				quads.add(createQuad(DefaultVertexFormats.ITEM, vh, ve, va, vd, r, g, b));
				quads.add(createQuad(DefaultVertexFormats.ITEM, vd, va, vb, vc, r, g, b));
				quads.add(createQuad(DefaultVertexFormats.ITEM, vc, vb, vf, vg, r, g, b));
				quads.add(createQuad(DefaultVertexFormats.ITEM, vg, vf, ve, vh, r, g, b));
			}

			private BakedQuad createQuad(VertexFormat format, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, float r, float g, float b) {
				return createQuad(format, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z, v4.x, v4.y, v4.z, r, g, b);
			}

			private BakedQuad createQuad(VertexFormat format, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b) {
				UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
				TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/snow");
				//		tex = ModelLoader.White.INSTANCE;
				tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
				//		tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/tnt_side");
				builder.setTexture(tex);
				Vec3d normal = new Vec3d(0, 0, 0);
				float diff1 = (float) new Vec3d(x1, y1, z1).distanceTo(new Vec3d(x2, y2, z2));
				float diff2 = (float) new Vec3d(x2, y2, z2).distanceTo(new Vec3d(x3, y3, z3));
				float diff3 = (float) new Vec3d(x3, y3, z3).distanceTo(new Vec3d(x4, y4, z4));
				diff1 = diff2 = diff3 = 1f;
				Point2f p1 = new Point2f(0, 0), p2 = new Point2f(0 * diff1, 16 * diff1), p3 = new Point2f(16 * diff2, 16 * diff2), p4 = new Point2f(16 * diff3, 0 * diff3);
				putVertex(format, tex, builder, normal, x1, y1, z1, p1.x, p1.y, r, g, b);
				putVertex(format, tex, builder, normal, x2, y2, z2, p2.x, p2.y, r, g, b);
				putVertex(format, tex, builder, normal, x3, y3, z3, p3.x, p3.y, r, g, b);
				putVertex(format, tex, builder, normal, x4, y4, z4, p4.x, p4.y, r, g, b);
				return builder.build();
			}

			private void putVertex(VertexFormat format, TextureAtlasSprite sprite, UnpackedBakedQuad.Builder builder, Vec3d normal, float x, float y, float z, float u, float v, float r, float g, float b) {
				for (int e = 0; e < format.getElementCount(); e++) {
					switch (format.getElement(e).getUsage()) {
					case POSITION:
						builder.put(e, x, y, z, 1.0f);
						break;
					case COLOR:
						builder.put(e, r, g, b);
						break;
					case UV:
						if (format.getElement(e).getIndex() == 0) {
							u = sprite.getInterpolatedU(u);
							v = sprite.getInterpolatedV(v);
							builder.put(e, u, v, 0f, 1f);
							break;
						}
					case NORMAL:
						builder.put(e, (float) normal.x, (float) normal.y, (float) normal.z, 0f);
						break;
					default:
						builder.put(e);
						break;
					}
				}
			}

			@Override
			public TextureAtlasSprite getParticleTexture() {
				return tex;
			}

			@Override
			public ItemOverrideList getOverrides() {
				return ItemOverrideList.NONE;
			}

		});
	}

}
