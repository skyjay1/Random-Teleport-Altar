package teleport_altar.config;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.common.ForgeConfigSpec;

public class TAConfig {

    private final ForgeConfigSpec.ConfigValue<String> TELEPORT_DIMENSION;
    public final ForgeConfigSpec.IntValue TELEPORT_DETECT_RANGE;
    private final ForgeConfigSpec.IntValue TELEPORT_COUNTDOWN;
    private final ForgeConfigSpec.LongValue TELEPORT_MAX_RANGE;
    private final ForgeConfigSpec.LongValue TELEPORT_MIN_RANGE;
    public final ForgeConfigSpec.IntValue SLOW_FALLING_AMPLIFIER;
    public final ForgeConfigSpec.IntValue SPEED_AMPLIFIER;

    public final ForgeConfigSpec.BooleanValue SDATAG_UTILS_ENABLED;
    private final ForgeConfigSpec.ConfigValue<String> RESET_DIMENSION;

    public TAConfig(final ForgeConfigSpec.Builder builder) {

        builder.push("options");
        TELEPORT_COUNTDOWN = builder
                .comment("The number of seconds to wait before teleporting")
                .defineInRange("teleport_countdown", 4, 0, 60);
        TELEPORT_DIMENSION = builder
                .comment("The registry ID of the dimension to teleport players")
                .define("teleport_dimension", Level.OVERWORLD.location().toString());
        TELEPORT_DETECT_RANGE = builder
                .comment("The number of blocks to detect players to teleport")
                .defineInRange("teleport_detect_range", 2, 1, 128);
        TELEPORT_MIN_RANGE = builder
                .comment("The minimum distance from world center to teleport players")
                .defineInRange("teleport_min_range", 0, 0, (long) WorldBorder.MAX_SIZE / 2 - 1);
        TELEPORT_MAX_RANGE = builder
                .comment("The maximum distance from world center to teleport players",
                        "Set to 0 to use the current world border size")
                .defineInRange("teleport_max_range", 0, 0, (long) WorldBorder.MAX_SIZE / 2);
        SLOW_FALLING_AMPLIFIER = builder
                .comment("The potion amplifier of the slow falling effect (-1 to disable)")
                .defineInRange("slow_falling_amplifier", 0, -1, 255);
        SPEED_AMPLIFIER = builder
                .comment("The potion amplifier of the speed effect (-1 to disable)")
                .defineInRange("speed_amplifier", 2, -1, 255);
        builder.pop();

        builder.push("sdatag_utils");
        SDATAG_UTILS_ENABLED = builder
                .comment("Set to true to enable custom features for SdataG")
                .define("enabled", false);
        RESET_DIMENSION = builder
                .comment("The registry ID of the dimension to turn off item limitations and HUD")
                .define("reset_dimension", "javd:void");

        builder.pop();
    }

    public ResourceKey<Level> getTeleportDimension() {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(TELEPORT_DIMENSION.get()));
    }

    public ResourceKey<Level> getResetDimension() {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(RESET_DIMENSION.get()));
    }

    public int getTeleportCountdownTicks() {
        return TELEPORT_COUNTDOWN.get() * 20 + 1;
    }

    public long getTeleportMaxRange(final long worldBorderSize) {
        final long range = TELEPORT_MAX_RANGE.get();
        if(range <= 0) {
            return worldBorderSize;
        }
        return range;
    }

    public long getTeleportMinRange(final long maxRange) {
        final long minRange = TELEPORT_MIN_RANGE.get();
        return Math.min(minRange, maxRange - 1);
    }
}
