package io.github.lucaargolo.slotlock.mixin;

import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public GameOptions options;

    @Shadow @Nullable public ClientPlayerEntity player;

    @SuppressWarnings("ConstantConditions")
    @Inject(at = @At("HEAD"), method = "joinWorld")
    public void joinWorld(ClientWorld world, CallbackInfo info) {
        Slotlock.handleJoinWorld(((MinecraftClient) ((Object) this)));
    }

    @Inject(at = @At("HEAD"), method = "handleInputEvents")
    public void handleInputEvents(CallbackInfo info) {
        Slotlock.handleInputEvents(options, player);
    }


    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSlotWithStack(Lnet/minecraft/item/ItemStack;)I"), method = "doItemPick", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void handleItemPick(CallbackInfo ci, boolean bl, BlockEntity blockEntity, ItemStack itemStack, HitResult.Type type, PlayerInventory playerInventory) {
        Slotlock.handleItemPick(playerInventory.selectedSlot, ci);
    }

}
