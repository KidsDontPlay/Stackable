package mrriegel.stackable.block;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mrriegel.stackable.client.ClientUtils;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class BlockPile extends Block {
	public static final IBlockState DAMAGE = new Block(Material.AIR).getDefaultState();
	public static final IUnlistedProperty<TileEntity> TILE_PROP = new IUnlistedProperty<TileEntity>() {

		@Override
		public String getName() {
			return "tile";
		}

		@Override
		public boolean isValid(TileEntity value) {
			return true;
		}

		@Override
		public Class<TileEntity> getType() {
			return TileEntity.class;
		}

		@Override
		public String valueToString(TileEntity value) {
			return value.toString();
		}
	};
	public static final ObjectOpenHashSet<UUID> ctrlMap = new ObjectOpenHashSet<>();

	public BlockPile(String name, Material materialIn) {
		super(materialIn);
		setRegistryName(name);
		setUnlocalizedName(getRegistryName().toString());
		setHardness(6f);
		translucent = true;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		if (worldIn instanceof World ? ((World) worldIn).isRemote : FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			if (ClientUtils.brokenBlocks.containsKey(pos) && false) {
				return DAMAGE;
			}
		}
		return super.getActualState(state, worldIn, pos);
	}

	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
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
		return ((IExtendedBlockState) state).withProperty(TILE_PROP, world.getTileEntity(pos));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[] {}, new IUnlistedProperty[] { TILE_PROP });
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileEntity t = world.getTileEntity(pos);
		if (t instanceof TilePile)
			return ItemHandlerHelper.copyStackWithSize(((TilePile) t).lookingStack(player), 1);
		return ItemStack.EMPTY;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileEntity t = source.getTileEntity(pos);
		if (t instanceof TilePile)
			return ((TilePile) t).getBox();
		return FULL_BLOCK_AABB;
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (t instanceof TilePile) {
			for (AxisAlignedBB aabb : ((TilePile) t).itemBoxes())
				addCollisionBoxToList(pos, entityBox, collidingBoxes, aabb);
		} else
			super.addCollisionBoxToList(state, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState);
	}

	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (worldIn.isAirBlock(pos.down()) && t instanceof TilePile && !((TilePile) t).isMaster)
			worldIn.setBlockToAir(pos);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		} else {
			TileEntity t = worldIn.getTileEntity(pos);
			if (t instanceof TilePile && hand == EnumHand.MAIN_HAND && ((TilePile) t).validItem(playerIn.getHeldItem(hand))) {
				boolean ctrl = ctrlMap.contains(playerIn.getUniqueID());
				ItemStack toInsert = ctrl ? ItemHandlerHelper.copyStackWithSize(playerIn.getHeldItem(hand), 1) : playerIn.getHeldItem(hand);
				ItemStack rest = ((TilePile) t).getMaster().inv.insertItem(toInsert, false);
				int inserted = rest.isEmpty() ? toInsert.getCount() : toInsert.getCount() - rest.getCount();
				worldIn.playSound(null, pos, ((TilePile) t).placeSound(playerIn.getHeldItem(hand)), SoundCategory.BLOCKS, .3f, worldIn.rand.nextFloat() / 2f + .5f);
				if (!playerIn.capabilities.isCreativeMode)
					playerIn.getHeldItem(hand).shrink(inserted);
				return true;
			}
			return false;
		}
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity t = worldIn.getTileEntity(pos);
		if (t instanceof TilePile) {
			TilePile tile = (TilePile) t;
			if (tile.isMaster) {
				tile.inv.items = null;
				IntStream.range(0, tile.inv.getSlots()).forEach(i -> spawnAsEntity(worldIn, pos, tile.inv.getStackInSlot(i)));
				worldIn.removeTileEntity(pos);
			} else {
				if (tile.getMaster() != null) {
					List<TilePile> ts = tile.getAllPileBlocks();
					for (int i = ts.size() - 1; i >= 1; i--) {
						TilePile t2 = ts.get(i);
						for (ItemStack s : t2.itemList()) {
							spawnAsEntity(worldIn, pos, t2.getMaster().inv.extractItem(s, s.getCount(), false));
						}
						if (t2 == tile)
							break;
					}
				}
				worldIn.removeTileEntity(pos);
			}
		}
		worldIn.removeTileEntity(pos);
	}

}
