package io.github.lucaargolo.slotlock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.lucaargolo.slotlock.mixin.CreativeSlotAccessor;
import io.github.lucaargolo.slotlock.mixin.KeyBindingAccessor;
import io.github.lucaargolo.slotlock.mixin.ServerWorldAccessor;
import io.github.lucaargolo.slotlock.mixin.SlotAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.integrated.IntegratedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

public class Slotlock implements ClientModInitializer {

    public static String MOD_ID = "slotlock";
    public static Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static KeyBinding lockBinding;
    public static String currentKey = "world";
    public static boolean isSaveDirty = false;

    private long lastDirtyCheck = System.currentTimeMillis();

    @Override
    public void onInitializeClient() {
        lockBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slotlock",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.inventory"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long currentDirtyCheck = System.currentTimeMillis();
            if(currentDirtyCheck - lastDirtyCheck > 2000) {
                if(isSaveDirty) {
                    File slotLockFile = new File(MinecraftClient.getInstance().runDirectory, "slotlock.json");
                    Path slotLockPath = Paths.get(slotLockFile.getAbsolutePath());
                    String json = "{ }";
                    try {
                        json = Files.readString(slotLockPath);
                    }catch (Exception ignored) { }
                    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
                    JsonArray jsonArray = new JsonArray();
                    lockedSlots.forEach(jsonArray::add);
                    jsonObject.add(currentKey, jsonArray);
                    try {
                        Files.writeString(slotLockPath, jsonObject.toString());
                        Slotlock.LOGGER.info("Successfully updated slotlock file");
                    }catch (Exception e) {
                        Slotlock.LOGGER.error("Failed to update slotlock file");
                    }
                    isSaveDirty = false;
                }
                lastDirtyCheck = currentDirtyCheck;
            }
        });
    }

    private static LinkedHashSet<Integer> lockedSlots = new LinkedHashSet<>();

    @SuppressWarnings({"unchecked", "unused"})
    public static LinkedHashSet<Integer> getLockedSlots() {
        return (LinkedHashSet<Integer>) lockedSlots.clone();
    }

    public static boolean isLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    public static void lockSlot(int slot) {
        lockedSlots.add(slot);
        isSaveDirty = true;
    }

    public static void unlockSlot(int slot) {
        lockedSlots.remove(slot);
        isSaveDirty = true;
    }

    public static void handleJoinWorld(MinecraftClient client) {
        String key = "world";
        if(client.isIntegratedServerRunning()) {
            IntegratedServer server = client.getServer();
            if(server != null) {
                key = ((ServerWorldAccessor) server.getOverworld()).getWorldProperties().getLevelName();
            }
        }else{
            ServerInfo info = client.getCurrentServerEntry();
            if(info != null) {
                key = info.address;
            }
        }
        Slotlock.LOGGER.info("Loading slotlock file");
        currentKey = key;
        Slotlock.lockedSlots = new LinkedHashSet<>();
        File slotLockFile = new File(MinecraftClient.getInstance().runDirectory, "slotlock.json");
        Path slotLockPath = Paths.get(slotLockFile.getAbsolutePath());
        if(Files.notExists(slotLockPath)) {
            try{
                Slotlock.LOGGER.info("File not found! Creating new slotlock file");
                Files.writeString(slotLockPath, "{ }");
                Slotlock.LOGGER.info("Successfully created new slotlock file");
            }catch (Exception e){
                Slotlock.LOGGER.error("An error occurred while creating the slotlock file.", e);
            }
        }
        String json;
        try {
            json = Files.readString(slotLockPath);
        }catch (Exception e) {
            Slotlock.LOGGER.error("An error occurred while loading the slotlock file.", e);
            json = "{ }";
        }
        try {
            JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
            JsonArray lockedSlotsJson = jsonObject.getAsJsonArray(key);
            if (lockedSlotsJson != null) {
                lockedSlotsJson.forEach(element -> {
                    int slot = -1;
                    try {
                        slot = element.getAsInt();
                    } catch (Exception ignored) {
                    }
                    if (slot != -1)
                        Slotlock.lockedSlots.add(slot);
                });
            }
            Slotlock.LOGGER.info("Successfully loaded slotlock file");
        }catch (Exception e) {
            Slotlock.LOGGER.error("An error occurred while reading the slotlock file.", e);
        }

    }

    public static void handleMouseClick(ScreenHandler handler, PlayerInventory playerInventory, Slot slot, Slot deleteItemSlot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        if(slot != null && slot.inventory == playerInventory && Slotlock.isLocked(((SlotAccessor) slot).getIndex())) {
            info.cancel();
        }

        if(slot != null && actionType == SlotActionType.PICKUP_ALL) {
            ItemStack pickedStack = handler.getCursorStack();
            handler.slots.forEach(handlerSlot -> {
                int slotIndex = ((SlotAccessor) handlerSlot).getIndex();
                if(handlerSlot.inventory == playerInventory && Slotlock.isLocked(slotIndex) && canMergeItems(pickedStack, handlerSlot.getStack())) {
                    info.cancel();
                }
            });
        }

        if(actionType == SlotActionType.QUICK_MOVE && invSlot >= 0 && invSlot < handler.slots.size()) {
            if (slot != null && slot == deleteItemSlot) {
                for (int i = 0; i < playerInventory.size(); ++i) {
                    if (!Slotlock.isLocked(i)) {
                        playerInventory.removeStack(i);
                    }
                }
                info.cancel();
                return;
            }

            Slot slot2 = handler.slots.get(invSlot);
            if(slot2.inventory == playerInventory && Slotlock.isLocked(((SlotAccessor) slot2).getIndex())) {
                info.cancel();
            }
        }

        if(actionType == SlotActionType.SWAP) {
            for(Slot slot3 : handler.slots) {
                Slot finalSlot = slot3;
                if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                    finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
                }
                int index = ((SlotAccessor) finalSlot).getIndex();
                if(finalSlot.inventory == playerInventory && index == clickData && Slotlock.isLocked(index)) {
                    info.cancel();
                }
            }
        }
    }

    public static void handleKeyPressed(Slot focusedSlot, PlayerInventory playerInventory, int keyCode, int scanCode, CallbackInfoReturnable<Boolean> info) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        if(keyCode != 256 && !MinecraftClient.getInstance().options.inventoryKey.matchesKey(keyCode, scanCode)) {
            Slot finalSlot = focusedSlot;
            if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            if(finalSlot != null) {
                int index = ((SlotAccessor) finalSlot).getIndex();
                if(finalSlot.inventory == playerInventory) {
                    if (Slotlock.lockBinding.matchesKey(keyCode, scanCode)) {
                        boolean locked = Slotlock.isLocked(index);
                        if (locked) {
                            Slotlock.unlockSlot(index);
                        } else if (!finalSlot.getStack().isEmpty()) {
                            Slotlock.lockSlot(index);
                        }
                    } else {
                        if (Slotlock.isLocked(index)) {
                            info.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }

    public static void handleHotbarKeyPressed(Slot focusedSlot, PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        Slot finalSlot = focusedSlot;
        if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
            finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
        }
        if(finalSlot != null && finalSlot.inventory == playerInventory && Slotlock.isLocked(((SlotAccessor) finalSlot).getIndex())) {
            info.setReturnValue(false);
        }
    }

    public static void handleDropSelectedItem(PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        int selectedSlot = playerInventory.selectedSlot;
        if(Slotlock.isLocked(selectedSlot)) {
            info.setReturnValue(false);
        }
    }

    public static void handleInputEvents(GameOptions options, ClientPlayerEntity player) {
        if(!MinecraftClient.getInstance().isOnThread()) return;
        boolean toPress = false;
        while(options.swapHandsKey.wasPressed()) {
            if (!player.isSpectator()) {
                int selectedSlot = player.getInventory().selectedSlot;
                if(!Slotlock.isLocked(selectedSlot)) {
                    toPress = true;
                }
            }
        }
        if(toPress) KeyBinding.onKeyPressed(((KeyBindingAccessor) options.swapHandsKey).getBoundKey());
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (first.getItem() != second.getItem()) {
            return false;
        } else if (first.getDamage() != second.getDamage()) {
            return false;
        } else if (first.getCount() > first.getMaxCount()) {
            return false;
        } else {
            return ItemStack.areNbtEqual(first, second);
        }
    }

}
