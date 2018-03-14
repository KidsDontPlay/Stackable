package mrriegel.stackable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(modid = Stackable.MODID, name = Stackable.NAME, version = Stackable.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
@EventBusSubscriber
public class Stackable {

	@Instance(Stackable.MODID)
	public static Stackable INSTANCE;

	public static final String VERSION = "1.0.0";
	public static final String NAME = "Stackable";
	public static final String MODID = "stackable";

	//config
	public static Configuration config;

	public static int itemsPerIngot, perX, perY, perZ;
	public static boolean useBlockTexture;

	public static final Block ingots = new BlockIngots();

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		itemsPerIngot = config.getInt("itemsPerIngot", config.CATEGORY_GENERAL, 4, 1, 64, "Items per Ingot");
		perX = config.getInt("x", "ingotsPerBlock", 6, 1, 32, "");
		perY = config.getInt("y", "ingotsPerBlock", 8, 1, 32, "");
		perZ = config.getInt("z", "ingotsPerBlock", 2, 1, 32, "");
		useBlockTexture = config.getBoolean("useBlockTexture", config.CATEGORY_CLIENT, true, "Use textures from blocks for ingots (e.g. iron block texture for iron ingot).");
		if (config.hasChanged())
			config.save();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			ClientUtils.init();
		}
		Method m = ReflectionHelper.findMethod(EnumConnectionState.class, "registerPacket", "func_179245_a", EnumPacketDirection.class, Class.class);
		try {
			m.invoke(EnumConnectionState.PLAY, EnumPacketDirection.CLIENTBOUND, PacketConfigSync.class);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@SubscribeEvent
	public static void register(@SuppressWarnings("rawtypes") RegistryEvent.Register event) {
		if (event.getGenericType() == Block.class) {
			event.getRegistry().register(ingots);
			GameRegistry.registerTileEntity(TileIngots.class, ingots.getRegistryName().toString());
		}
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP && event.getEntity().getServer().isDedicatedServer()) {
			PacketConfigSync p = new PacketConfigSync();
			p.nbt.setInteger("a", Stackable.itemsPerIngot);
			p.nbt.setInteger("x", Stackable.perX);
			p.nbt.setInteger("y", Stackable.perY);
			p.nbt.setInteger("z", Stackable.perZ);
			((EntityPlayerMP) event.getEntity()).connection.sendPacket(p);
		}
	}

}
