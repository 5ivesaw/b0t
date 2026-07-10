package dev.fivesaw.sawbot.common.observation;
public final class ItemSlotObservation {
    private final int slotIndex, itemId, metadata, count, durabilityClass, enchantmentBits;
    private final ItemCategory category;
    public ItemSlotObservation(int slotIndex, int itemId, int metadata, int count, int durabilityClass, int enchantmentBits, ItemCategory category) {
        if (slotIndex < 0 || itemId < 0 || metadata < 0 || count < 0 || count > 127 || durabilityClass < 0 || category == null) throw new IllegalArgumentException("slot");
        this.slotIndex=slotIndex; this.itemId=itemId; this.metadata=metadata; this.count=count; this.durabilityClass=durabilityClass; this.enchantmentBits=enchantmentBits; this.category=category;
    }
    public int slotIndex(){return slotIndex;} public int itemId(){return itemId;} public int metadata(){return metadata;} public int count(){return count;}
    public int durabilityClass(){return durabilityClass;} public int enchantmentBits(){return enchantmentBits;} public ItemCategory category(){return category;}
}
