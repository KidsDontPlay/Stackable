package mrriegel.stackable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemHandlerHelper;

public class IngotInventory implements INBTSerializable<NBTTagCompound> {

	private final TileIngots tile;
	public final Object2IntLinkedOpenCustomHashMap<ItemStack> inventory = new Object2IntLinkedOpenCustomHashMap<>(TileIngots.strategy);
	List<Ingot> ingots = null;

	public IngotInventory(TileIngots tile) {
		this.tile = tile;
	}

	public ItemStack extractItem(ItemStack stack, int amount, boolean simulate) {
		int i = inventory.getInt(stack);
		if (i <= 0)
			return ItemStack.EMPTY;
		i = Math.min(amount, i);
		if (!simulate) {
			inventory.addTo(stack, -i);
			if (inventory.getInt(stack) == 0) {
				inventory.removeInt(stack);
				if (inventory.isEmpty())
					new Thread(() -> tile.getWorld().getMinecraftServer().addScheduledTask(() -> tile.getWorld().setBlockToAir(tile.getPos()))).start();
			}
			onChange();
		}
		return ItemHandlerHelper.copyStackWithSize(stack, i);
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		if (!TileIngots.validItem(stack))
			return stack;
		int canInsert = freeItems(stack);
		//		System.out.println(canInsert+" can");
		if (!simulate && canInsert > 0) {
			inventory.addTo(stack, Math.min(stack.getCount(), canInsert));
			onChange();
		}
		return canInsert >= stack.getCount() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - canInsert);
	}

	private void onChange() {
		ingots = null;
		tile.needSync = true;
		tile.markDirty();
		tile.box = null;
		tile.positions = null;
	}

//	public List<Ingot> getIngots() {
//		if (ingots != null)
//			return ingots;
//		ingots = new ArrayList<>();
//		for (Object2IntMap.Entry<ItemStack> e : inventory.object2IntEntrySet()) {
//			int max = Math.min(e.getKey().getMaxStackSize(), Stackable.itemsPerIngot);
//			int stacks = (int) Math.ceil(e.getIntValue() / (double) max);
//			for (int i = 0; i < stacks - 1; i++)
//				ingots.add(new Ingot(e.getKey(), max));
//			int lastSize = e.getIntValue() % max;
//			if (lastSize > 0)
//				ingots.add(new Ingot(e.getKey(), lastSize));
//		}
//		return ingots;
//	}

	int freeItems(ItemStack stack) {
		int max = Math.min(stack.getMaxStackSize(), Stackable.itemsPerIngot);
		int free = 0;
		int occuIngots = 0;
		for (Object2IntMap.Entry<ItemStack> e : inventory.object2IntEntrySet()) {
			if (TileIngots.strategy.equals(e.getKey(), stack)) {
				int value = e.getIntValue();
				while (value > 0) {
					if (value > max) {
						occuIngots++;
						value -= max;
					} else {
						occuIngots++;
						free = e.getIntValue() % max;
						break;
					}
				}
			} else {
				occuIngots += Math.ceil(e.getIntValue() / (double) (Math.min(e.getKey().getMaxStackSize(), Stackable.itemsPerIngot)));
			}
		}
		int freeIngots = tile.maxIngotAmount - occuIngots;
		free += max * freeIngots;
		return free;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList list1 = new NBTTagList();
		IntArrayList list2 = new IntArrayList();
		for (Object2IntMap.Entry<ItemStack> e : inventory.object2IntEntrySet()) {
			list1.appendTag(e.getKey().writeToNBT(new NBTTagCompound()));
			list2.add(e.getIntValue());
		}
		compound.setTag("list1", list1);
		compound.setIntArray("list2", list2.toIntArray());
		return compound;
	}

	@Override
	public void deserializeNBT(NBTTagCompound compound) {
		inventory.clear();
		NBTTagList list1 = compound.getTagList("list1", 10);
		int[] list2 = compound.getIntArray("list2");
		Validate.isTrue(list1.tagCount() == list2.length);
		inventory.clear();
		for (int i = 0; i < list1.tagCount(); i++)
			inventory.put(new ItemStack(list1.getCompoundTagAt(i)), list2[i]);
	}

	private static class Ingot {
		ItemStack stack;
		int amount;

		public Ingot(ItemStack stack, int amount) {
			this.stack = stack;
			this.amount = amount;
		}
	}

}
