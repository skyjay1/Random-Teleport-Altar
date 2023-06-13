package teleport_altar.gui;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import teleport_altar.TeleportAltar;

public final class TAClientEvents {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
    }

    public static final class ForgeHandler {

    }

    public static final class ModHandler {

        @SubscribeEvent
        public static void onRegisterOverlays(final RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), TeleportAltar.MODID + "_extract", new ExtractOverlay());
        }

    }
}
