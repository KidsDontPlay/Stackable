package mrriegel.stackable;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.vecmath.Point2f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.google.common.collect.Streams;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = Stackable.MODID, value = Side.CLIENT)
public class ClientUtils {

	private static final Object2IntMap<ItemStack> cachedColors = new Object2IntOpenCustomHashMap<>(TileStackable.strategy);
	private static final Object2ObjectMap<ItemStack, TextureAtlasSprite> cachedSprites = new Object2ObjectOpenCustomHashMap<>(TileStackable.strategy);
	private static final ResourceLocation BACKGROUND_TEX = new ResourceLocation("textures/gui/demo_background.png");
	private static final ResourceLocation SLOT_TEX = new ResourceLocation("textures/gui/container/recipe_background.png");
	private static Minecraft mc;
	static TextureAtlasSprite defaultTas;
	public static Object2IntOpenHashMap<BlockPos> brokenBlocks = new Object2IntOpenHashMap<>();

	public static int color(ItemStack stack) {
		if (cachedColors.containsKey(stack))
			return cachedColors.getInt(stack);
		if (stack.isEmpty())
			return 0xffffff;
		IBakedModel model = ForgeHooksClient.handleCameraTransforms(mc.getRenderItem().getItemModelMesher().getItemModel(stack), TransformType.GUI, false);
		int red = 0, green = 0, blue = 0, count = 0;
		for (BakedQuad bq : model.getQuads(null, null, 0)) {
			TextureAtlasSprite tas = bq.getSprite();
			int itemC = -1;
			if (bq.hasTintIndex()) {
				itemC = mc.getItemColors().colorMultiplier(stack, bq.getTintIndex());
				if (EntityRenderer.anaglyphEnable)
					itemC = TextureUtil.anaglyphColor(itemC);
				itemC |= 0xff000000;
			}
			Color itemColor = new Color(itemC, true);
			if (tas == mc.getTextureMapBlocks().getMissingSprite() || tas.getIconHeight() <= 0 || tas.getIconWidth() <= 0 || tas.getFrameCount() <= 0)
				continue;
			BufferedImage img = new BufferedImage(tas.getIconWidth(), tas.getIconHeight() * tas.getFrameCount(), BufferedImage.TYPE_4BYTE_ABGR);
			for (int i = 0; i < tas.getFrameCount(); i++) {
				int[][] frameTextureData = tas.getFrameTextureData(i);
				int[] largestMipMapTextureData = frameTextureData[0];
				img.setRGB(0, i * tas.getIconHeight(), tas.getIconWidth(), tas.getIconHeight(), largestMipMapTextureData, 0, tas.getIconWidth());
			}
			for (int x = 0; x < img.getWidth(); x++)
				for (int y = 0; y < img.getHeight(); y++) {
					int rgb = img.getRGB(x, y);
					Color col = new Color(rgb, true);
					if (col.getAlpha() == 255) {
						red += col.getRed() * (itemColor.getRed() / 255f);
						green += col.getGreen() * (itemColor.getGreen() / 255f);
						blue += col.getBlue() * (itemColor.getBlue() / 255f);
						count++;
					}
				}
		}
		int color = count == 0 ? -1 : new Color((red / count), (green / count), (blue / count)).getRGB();
		cachedColors.put(stack, color);
		return color;
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
						IBlockState state = ((ItemBlock) b.getItem()).getBlock().getStateFromMeta(b.getMetadata());
						TextureAtlasSprite tmp = mc.getBlockRendererDispatcher().getModelForState(state).getParticleTexture();
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
		IngotModel.init();
		brokenBlocks.defaultReturnValue(-1);
		ClientRegistry.registerKeyBinding(Stackable.PLACE_KEY);
	}

	@SubscribeEvent
	public static void keyInput(KeyInputEvent event) {
		if (Keyboard.getEventKey() == Stackable.PLACE_KEY.getKeyCode())
			Stackable.snw.sendToServer(new MessagePlaceKey(Keyboard.getEventKeyState()));
	}

