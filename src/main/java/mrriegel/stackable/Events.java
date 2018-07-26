package mrriegel.stackable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

@EventBusSubscriber(modid = Stackable.MODID)
public class Events {

	//Sync
	@SubscribeEvent
	public static void tick(WorldTickEvent event) {
		if (event.phase == Phase.END && !event.world.isRemote && event.world.getTotalWorldTime() % 3 == 0) {
			try {
				event.world.loadedTileEntityList.stream().filter(t -> t instanceof TileIngots).map(t -> (TileIngots) t).filter(t -> t.needSync && t.getWorld() == event.world).forEach(t -> {
					t.markDirty();
					for (EntityPlayerMP player : event.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(t.getPos().add(-11, -11, -11), t.getPos().add(11, 11, 11)))) {
						if (player.ticksExisted > 20) {
							t.needSync = false;
							Packet<?> p = t.getUpdatePacket();
							if (p != null)
								player.connection.sendPacket(p);
						}
					}
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	//set new block
	@SubscribeEvent
	public static void rightclick(RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		if (player.isSneaking() && event.getFace() == EnumFacing.UP && TileIngots.validItem(event.getEntityPlayer().getHeldItemMainhand())) {
			if (event.getHand() == EnumHand.OFF_HAND) {
				event.setUseBlock(Result.DENY);
				return;
			}
			BlockPos newPos = event.getPos().offset(event.getFace());
			if (player.world.isAirBlock(newPos)) {
				if (!player.world.isRemote && event.getHand() == EnumHand.MAIN_HAND) {
					player.world.setBlockState(newPos, Stackable.ingots.getDefaultState());
					TileIngots t = (TileIngots) player.world.getTileEntity(newPos);
					t.isMaster = true;
					t.getBlockType().onBlockActivated(t.getWorld(), t.getPos(), t.getWorld().getBlockState(t.getPos()), player, event.getHand(), event.getFace(), 0, 0, 0);
					//					player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemHandlerHelper.insertItemStacked(t.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null), event.getItemStack(), false));
				}
				event.setUseBlock(Result.DENY);
			}
		}
	}

}
