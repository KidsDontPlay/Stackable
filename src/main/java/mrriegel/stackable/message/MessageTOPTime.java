package mrriegel.stackable.message;

import io.netty.buffer.ByteBuf;
import mrriegel.stackable.client.ClientUtils;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageTOPTime implements IMessage, IMessageHandler<MessageTOPTime, IMessage> {

	@Override
	public IMessage onMessage(MessageTOPTime message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			ClientUtils.topTime = FMLClientHandler.instance().getWorldClient().getTotalWorldTime();
		});
		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

}
