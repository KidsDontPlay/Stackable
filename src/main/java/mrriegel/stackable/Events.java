package mrriegel.stackable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
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
				event.world.loadedTileEntityList.stream().filter(t -> t instanceof TileIngots && ((TileIngots) t).needSync).forEach(t -> {
					t.markDirty();
					((TileIngots) t).needSync = false;
					for (EntityPlayerMP player : event.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(t.getPos().add(-11, -11, -11), t.getPos().add(11, 11, 11)))) {
						if (player.ticksExisted > 20 || true) {
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

	private static Set<UUID> placed = new HashSet<>();

	//set new block
	@SubscribeEvent
	public static void rightclick(RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		if (player.isSneaking() && event.getFace() == EnumFacing.UP && (placed.contains(player.getUniqueID()) || TileIngots.validItem(event.getEntityPlayer().getHeldItemMainhand()))) {
			placed.remove(player.getUniqueID());
			if (event.getHand() == EnumHand.OFF_HAND) {
				event.setUseBlock(Result.DENY);
				event.setCanceled(true);
				return;
			}
			BlockPos newPos = event.getPos().offset(event.getFace());
			if (player.world.isAirBlock(newPos)) {
				if (!player.world.isRemote && event.getHand() == EnumHand.MAIN_HAND) {
					if (player.world.setBlockState(newPos, Stackable.ingots.getDefaultState(), 2)) {
						TileIngots t = (TileIngots) player.world.getTileEntity(newPos);
						t.isMaster = true;
						player.world.notifyNeighborsOfStateChange(newPos, t.getBlockType(), true);
						Stackable.ingots.onBlockActivated(player.world, newPos, Stackable.ingots.getDefaultState(), player, event.getHand(), event.getFace(), 0, 0, 0);
						placed.add(player.getUniqueID());
					}
				}
				event.setUseBlock(Result.DENY);
				event.setCanceled(true);
			}
		}
	}

	//take ingot
	@SubscribeEvent
	public static void leftclick(LeftClickBlock event) {
		BlockPos pos = event.getPos();
		TileEntity t = event.getWorld().getTileEntity(pos);
		EntityPlayer player = event.getEntityPlayer();
		ItemStack h = player.getHeldItemMainhand();
		if (t instanceof TileIngots && h.getItem().getToolClasses(h).isEmpty()) {
			event.setCanceled(true);
			event.setUseBlock(Result.DENY);
			event.setUseItem(Result.DENY);
			if (event.getWorld().isRemote)
				return;
			ItemStack target = ((TileIngots) t).lookingStack(player);
			if (target.isEmpty())
				return;
			ItemStack s = ((TileIngots) t).getMaster().inv.extractItem(target, player.isSneaking() ? 64 : 1, false);
			if (!s.isEmpty()) {
				Vec3d point = player.getPositionEyes(1F).add(player.getLookVec().scale(1.5));
				EntityItem ei = new EntityItem(t.getWorld(), point.x, point.y, point.z, s);
				AxisAlignedBB aabb = ((TileIngots) t).lookingPos(player).getRight();
				if (aabb != null) {
					aabb = aabb.offset(t.getPos());
					Vec3d center = aabb.getCenter();
					ei.setPosition(center.x, aabb.minY, center.z);
				} else
					ei.setPosition(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
				t.getWorld().spawnEntity(ei);
				ei.onCollideWithPlayer(player);
			}
		}
	}

}
