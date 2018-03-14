package mrriegel.stackable;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketConfigSync implements Packet<INetHandlerPlayClient> {
	NBTTagCompound nbt = new NBTTagCompound();

	@Override
	public void readPacketData(PacketBuffer buf) throws IOException {
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void writePacketData(PacketBuffer buf) throws IOException {
		ByteBufUtils.writeTag(buf, nbt);
	}

	@Override
	public void processPacket(INetHandlerPlayClient handler) {
		FMLClientHandler.instance().getClient().addScheduledTask(() -> {
			Stackable.itemsPerIngot = nbt.getInteger("a");
			Stackable.perX = nbt.getInteger("x");
			Stackable.perY = nbt.getInteger("y");
			Stackable.perZ = nbt.getInteger("z");
		});
	}

}
