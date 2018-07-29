package mrriegel.stackable;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

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
	public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (t instanceof TileIngots) {
			for (AxisAlignedBB aabb : ((TileIngots) t).ingotBoxes())
				addCollisionBoxToList(pos, entityBox, collidingBoxes, aabb);
		} else
			super.addCollisionBoxToList(state, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState);
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
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (worldIn.isAirBlock(pos.down()) && t instanceof TileIngots && !((TileIngots) t).isMaster)
			worldIn.destroyBlock(pos, false);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		} else {
			TileEntity tile = worldIn.getTileEntity(pos);
			if (tile instanceof TileIngots && hand == EnumHand.MAIN_HAND && TileIngots.validItem(playerIn.getHeldItemMainhand())) {
				ItemStack rest = ((TileIngots) tile).getMaster().inv.insertItem(playerIn.getHeldItem(hand), false);
				if (!playerIn.capabilities.isCreativeMode)
					playerIn.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, rest);
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
			if (tile.isMaster) {
				IntStream.range(0, tile.inv.getSlots()).forEach(i -> spawnAsEntity(worldIn, pos, tile.inv.getStackInSlot(i)));
				worldIn.removeTileEntity(pos);
			} else {
				for(ItemStack s:tile.ingotList())
					spawnAsEntity(worldIn, pos, tile.getMaster().inv.extractItem(s, 64, false));
				worldIn.removeTileEntity(pos);
			}
		}
		worldIn.removeTileEntity(pos);
	}

}
