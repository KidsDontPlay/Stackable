package mrriegel.stackable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
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
		if (event.phase == Phase.END && !event.world.isRemote) {
			try {
				event.world.loadedTileEntityList.stream().filter(t -> t instanceof TileStackable && ((TileStackable) t).isMaster).forEach(t -> {
					if (!((TileStackable) t).needSync && (event.world.getTotalWorldTime() + t.getPos().hashCode()) % 100 != 0)
						return;
					((TileStackable) t).needSync = false;
					Set<EntityPlayerMP> players = new HashSet<>();
					List<TileStackable> tiles = ((TileStackable) t).getAllPileBlocks();
					for (TileStackable tile : tiles)
						players.addAll(event.world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(tile.getPos().add(-11, -11, -11), tile.getPos().add(11, 11, 11))));
					for (TileStackable tile : tiles) {
						tile.markDirty();
						for (EntityPlayerMP player : players) {
							if (player.ticksExisted > 20 || true) {
								SPacketUpdateTileEntity p = tile.getUpdatePacket();
								if (p != null)
									player.connection.sendPacket(p);
							}
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
		ItemStack main = player.getHeldItemMainhand();
		Block block = TileIngots.validItem1(main) ? Stackable.ingots : !main.isEmpty() ? Stackable.all : null;
		if (player.isSneaking() && event.getFace() == EnumFacing.UP && (placed.contains(player.getUniqueID()) || block!=null)) {
			placed.remove(player.getUniqueID());
			if (event.getHand() == EnumHand.OFF_HAND) {
				event.setUseBlock(Result.DENY);
				event.setCanceled(true);
				return;
			}
			BlockPos newPos = event.getPos().offset(event.getFace());
			if (player.world.isAirBlock(newPos)) {
				if (!player.world.isRemote && event.getHand() == EnumHand.MAIN_HAND) {
					if (player.world.setBlockState(newPos, block.getDefaultState(), 2)) {
						TileStackable t = (TileStackable) player.world.getTileEntity(newPos);
						t.isMaster = true;
						player.world.notifyNeighborsOfStateChange(newPos, t.getBlockType(), true);
						block.onBlockActivated(player.world, newPos, block.getDefaultState(), player, event.getHand(), event.getFace(), 0, 0, 0);
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
		if (t instanceof TileStackable && !h.getItem().getToolClasses(h).contains("pickaxe")) {
			event.setCanceled(true);
			event.setUseBlock(Result.DENY);
			event.setUseItem(Result.DENY);
			if (event.getWorld().isRemote)
				return;
			ItemStack target = ((TileStackable) t).lookingStack(player);
			if (target.isEmpty())
				return;
			ItemStack s = ((TileStackable) t).getMaster().inv.extractItem(target, player.isSneaking() ? 64 : 1, false);
			if (!s.isEmpty()) {
				Vec3d point = player.getPositionEyes(1F).add(player.getLookVec().scale(1.5));
				EntityItem ei = new EntityItem(t.getWorld(), point.x, point.y, point.z, s);
				AxisAlignedBB aabb = ((TileStackable) t).lookingPos(player).getRight();
				if (aabb != null) {
					aabb = aabb.offset(t.getPos());
					ei.setPosition(aabb.minX, aabb.minY, aabb.minZ);
				} else
					ei.setPosition(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
				t.getWorld().spawnEntity(ei);
				ei.onCollideWithPlayer(player);
			}
		}
	}

}
