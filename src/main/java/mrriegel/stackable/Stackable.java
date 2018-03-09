package mrriegel.stackable;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

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

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		itemsPerIngot = config.getInt("itemsPerIngot", config.CATEGORY_GENERAL, 4, 1, 64, "Items per Ingot");
		perX = config.getInt("x", "ingotsPerBlock", 6, 1, 32, "");
		perY = config.getInt("y", "ingotsPerBlock", 8, 1, 32, "");
		perZ = config.getInt("z", "ingotsPerBlock", 2, 1, 32, "");
		if (config.hasChanged())
			config.save();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Mod.EventHandler
	public void serverStart(FMLServerStartingEvent event) {
	}

}
