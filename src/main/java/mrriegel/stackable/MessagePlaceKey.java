package mrriegel.stackable;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePlaceKey implements IMessage, IMessageHandler<MessagePlaceKey, IMessage> {
	boolean down;

	public MessagePlaceKey() {
	}

	public MessagePlaceKey(boolean down) {
		super();
		this.down = down;
	}

	@Override
	public IMessage onMessage(MessagePlaceKey message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			Events.placeKeyDown = message.down;
		});
		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		down = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(down);
	}

}
