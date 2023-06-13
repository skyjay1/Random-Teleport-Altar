package teleport_altar.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import teleport_altar.capability.ExtractCapability;
import teleport_altar.gui.ExtractOverlay;

import java.util.Optional;

public final class ClientNetworkUtils {

    public static Optional<Level> getClientLevel() {
        return Optional.ofNullable(net.minecraft.client.Minecraft.getInstance().level);
    }

    public static Optional<Player> getClientPlayer() {
        return Optional.ofNullable(net.minecraft.client.Minecraft.getInstance().player);
    }

    public static void updateExtractOverlay(final Player player, final ExtractCapability capability) {
        final WorldBorder worldBorder = player.level.getWorldBorder();
        final BlockPos center = new BlockPos(worldBorder.getCenterX(), 0, worldBorder.getCenterZ());
        if(capability.isActive()) {
            ExtractOverlay.setText(ExtractOverlay.createText(player.blockPosition(), capability.getSpawnPoint(), center, capability.canExtract()));
            ExtractOverlay.setVisible(true);
        } else {
            ExtractOverlay.setVisible(false);
        }
    }
}
