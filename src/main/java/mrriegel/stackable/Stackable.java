package mrriegel.stackable;

import static net.minecraftforge.common.config.Configuration.CATEGORY_CLIENT;
import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;
import static net.minecraftforge.common.config.Configuration.NEW_LINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableBiMap;

import mrriegel.stackable.block.BlockAnyPile;
import mrriegel.stackable.block.BlockIngotPile;
import mrriegel.stackable.client.ClientUtils;
import mrriegel.stackable.compat.TOPPlugin;
import mrriegel.stackable.compat.WailaPlugin;
import mrriegel.stackable.message.MessageConfigSync;
import mrriegel.stackable.message.MessageKey;
import mrriegel.stackable.message.MessageTOPTime;
import mrriegel.stackable.tile.TileAnyPile;
import mrriegel.stackable.tile.TileIngotPile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Stackable.MODID, name = Stackable.NAME, version = Stackable.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
@EventBusSubscriber
public class Stackable {

	@Instance(Stackable.MODID)
	public static Stackable INSTANCE;

	public static final String VERSION = "1.2.0";
	public static final String NAME = "Stackable";
	public static final String MODID = "stackable";

	//config
	public static int itemsPerItemI, itemsPerItemA, sizeX, sizeY, sizeZ, overlay, maxPileHeightI, maxPileHeightA, size;
	public static boolean useBlockTexture, useCompressedTexture;
	public static Set<ResourceLocation> allowedIngots;

	public static SimpleNetworkWrapper snw;

	public static final Block ingots = new BlockIngotPile();
	public static final Block any = new BlockAnyPile();

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		itemsPerItemI = config.getInt("itemsPerItem", CATEGORY_GENERAL + ".ingot", 4, 1, 64, "Items per visual item");
		sizeX = config.getInt("sizeX", CATEGORY_GENERAL + ".ingot", 6, 1, 24, "");
		sizeY = config.getInt("sizeY", CATEGORY_GENERAL + ".ingot", 8, 1, 24, "");
		sizeZ = config.getInt("sizeZ", CATEGORY_GENERAL + ".ingot", 2, 1, 24, "");
		allowedIngots = Arrays.stream(config.getStringList("allowedIngots", CATEGORY_GENERAL + ".ingot", new String[] {}, "Items that are allowed to be added to the ingot block as well. (Notation: MODID:ITEMNAME)")).map(ResourceLocation::new).collect(Collectors.toSet());
		maxPileHeightI = config.getInt("maxPileHeight", CATEGORY_GENERAL + ".ingot", 25, 1, 200, "Maximum pile height (in blocks)");
		itemsPerItemA = config.getInt("itemsPerItem", CATEGORY_GENERAL + ".any", 4, 1, 64, "Items per visual item");
		size = config.getInt("size", CATEGORY_GENERAL + ".any", 4, 1, 24, "");
		maxPileHeightA = config.getInt("maxPileHeight", CATEGORY_GENERAL + ".any", 25, 1, 200, "Maximum pile height (in blocks)");

		useBlockTexture = config.getBoolean("useBlockTexture", CATEGORY_CLIENT + ".ingot", true, "Use textures from blocks for ingots (e.g. iron block texture for iron ingot).");
		useCompressedTexture = config.getBoolean("useCompressedTexture", CATEGORY_CLIENT + ".ingot", true, "Use compressed textures.");
		overlay = config.getInt("overlay", CATEGORY_CLIENT, 1, 0, 2, "0 - Overlay not visible" + NEW_LINE + "1 - Overlay visible while sneaking" + NEW_LINE + "2 - Overlay always visible");

		if (config.hasChanged())
			config.save();
		generateConstants();
		snw = new SimpleNetworkWrapper(MODID);
		snw.registerMessage(MessageConfigSync.class, MessageConfigSync.class, 0, Side.CLIENT);
		snw.registerMessage(MessageKey.class, MessageKey.class, 1, Side.SERVER);
		snw.registerMessage(MessageTOPTime.class, MessageTOPTime.class, 2, Side.CLIENT);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			ClientUtils.init();
		}
		if (Loader.isModLoaded("theoneprobe"))
			FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", TOPPlugin.class.getName());
		if (Loader.isModLoaded("waila"))
			FMLInterModComms.sendFunctionMessage("waila", "register", WailaPlugin.class.getName() + ".register");
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@SubscribeEvent
	public static void register(@SuppressWarnings("rawtypes") RegistryEvent.Register event) {
		if (event.getGenericType() == Block.class) {
			event.getRegistry().register(ingots);
			GameRegistry.registerTileEntity(TileIngotPile.class, ingots.getRegistryName().toString());
			event.getRegistry().register(any);
			GameRegistry.registerTileEntity(TileAnyPile.class, any.getRegistryName().toString());
		}
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP /*&& event.getEntity().getServer().isDedicatedServer()*/) {
			MessageConfigSync p = new MessageConfigSync();
			p.nbt.setInteger("iii", Stackable.itemsPerItemI);
			p.nbt.setInteger("x", Stackable.sizeX);
			p.nbt.setInteger("y", Stackable.sizeY);
			p.nbt.setInteger("z", Stackable.sizeZ);
			p.nbt.setInteger("iia", Stackable.itemsPerItemA);
			p.nbt.setInteger("s", Stackable.size);
			snw.sendTo(p, (EntityPlayerMP) event.getEntity());
		}
	}

	public static void generateConstants() {
		TileIngotPile.maxIngotAmount = Stackable.sizeX * Stackable.sizeY * Stackable.sizeZ;
		TileIngotPile.coordMap = ImmutableBiMap.<Integer, Vec3i> builder().putAll(Stream.of((Object) null).flatMap(n -> {
			List<Pair<Integer, Vec3i>> l = new ArrayList<>();
			int count = 0;
			for (int y = 0; y < Stackable.sizeY; y++) {
				for (int z = 0; z < Stackable.sizeZ; z++) {
					for (int x = 0; x < Stackable.sizeX; x++) {
						l.add(Pair.of(count, new Vec3i(x, y, z)));
						count++;
					}
				}
			}
			return l.stream();
		}).collect(Collectors.toList())).build();
		TileAnyPile.maxItemAmount = Stackable.size * Stackable.size * Stackable.size;
		TileAnyPile.coordMap = ImmutableBiMap.<Integer, Vec3i> builder().putAll(Stream.of((Object) null).flatMap(n -> {
			List<Pair<Integer, Vec3i>> l = new ArrayList<>();
			int count = 0;
			for (int y = 0; y < Stackable.size; y++) {
				for (int z = 0; z < Stackable.size; z++) {
					for (int x = 0; x < Stackable.size; x++) {
						l.add(Pair.of(count, new Vec3i(x, y, z)));
						count++;
					}
				}
			}
			return l.stream();
		}).collect(Collectors.toList())).build();
	}

}
