package net.minecraft.client.settings;
public class GameSettings {
    public KeyBinding keyBindForward = key(1);
    public KeyBinding keyBindBack = key(2);
    public KeyBinding keyBindLeft = key(3);
    public KeyBinding keyBindRight = key(4);
    public KeyBinding keyBindJump = key(5);
    public KeyBinding keyBindSneak = key(6);
    public KeyBinding keyBindSprint = key(7);
    public KeyBinding keyBindAttack = key(8);
    public KeyBinding keyBindUseItem = key(9);
    public KeyBinding keyBindDrop = key(10);
    public KeyBinding keyBindInventory = key(11);
    private static KeyBinding key(int code) { return new KeyBinding("stub", code, "stub"); }
}
