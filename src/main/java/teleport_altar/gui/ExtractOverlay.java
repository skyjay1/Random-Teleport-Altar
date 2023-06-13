package teleport_altar.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import teleport_altar.TeleportAltar;

import java.util.ArrayList;
import java.util.List;

public class ExtractOverlay implements IGuiOverlay {

    private static boolean visible;
    private static List<Component> text = new ArrayList<>();
    private static int textWidth;

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int width, int height) {
        // verify config allows
        if(!TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_VISIBLE.get()) {
            return;
        }
        // verify visible flag
        if(!isVisible()) {
            return;
        }
        // verify player exists
        final Player player = Minecraft.getInstance().player;
        if(null == player) {
            return;
        }
        // load text and verify non-empty
        final List<Component> text = getText();
        if(text.isEmpty() || textWidth <= 0) {
            return;
        }
        // calculate x and y position based on config
        final Vec2 position = TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_ANCHOR.get().getWithOffset(width, height, TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_X_OFFSET.get(), TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_Y_OFFSET.get());
        // calculate text width and height
        final Font font = Minecraft.getInstance().font;
        final int textHeight = font.lineHeight * text.size();
        final int x = (int) (position.x) - textWidth / 2;
        final int y = (int) (position.y) - textHeight / 2;
        final int bgMargin = 2;
        // draw the background
        final int bgColor = (TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_BG_COLOR.get()) | (Mth.floor(TeleportAltar.CLIENT_CONFIG.EXTRACT_HUD_BG_OPACITY.get() * 256) << 24);
        GuiComponent.fill(poseStack, x - bgMargin, y - bgMargin, x + textWidth + bgMargin, y + textHeight + bgMargin, bgColor);
        // draw the component
        for(int i = 0, n = text.size(); i < n; i++) {
            Screen.drawCenteredString(poseStack, Minecraft.getInstance().font, text.get(i), (int) (position.x), (int) (position.y - textHeight / 2.0F + i * font.lineHeight), textWidth);
            //Minecraft.getInstance().font.drawShadow(poseStack, text.get(i), position.x - textWidth / 2.0F, position.y - textHeight / 2.0F + i * font.lineHeight, color);
        }
    }

    public static List<Component> getText() {
        return text;
    }

    public static void setText(List<Component> display) {
        ExtractOverlay.text.clear();
        ExtractOverlay.text.addAll(display);
        ExtractOverlay.textWidth = text.stream().map(Minecraft.getInstance().font::width).max(Integer::compareTo).orElse(0);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean visible) {
        ExtractOverlay.visible = visible;
    }

    public static List<Component> createText(final BlockPos playerPoint, final BlockPos spawnPoint, final BlockPos center, final boolean canExtract) {
        final BlockPos playerPos = playerPoint.subtract(center);
        final BlockPos origin = spawnPoint.subtract(center);
        final boolean simple = center.getX() == 0 && center.getZ() == 0;
        final String greaterThan;
        final String lessThan;
        if(simple) {
            greaterThan = "+";
            lessThan = "-";
        } else {
            greaterThan = ">";
            lessThan = "<";
        }
        // create x component
        final boolean canExtractX = Mth.sign(playerPos.getX()) != Mth.sign(origin.getX());
        final String xSign = origin.getX() > 0 ? lessThan : greaterThan;
        final String xText = simple ? (xSign + "X") : ("X" + xSign + center.getX());
        final Component xComponent = Component.literal(xText).withStyle(canExtractX ? ChatFormatting.GREEN : ChatFormatting.RED);
        // create z component
        final boolean canExtractZ = Mth.sign(playerPos.getZ()) != Mth.sign(origin.getZ());
        final String zSign = origin.getZ() > 0 ? lessThan : greaterThan;
        final String zText = simple ? (zSign + "Z") : ("Z" + zSign + center.getZ());
        final Component zComponent = Component.literal(zText).withStyle(canExtractZ ? ChatFormatting.GREEN : ChatFormatting.RED);
        // create components
        final Component line0 = Component.translatable("message.overlay.extract.extract").withStyle(canExtract ? ChatFormatting.GREEN : ChatFormatting.RED);
        final Component line1 = Component.empty();
        line1.getSiblings().add(xComponent);
        line1.getSiblings().add(Component.literal("  "));
        line1.getSiblings().add(zComponent);
        // create list
        final List<Component> list = new ArrayList<>();
        list.add(line0);
        list.add(line1);
        return list;
    }
}
