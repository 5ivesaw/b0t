package net.minecraftforge.fml.common.gameevent; public class TickEvent { public enum Phase{START,END} public static class ClientTickEvent { public Phase phase; } }
