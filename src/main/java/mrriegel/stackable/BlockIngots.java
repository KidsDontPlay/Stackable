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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

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
		setHardness(6f);
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
			if (tile instanceof TileIngots && hand == EnumHand.MAIN_HAND && TileIngots.validItem(playerIn.getHeldItemMainhand())) {
				ItemStack rest = ItemHandlerHelper.insertItemStacked(((TileIngots) tile).handler, playerIn.getHeldItem(hand), false);
				if (!playerIn.capabilities.isCreativeMode)
					playerIn.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, rest);
				return true;
			}
			return false;
		}
	}

	@Override
	public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
		TileEntity t;
		if (worldIn.isRemote || !((t = worldIn.getTileEntity(pos)) instanceof TileIngots) || playerIn.getHeldItemMainhand().getItem() instanceof ItemTool)
			return;
		RayTraceResult rtr=playerIn.rayTrace(playerIn.getAttributeMap().getAttributeInstance(EntityPlayer.REACH_DISTANCE).getAttributeValue(), 0);
		if(rtr==null)return;
		//		TileIngots tile = (TileIngots) t;
		IItemHandler handler = ((TileIngots) t).handler;
		for (int i = handler.getSlots() - 1; i >= 0; i--) {
			ItemStack s = handler.extractItem(i, playerIn.isSneaking() ? 64 : 1, false);
			if (!s.isEmpty()) {
				Vec3d point = playerIn.getPositionEyes(1F).add(playerIn.getLookVec().scale(1.5));
				EntityItem ei = new EntityItem(worldIn, point.x, point.y, point.z, s);
				worldIn.spawnEntity(ei);
				if (ItemHandlerHelper.insertItem(new PlayerMainInvWrapper(playerIn.inventory), ei.getItem(), true).isEmpty()) {
					Vec3d vec = new Vec3d(playerIn.posX - ei.posX, playerIn.posY + .5 - ei.posY, playerIn.posZ - ei.posZ).normalize().scale(1.5);
					ei.motionX = vec.x;
					ei.motionY = vec.y;
					ei.motionZ = vec.z;
				}
				return;
			}
		}
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		if (player.capabilities.isCreativeMode && !(player.getHeldItemMainhand().getItem() instanceof ItemTool)) {
			onBlockClicked(world, pos, player);
			return false;
		}
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (t instanceof TileIngots) {
			IItemHandler handler = ((TileIngots) t).handler;
			IntStream.range(0, handler.getSlots()).forEach(i -> spawnAsEntity(worldIn, pos, handler.getStackInSlot(i)));
		}
		worldIn.removeTileEntity(pos);
	}

}
