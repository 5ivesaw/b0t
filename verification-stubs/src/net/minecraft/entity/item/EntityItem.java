package net.minecraft.entity.item;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
public class EntityItem extends Entity {
    private ItemStack stack;
    public EntityItem(){this(null);}
    public EntityItem(ItemStack stack){this.stack=stack;}
    public ItemStack getEntityItem(){return stack;}
    public void setEntityItemForTest(ItemStack value){stack=value;}
}
