package dev.fivesaw.sawbot.forge.tracking;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.EntityKind;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.EntitySetSnapshot;
import dev.fivesaw.sawbot.common.observation.EntityType;
import dev.fivesaw.sawbot.common.observation.ItemCategory;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import dev.fivesaw.sawbot.forge.sensors.ItemClassifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class EntityTrackerSensor {
    private static final double MAX_DISTANCE = 64.0D;
    private static final float LEGITIMATE_ATTACK_DISTANCE = 3.1F;
    private static final long GRACE_TICKS = 40L;

    private final Map<Integer, Track> tracks = new HashMap<Integer, Track>();
    private final VisibilitySampler visibilitySampler = new VisibilitySampler();
    private int nextTrackingId = 1;

    public EntitySetSnapshot capture(EntityPlayerSP player, World world, long clientTick) {
        List<Candidate> candidates = new ArrayList<Candidate>();
        Set<Integer> seen = new HashSet<Integer>();
        for (Object raw : world.loadedEntityList) {
            if (!(raw instanceof Entity)) continue;
            Entity entity = (Entity)raw;
            if (entity == player || entity.isDead) continue;
            double distance = player.getDistanceToEntity(entity);
            if (distance > MAX_DISTANCE) continue;
            Integer minecraftId = Integer.valueOf(entity.getEntityId());
            seen.add(minecraftId);
            UUID uniqueId = entity.getUniqueID();
            Track track = tracks.get(minecraftId);
            if (track == null || !track.uniqueId.equals(uniqueId)) {
                track = new Track(nextTrackingId++, uniqueId);
                tracks.put(minecraftId, track);
            }
            track.lastSeenTick = clientTick;
            candidates.add(new Candidate(entity, track, (float)distance, priority(entity, player)));
        }

        Iterator<Map.Entry<Integer, Track>> iterator = tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Track> entry = iterator.next();
            if (!seen.contains(entry.getKey()) && clientTick - entry.getValue().lastSeenTick > GRACE_TICKS) iterator.remove();
        }

        Collections.sort(candidates, new Comparator<Candidate>() {
            @Override public int compare(Candidate left, Candidate right) {
                int byPriority = Integer.compare(right.priority, left.priority);
                return byPriority != 0 ? byPriority : Integer.compare(left.track.trackingId, right.track.trackingId);
            }
        });

        int count = Math.min(EntitySetSnapshot.MAX_ENTITIES, candidates.size());
        List<EntityObservation> result = new ArrayList<EntityObservation>(count);
        for (int index = 0; index < count; index++) result.add(observe(candidates.get(index), player, world));
        return new EntitySetSnapshot(result, Math.max(0, candidates.size() - count));
    }

    private EntityObservation observe(Candidate candidate, EntityPlayerSP player, World world) {
        Entity entity = candidate.entity;
        Track track = candidate.track;
        double dx = entity.posX - player.posX;
        double dy = entity.posY - player.posY;
        double dz = entity.posZ - player.posZ;
        float right = EgocentricTransform.right(dx, dz, player.rotationYaw);
        float forward = EgocentricTransform.forward(dx, dz, player.rotationYaw);

        double relativeMotionX = entity.motionX - player.motionX;
        double relativeMotionY = entity.motionY - player.motionY;
        double relativeMotionZ = entity.motionZ - player.motionZ;
        float velocityRight = EgocentricTransform.right(relativeMotionX, relativeMotionZ, player.rotationYaw);
        float velocityForward = EgocentricTransform.forward(relativeMotionX, relativeMotionZ, player.rotationYaw);
        float accelerationRight = track.hasVelocity
            ? EgocentricTransform.right(relativeMotionX - track.previousMotionX, relativeMotionZ - track.previousMotionZ, player.rotationYaw) : 0f;
        float accelerationUp = track.hasVelocity ? (float)(relativeMotionY - track.previousMotionY) : 0f;
        float accelerationForward = track.hasVelocity
            ? EgocentricTransform.forward(relativeMotionX - track.previousMotionX, relativeMotionZ - track.previousMotionZ, player.rotationYaw) : 0f;
        track.previousMotionX = relativeMotionX;
        track.previousMotionY = relativeMotionY;
        track.previousMotionZ = relativeMotionZ;
        track.hasVelocity = true;

        AxisAlignedBB box = entity.getEntityBoundingBox();
        float width = (float)(box.maxX - box.minX);
        float height = (float)(box.maxY - box.minY);
        float health = 0f;
        float armour = 0f;
        int heldItem = ItemCategory.EMPTY.ordinal();
        int payloadItem = ItemCategory.EMPTY.ordinal();
        int hurt = 0;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase)entity;
            health = Math.max(0f, living.getHealth());
            hurt = Math.max(0, living.hurtTime);
            ItemStack stack = living.getHeldItem();
            heldItem = ItemClassifier.category(stack).ordinal();
            if (entity instanceof EntityPlayer) armour = ((EntityPlayer)entity).getTotalArmorValue();
        }
        if (entity instanceof EntityItem) {
            payloadItem = ItemClassifier.category(((EntityItem)entity).getEntityItem()).ordinal();
        }
        boolean lineOfSight = visibilitySampler.hasLineOfSight(player, entity, world);
        boolean attackable = entity instanceof EntityLivingBase && !entity.isDead && lineOfSight
            && candidate.distance <= LEGITIMATE_ATTACK_DISTANCE;
        return new EntityObservation(track.trackingId, entity.getEntityId(), kind(entity), EntityTypeClassifier.classify(entity), relation(entity, player),
            right, (float)dy, forward,
            velocityRight, (float)relativeMotionY, velocityForward,
            accelerationRight, accelerationUp, accelerationForward,
            normalizeYaw(entity.rotationYaw), MathHelper.clamp_float(entity.rotationPitch, -90f, 90f),
            width, height, health, armour, candidate.distance,
            heldItem, payloadItem, hurt, entity.onGround, entity.isSprinting(), entity.isSneaking(),
            lineOfSight, !lineOfSight, attackable, true, 1f);
    }

    private static int priority(Entity entity, EntityPlayerSP player) {
        if (entity instanceof EntityPlayer) return relation(entity, player) == TeamRelation.ENEMY ? 100 : 90;
        if (isProjectile(entity)) return 80;
        if (entity instanceof EntityItem) return 70;
        if (entity instanceof EntityVillager) return 60;
        if (entity instanceof EntityMob) return 50;
        if (entity instanceof EntityLivingBase) return 40;
        return 10;
    }

    private static EntityKind kind(Entity entity) {
        if (entity instanceof EntityPlayer) return EntityKind.PLAYER;
        if (entity instanceof EntityVillager) return EntityKind.NPC;
        if (isProjectile(entity)) return EntityKind.PROJECTILE;
        if (entity instanceof EntityItem) return EntityKind.DROPPED_ITEM;
        if (entity instanceof EntityLivingBase) return EntityKind.LIVING;
        return EntityKind.OTHER;
    }

    private static boolean isProjectile(Entity entity) {
        return entity instanceof EntityArrow || entity instanceof EntityThrowable || entity instanceof EntityFireball;
    }

    private static TeamRelation relation(Entity entity, EntityPlayerSP player) {
        if (entity == player) return TeamRelation.SELF;
        if (entity instanceof EntityPlayer) {
            EntityLivingBase other = (EntityLivingBase)entity;
            if (player.getTeam() == null || other.getTeam() == null) return TeamRelation.UNKNOWN;
            return player.isOnSameTeam(other) ? TeamRelation.TEAMMATE : TeamRelation.ENEMY;
        }
        if (entity instanceof EntityVillager) return TeamRelation.NEUTRAL;
        return TeamRelation.UNKNOWN;
    }

    private static float normalizeYaw(float yaw) {
        float value = yaw % 360f;
        if (value >= 180f) value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }

    private static final class Track {
        final int trackingId;
        final UUID uniqueId;
        long lastSeenTick;
        double previousMotionX;
        double previousMotionY;
        double previousMotionZ;
        boolean hasVelocity;
        Track(int trackingId, UUID uniqueId) {
            this.trackingId = trackingId;
            this.uniqueId = uniqueId;
        }
    }

    private static final class Candidate {
        final Entity entity;
        final Track track;
        final float distance;
        final int priority;
        Candidate(Entity entity, Track track, float distance, int priority) {
            this.entity = entity;
            this.track = track;
            this.distance = distance;
            this.priority = priority;
        }
    }
}
