package io.github.lucaargolo.slotlock.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow public abstract TextRenderer getTextRenderer();

    private static final Identifier SLOT_LOCK_TEXTURE = new Identifier(Slotlock.MOD_ID, "textures/gui/lock_overlay.png");
    private MatrixStack matrices;
    private int slotIndex = 0;

    @Inject(at = @At("HEAD"), method = "renderHotbar")
    public void renderHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        matrices = context.getMatrices();
        slotIndex = 0;
    }

    @Inject(at = @At("HEAD"), method = "renderHotbarItem")
    public void renderHotbarItem(DrawContext context, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (Slotlock.isLocked(slotIndex)) {
            if (player.getInventory().getStack(slotIndex).isEmpty()) {
                Slotlock.unlockSlot(slotIndex);
            }
            else {
                RenderSystem.setShaderTexture(0, SLOT_LOCK_TEXTURE);
                context.drawTexture(SLOT_LOCK_TEXTURE, x, y, 0, 0, 16, 16);
            }
        }
        slotIndex++;
        if(slotIndex == 9) {
            slotIndex = 40;
        }
    }

}
