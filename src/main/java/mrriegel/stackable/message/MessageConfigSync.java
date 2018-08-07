package mrriegel.stackable.message;

import io.netty.buffer.ByteBuf;
import mrriegel.stackable.Stackable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageConfigSync implements IMessage, IMessageHandler<MessageConfigSync, IMessage> {
	public NBTTagCompound nbt = new NBTTagCompound();

	@Override
	public IMessage onMessage(MessageConfigSync message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			nbt = message.nbt;
			Stackable.itemsPerItemI = nbt.getInteger("iii");
			Stackable.sizeX = nbt.getInteger("x");
			Stackable.sizeY = nbt.getInteger("y");
			Stackable.sizeZ = nbt.getInteger("z");
			Stackable.itemsPerItemA = nbt.getInteger("iia");
			Stackable.size = nbt.getInteger("s");
			Stackable.generateConstants();
		});
		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, nbt);
	}

}
