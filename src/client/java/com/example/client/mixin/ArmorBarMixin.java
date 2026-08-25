package com.example.client.mixin;

import com.example.client.armor.ClientArmorState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Hud.class)
public abstract class ArmorBarMixin {

    @Unique
    private static final Identifier PAYDAY_ARMOR_EMPTY = Identifier.withDefaultNamespace("hud/armor_empty");
    @Unique
    private static final Identifier PAYDAY_ARMOR_HALF = Identifier.withDefaultNamespace("hud/armor_half");
    @Unique
    private static final Identifier PAYDAY_ARMOR_FULL = Identifier.withDefaultNamespace("hud/armor_full");
    @Unique
    private static final Random PAYDAY_RANDOM = new Random();

    @Inject(at = @At("HEAD"), method = "extractArmor", cancellable = true)
    private static void renderCustomArmor(
        GuiGraphicsExtractor graphics, Player player, int yLineBase,
        int numHealthRows, int healthRowHeight, int xLeft, CallbackInfo ci
    ) {
        ci.cancel();

        // Don't show armor HUD if player has no armor
        if (!ClientArmorState.INSTANCE.hasArmor()) {
            return;
        }

        float currentArmor = ClientArmorState.INSTANCE.getCurrentArmor();
        float maxArmor = ClientArmorState.INSTANCE.getMaxArmor();

        if (maxArmor <= 0f) {
            return;
        }

        boolean isBlinking = ClientArmorState.INSTANCE.isBlinking();
        boolean isShaking = ClientArmorState.INSTANCE.isShaking();

        int yLineArmor = yLineBase - (numHealthRows - 1) * healthRowHeight - 10;

        // Convert armor to half-icons (armor * 2 for half-icon precision)
        // Scale: 10 icons = maxArmor, each icon = maxArmor/10
        float armorPerIcon = maxArmor / 10f;
        float armorHalfIcons = (currentArmor / armorPerIcon) * 2f;

        // Blink alternates every 3 ticks
        Minecraft mc = Minecraft.getInstance();
        boolean showBlinkFrame = isBlinking && mc.level != null && (mc.level.getGameTime() / 3L) % 2L == 1L;

        for (int i = 0; i < 10; i++) {
            int xo = xLeft + i * 8;
            int yo = yLineArmor;

            // Shake effect: random Y offset when armor is low
            if (isShaking) {
                yo += PAYDAY_RANDOM.nextInt(2);
            }

            // Always render background (empty)
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAYDAY_ARMOR_EMPTY, xo, yo, 9, 9);

            // Calculate fill for this icon
            float iconFill = armorHalfIcons - (i * 2);

            if (iconFill >= 1.5f) {
                // Full icon
                if (showBlinkFrame) {
                    // Blink: render empty (flash effect)
                    // This creates the white flash by alternating visibility
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAYDAY_ARMOR_FULL, xo, yo, 9, 9);
                }
            } else if (iconFill >= 0.5f) {
                // Half icon
                if (showBlinkFrame) {
                    // Blink: render empty (flash effect)
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAYDAY_ARMOR_HALF, xo, yo, 9, 9);
                }
            }
            // else: empty (already rendered as background)
        }
    }
}
