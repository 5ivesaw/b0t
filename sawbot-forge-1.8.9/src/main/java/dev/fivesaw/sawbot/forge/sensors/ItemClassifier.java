package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.ItemCategory;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public final class ItemClassifier {
    private ItemClassifier() { }
    public static ItemCategory category(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return ItemCategory.EMPTY;
        Item item=stack.getItem();
        if(item==Items.iron_ingot)return ItemCategory.IRON;
        if(item==Items.gold_ingot)return ItemCategory.GOLD;
        if(item==Items.diamond)return ItemCategory.DIAMOND;
        if(item==Items.emerald)return ItemCategory.EMERALD;
        if(item instanceof ItemBlock){Block block=((ItemBlock)item).getBlock();if(block==Blocks.wool)return ItemCategory.WOOL;return ItemCategory.BLOCK;}
        if(item instanceof ItemSword)return ItemCategory.SWORD;
        if(item instanceof ItemPickaxe)return ItemCategory.PICKAXE;
        if(item instanceof ItemAxe)return ItemCategory.AXE;
        if(item instanceof ItemShears)return ItemCategory.SHEARS;
        if(item instanceof ItemArmor)return ItemCategory.ARMOUR;
        if(item instanceof ItemFood)return ItemCategory.FOOD;
        if(item instanceof ItemBow)return ItemCategory.BOW;
        if(item==Items.arrow||item==Items.snowball||item==Items.egg)return ItemCategory.PROJECTILE;
        if(item==Items.flint_and_steel||item==Items.fishing_rod||item==Items.ender_pearl||item==Items.bucket||item==Items.water_bucket||item==Items.lava_bucket)return ItemCategory.UTILITY;
        return ItemCategory.OTHER;
    }
    public static int durabilityClass(ItemStack stack){if(stack==null||!stack.isItemStackDamageable())return 0;int max=Math.max(1,stack.getMaxDamage());float remaining=1f-(stack.getItemDamage()/(float)max);if(remaining<=0.25f)return 1;if(remaining<=0.5f)return 2;if(remaining<=0.75f)return 3;return 4;}
    public static int enchantmentBits(ItemStack stack){return stack!=null&&stack.isItemEnchanted()?1:0;}
}
