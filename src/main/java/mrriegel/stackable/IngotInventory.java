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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class IngotInventory implements INBTSerializable<NBTTagCompound>, IItemHandler {

	private final TileIngots tile;
	public final Object2IntLinkedOpenCustomHashMap<ItemStack> inventory = new Object2IntLinkedOpenCustomHashMap<>(TileIngots.strategy);
	List<ItemStack> items = null;
	boolean threadStarted = false;

	public IngotInventory(TileIngots tile) {
		this.tile = tile;
	}

	public ItemStack extractItem(ItemStack stack, int amount, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		int i = inventory.getInt(stack);
		i = Math.min(amount, i);
		if (i <= 0)
			return ItemStack.EMPTY;
		if (!simulate) {
			inventory.addTo(stack, -i);
			if (inventory.getInt(stack) == 0) {
				inventory.removeInt(stack);
				if (inventory.isEmpty() && !tile.getWorld().isRemote)
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
		boolean noSpace = false;
		while (canInsert <= stack.getCount() && !noSpace) {
			List<TileIngots> l = tile.getAllIngotBlocks();
			TileIngots last = l.get(l.size() - 1);
			if (tile.getWorld().isAirBlock(last.getPos().up())) {
				tile.getWorld().setBlockState(last.getPos().up(), Stackable.ingots.getDefaultState());
				TileIngots n = (TileIngots) tile.getWorld().getTileEntity(last.getPos().up());
				n.masterPos = tile.getPos();
				canInsert = freeItems(stack);
			} else
				noSpace = true;
		}
		if (!simulate && canInsert > 0) {
			inventory.addTo(stack, Math.min(stack.getCount(), canInsert));
			onChange();
		}
		return canInsert >= stack.getCount() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - canInsert);
	}

	private void onChange() {
		if (tile.getWorld().isRemote)
			return;
		for (TileIngots t : tile.getAllIngotBlocks()) {
			t.needSync = true;
			t.markDirty();
			t.box = null;
			t.positions = null;
			t.ingots = null;
			t.raytrace = null;
			if (!t.isMaster && t.ingotList().stream().allMatch(ItemStack::isEmpty))
				t.getWorld().setBlockToAir(t.getPos());
		}
		if (!threadStarted) {
			threadStarted = true;
			new Thread(() -> tile.getWorld().getMinecraftServer().addScheduledTask(() -> {
				items = null;
				threadStarted = false;
			})).start();
		}
	}

	private int freeItems(ItemStack stack) {
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
						free = value % max;
						break;
					}
				}
			} else {
				occuIngots += Math.ceil(e.getIntValue() / (double) (Math.min(e.getKey().getMaxStackSize(), Stackable.itemsPerIngot)));
			}
		}
		int freeIngots = TileIngots.maxIngotAmount * (tile.getAllIngotBlocks().size()) - occuIngots;
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
		NBTTagList list1 = compound.getTagList("list1", 10);
		int[] list2 = compound.getIntArray("list2");
		Validate.isTrue(list1.tagCount() == list2.length);
		inventory.clear();
		for (int i = 0; i < list1.tagCount(); i++)
			inventory.put(new ItemStack(list1.getCompoundTagAt(i)), list2[i]);
	}

	@Override
	public int getSlots() {
		return getItems().size() + 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		List<ItemStack> l = getItems();
		return slot >= 0 && slot < l.size() ? l.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return insertItem(stack, simulate);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return extractItem(getStackInSlot(slot), amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	public List<ItemStack> getItems() {
		if (items != null)
			return items;
		items = new ArrayList<>();
		for (Object2IntMap.Entry<ItemStack> e : inventory.object2IntEntrySet()) {
			int value = e.getIntValue();
			int max = e.getKey().getMaxStackSize();
			while (value > 0) {
				int f = Math.min(max, value);
				items.add(ItemHandlerHelper.copyStackWithSize(e.getKey(), f));
				value -= f;
			}
		}
		return items;
	}

}
