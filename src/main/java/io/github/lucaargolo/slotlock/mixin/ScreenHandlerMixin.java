package io.github.lucaargolo.slotlock.mixin;

import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Shadow @Final public List<Slot> slots;

    @Inject(at = @At("HEAD"), method = "onSlotClick", cancellable = true)
    public void onSlotClick(int i, int j, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> info) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        if(i >= 0 && i < this.slots.size()) {
            Slot finalSlot = this.slots.get(i);
            if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            if (finalSlot.inventory == playerEntity.inventory && Slotlock.isLocked(((SlotAccessor) finalSlot).getIndex())) {
                info.setReturnValue(ItemStack.EMPTY);
            }
        }
    }


}
