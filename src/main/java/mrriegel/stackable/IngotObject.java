package mrriegel.stackable;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class IngotObject {
	TileIngots tile;

	public IngotObject(TileIngots tile) {
		super();
		this.tile = tile;
	}

	public ItemStack lookingStack(EntityPlayer player) {
		Vec3i v = lookingPos(player).getLeft();
		if (v == null)
			return ItemStack.EMPTY;
		return tile.ingotList().get(TileIngots.coordMap.inverse().get(v));
	}

	public Pair<Vec3i, Pair<Vec3d, Vec3d>> lookingPos(EntityPlayer player) {
		double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
		Vec3d p1 = player.getPositionEyes(0);
		Vec3d look = player.getLook(1);
		Vec3d p2 = p1.add(look.scale(reach));
		Object2ObjectOpenHashMap<Pair<Vec3d, Vec3d>, Vec3i> hitMap = new Object2ObjectOpenHashMap<>();
		List<Pair<Vec3d, Vec3d>> l = tile.ingotPositions();
		for (int i = 0; i < l.size(); i++) {
			Pair<Vec3d, Vec3d> pp = l.get(i);
			AxisAlignedBB aabb = new AxisAlignedBB(pp.getLeft(), pp.getRight()).offset(tile.getPos());
			if (aabb.calculateIntercept(p1, p2) != null) {
				//							hits.add(pp);
				Vec3i v = TileIngots.coordMap.get(i);
				hitMap.put(pp, v);
			}
		}
		Pair<Vec3d, Vec3d> fin = null;
		for (Pair<Vec3d, Vec3d> pp : hitMap.keySet()) {
			if (fin == null || new AxisAlignedBB(pp.getLeft(), pp.getRight()).offset(tile.getPos()).getCenter().distanceTo(p1) < new AxisAlignedBB(fin.getLeft(), fin.getRight()).offset(tile.getPos()).getCenter().distanceTo(p1))
				fin = pp;
		}
		return Pair.of(hitMap.get(fin), fin);
	}

	public List<AxisAlignedBB> ingotBoxes() {

		return tile.ingotPositions().stream().map(p -> new AxisAlignedBB(p.getLeft().x, p.getLeft().y, p.getLeft().z, p.getRight().x, p.getRight().y, p.getRight().z)).collect(Collectors.toList());
	}

}
