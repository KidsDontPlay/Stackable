package mrriegel.stackable;

import java.util.stream.IntStream;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

@EventBusSubscriber(modid = Stackable.MODID)
public class BlockIngots extends Block {
	public static final IUnlistedProperty<TileIngots> prop = new IUnlistedProperty<TileIngots>() {

		@Override
		public String getName() {
			return "tile";
		}

		@Override
		public boolean isValid(TileIngots value) {
			return true;
		}

		@Override
		public Class<TileIngots> getType() {
			return TileIngots.class;
		}

		@Override
		public String valueToString(TileIngots value) {
			return value.toString();
		}
	};

	public BlockIngots() {
		super(Material.IRON);
		setRegistryName("ingots");
		setUnlocalizedName(getRegistryName().toString());
		setHardness(4.5f);
		setDefaultState(((IExtendedBlockState) getDefaultState()).withProperty(prop, null));
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileIngots();
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileEntity t = source.getTileEntity(pos);
		if (t instanceof TileIngots) {
			return ((TileIngots) t).getBox();
		} else
			return FULL_BLOCK_AABB;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ((IExtendedBlockState) state).withProperty(prop, (TileIngots) world.getTileEntity(pos));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[] {}, new IUnlistedProperty[] { prop });
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		} else {
			TileEntity tile = worldIn.getTileEntity(pos);
			if (tile instanceof TileIngots) {
				playerIn.setItemStackToSlot(hand == EnumHand.MAIN_HAND ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, ItemHandlerHelper.insertItemStacked(tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null), playerIn.getHeldItem(hand), false));
				return true;
			}
			return false;
		}
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (t instanceof TileIngots) {
			TileIngots tile = (TileIngots) t;
			IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			IntStream.range(0, handler.getSlots()).mapToObj(handler::getStackInSlot).forEach(s -> {
				spawnAsEntity(worldIn, pos, s.copy());
			});
		}
		worldIn.removeTileEntity(pos);
	}

	@SubscribeEvent
	public static void leftclick(LeftClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		TileEntity tile = event.getWorld().getTileEntity(event.getPos());
		if (tile instanceof TileIngots && !(player.getHeldItemMainhand().getItem() instanceof ItemTool)) {
			IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			if (player.world.isRemote) {
				event.setCanceled(true);
				event.setResult(Result.DENY);
				event.setUseBlock(Result.DENY);
				return;
			} else {
				for (int i = handler.getSlots() - 1; i >= 0; i--) {
					ItemStack s = handler.getStackInSlot(i);
					if (!(s = handler.extractItem(i, 64, false)).isEmpty()) {
						EntityItem ei = new EntityItem(player.world, event.getPos().offset(event.getFace()).getX() + .5, event.getPos().getY() + .3, event.getPos().offset(event.getFace()).getZ() + .5, s);
						player.world.spawnEntity(ei);
						if (ItemHandlerHelper.insertItem(new PlayerMainInvWrapper(player.inventory), ei.getItem(), true).isEmpty()) {
							Vec3d vec = new Vec3d(player.posX - ei.posX, player.posY + .5 - ei.posY, player.posZ - ei.posZ).normalize().scale(1.5);
							ei.motionX = vec.x;
							ei.motionY = vec.y;
							ei.motionZ = vec.z;
						}
						event.setCanceled(true);
						event.setResult(Result.DENY);
						event.setUseBlock(Result.DENY);
						return;
					}
				}
			}
		}
	}

}
