package net.minecraft.item; public class Item { private static int next; private final int id=next++; public static int getIdFromItem(Item item){return item==null?0:item.id;} }
