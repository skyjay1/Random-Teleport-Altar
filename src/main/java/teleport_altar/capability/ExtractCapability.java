package teleport_altar.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import teleport_altar.TeleportAltar;
import teleport_altar.network.ClientBoundSyncExtractCapabilityPacket;
import teleport_altar.network.TANetwork;

public class ExtractCapability implements INBTSerializable<CompoundTag> {

    public static final ExtractCapability EMPTY = new ExtractCapability();

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(TeleportAltar.MODID, "extract");

    private boolean active;
    private boolean canExtract;
    private BlockPos.MutableBlockPos spawnPoint = BlockPos.ZERO.mutable();
    private BlockPos.MutableBlockPos lastPosition = BlockPos.ZERO.mutable();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BlockPos getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(BlockPos spawnPoint) {
        this.spawnPoint.set(spawnPoint);
    }

    public void activate(final ServerPlayer player, final BlockPos spawnPoint) {
        setActive(true);
        setCanExtract(false);
        setSpawnPoint(spawnPoint);
        sendToClient(player);
    }

    public void setCanExtract(final boolean canExtract) {
        this.canExtract = canExtract;
    }

    public boolean canExtract() {
        return this.canExtract;
    }

    private boolean checkCanExtract(final BlockPos blockPos, final BlockPos worldCenter) {
        // verify spawnpoint was set
        if(spawnPoint == BlockPos.ZERO) {
            return true;
        }
        final BlockPos spawnPos = spawnPoint.subtract(worldCenter);
        final BlockPos pos = blockPos.subtract(worldCenter);
        // calculate quadrants and verify pos and spawnpoint are diagonally opposite (x and z are all opposite signs)
        return Mth.sign(spawnPos.getX()) != Mth.sign(pos.getX()) && Mth.sign(spawnPos.getZ()) != Mth.sign(pos.getZ());
    }

    public void checkAndUpdate(final ServerPlayer player, final BlockPos center) {
        // verify capability is active
        if(!isActive()) {
            return;
        }
        // check for reset dimension
        if(player.getLevel().dimension().equals(TeleportAltar.CONFIG.getResetDimension())) {
            setActive(false);
            setCanExtract(false);
            sendToClient(player);
            return;
        }
        // check distance to last position
        if(player.tickCount % 50 != 0 && player.blockPosition().closerThan(lastPosition, 2.0D)) {
            return;
        }
        // update last position
        lastPosition.set(player.blockPosition());
        // check if player can extract here
        this.canExtract = checkCanExtract(player.blockPosition(), center);
        sendToClient(player);
    }

    public void sendToClient(final ServerPlayer player) {
        TANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientBoundSyncExtractCapabilityPacket(this.isActive(), this.canExtract(), this.getSpawnPoint()));
    }

    //// NBT ////

    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_SPAWNPOINT = "Spawnpoint";
    private static final String KEY_CAN_EXTRACT = "CanExtract";

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(KEY_ACTIVE, active);
        tag.put(KEY_SPAWNPOINT, NbtUtils.writeBlockPos(getSpawnPoint()));
        tag.putBoolean(KEY_CAN_EXTRACT, canExtract);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.active = tag.getBoolean(KEY_ACTIVE);
        this.spawnPoint = NbtUtils.readBlockPos(tag.getCompound(KEY_SPAWNPOINT)).mutable();
        this.canExtract = tag.getBoolean(KEY_CAN_EXTRACT);
    }

    //// PROVIDER ////

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final ExtractCapability instance;
        private final LazyOptional<ExtractCapability> storage;

        public Provider(Player player) {
            instance = new ExtractCapability();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == TeleportAltar.EXTRACT_CAPABILITY) {
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
