package mrriegel.stackable.compat;

import com.google.common.base.Function;

import mcjty.theoneprobe.api.ITheOneProbe;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.message.MessageTOPTime;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

public class TOPPlugin implements Function<ITheOneProbe, Void> {

	private static final ItemStack barrier = new ItemStack(Blocks.BARRIER);

	@Override
	public Void apply(ITheOneProbe t) {
		t.registerBlockDisplayOverride((mode, probeInfo, player, world, blockState, data) -> {
			BlockPos pos = data.getPos();
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TilePile))
				return false;
			TilePile tp = (TilePile) tile;
			ItemStack s = tp.lookingStack(player);
			Stackable.snw.sendTo(new MessageTOPTime(), (EntityPlayerMP) player);
			boolean empty;
			s = (empty = s.isEmpty()) ? barrier : ItemHandlerHelper.copyStackWithSize(s, 1);
			probeInfo = probeInfo.horizontal().item(s).vertical().text(blockState.getBlock().getLocalizedName());
			if (!empty)
				probeInfo = probeInfo.text(TextFormatting.YELLOW + TilePile.getOverlayText(s, tp));
			probeInfo.text(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + Stackable.NAME);
			return true;

		});
		return null;
	}

}