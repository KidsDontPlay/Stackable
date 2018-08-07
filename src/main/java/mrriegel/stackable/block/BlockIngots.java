package mrriegel.stackable.block;

import mrriegel.stackable.tile.TileIngots;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockIngots extends BlockStackable {

	public BlockIngots() {
		super("ingots", Material.IRON);
		setSoundType(SoundType.METAL);
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileIngots();
	}

}
