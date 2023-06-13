package teleport_altar;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import teleport_altar.capability.ExtractCapability;
import teleport_altar.capability.SlowFallCapability;
import teleport_altar.config.TAClientConfig;
import teleport_altar.config.TAConfig;
import teleport_altar.gui.TAClientEvents;
import teleport_altar.network.TANetwork;

import java.util.Optional;

@Mod(TeleportAltar.MODID)
public class TeleportAltar {

    public static final String MODID = "teleport_altar";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static TAConfig CONFIG;
    public static TAClientConfig CLIENT_CONFIG;

    public static Capability<SlowFallCapability> SLOW_FALL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static Capability<ExtractCapability> EXTRACT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<Block> TELEPORT_ALTAR_BLOCK = BLOCKS.register("teleport_altar", () -> new TABlock(BlockBehaviour.Properties
            .of(Material.METAL)
            .strength(4.0F, 1200.0F)
            .noOcclusion()));
    public static final RegistryObject<Item> TELEPORT_ALTAR_ITEM = ITEMS.register("teleport_altar", () -> new BlockItem(TELEPORT_ALTAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final RegistryObject<BlockEntityType<?>> TELEPORT_ALTAR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("teleport_altar", () ->
            BlockEntityType.Builder.of((pos, state) -> new TABlockEntity(TeleportAltar.TELEPORT_ALTAR_BLOCK_ENTITY.get(), pos, state),
                            TELEPORT_ALTAR_BLOCK.get())
                    .build(null));

    public TeleportAltar() {
        // create config specs
        // common config
        Pair<TAConfig, ForgeConfigSpec> commonConfig = new ForgeConfigSpec.Builder().configure(TAConfig::new);
        CONFIG = commonConfig.getLeft();
        // client config
        Pair<TAClientConfig, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(TAClientConfig::new);
        CLIENT_CONFIG = clientConfig.getLeft();
        // register config specs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, commonConfig.getRight());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientConfig.getRight());
        // register network
        TANetwork.register();
        // registry events
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        // other events
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
        // client events
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TAClientEvents::register);
    }

    public static final class ModHandler {
        @SubscribeEvent
        public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(SlowFallCapability.class);
            event.register(ExtractCapability.class);
        }
    }

    public static final class ForgeHandler {

        private static final TagKey<Item> BLACKLISTED_ITEM = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(MODID, "extract_blacklist"));
        private static final TagKey<Block> BLACKLISTED_BLOCK = ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(MODID, "extract_blacklist"));

        @SubscribeEvent
        public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if(event.getObject() instanceof Player player) {
                event.addCapability(SlowFallCapability.REGISTRY_NAME, new SlowFallCapability.Provider(player));
                event.addCapability(ExtractCapability.REGISTRY_NAME, new ExtractCapability.Provider(player));
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if(event.phase != TickEvent.Phase.END || event.player.level.isClientSide() || !event.player.isAlive()) {
                return;
            }
            final ServerPlayer player = (ServerPlayer) event.player;
            player.getCapability(SLOW_FALL_CAPABILITY).ifPresent(c -> c.checkAndUpdate(player));
            // load world border information
            final WorldBorder border = player.getLevel().getWorldBorder();
            final BlockPos center = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
            player.getCapability(EXTRACT_CAPABILITY).ifPresent(c -> c.checkAndUpdate(player, center));
        }

        @SubscribeEvent
        public static void onPlayerClone(final PlayerEvent.Clone event) {
            cloneCapabilities(event.getOriginal(), event.getEntity(), SLOW_FALL_CAPABILITY);
            cloneCapabilities(event.getOriginal(), event.getEntity(), EXTRACT_CAPABILITY);
        }

        @SubscribeEvent
        public static void onUseItem(final LivingEntityUseItemEvent.Start event) {
            if(!TeleportAltar.CONFIG.SDATAG_UTILS_ENABLED.get() || !(event.getEntity() instanceof Player player)) {
                return;
            }
            // check for extract capability plus blacklisted item and cancel event
            player.getCapability(EXTRACT_CAPABILITY).ifPresent(c -> {
                if(c.isActive() && event.getItem().is(BLACKLISTED_ITEM) && !c.canExtract()) {
                    event.setCanceled(true);
                    event.setDuration(0);
                    // send message
                    player.displayClientMessage(Component.translatable("message.extract.deny.not_in_quadrant").withStyle(ChatFormatting.RED), false);
                }
            });
        }

        @SubscribeEvent
        public static void onUseBlock(final PlayerInteractEvent.RightClickBlock event) {
            if(event.getLevel().isClientSide() || !TeleportAltar.CONFIG.SDATAG_UTILS_ENABLED.get()) {
                return;
            }
            // check for extract capability plus blacklisted block and cancel event
            final BlockState blockState = event.getLevel().getBlockState(event.getPos());
            final ServerPlayer player = (ServerPlayer) event.getEntity(); 
            player.getCapability(EXTRACT_CAPABILITY).ifPresent(c -> {
                if(c.isActive() && blockState.is(BLACKLISTED_BLOCK) && !c.canExtract()) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    // update block state
                    player.level.sendBlockUpdated(event.getPos(), blockState, blockState, Block.UPDATE_CLIENTS);
                    // sync inventory
                    player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, player.getInventory().selected, player.getInventory().getSelected()));
                    // send message
                    player.displayClientMessage(Component.translatable("message.extract.deny.not_in_quadrant").withStyle(ChatFormatting.RED), false);
                }
            });
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity().getLevel().isClientSide() || !TeleportAltar.CONFIG.SDATAG_UTILS_ENABLED.get()) {
                return;
            }
            // sync capability
            final ServerPlayer player = (ServerPlayer) event.getEntity();
            player.getCapability(EXTRACT_CAPABILITY).ifPresent(c -> c.sendToClient(player));
        }

    }

    /**
     * Helper method to copy capabilities during the player clone event
     * @param original the original player
     * @param clone the clone player
     * @param capability the capability to copy
     * @param <T> a capability that is NBT Serializable to a CompoundTag
     */
    private static <T extends INBTSerializable<CompoundTag>> void cloneCapabilities(final Player original, final Player clone, final Capability<T> capability) {
        // revive capabilities in order to copy to the clone
        original.reviveCaps();
        // load capabilities
        Optional<T> originalCap = original.getCapability(capability).resolve();
        Optional<T> cloneCap = clone.getCapability(capability).resolve();
        // copy capabilities to the clone
        if(originalCap.isPresent() && cloneCap.isPresent()) {
            cloneCap.ifPresent(f -> f.deserializeNBT(originalCap.get().serializeNBT()));
        }
        // invalidate capabilities afterwards
        original.invalidateCaps();
    }
}
