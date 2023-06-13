package teleport_altar.config;

import net.minecraftforge.common.ForgeConfigSpec;
import teleport_altar.gui.HudAnchor;

public class TAClientConfig {
    // hud
    public final ForgeConfigSpec.BooleanValue EXTRACT_HUD_VISIBLE;
    public final ForgeConfigSpec.EnumValue<HudAnchor> EXTRACT_HUD_ANCHOR;
    public final ForgeConfigSpec.IntValue EXTRACT_HUD_X_OFFSET;
    public final ForgeConfigSpec.IntValue EXTRACT_HUD_Y_OFFSET;
    public final ForgeConfigSpec.IntValue EXTRACT_HUD_BG_COLOR;
    public final ForgeConfigSpec.DoubleValue EXTRACT_HUD_BG_OPACITY;

    public TAClientConfig(final ForgeConfigSpec.Builder builder) {

        builder.push("extract_hud");
        EXTRACT_HUD_VISIBLE = builder
                .comment("Set to false to hide the HUD")
                .define("visible", true);
        EXTRACT_HUD_ANCHOR = builder
                .comment("The anchor position of the HUD")
                .defineEnum("anchor", HudAnchor.BOTTOM_RIGHT);
        EXTRACT_HUD_X_OFFSET = builder
                .comment("The X offset of the HUD")
                .defineInRange("x_offset", -90, -1024, 1024);
        EXTRACT_HUD_Y_OFFSET = builder
                .comment("The Y offset of the HUD")
                .defineInRange("y_offset", -12, -1024, 1024);
        EXTRACT_HUD_BG_COLOR = builder
                .comment("The HUD background color",
                        "Accepts decimal or hexadecimal (eg 0xFF0000=red)")
                .defineInRange("bg_color", 0x000000, 0x000000, 0xFFFFFF);
        EXTRACT_HUD_BG_OPACITY = builder
                .comment("The opacity percent of the HUD background color")
                .defineInRange("bg_opacity", 0.15F, 0.0F, 1.0F);
        builder.pop();
    }
}
