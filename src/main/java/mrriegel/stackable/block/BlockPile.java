package mrriegel.stackable.block;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class BlockPile extends Block {
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
	public static final ObjectOpenHashSet<UUID> ctrlSet = new ObjectOpenHashSet<>();

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
			if (t instanceof TilePile && hand == EnumHand.MAIN_HAND) {
				TilePile tile = (TilePile) t;
				ItemStack stack = playerIn.getHeldItem(hand);
				if (stack.getItem() == Stackable.changer) {
					Stackable.changer.getProperty(stack).action(tile, playerIn);
				} else if (tile.validItem(stack)) {
					boolean ctrl = ctrlSet.contains(playerIn.getUniqueID());
					ItemStack toInsert = ctrl ? ItemHandlerHelper.copyStackWithSize(stack, 1) : stack;
					ItemStack rest = tile.getMaster().inv.insertItem(toInsert, false);
					int inserted = rest.isEmpty() ? toInsert.getCount() : toInsert.getCount() - rest.getCount();
					worldIn.playSound(null, pos, tile.placeSound(stack), SoundCategory.BLOCKS, .3f, worldIn.rand.nextFloat() / 2f + .5f);
					if (!playerIn.capabilities.isCreativeMode)
						stack.shrink(inserted);
					return true;
				} else if (playerIn.getHeldItemMainhand().isEmpty()) {
					ItemStack looking = tile.lookingStack(playerIn);
					PlayerMainInvWrapper playerInv = new PlayerMainInvWrapper(playerIn.inventory);
					for (int i = 0; i < playerInv.getSlots(); i++) {
						ItemStack s = playerInv.getStackInSlot(i);
						if (s.isItemEqual(looking)) {
							ItemStack rest = tile.getMaster().inv.insertItem(playerInv.extractItem(i, 64, false), false);
							worldIn.playSound(null, pos, tile.placeSound(stack), SoundCategory.BLOCKS, .3f, worldIn.rand.nextFloat() / 2f + .5f);
							if (!rest.isEmpty()) {
								playerInv.insertItem(i, rest, false);
							}
						}
					}
					return true;
				}
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

	@Override
	public boolean addHitEffects(IBlockState state, World worldObj, RayTraceResult target, ParticleManager manager) {
		return true;
	}

	@Override
	public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
		return true;
	}

	@Override
	public boolean addLandingEffects(IBlockState state, WorldServer worldObj, BlockPos blockPosition, IBlockState iblockstate, EntityLivingBase entity, int numberOfParticles) {
		return true;
	}

	@Override
	public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
		return true;
	}
}
