package io.github.lucaargolo.slotlock.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.slotlock.Slotlock;
import io.github.lucaargolo.slotlock.mixed.HandledScreenMixed;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements HandledScreenMixed {

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    private static final Identifier SLOT_LOCK_TEXTURE = new Identifier(Slotlock.MOD_ID, "textures/gui/lock_overlay.png");
    @Shadow @Nullable protected Slot focusedSlot;
    @Shadow @Final protected T handler;

    private PlayerInventory slotlock$playerInventory;

    @Override
    public PlayerInventory slotlock$getPlayerInventory() {
        return slotlock$playerInventory;
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void onInit(T handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        slotlock$playerInventory = inventory;
    }

    @Inject(at = @At("HEAD"), method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", cancellable = true)
    public void onMouseClick(Slot slot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        Slotlock.handleMouseClick(handler, slotlock$playerInventory, slot, null, invSlot, clickData, actionType, info);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        Slotlock.handleKeyPressed(focusedSlot, slotlock$playerInventory, keyCode, scanCode, info);
    }

    @Inject(at = @At("HEAD"), method = "handleHotbarKeyPressed", cancellable = true)
    public void handleHotbarKeyPressed(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> info) {
        Slotlock.handleHotbarKeyPressed(focusedSlot, slotlock$playerInventory, info);
    }

    @Inject(at = @At("HEAD"), method = "drawMouseoverTooltip", cancellable = true)
    public void drawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo info) {
        if (handler.getCursorStack().isEmpty() && this.focusedSlot != null && this.focusedSlot.inventory == this.slotlock$playerInventory) {
            Slot finalSlot = focusedSlot;
            if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            if(finalSlot != null && Slotlock.isLocked(((SlotAccessor) finalSlot).getIndex())) {
                ItemStack stack = finalSlot.hasStack() ? this.focusedSlot.getStack() : ItemStack.EMPTY;
                List<Text> tooltip = this.getTooltipFromItem(stack);
                tooltip.add(Text.translatable("slotlock.locked"));
                tooltip.add(Text.translatable("slotlock.press1").append(Slotlock.lockBinding.getBoundKeyLocalizedText().copy().append(Text.translatable("slotlock.press2"))));
                this.renderTooltip(matrices, tooltip, x, y);
                info.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "drawSlot")
    public void drawSlot(MatrixStack matrices, Slot slot, CallbackInfo info) {
        Slot finalSlot = slot;
        if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
            finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
        }
        if(this.client != null && slot.inventory == slotlock$playerInventory && Slotlock.isLocked(((SlotAccessor) finalSlot).getIndex())) {
            if (!finalSlot.hasStack()) {
                Slotlock.unlockSlot(((SlotAccessor) finalSlot).getIndex());
                return;
            }
            RenderSystem.setShaderTexture(0, SLOT_LOCK_TEXTURE);
            this.drawTexture(matrices, slot.x, slot.y, 0, 0, 16, 16);
        }
    }

}
