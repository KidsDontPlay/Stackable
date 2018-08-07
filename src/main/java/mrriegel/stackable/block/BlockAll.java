package mrriegel.stackable.block;

import mrriegel.stackable.tile.TileAll;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockAll extends BlockStackable {

	public BlockAll() {
		super("all", Material.ROCK);
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileAll();
	}

}
