package mrriegel.stackable.item;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
		tooltip.add(getProperty(stack).toString());
		int foo = addAndGet(stack, 0);
		tooltip.add("Num: " + (foo == -1 ? TextFormatting.ITALIC + "remove" : foo));
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
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return super.initCapabilities(stack, nbt);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (!worldIn.isRemote) {
			ItemStack stack = playerIn.getHeldItem(handIn);
			Property p = incProperty(stack);
			playerIn.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + p.name() + " new"), true);
			for (IRecipe r : ForgeRegistries.RECIPES) {
				if (r.getRecipeOutput().getItem() == Items.STICK) {
					System.out.println(r.getClass());
					System.out.println(r.getIngredients());
					break;
				}
			}
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	public enum Property {
		INFO, NOEMPTY, BLACKADD, BLACKREMOVE, WHITEADD, WHITEREMOVE;

		public static final Property[] VALUES = values().clone();
	}

}
