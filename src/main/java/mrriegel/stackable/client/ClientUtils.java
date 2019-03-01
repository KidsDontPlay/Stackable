package mrriegel.stackable.client;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.vecmath.Point2f;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.google.common.collect.Streams;
import com.google.common.hash.HashCode;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.item.ItemChanger.Mode;
import mrriegel.stackable.message.MessageKey;
import mrriegel.stackable.tile.TileAnyPile;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
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
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBlockSpecial;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = Stackable.MODID, value = Side.CLIENT)
public class ClientUtils {

	private static final Object2IntMap<ItemStack> cachedColors = new Object2IntOpenCustomHashMap<>(TilePile.strategyExact);
	private static final Object2ObjectMap<ItemStack, TextureAtlasSprite> cachedSprites = new Object2ObjectOpenCustomHashMap<>(TilePile.strategyFuzzy);
	private static final ResourceLocation BACKGROUND_TEX = new ResourceLocation("textures/gui/demo_background.png");
	private static final ResourceLocation SLOT_TEX = new ResourceLocation("textures/gui/container/recipe_background.png");
	public static final KeyBinding PLACE_KEY = new KeyBinding("key.stackable.place", KeyConflictContext.IN_GAME, Keyboard.KEY_P, Stackable.NAME);
	public static final KeyBinding CYCLE_KEY = new KeyBinding("key.stackable.cycle", KeyConflictContext.IN_GAME, Keyboard.KEY_C, Stackable.NAME);
	private static Minecraft mc;
	static TextureAtlasSprite defaultTas;
	public static long wailaTime = 0, topTime = 0;

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
			List<IBlockState> states = new ArrayList<>();
			for (String s : Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).filter(s -> s.startsWith("ingot")).collect(Collectors.toList())) {
				String block = s.replace("ingot", "block");
				for (ItemStack b : OreDictionary.getOres(block)) {
					if (b.getItem() instanceof ItemBlock) {
						IBlockState state = ((ItemBlock) b.getItem()).getBlock().getStateFromMeta(b.getMetadata());
						states.add(state);
					}
				}
			}
			states.sort((s1, s2) -> {
				int i1 = Stackable.preferredTextures.indexOf(s1.getBlock().getRegistryName().getResourceDomain()), i2 = Stackable.preferredTextures.indexOf(s2.getBlock().getRegistryName().getResourceDomain());
				return Integer.compare(i1 == -1 ? 999 : i1, i2 == -1 ? 999 : i2);
			});
			for (IBlockState state : states) {
				TextureAtlasSprite tmp = mc.getBlockRendererDispatcher().getModelForState(state).getParticleTexture();
				if (tmp != mc.getTextureMapBlocks().getMissingSprite()) {
					tas = tmp;
					break;
				}
			}
		}
		cachedSprites.put(stack, tas);
		return tas;
	}

	public static void init() {
		PileModel.init();
		ClientRegistry.registerKeyBinding(PLACE_KEY);
		ClientRegistry.registerKeyBinding(CYCLE_KEY);
		ClientRegistry.bindTileEntitySpecialRenderer(TileAnyPile.class, new TESRAnyPile());
	}

	@SubscribeEvent
	public static void keyInput(KeyInputEvent event) {
		RayTraceResult rtr = mc.objectMouseOver;
		if (rtr != null && rtr.typeOfHit == Type.BLOCK) {
			if (PLACE_KEY.isPressed()) {
				if (rtr.sideHit == EnumFacing.UP)
					Stackable.snw.sendToServer(new MessageKey((byte) 0, rtr.getBlockPos()));
			} else if (CYCLE_KEY.isPressed()) {
				Stackable.snw.sendToServer(new MessageKey((byte) 1, rtr.getBlockPos()));
			}
		}
		if (Keyboard.getEventKey() == (Minecraft.IS_RUNNING_ON_MAC ? Keyboard.KEY_LMETA : Keyboard.KEY_LCONTROL))
			Stackable.snw.sendToServer(new MessageKey((byte) 2, new BlockPos(Keyboard.getEventKeyState() ? 1 : 0, 0, 0)));
	}

	@SubscribeEvent
	public static void draw(DrawBlockHighlightEvent event) {
		RayTraceResult rtr = event.getTarget();
		if (rtr != null && rtr.typeOfHit == Type.BLOCK && rtr.getBlockPos() != null) {
			TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
			if (t instanceof TilePile) {
				if (TilePile.canPlayerBreak(mc.player))
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
				Pair<Vec3i, AxisAlignedBB> p = ((TilePile) t).lookingPos(mc.player);
				if (p.getRight() != null) {
					boolean stackdepend = false;
					float f1, f2, f3;
					if (stackdepend) {
						Color c = new Color(color(((TilePile) t).itemList().get(((TilePile) t).getCoordMap().inverse().get(p.getLeft()))));
						float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
						//						hsb[2]=.9f;
						//						c=new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
						//						f1=c.getRed()/255f;
						//						f2=c.getGreen()/255f;
						//						f3=c.getBlue()/255f;
						if (hsb[2] < .5)
							f1 = f2 = f3 = 1f;
						else
							f1 = f2 = f3 = 0f;
					} else
						f1 = f2 = f3 = 1f;
					double grow = Math.sin((mc.world.getTotalWorldTime() + event.getPartialTicks()) / 3) * .0015;
					grow += .0015;
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
		long time = mc.world.getTotalWorldTime();
		boolean waila = wailaTime + 10 >= time;
		boolean top = topTime + 10 >= time;
		if (((Stackable.overlay == 1 && mc.player.isSneaking()) || Stackable.overlay == 2) && event.getType() == ElementType.ALL && !waila && !top) {
			RayTraceResult rtr = mc.objectMouseOver;
			if (rtr != null && rtr.typeOfHit == Type.BLOCK) {
				TileEntity t = mc.world.getTileEntity(rtr.getBlockPos());
				if (t instanceof TilePile) {
					if (TilePile.canPlayerBreak(mc.player))
						return;
					ItemStack s = ((TilePile) t).lookingStack(mc.player);
					if (!s.isEmpty()) {
						ScaledResolution sr = event.getResolution();
						String text = TilePile.getOverlayText(s, (TilePile) t);
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
		} else if (event.getType() == ElementType.HOTBAR && mc.player.getHeldItemMainhand().getItem() == Stackable.changer) {
			Mode mode = Stackable.changer.getMode(mc.player.getHeldItemMainhand());
			if (mode != Mode.BLACKADD && mode != Mode.WHITEADD && //
					mode != Mode.BLACKREMOVE && mode != Mode.WHITEREMOVE && //
					mode != Mode.MIN && mode != Mode.MAX)
				return;
			ScaledResolution sr = event.getResolution();
			int i = sr.getScaledWidth() / 2;
			int l = mc.player.inventory.currentItem + 1;
			time = System.currentTimeMillis() / 20;
			if (l < 9) {
				for (int j = 0; j < 8; j++) {
					int x = i - 90 + l * 20 + 2;
					int y = sr.getScaledHeight() - 16 - 3;
					long t = (time + j);
					long pos = t % 48;
					if (mode == Mode.BLACKREMOVE || mode == Mode.WHITEREMOVE)
						pos = 48 - pos;
					if (pos >= 0 && pos <= 11) {
						x += pos % 12;
					} else if (pos >= 12 && pos <= 23) {
						x += 12;
						y += pos % 12;
					} else if (pos >= 24 && pos <= 35) {
						x += 12 - pos % 12;
						y += 12;
					} else if (pos >= 36 && pos <= 47) {
						y += 12 - pos % 12;
					}
					//					x += 1;
					//					y += 1;
					//					int cc = prop == Property.BLACKADD || prop == Property.BLACKREMOVE ? 0 : 255;
					//					Color c = new Color(cc, cc, cc, j * 30);
					Color c = Color.getHSBColor((mc.world.getTotalWorldTime() * 10) / 360f, .5f, .8f);
					c = new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, (j * 30) / 255f);
					int color = c.getRGB();
					GuiUtils.drawGradientRect(700, x, y, x + 4, y + 4, color, color);
				}
			}
		}
	}

	private static long lastMessage = 0;

	@SubscribeEvent
	public static void mouseWheel(MouseInputEvent.Pre event) {
		int wheel = Mouse.getEventDWheel();
		long time = System.currentTimeMillis();
		if (event.getGui() instanceof GuiContainer && wheel != 0) {
			GuiContainer gui = (GuiContainer) event.getGui();
			Slot slot = gui.getSlotUnderMouse();
			if (slot != null && slot.getHasStack() && slot.getStack().getItem() == Stackable.changer && slot.inventory instanceof InventoryPlayer) {
				if (time > lastMessage + 65) {
					lastMessage = time;
					int index = slot.getSlotIndex();
					int add = wheel > 0 ? 1 : -1;
					if (GuiScreen.isShiftKeyDown() && GuiScreen.isCtrlKeyDown())
						add *= 1000;
					else if (GuiScreen.isShiftKeyDown())
						add *= 10;
					else if (GuiScreen.isCtrlKeyDown())
						add *= 100;
					Stackable.snw.sendToServer(new MessageKey((byte) 3, new BlockPos(add, 0, index)));
				}
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void texture(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Stackable.changer, 0, new ModelResourceLocation(Stackable.changer.getRegistryName(), "inventory"));
	}

	@SubscribeEvent
	public static void stich(TextureStitchEvent event) {
		defaultTas = event.getMap().registerSprite(new ResourceLocation("stackable:blocks/ingots"));
	}

	@SubscribeEvent
	public static void bake(ModelBakeEvent event) {
		mc = Minecraft.getMinecraft();
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.ingots.getRegistryName().toString()), new IngotPileModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(Stackable.any.getRegistryName().toString()), new AnyPileModel());
	}

	public static List<BakedQuad> getBakedQuads(ItemStack stack) {
		Block block = stack.getItem() instanceof ItemBlock ? ((ItemBlock) stack.getItem()).getBlock() : stack.getItem() instanceof ItemBlockSpecial ? ((ItemBlockSpecial) stack.getItem()).getBlock() : null;
		if (block != null) {
			IBlockState state = null;
			try {
				state = block.getStateForPlacement(mc.world, BlockPos.ORIGIN, EnumFacing.UP, 0, 0, 0, stack.getMetadata(), mc.player, EnumHand.MAIN_HAND);
			} catch (Exception e) {
				state = block.getStateFromMeta(stack.getMetadata());
			}
			IBlockState sstate = state;
			if (state.getRenderType() != EnumBlockRenderType.MODEL)
				return Collections.emptyList();
			IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(state);
			return Streams.concat(Stream.of((EnumFacing) null), Arrays.stream(EnumFacing.VALUES)).flatMap(f -> model.getQuads(sstate, f, 0).stream()).map(bq -> {
				if (bq.hasTintIndex()) {
					int color = mc.getBlockColors().colorMultiplier(sstate, mc.world, BlockPos.ORIGIN, bq.getTintIndex());
					if (EntityRenderer.anaglyphEnable)
						color = TextureUtil.anaglyphColor(color);
					color = correctColor(color);
					int[] data = Arrays.copyOf(bq.getVertexData(), bq.getVertexData().length);
					data[3] = data[10] = data[17] = data[24] = color;
					bq = new BakedQuad(data, bq.getTintIndex(), bq.getFace(), bq.getSprite(), bq.shouldApplyDiffuseLighting(), bq.getFormat());
				}
				return bq;
			}).collect(Collectors.toList());
		} else {
			IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
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
					color = correctColor(color);
					int[] data = Arrays.copyOf(bq.getVertexData(), bq.getVertexData().length);
					data[3] = data[10] = data[17] = data[24] = color;
					bq = new BakedQuad(data, bq.getTintIndex(), bq.getFace(), bq.getSprite(), bq.shouldApplyDiffuseLighting(), bq.getFormat());
				}
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

				Color col = new Color(color(stack));
				float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
				col = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + .25f));
				float r = col.getRed() / 255f, g = col.getGreen() / 255f, b = col.getBlue() / 255f, a = 1f;
				//west
				ret.add(createQuad(defaultTas, //
						0, 1, 0, //
						0, 0, 0, //
						0, 0, 1, //
						0, 1, 1, //
						r, g, b, a));
				//east
				ret.add(createQuad(defaultTas, //
						1, 1, 1, //
						1, 0, 1, //
						1, 0, 0, //
						1, 1, 0, //
						r, g, b, a));
				//down
				ret.add(createQuad(defaultTas, //
						1, 0, 1, //
						0, 0, 1, //
						0, 0, 0, //
						1, 0, 0, //
						r, g, b, a));
				//up
				ret.add(createQuad(defaultTas, //
						1, 1, 0, //
						0, 1, 0, //
						0, 1, 1, //
						1, 1, 1, //
						r, g, b, a));
				//south
				ret.add(createQuad(defaultTas, //
						1, 1, 1, //
						0, 1, 1, //
						0, 0, 1, //
						1, 0, 1, //
						r, g, b, a));
				//north
				ret.add(createQuad(defaultTas, //
						1, 0, 0, //
						0, 0, 0, //
						0, 1, 0, //
						1, 1, 0, //
						r, g, b, a));

			}
			return ret;
		}
	}

	private static int correctColor(int color) {
		return 0xFF000000 | ((((color >> 16) & 0xFF) & 0xFF) << 0) | ((((color >> 8) & 0xFF) & 0xFF) << 8) | (((color & 0xFF) & 0xFF) << 16);
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
		Vector3f min = new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ);
		Vector3f max = new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
		Vector3f dimension = Vector3f.sub(max, min, null);
		float diffX, diffZ, diffXD, diffZD;
		diffX = diffZ = Math.min(dimension.x, dimension.z) * .1f;
		diffXD = diffX * .0f;
		diffZD = diffZ * .0f;
		boolean scaling = true;
		float scale = Stackable.scaleI;
		Vector3f va, vb, vc, vd, ve, vf, vg, vh;
		if (!scaling) {
			va = new Vector3f(min.x + diffXD, min.y, min.z + diffZD);
			vb = new Vector3f(min.x + diffXD, min.y, max.z - diffZD);
			vc = new Vector3f(min.x + diffX, max.y, max.z - diffZ);
			vd = new Vector3f(min.x + diffX, max.y, min.z + diffZ);
			ve = new Vector3f(max.x - diffXD, min.y, min.z + diffZD);
			vf = new Vector3f(max.x - diffXD, min.y, max.z - diffZD);
			vg = new Vector3f(max.x - diffX, max.y, max.z - diffZ);
			vh = new Vector3f(max.x - diffX, max.y, min.z + diffZ);
		} else {
			va = (Vector3f) new Vector3f(0 + diffXD, 0, 0 + diffZD).scale(scale);
			vb = (Vector3f) new Vector3f(0 + diffXD, 0, dimension.z - diffZD).scale(scale);
			vc = (Vector3f) new Vector3f(0 + diffX, dimension.y, dimension.z - diffZ).scale(scale);
			vd = (Vector3f) new Vector3f(0 + diffX, dimension.y, 0 + diffZ).scale(scale);
			ve = (Vector3f) new Vector3f(dimension.x - diffXD, 0, 0 + diffZD).scale(scale);
			vf = (Vector3f) new Vector3f(dimension.x - diffXD, 0, dimension.z - diffZD).scale(scale);
			vg = (Vector3f) new Vector3f(dimension.x - diffX, dimension.y, dimension.z - diffZ).scale(scale);
			vh = (Vector3f) new Vector3f(dimension.x - diffX, dimension.y, 0 + diffZ).scale(scale);
			Vector3f scaledDimension = (Vector3f) new Vector3f(dimension).scale(.5f * scale);
			Vector3f add = Vector3f.add(min, (Vector3f) new Vector3f(dimension).scale(.5f), null);
			Vector3f.sub(add, scaledDimension, add);
			Vector3f.add(va, add, va);
			Vector3f.add(vb, add, vb);
			Vector3f.add(vc, add, vc);
			Vector3f.add(vd, add, vd);
			Vector3f.add(ve, add, ve);
			Vector3f.add(vf, add, vf);
			Vector3f.add(vg, add, vg);
			Vector3f.add(vh, add, vh);
		}
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
		Vector3f[] vecs = getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = scale(vecs[i], x, y, z);
		return setCoords(q, vecs);
	}

	static BakedQuad rotate(BakedQuad q, float degree, float x, float y, float z) {
		Vector3f[] vecs = getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = rotate(vecs[i], degree, x, y, z);
		return setCoords(q, vecs);
	}

	static BakedQuad translate(BakedQuad q, float x, float y, float z) {
		Vector3f[] vecs = getCoords(q);
		for (int i = 0; i < vecs.length; i++)
			vecs[i] = translate(vecs[i], x, y, z);
		return setCoords(q, vecs);
	}

	static Vector3f[] getCoords(BakedQuad quad) {
		Validate.isTrue(DefaultVertexFormats.POSITION_3F.equals(quad.getFormat().getElements().get(0)), "Wrong VertexFormat! " + quad.getClass() + " (" + quad.getFormat() + ") (" + quad.getSprite() + ")");
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
