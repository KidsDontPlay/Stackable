package mrriegel.stackable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.vecmath.Point2f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = Stackable.MODID, value = Side.CLIENT)
public class ClientUtils {

	private static final Object2IntMap<ItemStack> cachedColors = new Object2IntOpenCustomHashMap<>(TileIngots.strategy);
	private static final Object2ObjectMap<ItemStack, TextureAtlasSprite> cachedSprites = new Object2ObjectOpenCustomHashMap<>(TileIngots.strategy);
	private static final Map<TileIngots, List<BakedQuad>> cachedQuads = new WeakHashMap<>();
	private static Minecraft mc;
	private static TextureAtlasSprite defaultTas;

	public static int color(ItemStack stack) {
		if (cachedColors.containsKey(stack))
			return cachedColors.getInt(stack);
		if (stack.isEmpty())
			return 0xffffff;
		TextureAtlasSprite tas = mc.getRenderItem().getItemModelMesher().getItemModel(stack).getParticleTexture();
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
		cachedColors.put(stack, c);
		return c;
	}

	public static TextureAtlasSprite sprite(ItemStack stack) {
		if (cachedSprites.containsKey(stack))
			return cachedSprites.get(stack);
		TextureAtlasSprite tas = null;
		if (!stack.isEmpty()) {
			main: for (String s : Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).filter(s -> s.startsWith("ingot")).collect(Collectors.toList())) {
				String block = s.replace("ingot", "block");
				for (ItemStack b : OreDictionary.getOres(block)) {
					if (b.getItem() instanceof ItemBlock) {
						TextureAtlasSprite tmp = mc.getBlockRendererDispatcher().getModelForState(((ItemBlock) b.getItem()).getBlock().getDefaultState()).getParticleTexture();
						if (tmp != mc.getTextureMapBlocks().getMissingSprite()) {
							tas = tmp;
							break main;
						}
					}
				}

			}
		}
		cachedSprites.put(stack, tas);
		return tas;
	}

	public static void init() {
		defaultTas = mc.getTextureMapBlocks().getAtlasSprite("stackable:blocks/ingots");
	}

	@SubscribeEvent
	public static void draw(DrawBlockHighlightEvent event) {
		RayTraceResult rtr = event.getTarget();
		if (rtr != null && rtr.typeOfHit == Type.BLOCK && rtr.getBlockPos() != null) {
			TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
			if (t instanceof TileIngots) {
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(2.5F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				EntityPlayer player = event.getPlayer();
				float partialTicks = event.getPartialTicks();
				double d3 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
				double d4 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
				double d5 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
				if (p != null) {
					Color c = new Color(color);
					float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					float f1, f2, f3;
					if (hsb[2] < .5)
						f1 = f2 = f3 = 1f;
					else
						f1 = f2 = f3 = 0f;
					//					f1 = f2 = f3 = 1f;
					RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(p.getLeft(), p.getRight()).offset(t.getPos()).grow(-0.0040000000949949026D).offset(-d3, -d4, -d5), f1, f2, f3, 0.8F);
					event.setCanceled(true);
				}
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
			}
		}
	}

	static Pair<Vec3d, Vec3d> p = null;
	static int color;

	@SubscribeEvent
	public static void tick(ClientTickEvent event) {
		if (mc != null && mc.player != null && !mc.isGamePaused()) {
			RayTraceResult rtr = mc.objectMouseOver;
			if (rtr != null && rtr.typeOfHit == Type.BLOCK) {
				TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
				if (t instanceof TileIngots) {
					double reach = mc.player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
					Vec3d p1 = mc.player.getPositionEyes(0);
					Vec3d look = mc.player.getLook(1);
					Vec3d p2 = p1.add(look.scale(reach));
					HashMap<Pair<Vec3d, Vec3d>, Pair<Integer, RayTraceResult>> hitMap = new HashMap<>();
					List<Pair<Vec3d, Vec3d>> l = ((TileIngots) t).ingotPositions();
					for (int i = 0; i < l.size(); i++) {
						Pair<Vec3d, Vec3d> pp = l.get(i);
						AxisAlignedBB aabb = new AxisAlignedBB(pp.getLeft(), pp.getRight()).offset(t.getPos());
						RayTraceResult rtr2 = null;
						if ((rtr2 = aabb.calculateIntercept(p1, p2)) != null) {
							Vec3i v = TileIngots.coordMap.get(i);
							hitMap.put(pp, Pair.of(color(((TileIngots) t).ingotList().get(TileIngots.coordMap.inverse().get(v))), rtr2));
						}
					}
					Pair<Vec3d, Vec3d> fin = null;
					RayTraceResult r1 = null;
					for (Map.Entry<Pair<Vec3d, Vec3d>, Pair<Integer, RayTraceResult>> e : hitMap.entrySet()) {
						Pair<Vec3d, Vec3d> pp = e.getKey();
						if (fin == null) {
							fin = pp;
							r1 = hitMap.get(pp).getRight();
							continue;
						}
						RayTraceResult r2 = e.getValue().getRight();
						Vec3d v1 = e.getValue().getRight().hitVec, v2 = r1.hitVec;
						if (v1.distanceTo(p1) < v2.distanceTo(p1)) {
							fin = pp;
							r1 = r2;
						}
					}
					p = fin;
					if (p != null)
						color = hitMap.get(p).getLeft();
				}
			}
		}
	}

	@SubscribeEvent
	public static void stich(TextureStitchEvent event) {
		event.getMap().registerSprite(new ResourceLocation("stackable:blocks/ingots"));
	}

	@SubscribeEvent
	public static void bake(ModelBakeEvent event) {
		mc = Minecraft.getMinecraft();
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.ingots.getRegistryName().toString()), new IBakedModel() {

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
				if (side != null)
					return Collections.emptyList();
				StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
				if ("getDamageModel".equals(ste.getMethodName())) {
					IBlockState cobble = Blocks.COBBLESTONE.getDefaultState();
					IBakedModel m = mc.getBlockRendererDispatcher().getModelForState(cobble);
					return m.getQuads(cobble, side, rand);
				}
				TileIngots tile = ((IExtendedBlockState) state).getValue(BlockIngots.prop);
				List<BakedQuad> quads = new ArrayList<>();
				if (tile != null) {
					//					cachedQuads.clear();
					if (!tile.changedClient && cachedQuads.containsKey(tile))
						return cachedQuads.get(tile);
					List<ItemStack> stacks = tile.ingotList();
					List<AxisAlignedBB> aabbs = tile.io.ingotBoxes();
					int size = Math.min(stacks.size(), aabbs.size());
					for (int i = 0; i < size; i++) {
						ItemStack s = stacks.get(i);
						createIngot(quads, s, aabbs.get(i), Stackable.useBlockTexture ? sprite(s) : null);
					}
					tile.changedClient = false;
					cachedQuads.put(tile, quads);
				}
				return quads;
			}

			@Override
			public TextureAtlasSprite getParticleTexture() {
				return defaultTas;
			}

			@Override
			public ItemOverrideList getOverrides() {
				return ItemOverrideList.NONE;
			}

		});
	}

	private static void createIngot(List<BakedQuad> quads, ItemStack stack, AxisAlignedBB aabb, @Nullable TextureAtlasSprite tas) {
		float r = 1f, g = 1f, b = 1f;
		if (tas == null) {
			tas = defaultTas;
			Color col = new Color(color(stack));
			float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
			col = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + .15f));
			r = col.getRed() / 255f;
			g = col.getGreen() / 255f;
			b = col.getBlue() / 255f;
		}
		boolean flag = aabb.maxX - aabb.minX > aabb.maxZ - aabb.minZ;
		double a1 = !flag ? aabb.maxX - aabb.minX : aabb.maxZ - aabb.minZ;
		double a2 = !flag ? aabb.maxX - aabb.minX : aabb.maxZ - aabb.minZ;
		float diffX = (float) (a1 * .1), diffZ = (float) (a2 * .1);
		Vector3f va = new Vector3f((float) aabb.minX + (diffX * .2f), (float) aabb.minY, (float) aabb.minZ + (diffZ * .2f)), //
				vb = new Vector3f((float) aabb.minX + (diffX * .2f), (float) aabb.minY, (float) aabb.maxZ - (diffZ * .2f)), //
				vc = new Vector3f((float) aabb.minX + diffX, (float) aabb.maxY, (float) aabb.maxZ - diffZ), //
				vd = new Vector3f((float) aabb.minX + diffX, (float) aabb.maxY, (float) aabb.minZ + diffZ), //
				ve = new Vector3f((float) aabb.maxX - (diffX * .2f), (float) aabb.minY, (float) aabb.minZ + (diffZ * .2f)), //
				vf = new Vector3f((float) aabb.maxX - (diffX * .2f), (float) aabb.minY, (float) aabb.maxZ - (diffZ * .2f)), //
				vg = new Vector3f((float) aabb.maxX - diffX, (float) aabb.maxY, (float) aabb.maxZ - diffZ), //
				vh = new Vector3f((float) aabb.maxX - diffX, (float) aabb.maxY, (float) aabb.minZ + diffZ);
		//bottom
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, va, ve, vf, vb, r, g, b));
		//top
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, vh, vd, vc, vg, r, g, b));
		//sides NESW
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, vh, ve, va, vd, r, g, b));
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, vg, vf, ve, vh, r, g, b));
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, vc, vb, vf, vg, r, g, b));
		quads.add(createQuad(DefaultVertexFormats.ITEM, tas, vd, va, vb, vc, r, g, b));
	}

	private static BakedQuad createQuad(VertexFormat format, TextureAtlasSprite tas, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, float r, float g, float b) {
		return createQuad(format, tas, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z, v4.x, v4.y, v4.z, r, g, b);
	}

	private static BakedQuad createQuad(VertexFormat format, TextureAtlasSprite tas, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b) {
		UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
		builder.setTexture(tas);
		Vec3d normal = new Vec3d(x3, y3, z3).subtract(new Vec3d(x2, y2, z2)).crossProduct(new Vec3d(x1, y1, z1).subtract(new Vec3d(x2, y2, z2))).normalize();
		Point2f p1 = new Point2f(0, 0), p2 = new Point2f(0, 16), p3 = new Point2f(16, 16), p4 = new Point2f(16, 0);
		if (!Stackable.useCompressedTexture) {
			//z
			if (normal.z > .5 || normal.z < -.5) {
				p1.x = x1 * 16;
				p1.y = y1 * 16;
				p2.x = x2 * 16;
				p2.y = y2 * 16;
				p3.x = x3 * 16;
				p3.y = y3 * 16;
				p4.x = x4 * 16;
				p4.y = y4 * 16;
			} else
			//y
			if (normal.y > .5 || normal.y < -.5) {
				p1.x = x1 * 16;
				p1.y = z1 * 16;
				p2.x = x2 * 16;
				p2.y = z2 * 16;
				p3.x = x3 * 16;
				p3.y = z3 * 16;
				p4.x = x4 * 16;
				p4.y = z4 * 16;
			} else
			//x
			if (normal.x > .5 || normal.x < -.5) {
				p1.x = z1 * 16;
				p1.y = y1 * 16;
				p2.x = z2 * 16;
				p2.y = y2 * 16;
				p3.x = z3 * 16;
				p3.y = y3 * 16;
				p4.x = z4 * 16;
				p4.y = y4 * 16;
			}
		}
		putVertex(format, tas, builder, normal, x1, y1, z1, p1.x, p1.y, r, g, b);
		putVertex(format, tas, builder, normal, x2, y2, z2, p2.x, p2.y, r, g, b);
		putVertex(format, tas, builder, normal, x3, y3, z3, p3.x, p3.y, r, g, b);
		putVertex(format, tas, builder, normal, x4, y4, z4, p4.x, p4.y, r, g, b);
		return builder.build();
	}

	private static void putVertex(VertexFormat format, TextureAtlasSprite sprite, UnpackedBakedQuad.Builder builder, Vec3d normal, float x, float y, float z, float u, float v, float r, float g, float b) {
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
				}
				break;
			case NORMAL:
				builder.put(e, (float) normal.x, (float) normal.y, (float) normal.z, 0f);
				break;
			default:
				builder.put(e);
				break;
			}
		}
	}
}
