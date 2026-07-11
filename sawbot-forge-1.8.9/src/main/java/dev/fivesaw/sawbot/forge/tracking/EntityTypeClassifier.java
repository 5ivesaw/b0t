package dev.fivesaw.sawbot.forge.tracking;

import dev.fivesaw.sawbot.common.observation.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityList;

/** Clean bounded mapping from Minecraft's registry names to the observation contract. */
public final class EntityTypeClassifier {
    private EntityTypeClassifier() { }

    public static EntityType classify(Entity entity) {
        if (entity == null) return EntityType.OTHER;
        if (entity instanceof EntityPlayer) return EntityType.PLAYER;
        if (entity instanceof EntityItem) return EntityType.DROPPED_ITEM;
        String registryName = EntityList.getEntityString(entity);
        if (registryName == null) {
            return entity instanceof EntityLivingBase ? EntityType.OTHER_LIVING : EntityType.OTHER;
        }
        switch (registryName) {
            case "ArmorStand": return EntityType.ARMOR_STAND;
            case "Villager": return EntityType.VILLAGER;
            case "VillagerGolem": return EntityType.IRON_GOLEM;
            case "SnowMan": return EntityType.SNOW_GOLEM;
            case "Cow": return EntityType.COW;
            case "MushroomCow": return EntityType.MOOSHROOM;
            case "Pig": return EntityType.PIG;
            case "Sheep": return EntityType.SHEEP;
            case "Chicken": return EntityType.CHICKEN;
            case "EntityHorse": return EntityType.HORSE;
            case "Wolf": return EntityType.WOLF;
            case "Ozelot": return EntityType.OCELOT;
            case "Rabbit": return EntityType.RABBIT;
            case "Squid": return EntityType.SQUID;
            case "Bat": return EntityType.BAT;
            case "Zombie": return EntityType.ZOMBIE;
            case "PigZombie": return EntityType.ZOMBIE_PIGMAN;
            case "Skeleton": return EntityType.SKELETON;
            case "Creeper": return EntityType.CREEPER;
            case "Spider": return EntityType.SPIDER;
            case "CaveSpider": return EntityType.CAVE_SPIDER;
            case "Enderman": return EntityType.ENDERMAN;
            case "Witch": return EntityType.WITCH;
            case "Slime": return EntityType.SLIME;
            case "LavaSlime": return EntityType.MAGMA_CUBE;
            case "Blaze": return EntityType.BLAZE;
            case "Ghast": return EntityType.GHAST;
            case "Guardian": return EntityType.GUARDIAN;
            case "Silverfish": return EntityType.SILVERFISH;
            case "Endermite": return EntityType.ENDERMITE;
            case "Giant": return EntityType.GIANT;
            case "WitherBoss": return EntityType.WITHER;
            case "EnderDragon": return EntityType.ENDER_DRAGON;
            case "EnderCrystal": return EntityType.ENDER_CRYSTAL;
            case "Item": return EntityType.DROPPED_ITEM;
            case "XPOrb": return EntityType.XP_ORB;
            case "Arrow": return EntityType.ARROW;
            case "Snowball": return EntityType.SNOWBALL;
            case "Fireball": return EntityType.FIREBALL;
            case "SmallFireball": return EntityType.SMALL_FIREBALL;
            case "ThrownEnderpearl": return EntityType.ENDER_PEARL;
            case "EyeOfEnderSignal": return EntityType.EYE_OF_ENDER;
            case "ThrownPotion": return EntityType.POTION;
            case "ThrownExpBottle": return EntityType.XP_BOTTLE;
            case "WitherSkull": return EntityType.WITHER_SKULL;
            case "FireworksRocketEntity": return EntityType.FIREWORK;
            case "PrimedTnt": return EntityType.PRIMED_TNT;
            case "FallingSand": return EntityType.FALLING_BLOCK;
            case "ItemFrame": return EntityType.ITEM_FRAME;
            case "Painting": return EntityType.PAINTING;
            case "Boat": return EntityType.BOAT;
            case "MinecartRideable":
            case "MinecartChest":
            case "MinecartFurnace":
            case "MinecartTNT":
            case "MinecartHopper":
            case "MinecartSpawner":
            case "MinecartCommandBlock": return EntityType.MINECART;
            case "LeashKnot": return EntityType.LEASH_KNOT;
            case "LightningBolt": return EntityType.LIGHTNING;
            default: return entity instanceof EntityLivingBase ? EntityType.OTHER_LIVING : EntityType.OTHER;
        }
    }
}
