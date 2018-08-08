package mrriegel.stackable.compat;

import java.util.List;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.block.BlockAnyPile;
import mrriegel.stackable.block.BlockIngotPile;
import mrriegel.stackable.client.ClientUtils;
import mrriegel.stackable.tile.TileStackable;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

public class WailaPlugin {

	private static final ItemStack barrier = new ItemStack(Blocks.BARRIER);

	public static void register(IWailaRegistrar reg) {
		IWailaDataProvider provider = new IWailaDataProvider() {
			@Override
			public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
				ItemStack s = IWailaDataProvider.super.getWailaStack(accessor, config);
				return s.isEmpty() ? barrier : s;
			}

			@Override
			public List<String> getWailaHead(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
				if (accessor.getWorld().isRemote)
					ClientUtils.wailaTime = accessor.getWorld().getTotalWorldTime();
				tooltip.clear();
				tooltip.add(TextFormatting.WHITE + accessor.getBlock().getLocalizedName());
				return tooltip;
			}

			@Override
			public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
				ItemStack s = ((TileStackable) accessor.getTileEntity()).lookingStack(accessor.getPlayer());
				if (!s.isEmpty())
					tooltip.add(TextFormatting.YELLOW + TileStackable.getOverlayText(s, (TileStackable) accessor.getTileEntity()));
				return tooltip;
			}

			@Override
			public List<String> getWailaTail(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
				tooltip.clear();
				tooltip.add(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + Stackable.NAME);
				return tooltip;
			}

		};
		for (Class<?> c : new Class[] { BlockIngotPile.class, BlockAnyPile.class }) {
			reg.registerHeadProvider(provider, c);
			reg.registerBodyProvider(provider, c);
			reg.registerStackProvider(provider, c);
			reg.registerTailProvider(provider, c);
		}
	}

}
