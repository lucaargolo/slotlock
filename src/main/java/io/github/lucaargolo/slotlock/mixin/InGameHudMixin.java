package io.github.lucaargolo.slotlock.mixin;

import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin extends DrawableHelper {

    @Shadow @Final private MinecraftClient client;

    private static final Identifier SLOT_LOCK_TEXTURE = new Identifier(Slotlock.MOD_ID, "textures/gui/lock_overlay.png");
    private MatrixStack matrices;
    private int slotIndex = 0;

    @Inject(at = @At("HEAD"), method = "renderHotbar")
    public void renderHotbar(float f, MatrixStack matrixStack, CallbackInfo info) {
        matrices = matrixStack;
        slotIndex = 0;
    }

    @Inject(at = @At("HEAD"), method = "renderHotbarItem")
    public void renderHotbarItem(int i, int j, float f, PlayerEntity playerEntity, ItemStack itemStack, CallbackInfo info) {
        if(Slotlock.isLocked(slotIndex)) {
            this.client.getTextureManager().bindTexture(SLOT_LOCK_TEXTURE);
            this.drawTexture(matrices, i, j, 0, 0, 16, 16);
        }
        slotIndex++;
    }

}
