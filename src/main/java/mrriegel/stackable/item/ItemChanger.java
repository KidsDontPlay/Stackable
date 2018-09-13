package mrriegel.stackable.item;

import java.util.List;

import mrriegel.stackable.PileInventory;
import mrriegel.stackable.Stackable;
import mrriegel.stackable.tile.TilePile;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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
		Mode m = getMode(stack);
		tooltip.add(TextFormatting.AQUA + m.name + TextFormatting.GRAY + " - " + TextFormatting.BLUE + m.tooltip);
		//		tooltip.add(TextFormatting.BLUE + p.tooltip);
		int foo = addAndGet(stack, 0);
		tooltip.add("Number: " + (foo == -1 ? TextFormatting.ITALIC + TextFormatting.RED.toString() + "REMOVE" : foo) + TextFormatting.RESET + TextFormatting.GRAY + " (scroll to change)");
	}

	public Mode getMode(ItemStack stack) {
		NBTTagCompound nbt = null;
		if ((nbt = stack.getTagCompound()) == null) {
			nbt = new NBTTagCompound();
			nbt.setByte("m0de", (byte) 0);
			stack.setTagCompound(nbt);
		}
		return Mode.VALUES[nbt.getByte("m0de") % Mode.VALUES.length];
	}

	public Mode incMode(ItemStack stack) {
		Mode m = getMode(stack);
		m = Mode.VALUES[(m.ordinal() + 1) % Mode.VALUES.length];
		stack.getTagCompound().setByte("m0de", (byte) m.ordinal());
		return m;
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
			Mode m = incMode(stack);
			playerIn.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "Mode: " + m.name), true);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		if (world.isRemote)
			return EnumActionResult.SUCCESS;
		ItemStack stack = player.getHeldItem(hand);
		if (Stackable.changer.getMode(stack) == Mode.MOVE && stack.getTagCompound().hasKey("p0s")) {
			BlockPos originPos = BlockPos.fromLong(stack.getTagCompound().getLong("p0s"));
			if (world.provider.getDimension() == stack.getTagCompound().getInteger("d1m")) {
				TileEntity t = world.getTileEntity(originPos);
				if (t instanceof TilePile) {
					TilePile tile = (TilePile) t;
					Block block = tile.getBlockType();
					if (tile.isMaster) {
						List<TilePile> tiles = tile.getAllPileBlocks();
						BlockPos neu = pos.offset(side);
						boolean noSpace = false;
						for (int i = 0; i < tiles.size(); i++) {
							if (!world.isAirBlock(neu.up(i))) {
								noSpace = true;
								break;
							}
						}
						if (!noSpace) {
							if (world.setBlockState(neu, block.getDefaultState(), 2)) {
								stack.getTagCompound().removeTag("p0s");
								stack.getTagCompound().removeTag("d1m");
								PileInventory pi = tile.inv;
								pi.items = null;
								tile = (TilePile) world.getTileEntity(neu);
								tile.isMaster = true;
								world.notifyNeighborsOfStateChange(neu, block, true);
								for (ItemStack s : pi.getItems()) {
									ItemStack rest = tile.inv.insertItem(s, false);
									if (!rest.isEmpty()) {
										world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ));
									}
								}
								for (TilePile tp : tiles)
									world.removeTileEntity(tp.getPos());
								for (TilePile tp : tiles)
									world.setBlockToAir(tp.getPos());
								return EnumActionResult.SUCCESS;

							} else {
								player.sendStatusMessage(new TextComponentString("Could not place pile here."), false);
							}
						} else {
							player.sendStatusMessage(new TextComponentString("Not enough space."), false);
						}
					} else {
						player.sendStatusMessage(new TextComponentString("Something went wrong."), false);
					}
				} else {
					player.sendStatusMessage(new TextComponentString("Pile is gone."), false);
				}
			} else {
				player.sendStatusMessage(new TextComponentString("Only within same dimension."), false);
			}
		}
		return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return oldStack.getItem() != newStack.getItem();
	}

	public enum Mode {
		INFO("Info", "Shows properties"), //
		NOEMPTY("Persistence", "Toggles persistence."), //
		BLACKWHITE("Black/White", "Toggles whitelist"), //
		BLACKADD("Black+", "Adds items to blacklist"), //
		BLACKREMOVE("Black-", "Removes items from blacklist"), //
		WHITEADD("White+", "Adds items to whitelist"), //
		WHITEREMOVE("White-", "Removes items from whitelist"), //
		MIN("Minimum", "Sets minimum amount for an item."), //
		MAX("Maximum", "Sets maximum amount for an item."), //
		MOVE("Move", "Moves pile to another location.");

		String name, tooltip;

		private Mode(String name, String tooltip) {
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
			case MOVE:
				ItemStack stack = player.getHeldItemMainhand();
				NBTTagCompound nbt;
				if (stack.hasTagCompound()) {
					nbt = stack.getTagCompound();
				} else {
					nbt = new NBTTagCompound();
					stack.setTagCompound(nbt);
				}
				nbt.setLong("p0s", tile.getMaster().getPos().toLong());
				nbt.setInteger("d1m", tile.getMaster().getWorld().provider.getDimension());
				player.sendStatusMessage(new TextComponentString("Prepared to move the pile."), false);
				player.sendStatusMessage(new TextComponentString("Click on a block to move it there."), false);
				break;
			}
		}

		public static final Mode[] VALUES = values();

		private static ItemStack right(EntityPlayer player) {
			ItemStack right = null;
			ItemStack tmp = player.inventory.getStackInSlot(player.inventory.currentItem + 1);
			if (player.inventory.currentItem < 8 && !tmp.isEmpty())
				right = tmp.copy();
			return right;
		}
	}

}
