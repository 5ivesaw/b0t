package dev.fivesaw.sawbot.forge.actuator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Enforces the private/local-only runtime boundary before any autonomous input is applied. */
public final class EnvironmentGuard {
    private final Minecraft minecraft;
    private final boolean allowSingleplayer;
    private final Set<String> allowedServerHosts;

    public EnvironmentGuard(Minecraft minecraft, boolean allowSingleplayer, String allowedServersCsv) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
        this.allowSingleplayer = allowSingleplayer;
        HashSet<String> hosts = new HashSet<String>();
        if (allowedServersCsv != null) {
            String[] parts = allowedServersCsv.split(",");
            for (String part : parts) {
                String value = normalizeHost(part);
                if (!value.isEmpty()) hosts.add(value);
            }
        }
        this.allowedServerHosts = Collections.unmodifiableSet(hosts);
    }

    public boolean isAllowed() {
        if (minecraft.theWorld == null || minecraft.thePlayer == null) return false;
        if (allowSingleplayer && minecraft.isSingleplayer()) return true;
        ServerData server = minecraft.getCurrentServerData();
        if (server == null || server.serverIP == null) return false;
        return allowedServerHosts.contains(normalizeHost(server.serverIP));
    }

    public String description() {
        if (minecraft.theWorld == null || minecraft.thePlayer == null) return "NO_WORLD";
        if (allowSingleplayer && minecraft.isSingleplayer()) return "SINGLEPLAYER";
        ServerData server = minecraft.getCurrentServerData();
        if (server == null || server.serverIP == null) return "BLOCKED";
        String host = normalizeHost(server.serverIP);
        return allowedServerHosts.contains(host) ? "PRIVATE:" + host : "BLOCKED:" + host;
    }

    private static String normalizeHost(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close > 0) return value.substring(1, close);
        }
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon) value = value.substring(0, colon);
        return value;
    }
}
