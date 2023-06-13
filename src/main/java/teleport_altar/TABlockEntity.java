package teleport_altar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayList;
import java.util.List;

public class TABlockEntity extends BlockEntity {

    protected int timer;

    public TABlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Updates the block entity each tick
     * @param level the level
     * @param blockPos the block entity position
     * @param blockState the block state
     * @param blockEntity the block entity
     */
    public static void tick(Level level, BlockPos blockPos, BlockState blockState, TABlockEntity blockEntity) {
        if(!level.isClientSide() && blockEntity.timer > 0) {
            // decrement timer
            --blockEntity.timer;
            // check for timer interval
            List<ServerPlayer> nearbyPlayers = new ArrayList<>();
            if(blockEntity.timer % 20 == 0) {
                // determine nearby players
                nearbyPlayers = blockEntity.getPlayersInRange();
                if(nearbyPlayers.isEmpty()) {
                    // reset timer
                    blockEntity.setTimer(0);
                    // play canceled sound
                    level.playSound(null, blockPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.5F, 1.0F);
                    return;
                }
                // send message
                final int secondsRemaining = 1 + (blockEntity.timer / 20);
                final Component message = createIntervalMessage(nearbyPlayers.size(), secondsRemaining, ChatFormatting.GREEN);
                nearbyPlayers.forEach(p -> p.displayClientMessage(message, true));
                // play ticking sound
                level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.5F, 1.4F - (0.1F * secondsRemaining));
                // check for timer completion
                if(blockEntity.timer <= 0) {
                    // teleport players
                    blockEntity.teleportPlayers(nearbyPlayers);
                    blockEntity.setTimer(0);
                }
            }

        }

    }

    /**
     * @param count the number of players
     * @param time the number of seconds remaining
     * @param color the component style
     * @return a component with a message informing players of the time remaining before teleport
     */
    protected static Component createIntervalMessage(final int count, final int time, final ChatFormatting color) {
        if(count == 1) {
            return Component.translatable("message.teleport_altar.interval.single", time)
                    .withStyle(color);
        } else {
            return Component.translatable("message.teleport_altar.interval.multiple", count, time)
                    .withStyle(color);
        }
    }

    /**
     * @return all players within range of the block entity, based on the config value
     */
    protected List<ServerPlayer> getPlayersInRange() {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(getBlockPos()).inflate(TeleportAltar.CONFIG.TELEPORT_DETECT_RANGE.get()));
    }

    /**
     * Sends the players to the destination level as a group
     * @param players the players to teleport (no validation checks are performed)
     */
    protected void teleportPlayers(final List<ServerPlayer> players) {
        // validate player list
        if(players.isEmpty()) {
            return;
        }
        // validate server side
        if(null == level || null == level.getServer()) {
            return;
        }
        // load destination level
        final ResourceKey<Level> levelKey = TeleportAltar.CONFIG.getTeleportDimension();
        final ServerLevel destLevel = level.getServer().getLevel(levelKey);
        if(null == destLevel) {
            TeleportAltar.LOGGER.error("Failed to load level from resource key: " + levelKey.location());
            return;
        }
        // calculate destination
        final Vec3 destination = calculateDestination(destLevel);
        // teleport all players
        players.forEach(p -> level.getServer().submitAsync(() -> {
            // determine relative offset to this block
            BlockPos offset = p.blockPosition().subtract(getBlockPos());
            Vec3 targetVec = destination.add(new Vec3(offset.getX() + 0.5D, 0, offset.getZ() + 0.5D));
            // teleport the player
            sendToDimension(p, destLevel, targetVec);
        }));
    }

    /**
     * @param level the destination level
     * @return the target destination, within X blocks and at least Y blocks away from the block center, as defined in the config
     */
    private static Vec3 calculateDestination(final ServerLevel level) {
        // determine destination coordinates
        final long maxRange = TeleportAltar.CONFIG.getTeleportMaxRange((long) Math.max(1, (level.getWorldBorder().getSize() / 2) - TeleportAltar.CONFIG.TELEPORT_DETECT_RANGE.get()));
        final long minRange = TeleportAltar.CONFIG.getTeleportMinRange(maxRange);
        final long range = maxRange - minRange;
        final Vec3 center = new Vec3(level.getWorldBorder().getCenterX(), level.getMaxBuildHeight() - 1, level.getWorldBorder().getCenterZ());
        // calculate random positions until one is found that is within range
        // if no position was found after many attempts, the most recent one will be used instead
        int attempts = 128;
        Vec3 destination;
        do {
            destination = new Vec3(level.getRandom().nextLong() % maxRange, 0, level.getRandom().nextLong() % maxRange);
        } while(attempts-- > 0 && destination.x() < minRange && destination.z() < minRange);
        // randomize signs
        destination = destination
                .multiply(Math.signum(level.getRandom().nextDouble() - 0.5D), 1.0D, Math.signum(level.getRandom().nextDouble() - 0.5D))
                .add(center);
        return destination;
    }

    /**
     * Helper method that creates a {@link DirectTeleporter} to send the entity directly to the given dimension
     * and coordinates.
     *
     * @param entity      the entity
     * @param targetWorld the dimension
     * @param targetVec   the location
     */
    private static void sendToDimension(ServerPlayer entity, ServerLevel targetWorld, Vec3 targetVec) {
        // ensure destination chunk is loaded before we put the player in it
        targetWorld.getChunk(new BlockPos(targetVec));
        // give slow fall effect
        final int slowFallAmplifier = TeleportAltar.CONFIG.SLOW_FALLING_AMPLIFIER.get();
        if(slowFallAmplifier >= 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10 * 60 * 20, slowFallAmplifier, true, true, true));
        }
        // give speed effect
        final int speedAmplifier = TeleportAltar.CONFIG.SPEED_AMPLIFIER.get();
        if(speedAmplifier >= 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 60 * 20, speedAmplifier, true, true, true));
        }
        // mark capability as active
        entity.getCapability(TeleportAltar.SLOW_FALL_CAPABILITY).ifPresent(c -> c.setActive(true));
        // teleport the entity
        ITeleporter teleporter = DirectTeleporter.create(entity, targetVec);
        entity.changeDimension(targetWorld, teleporter);
        // activate extract capability
        if(TeleportAltar.CONFIG.SDATAG_UTILS_ENABLED.get()) {
            // activate capability and set spawnpoint
            entity.getCapability(TeleportAltar.EXTRACT_CAPABILITY).ifPresent(c -> c.activate(entity, new BlockPos(targetVec)));
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        final BlockPos pos = getBlockPos();
        return new AABB(pos.offset(-1, 0, -1), pos.offset(2, 2, 2));
    }

    //// GETTERS AND SETTERS ////

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }
}
