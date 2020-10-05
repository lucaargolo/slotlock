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
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
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
import java.nio.charset.StandardCharsets;
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
                        json = new String(Files.readAllBytes(slotLockPath), StandardCharsets.UTF_8);
                    }catch (Exception ignored) { }
                    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
                    JsonArray jsonArray = new JsonArray();
                    lockedSlots.forEach(jsonArray::add);
                    jsonObject.add(currentKey, jsonArray);
                    try {
                        Files.write(slotLockPath, jsonObject.toString().getBytes(StandardCharsets.UTF_8));
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
                Files.write(slotLockPath, "{ }".getBytes(StandardCharsets.UTF_8));
                Slotlock.LOGGER.info("Successfully created new slotlock file");
            }catch (Exception e){
                Slotlock.LOGGER.error("Failed to create slotlock file");
                Slotlock.LOGGER.error(e.getStackTrace());
            }
        }
        String json;
        try {
            json = new String(Files.readAllBytes(slotLockPath), StandardCharsets.UTF_8);
        }catch (Exception e) {
            Slotlock.LOGGER.error("Failed to load slotlock file");
            Slotlock.LOGGER.error(e.getStackTrace());
            json = "{ }";
        }
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonArray lockedSlotsJson = jsonObject.getAsJsonArray(key);
        if(lockedSlotsJson != null) {
            lockedSlotsJson.forEach(element -> {
                int slot = -1;
                try {
                    slot = element.getAsInt();
                }catch (Exception ignored) {}
                if(slot != -1)
                    Slotlock.lockedSlots.add(slot);
            });
        }
        Slotlock.LOGGER.info("Successfully loaded slotlock file");

    }

    public static void handleMouseClick(ScreenHandler handler, PlayerInventory playerInventory,  Slot slot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        if(slot != null && slot.inventory == playerInventory && Slotlock.isLocked(((SlotAccessor) slot).getIndex())) {
            info.cancel();
        }
        if(actionType == SlotActionType.QUICK_MOVE && invSlot >= 0 && invSlot < handler.slots.size()) {
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
        if(keyCode != 256 && !MinecraftClient.getInstance().options.keyInventory.matchesKey(keyCode, scanCode)) {
            Slot finalSlot = focusedSlot;
            if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            if(finalSlot != null) {
                int index = ((SlotAccessor) finalSlot).getIndex();
                if (Slotlock.lockBinding.matchesKey(keyCode, scanCode) && finalSlot.inventory == playerInventory) {
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

    public static void handleHotbarKeyPressed(Slot focusedSlot, PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        Slot finalSlot = focusedSlot;
        if(finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
            finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
        }
        if(finalSlot != null && finalSlot.inventory == playerInventory && Slotlock.isLocked(((SlotAccessor) finalSlot).getIndex())) {
            info.setReturnValue(false);
        }
    }

    public static void handleDropSelectedItem(PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        int selectedSlot = playerInventory.selectedSlot;
        if(Slotlock.isLocked(selectedSlot)) {
            info.setReturnValue(false);
        }
    }

    public static void handleInputEvents(GameOptions options, ClientPlayerEntity player) {
        boolean toPress = false;
        while(options.keySwapHands.wasPressed()) {
            if (!player.isSpectator()) {
                int selectedSlot = player.inventory.selectedSlot;
                if(!Slotlock.isLocked(selectedSlot)) {
                    toPress = true;
                }
            }
        }
        if(toPress) KeyBinding.onKeyPressed(((KeyBindingAccessor) options.keySwapHands).getBoundKey());
    }




}
