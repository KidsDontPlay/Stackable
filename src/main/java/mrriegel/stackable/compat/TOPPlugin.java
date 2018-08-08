package mrriegel.stackable.compat;

import com.google.common.base.Function;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.message.MessageTOPTime;
import mrriegel.stackable.tile.TileStackable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

public class TOPPlugin implements Function<ITheOneProbe, Void> {

	private static final ItemStack barrier = new ItemStack(Blocks.BARRIER);

	@Override
	public Void apply(ITheOneProbe t) {
		t.registerBlockDisplayOverride(new IBlockDisplayOverride() {

			@Override
			public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
				BlockPos pos = data.getPos();
				if (pos != null) {
					TileEntity tile = world.getTileEntity(pos);
					if (!(tile instanceof TileStackable))
						return false;
					TileStackable t = (TileStackable) tile;
					ItemStack s = t.lookingStack(player);
					Stackable.snw.sendTo(new MessageTOPTime(), (EntityPlayerMP) player);
					boolean empty;
					s = (empty = s.isEmpty()) ? barrier : ItemHandlerHelper.copyStackWithSize(s, 1);
					probeInfo = probeInfo.horizontal().item(s).vertical().text(blockState.getBlock().getLocalizedName());
					if (!empty)
						probeInfo = probeInfo.text(TextFormatting.YELLOW + TileStackable.getOverlayText(s, t));
					probeInfo.text(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + Stackable.NAME);
				}
				return true;
			}
		});
		return null;
	}

}