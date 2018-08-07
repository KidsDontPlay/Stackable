package mrriegel.stackable.block;

import mrriegel.stackable.tile.TileAnyPile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockAnyPile extends BlockPile {

	public BlockAnyPile() {
		super("any_pile", Material.ROCK);
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileAnyPile();
	}

}