	@SubscribeEvent
	public static void draw(DrawBlockHighlightEvent event) {
		RayTraceResult rtr = event.getTarget();
		if (rtr != null && rtr.typeOfHit == Type.BLOCK && rtr.getBlockPos() != null) {
			TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
			if (t instanceof TileStackable) {
				ItemStack h = mc.player.getHeldItemMainhand();
				if (h.getItem().getToolClasses(h).contains("pickaxe"))
					return;
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
				GlStateManager.glLineWidth(2.5F);
				GlStateManager.disableTexture2D();
				GlStateManager.depthMask(false);
				EntityPlayer player = event.getPlayer();
				float partialTicks = event.getPartialTicks();
				double d3 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
				double d4 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
				double d5 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
				Pair<Vec3i, AxisAlignedBB> p = ((TileStackable) t).lookingPos(mc.player);
				if (p.getRight() != null) {
					boolean stackdepend = false;
					float f1, f2, f3;
					if (stackdepend) {
						Color c = new Color(color(((TileStackable) t).itemList().get(((TileStackable) t).getCoordMap().inverse().get(p.getLeft()))));
						float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
						if (hsb[2] < .5)
							f1 = f2 = f3 = 1f;
						else
							f1 = f2 = f3 = 0f;
					} else
						f1 = f2 = f3 = 1f;
					double grow = Math.sin((mc.world.getTotalWorldTime() + event.getPartialTicks()) / 3) * .003;
					RenderGlobal.drawSelectionBoundingBox(p.getRight().offset(t.getPos()).grow(grow).offset(-d3, -d4, -d5), f1, f2, f3, 0.8F);
					event.setCanceled(true);
				}
				GlStateManager.depthMask(true);
				GlStateManager.enableTexture2D();
				GlStateManager.disableBlend();
			}
		}
	}

	@SubscribeEvent
	public static void renderText(RenderGameOverlayEvent.Post event) {
		if (((Stackable.overlay == 1 && mc.player.isSneaking()) || Stackable.overlay == 2) && event.getType() == ElementType.ALL /*&& !WAILAorTOP*/) {
			RayTraceResult rtr = mc.objectMouseOver;
			if (rtr != null && rtr.typeOfHit == Type.BLOCK) {
				TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
				if (t instanceof TileStackable) {
					ItemStack h = mc.player.getHeldItemMainhand();
					if (h.getItem().getToolClasses(h).contains("pickaxe"))
						return;
					ItemStack s = ((TileStackable) t).lookingStack(mc.player);
					if (!s.isEmpty()) {
						TileStackable m = ((TileStackable) t).getMaster();
						ScaledResolution sr = event.getResolution();
						String text = m.inv.inventory.getInt(s) + "x " + s.getDisplayName();
						int textWidth = mc.fontRenderer.getStringWidth(text);
						int x = sr.getScaledWidth() / 2 - textWidth / 2, y = sr.getScaledHeight() / 2 + mc.fontRenderer.FONT_HEIGHT + 5;
						mc.fontRenderer.drawString(TextFormatting.YELLOW + text, x, y, 0, true);
						GlStateManager.color(1, 1, 1, 1);
						mc.getTextureManager().bindTexture(BACKGROUND_TEX);
						GuiUtils.drawContinuousTexturedBox(x + textWidth + 2, y - 5, 0, 0, 24, 24, 248, 166, 4, 0);
						mc.getTextureManager().bindTexture(SLOT_TEX);
						GuiUtils.drawTexturedModalRect(x + textWidth + 5, y - 2, 12, 12, 18, 18, 0);
						RenderHelper.enableGUIStandardItemLighting();
						mc.getRenderItem().renderItemAndEffectIntoGUI(s, x + textWidth + 6, y - 1);
						RenderHelper.disableStandardItemLighting();
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void stich(TextureStitchEvent event) {
		defaultTas = event.getMap().registerSprite(new ResourceLocation("stackable:blocks/ingots"));
	}

	@SubscribeEvent
	public static void bake(ModelBakeEvent event) {
		mc = Minecraft.getMinecraft();
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.ingots.getRegistryName().toString()), new IngotModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.all.getRegistryName().toString()), new AllModel());
	}

	@SubscribeEvent
	public static void load(Load event) {
		Map<IBlockState, IBakedModel> bakedModelStore = ReflectionHelper.getPrivateValue(BlockModelShapes.class, Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes(), "bakedModelStore", "field_178129_a");
		bakedModelStore.put(BlockIngots.DAMAGE, Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(Blocks.COBBLESTONE.getDefaultState()));
		brokenBlocks.clear();
		event.getWorld().addEventListener(new IWorldEventListener() {

			@Override
			public void spawnParticle(int id, boolean ignoreRange, boolean p_190570_3_, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
			}

			@Override
			public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
			}

			@Override
			public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
				if (progress >= 0 && progress < 10) {
					int destroyblockprogress = brokenBlocks.getInt(pos);
					if (destroyblockprogress == -1) {
						brokenBlocks.clear();
					}
					brokenBlocks.put(pos, progress);
				} else {
					brokenBlocks.removeInt(pos);
				}
			}

			@Override
			public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch) {
			}

			@Override
			public void playRecord(SoundEvent soundIn, BlockPos pos) {
			}

			@Override
			public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {
			}

			@Override
			public void onEntityRemoved(Entity entityIn) {
			}

			@Override
			public void onEntityAdded(Entity entityIn) {
			}

			@Override
			public void notifyLightSet(BlockPos pos) {
			}

			@Override
			public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
			}

			@Override
			public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
			}

