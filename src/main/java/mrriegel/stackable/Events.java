package mrriegel.stackable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mrriegel.stackable.tile.TilePile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
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
		if (event.phase == Phase.END && !event.world.isRemote) {
			try {
				event.world.loadedTileEntityList.stream().filter(t -> t instanceof TilePile && ((TilePile) t).isMaster).forEach(t -> {
					if (!((TilePile) t).needSync && (event.world.getTotalWorldTime() + t.getPos().hashCode()) % 100 != 0)
						return;
					((TilePile) t).needSync = false;
					Set<EntityPlayerMP> players = new HashSet<>();
					List<TilePile> tiles = ((TilePile) t).getAllPileBlocks();
					for (TilePile tile : tiles)
						players.addAll(event.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(tile.getPos().add(-11, -11, -11), tile.getPos().add(11, 11, 11))));
					for (TilePile tile : tiles) {
						tile.markDirty();
						for (EntityPlayerMP player : players) {
							SPacketUpdateTileEntity p = tile.getUpdatePacket();
							if (p != null)
								player.connection.sendPacket(p);
						}
					}
				});
			} catch (Exception e) {
			}
		}
	}

	//take ingot
	@SubscribeEvent
	public static void leftclick(LeftClickBlock event) {
		BlockPos pos = event.getPos();
		TileEntity t = event.getWorld().getTileEntity(pos);
		EntityPlayer player = event.getEntityPlayer();
		long time = System.currentTimeMillis();
		if (t instanceof TilePile && !TilePile.canPlayerBreak(player)) {
			event.setCanceled(true);
			event.setUseBlock(Result.DENY);
			event.setUseItem(Result.DENY);
			if (event.getWorld().isRemote || ((TilePile) t).lastTake >= time - 150L)
				return;
			ItemStack target = ((TilePile) t).lookingStack(player);
			if (target.isEmpty())
				return;
			ItemStack s = ((TilePile) t).getMaster().inv.extractItem(target, player.isSneaking() ? 64 : 1, false);
			if (!s.isEmpty()) {
				((TilePile) t).lastTake = time;
				EntityItem ei = new EntityItem(t.getWorld(), 0, 0, 0, s);
				AxisAlignedBB aabb = ((TilePile) t).lookingPos(player).getRight();
				if (aabb != null) {
					aabb = aabb.offset(t.getPos());
					ei.setPosition(aabb.minX, aabb.minY, aabb.minZ);
				} else
					ei.setPosition(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
				if (t.getWorld().spawnEntity(ei)) {
					ei.onCollideWithPlayer(player);
				}
			}
		}
	}

}
