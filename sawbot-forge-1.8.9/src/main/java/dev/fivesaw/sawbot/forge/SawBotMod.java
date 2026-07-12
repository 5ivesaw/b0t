package dev.fivesaw.sawbot.forge;

import dev.fivesaw.sawbot.forge.client.ClientRuntime;
import dev.fivesaw.sawbot.forge.config.SawBotConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = SawBotMod.MOD_ID, name = SawBotMod.NAME, useMetadata = true,
    acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public final class SawBotMod {
    public static final String MOD_ID = "sawbotv1";
    public static final String NAME = "SawBotV1";
    public static final String VERSION = "0.8.0-alpha.0";

    private Logger logger;
    private ClientRuntime clientRuntime;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        SawBotConfig config = SawBotConfig.load(event.getSuggestedConfigurationFile(), logger);
        clientRuntime = new ClientRuntime(config, logger);
        logger.info("SawBotV1 Phase 7 adaptive navigation initialized; autonomous control is disabled by default.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (clientRuntime == null) throw new IllegalStateException("preInit did not create ClientRuntime");
        clientRuntime.register();
        logger.info("SawBotV1 Phase 7 event handlers registered.");
    }
}
