package io.github.lucaargolo.slotlock.mixin;

import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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


}
