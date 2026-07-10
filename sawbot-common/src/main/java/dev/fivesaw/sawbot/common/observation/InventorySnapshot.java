package dev.fivesaw.sawbot.common.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InventorySnapshot {
    public static final int EXPECTED_SLOT_COUNT = 41;
    private final List<ItemSlotObservation> slots;
    private final int selectedSlot;
    private final String openContainerType;
    private final int iron, gold, diamonds, emeralds, wool;
    public InventorySnapshot(List<ItemSlotObservation> slots, int selectedSlot, String openContainerType,
                             int iron, int gold, int diamonds, int emeralds, int wool) {
        if (slots == null || slots.size() != EXPECTED_SLOT_COUNT || selectedSlot < 0 || selectedSlot > 8 || openContainerType == null || openContainerType.length() > 48) throw new IllegalArgumentException("inventory");
        if (iron < 0 || gold < 0 || diamonds < 0 || emeralds < 0 || wool < 0) throw new IllegalArgumentException("resource count");
        this.slots=Collections.unmodifiableList(new ArrayList<ItemSlotObservation>(slots)); this.selectedSlot=selectedSlot; this.openContainerType=openContainerType;
        this.iron=iron; this.gold=gold; this.diamonds=diamonds; this.emeralds=emeralds; this.wool=wool;
    }
    public List<ItemSlotObservation> slots(){return slots;} public int selectedSlot(){return selectedSlot;} public String openContainerType(){return openContainerType;}
    public int iron(){return iron;} public int gold(){return gold;} public int diamonds(){return diamonds;} public int emeralds(){return emeralds;} public int wool(){return wool;}
}
