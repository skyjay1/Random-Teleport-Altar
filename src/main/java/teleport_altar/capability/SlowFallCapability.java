package teleport_altar.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import teleport_altar.TeleportAltar;

public class SlowFallCapability implements INBTSerializable<CompoundTag> {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(TeleportAltar.MODID, "slow_fall");

    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if the capability is active and attempts to deactivate
     * @param player the player
     * @return true if the capability was deactivated
     */
    public boolean checkAndUpdate(final ServerPlayer player) {
        if(isActive() && player.hasEffect(MobEffects.SLOW_FALLING)) {
            return update(player);
        }
        return false;
    }

    /**
     * Checks if the player is on solid ground or water
     * and removes the slow fall effect
     * @param player the player
     * @return true if the slow fall effect was removed
     */
    private boolean update(final ServerPlayer player) {
        final BlockPos posBelow = player.getOnPos();
        // validate position
        if(player.level.isOutsideBuildHeight(posBelow)) {
            return false;
        }
        // query block below the player
        final BlockState blockBelow = player.level.getBlockState(posBelow);
        // check for valid block
        if(blockBelow.isValidSpawn(player.level, posBelow, player.getType()) || blockBelow.is(Blocks.WATER)) {
            // remove effect
            player.removeEffect(MobEffects.SLOW_FALLING);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            // deactivate capability
            setActive(false);
            // send particles
            final double halfWidth = player.getBbWidth() * 0.5D;
            final Vec3 pos = player.position().add(0, halfWidth, 0);
            player.getLevel().sendParticles(ParticleTypes.CLOUD, pos.x(), pos.y(), pos.z(), 10, halfWidth, halfWidth, halfWidth, 0.1D);
            return true;
        }
        // no change
        return false;
    }

    //// NBT ////

    private static final String KEY_ACTIVE = "Active";

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(KEY_ACTIVE, active);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.active = tag.getBoolean(KEY_ACTIVE);
    }

    //// PROVIDER ////

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final SlowFallCapability instance;
        private final LazyOptional<SlowFallCapability> storage;

        public Provider(Player player) {
            instance = new SlowFallCapability();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == TeleportAltar.SLOW_FALL_CAPABILITY) {
                return storage.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
