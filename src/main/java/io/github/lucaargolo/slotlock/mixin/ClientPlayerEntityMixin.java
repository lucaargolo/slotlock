package io.github.lucaargolo.slotlock.mixin;

import com.mojang.authlib.GameProfile;
import io.github.lucaargolo.slotlock.Slotlock;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At("HEAD"), method = "dropSelectedItem", cancellable = true)
    public void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> info){
        Slotlock.handleDropSelectedItem(this.getInventory(), info);
    }

//    @Inject(at = @At("HEAD"), method = "tick")
//    public void tick(CallbackInfo info) {
//        Iterator<Integer> it =  Slotlock.getLockedSlots().iterator();
//        while (it.hasNext()) {
//            int index = it.next();
//            if(this.inventory.getStack(index).isEmpty()) {
//                it.remove();
//                Slotlock.unlockSlot(index);
//            }
//        }
//    }

}
