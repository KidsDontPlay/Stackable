package mrriegel.stackable;

import java.awt.Color;
import java.awt.image.BufferedImage;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Stackable.MODID)
public class ClientUtils {

	private static Object2IntMap<ItemStack> colors = new Object2IntOpenCustomHashMap<>(new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			return o == null ? 0 : (o.getItemDamage() + "" + o.getItem().getRegistryName()).hashCode();
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == null || b == null ? false : a.getItemDamage() == b.getItemDamage() && a.getItem() == b.getItem();
		}
	});

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

}
