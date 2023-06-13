package teleport_altar.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import teleport_altar.TeleportAltar;

import java.util.Optional;

public final class TANetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(TeleportAltar.MODID, "channel"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        int messageId = 0;
        CHANNEL.registerMessage(messageId++, ClientBoundSyncExtractCapabilityPacket.class, ClientBoundSyncExtractCapabilityPacket::toBytes, ClientBoundSyncExtractCapabilityPacket::fromBytes, ClientBoundSyncExtractCapabilityPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
