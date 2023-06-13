package teleport_altar.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import teleport_altar.TeleportAltar;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Sent from the server to the client to update extract capability flags
 **/
public class ClientBoundSyncExtractCapabilityPacket {

    private final boolean isActive;
    private final boolean canExtract;
    private final BlockPos spawnPoint;

    public ClientBoundSyncExtractCapabilityPacket(final boolean isActive, final boolean canExtract, BlockPos spawnPoint) {
        this.isActive = isActive;
        this.canExtract = canExtract;
        this.spawnPoint = spawnPoint;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a ClientBoundSyncExtractCapabilityPacket based on the PacketBuffer
     */
    public static ClientBoundSyncExtractCapabilityPacket fromBytes(final FriendlyByteBuf buf) {
        final boolean isActive = buf.readBoolean();
        final boolean canExtract = buf.readBoolean();
        final BlockPos spawnPoint = isActive ? buf.readBlockPos() : BlockPos.ZERO;
        return new ClientBoundSyncExtractCapabilityPacket(isActive, canExtract, spawnPoint);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the ClientBoundSyncExtractCapabilityPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ClientBoundSyncExtractCapabilityPacket msg, final FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isActive);
        buf.writeBoolean(msg.canExtract);
        if(msg.isActive) {
            buf.writeBlockPos(msg.spawnPoint);
        }
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the ClientBoundSyncExtractCapabilityPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ClientBoundSyncExtractCapabilityPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                Optional<Player> player = ClientNetworkUtils.getClientPlayer();
                if(player.isEmpty()) {
                    return;
                }
                player.get().getCapability(TeleportAltar.EXTRACT_CAPABILITY).ifPresent(c -> {
                    c.setActive(message.isActive);
                    c.setCanExtract(message.canExtract);
                    c.setSpawnPoint(message.spawnPoint);
                    ClientNetworkUtils.updateExtractOverlay(player.get(), c);
                });
            });
        }
        context.setPacketHandled(true);
    }
}
