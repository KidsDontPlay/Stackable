package mrriegel.stackable.item;

import java.util.List;

import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class ItemChanger extends Item {

	public ItemChanger() {
		setRegistryName("changer");
		setUnlocalizedName(getRegistryName().toString());
		setMaxStackSize(1);
		setCreativeTab(CreativeTabs.MISC);
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		tooltip.add("Right-click to change mode.");
		Property p = getProperty(stack);
		tooltip.add(TextFormatting.AQUA + p.name + TextFormatting.GRAY + " - " + TextFormatting.BLUE + p.tooltip);
		//		tooltip.add(TextFormatting.BLUE + p.tooltip);
		int foo = addAndGet(stack, 0);
		tooltip.add("Number: " + (foo == -1 ? TextFormatting.ITALIC + TextFormatting.RED.toString() + "REMOVE" : foo) + TextFormatting.RESET + TextFormatting.GRAY + " (scroll to change)");
	}

	public Property getProperty(ItemStack stack) {
		NBTTagCompound nbt = null;
		if ((nbt = stack.getTagCompound()) == null) {
			nbt = new NBTTagCompound();
			nbt.setByte("pr0p", (byte) 0);
			stack.setTagCompound(nbt);
		}
		return Property.VALUES[nbt.getByte("pr0p") % Property.VALUES.length];
	}

	public Property incProperty(ItemStack stack) {
		Property p = getProperty(stack);
		p = Property.VALUES[(p.ordinal() + 1) % Property.VALUES.length];
		stack.getTagCompound().setByte("pr0p", (byte) p.ordinal());
		return p;
	}

	public int addAndGet(ItemStack stack, int num) {
		NBTTagCompound nbt = null;
		if ((nbt = stack.getTagCompound()) == null) {
			nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
		}
		int nn = nbt.getInteger("num_");
		if (num == 0)
			return nn;
		nn = MathHelper.clamp(nn + num, -1, 1_000_000);
		nbt.setInteger("num_", nn);
		return nn;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (!worldIn.isRemote) {
			ItemStack stack = playerIn.getHeldItem(handIn);
			Property p = incProperty(stack);
			playerIn.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "Mode: " + p.name), true);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return oldStack.getItem() != newStack.getItem();
	}

	public enum Property {
		INFO("Info", "Shows properties"), //
		NOEMPTY("Persistence", "Toggles persistence."), //
		BLACKWHITE("Black/White", "Toggles whitelist"), //
		BLACKADD("Black+", "Adds items to blacklist"), //
		BLACKREMOVE("Black-", "Removes items from blacklist"), //
		WHITEADD("White+", "Adds items to whitelist"), //
		WHITEREMOVE("White-", "Removes items from whitelist"), //
		MIN("Minimum", "Sets minimum amount for an item."), //
		MAX("Maximum", "Sets maximum amount for an item.");

		String name, tooltip;

		private Property(String name, String tooltip) {
			this.name = name;
			this.tooltip = tooltip;
		}

		public void action(TilePile tile, EntityPlayer player) {
			ItemStack right = right(player);
			switch (this) {
			case BLACKADD:
				if (right != null) {
					if (tile.getMaster().blacklist.add(right))
						player.sendStatusMessage(new TextComponentString("Added " + right.getDisplayName() + " to blacklist."), false);
					else
						;
				}
				break;
			case BLACKREMOVE:
				if (right != null) {
					if (tile.getMaster().blacklist.remove(right))
						player.sendStatusMessage(new TextComponentString("Removed " + right.getDisplayName() + " from blacklist."), false);
					else
						;
				}
				break;
			case BLACKWHITE:
				boolean neuB = tile.getMaster().useWhitelist ^= true;
				player.sendStatusMessage(new TextComponentString("Whitelist: " + neuB), false);
				break;
			case INFO:
				for (String s : tile.getMaster().getProperties())
					player.sendStatusMessage(new TextComponentString(s), false);
				break;
			case MIN:
				if (right != null) {
					int num = Stackable.changer.addAndGet(player.getHeldItemMainhand(), 0);
					if (num != -1) {
						tile.getMaster().min.put(right, num);
						player.sendStatusMessage(new TextComponentString("Set minimum for " + right.getDisplayName() + " to " + num + "."), false);
					} else {
						boolean contain = tile.getMaster().min.containsKey(right);
						if (contain) {
							tile.getMaster().min.removeInt(right);
							player.sendStatusMessage(new TextComponentString("Removed minimum for " + right.getDisplayName() + "."), false);
						}
					}
				}
				break;
			case MAX:
				if (right != null) {
					int num = Stackable.changer.addAndGet(player.getHeldItemMainhand(), 0);
					if (num != -1) {
						tile.getMaster().max.put(right, num);
						player.sendStatusMessage(new TextComponentString("Set maximum for " + right.getDisplayName() + " to " + num + "."), false);
					} else {
						boolean contain = tile.getMaster().max.containsKey(right);
						if (contain) {
							tile.getMaster().max.removeInt(right);
							player.sendStatusMessage(new TextComponentString("Removed maximum for " + right.getDisplayName() + "."), false);
						}
					}
				}
				break;
			case NOEMPTY:
				boolean neuP = tile.getMaster().persistent ^= true;
				player.sendStatusMessage(new TextComponentString("Persistence: " + neuP), false);
				break;
			case WHITEADD:
				if (right != null) {
					if (tile.getMaster().whitelist.add(right))
						player.sendStatusMessage(new TextComponentString("Added " + right.getDisplayName() + " to whitelist."), false);
					else
						;
				}
				break;
			case WHITEREMOVE:
				if (right != null) {
					if (tile.getMaster().whitelist.remove(right))
						player.sendStatusMessage(new TextComponentString("Removed " + right.getDisplayName() + " from whitelist."), false);
					else
						;
				}
				break;
			}
		}

		public static final Property[] VALUES = values().clone();

		private static ItemStack right(EntityPlayer player) {
			ItemStack right = null;
			ItemStack tmp = player.inventory.getStackInSlot(player.inventory.currentItem + 1);
			if (player.inventory.currentItem < 8 && !tmp.isEmpty())
				right = tmp.copy();
			return right;
		}
	}

}