			@Override
			public void broadcastSound(int soundID, BlockPos pos, int data) {
			}
		});
	}

	public static List<BakedQuad> getBakedQuads(ItemStack stack) {
		if (stack.getItem() instanceof ItemBlock) {
			Block block = ((ItemBlock) stack.getItem()).getBlock();
			IBlockState state = null;
			try {
				state = block.getStateForPlacement(mc.world, BlockPos.ORIGIN, EnumFacing.UP, 0, 0, 0, stack.getMetadata(), mc.player, EnumHand.MAIN_HAND);
			} catch (Exception e) {
				state = block.getStateFromMeta(stack.getMetadata());
			}
			IBlockState sstate = state;
			if (block.getRenderType(state) != EnumBlockRenderType.MODEL)
				return Collections.emptyList();
			IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
			List<BakedQuad> ret = Streams.concat(Stream.of((EnumFacing) null), Arrays.stream(EnumFacing.VALUES)).flatMap(f -> model.getQuads(sstate, f, 0).stream()).map(bq -> {
				if (bq.hasTintIndex()) {
					int color = mc.getBlockColors().colorMultiplier(sstate, mc.world, BlockPos.ORIGIN, bq.getTintIndex());
					if (EntityRenderer.anaglyphEnable)
						color = TextureUtil.anaglyphColor(color);
					color |= 0xFF000000;
					float a = (color >> 24 & 255) / 255f, //
							r = (color >> 16 & 255) / 255f, //
							g = (color >> 8 & 255) / 255f, //
							b = (color >> 0 & 255) / 255f;
					UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
					builder.setTexture(bq.getSprite());
					VertexFormat format = bq.getFormat();
					for (int k = 0; k < 4; k++)
						for (int e = 0; e < format.getElementCount(); e++)
							builder.put(e, r, g, b, a);
					int[] colorData = builder.build().getVertexData();
					int[] data = Arrays.copyOf(bq.getVertexData(), bq.getVertexData().length);
					data[3] = colorData[3];
					data[10] = colorData[10];
					data[17] = colorData[17];
					data[24] = colorData[24];
					bq = new BakedQuad(data, bq.getTintIndex(), bq.getFace(), bq.getSprite(), bq.shouldApplyDiffuseLighting(), bq.getFormat());
				}
				return bq;
			}).collect(Collectors.toList());
			Set<IntArrayList> intset = new HashSet<>();
			Iterator<BakedQuad> it = ret.iterator();
			while (it.hasNext()) {
				BakedQuad b = it.next();
				if (intset.contains(new IntArrayList(b.getVertexData())))
					it.remove();
				else
					intset.add(new IntArrayList(b.getVertexData()));

			}
			return ret;
		} else {
			IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(stack);
			if (model.isGui3d())
				return model.getQuads(null, null, 0);
			model = ForgeHooksClient.handleCameraTransforms(model, TransformType.GUI, false);
			List<BakedQuad> quads = model.getQuads(null, null, 0);
			List<BakedQuad> ret = new ArrayList<>(quads.size() * 6);
			for (int i = 0; i < quads.size(); i++) {
				BakedQuad bq = quads.get(i);
				if (bq.hasTintIndex()) {
					int color = mc.getItemColors().colorMultiplier(stack, bq.getTintIndex());
					if (EntityRenderer.anaglyphEnable)
						color = TextureUtil.anaglyphColor(color);
					color |= 0xFF000000;
					float a = (color >> 24 & 255) / 255f, //
							r = (color >> 16 & 255) / 255f, //
							g = (color >> 8 & 255) / 255f, //
							b = (color >> 0 & 255) / 255f;
					UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
					builder.setTexture(bq.getSprite());
					VertexFormat format = bq.getFormat();
					for (int k = 0; k < 4; k++)
						for (int e = 0; e < format.getElementCount(); e++)
							builder.put(e, r, g, b, a);
					int[] colorData = builder.build().getVertexData();
					int[] data = Arrays.copyOf(bq.getVertexData(), bq.getVertexData().length);
					data[3] = colorData[3];
					data[10] = colorData[10];
					data[17] = colorData[17];
					data[24] = colorData[24];
					bq = new BakedQuad(data, bq.getTintIndex(), bq.getFace(), bq.getSprite(), bq.shouldApplyDiffuseLighting(), bq.getFormat());
				}
				boolean hard = "".isEmpty();
				if (hard) {
					//south
					ret.add(translate(bq, 0, 0, .5f));
					//north
					ret.add(translate(rotate(bq, 180, 0, 1, 0), 1, 0, .5f));
					//east
					ret.add(translate(rotate(bq, 90, 0, 1, 0), .5f, 0, 1));
					//west
					ret.add(translate(rotate(bq, 270, 0, 1, 0), .5f, 0, 0));
					//down
					ret.add(translate(rotate(bq, 90, 1, 0, 0), 0, .5f, 0));
					//up
					ret.add(translate(rotate(bq, 270, 1, 0, 0), 0, .5f, 1));

					TextureAtlasSprite tas = defaultTas;
					float r, g, b, a = 1f;
					Color col = new Color(color(stack));
					float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
					col = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + .25f));
					r = col.getRed() / 255f;
					g = col.getGreen() / 255f;
					b = col.getBlue() / 255f;
					//west
					ret.add(createQuad(tas, //
							0, 1, 0, //
							0, 0, 0, //
							0, 0, 1, //
							0, 1, 1, //
							r, g, b, a));
					//east
					ret.add(createQuad(tas, //
							1, 1, 1, //
							1, 0, 1, //
							1, 0, 0, //
							1, 1, 0, //
							r, g, b, a));
					//down
					ret.add(createQuad(tas, //
							1, 0, 1, //
							0, 0, 1, //
							0, 0, 0, //
							1, 0, 0, //
							r, g, b, a));
					//up
					ret.add(createQuad(tas, //
							1, 1, 0, //
							0, 1, 0, //
							0, 1, 1, //
							1, 1, 1, //
							r, g, b, a));
					//south
					ret.add(createQuad(tas, //
							1, 1, 1, //
							0, 1, 1, //
							0, 0, 1, //
							1, 0, 1, //
							r, g, b, a));
					ret.add(createQuad(tas, //
							1, 0, 0, //
							0, 0, 0, //
							0, 1, 0, //
							1, 1, 0, //
							r, g, b, a));

					//					ret.add(translate(bq, 0, 0, .5f));
					//					ret.add(translate(bq, 0, 0, -.5f));
					//					ret.add(translate(rotate(bq, 90, 0, 1, 0), .5f, 0, 1));
					//					ret.add(translate(rotate(bq, 90, 0, 1, 0), -.5f, 0, 1));
					//					ret.add(translate(rotate(bq, 270, 1, 0, 0), 0, .5f, 1));
					//					ret.add(translate(rotate(bq, 270, 1, 0, 0), 0, -.5f, 1));
				} else {
					BakedQuad rotated = rotate(bq, 270, 1, 0, 0);
					ret.add(translate(rotated, 0, -.5f, 1));
					ret.add(translate(rotated, 0, -.4f, 1));
					ret.add(translate(rotated, 0, -.3f, 1));
				}
			}
			return ret;
		}
	}

	static void createIngot(List<BakedQuad> quads, ItemStack stack, AxisAlignedBB aabb, @Nullable TextureAtlasSprite tas) {
		float r = 1f, g = 1f, b = 1f;
		if (tas == null) {
			tas = defaultTas;
			Color col = new Color(color(stack));
			float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
			col = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + .25f));
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
		quads.add(createQuad(tas, va, ve, vf, vb, r, g, b, 1));
		//top
		quads.add(createQuad(tas, vh, vd, vc, vg, r, g, b, 1));
		//sides NESW
		quads.add(createQuad(tas, vh, ve, va, vd, r, g, b, 1));
		quads.add(createQuad(tas, vg, vf, ve, vh, r, g, b, 1));
		quads.add(createQuad(tas, vc, vb, vf, vg, r, g, b, 1));
		quads.add(createQuad(tas, vd, va, vb, vc, r, g, b, 1));
	}

	static Vector3f scale(Vector3f v, float x, float y, float z) {
		Matrix4f m = new Matrix4f();
		m.scale(new Vector3f(x, y, z));
		Vector4f v4 = new Vector4f(v.x, v.y, v.z, 1);
		Matrix4f.transform(m, v4, v4);
		v = new Vector3f(v4.x, v4.y, v4.z);
		return v;
	}

	static Vector3f rotate(Vector3f v, float degree, float x, float y, float z) {
		Matrix4f m = new Matrix4f();
		m.rotate((float) Math.toRadians(degree), new Vector3f(x, y, z));
		Vector4f v4 = new Vector4f(v.x, v.y, v.z, 1);
		Matrix4f.transform(m, v4, v4);
		v = new Vector3f(v4.x, v4.y, v4.z);
		return v;
	}

	static Vector3f translate(Vector3f v, float x, float y, float z) {
		Matrix4f m = new Matrix4f();
		m.translate(new Vector3f(x, y, z));
		Vector4f v4 = new Vector4f(v.x, v.y, v.z, 1);
		Matrix4f.transform(m, v4, v4);
		v = new Vector3f(v4.x, v4.y, v4.z);
		return v;
	}

	static BakedQuad scale(BakedQuad q, float x, float y, float z) {
		Vector3f[] vecs = ClientUtils.getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = scale(vecs[i], x, y, z);
		return ClientUtils.setCoords(q, vecs);
	}

	static BakedQuad rotate(BakedQuad q, float degree, float x, float y, float z) {
		Vector3f[] vecs = ClientUtils.getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = rotate(vecs[i], degree, x, y, z);
		return ClientUtils.setCoords(q, vecs);
	}

	static BakedQuad translate(BakedQuad q, float x, float y, float z) {
		Vector3f[] vecs = ClientUtils.getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = translate(vecs[i], x, y, z);
		return ClientUtils.setCoords(q, vecs);
	}

	static Vector3f[] getCoords(BakedQuad quad) {
		Vector3f[] ret = new Vector3f[4];
		int[] data = quad.getVertexData();
		ret[0] = new Vector3f(Float.intBitsToFloat(data[0]), Float.intBitsToFloat(data[1]), Float.intBitsToFloat(data[2]));
		ret[1] = new Vector3f(Float.intBitsToFloat(data[7]), Float.intBitsToFloat(data[8]), Float.intBitsToFloat(data[9]));
		ret[2] = new Vector3f(Float.intBitsToFloat(data[14]), Float.intBitsToFloat(data[15]), Float.intBitsToFloat(data[16]));
		ret[3] = new Vector3f(Float.intBitsToFloat(data[21]), Float.intBitsToFloat(data[22]), Float.intBitsToFloat(data[23]));
		return ret;
	}

	static BakedQuad setCoords(BakedQuad quad, Vector3f[] vecs) {
		int[] data = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);
		data[0] = Float.floatToRawIntBits(vecs[0].x);
		data[1] = Float.floatToRawIntBits(vecs[0].y);
		data[2] = Float.floatToRawIntBits(vecs[0].z);
		data[7] = Float.floatToRawIntBits(vecs[1].x);
		data[8] = Float.floatToRawIntBits(vecs[1].y);
		data[9] = Float.floatToRawIntBits(vecs[1].z);
		data[14] = Float.floatToRawIntBits(vecs[2].x);
		data[15] = Float.floatToRawIntBits(vecs[2].y);
		data[16] = Float.floatToRawIntBits(vecs[2].z);
		data[21] = Float.floatToRawIntBits(vecs[3].x);
		data[22] = Float.floatToRawIntBits(vecs[3].y);
		data[23] = Float.floatToRawIntBits(vecs[3].z);
		return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat());
	}

	private static BakedQuad createQuad(TextureAtlasSprite tas, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, float r, float g, float b, float a) {
		return createQuad(tas, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z, v4.x, v4.y, v4.z, r, g, b, a);
	}

	private static BakedQuad createQuad(TextureAtlasSprite tas, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
		UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
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
		putVertex(tas, builder, normal, x1, y1, z1, p1.x, p1.y, r, g, b, a);
		putVertex(tas, builder, normal, x2, y2, z2, p2.x, p2.y, r, g, b, a);
		putVertex(tas, builder, normal, x3, y3, z3, p3.x, p3.y, r, g, b, a);
		putVertex(tas, builder, normal, x4, y4, z4, p4.x, p4.y, r, g, b, a);
		return builder.build();
	}

	private static void putVertex(TextureAtlasSprite sprite, UnpackedBakedQuad.Builder builder, Vec3d normal, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
		VertexFormat format = DefaultVertexFormats.ITEM;
		for (int e = 0; e < format.getElementCount(); e++) {
			switch (format.getElement(e).getUsage()) {
			case POSITION:
				builder.put(e, x, y, z, 1f);
				break;
			case COLOR:
				builder.put(e, r, g, b, a);
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
