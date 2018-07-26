package mrriegel.stackable;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageConfigSync implements IMessage, IMessageHandler<MessageConfigSync, IMessage> {
	NBTTagCompound nbt = new NBTTagCompound();

	@Override
	public IMessage onMessage(MessageConfigSync message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			nbt = message.nbt;
			Stackable.itemsPerIngot = nbt.getInteger("a");
			Stackable.perX = nbt.getInteger("x");
			Stackable.perY = nbt.getInteger("y");
			Stackable.perZ = nbt.getInteger("z");
			System.out.println("neggg");
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
