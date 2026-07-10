package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemCategory;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class InventorySensor {
    public InventorySnapshot capture(Minecraft minecraft,EntityPlayerSP player){
        List<ItemSlotObservation> slots=new ArrayList<ItemSlotObservation>(InventorySnapshot.EXPECTED_SLOT_COUNT);
        int iron=0,gold=0,diamonds=0,emeralds=0,wool=0;
        for(int i=0;i<36;i++){ItemStack stack=player.inventory.mainInventory[i];ItemSlotObservation slot=encode(i,stack);slots.add(slot);int count=slot.count();switch(slot.category()){case IRON:iron+=count;break;case GOLD:gold+=count;break;case DIAMOND:diamonds+=count;break;case EMERALD:emeralds+=count;break;case WOOL:wool+=count;break;default:break;}}
        for(int i=0;i<4;i++)slots.add(encode(36+i,player.inventory.armorInventory[i]));
        slots.add(encode(40,player.inventory.getItemStack()));
        String container=minecraft.currentScreen==null?"NONE":minecraft.currentScreen.getClass().getSimpleName();
        return new InventorySnapshot(slots,player.inventory.currentItem,container,iron,gold,diamonds,emeralds,wool);
    }
    private static ItemSlotObservation encode(int index,ItemStack stack){if(stack==null||stack.getItem()==null)return new ItemSlotObservation(index,0,0,0,0,0,ItemCategory.EMPTY);return new ItemSlotObservation(index,Item.getIdFromItem(stack.getItem()),Math.max(0,stack.getMetadata()),Math.max(0,Math.min(127,stack.stackSize)),ItemClassifier.durabilityClass(stack),ItemClassifier.enchantmentBits(stack),ItemClassifier.category(stack));}
}
