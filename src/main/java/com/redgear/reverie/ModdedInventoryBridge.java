package com.redgear.reverie;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Predicate;

/** Runtime bridges keep Curios and Accessories optional while clearing functional and cosmetic slots. */
public final class ModdedInventoryBridge {
    private ModdedInventoryBridge() {}
    public static void clearAll(ServerPlayer player) {
        clearCurios(player);
        clearAccessories(player);
        repairWhoopeeCushionAttribute(player);
    }

    public static void refreshAll(ServerPlayer player) {
        refreshCurios(player);
        refreshAccessories(player);
        repairWhoopeeCushionAttribute(player);
    }

    /** Removes prohibited equipped stacks and returns them to the ordinary inventory when possible. */
    public static void ejectBlocked(ServerPlayer player, Predicate<ItemStack> blocked) {
        ejectBlockedCurios(player, blocked);
        ejectBlockedAccessories(player, blocked);
        repairWhoopeeCushionAttribute(player);
    }

    private static void returnToInventory(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) player.getInventory().add(stack);
    }

    private static void ejectBlockedCurios(ServerPlayer player, Predicate<ItemStack> blocked) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return;
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Object stacks : curios.values()) {
                ejectHandler(player, stacks.getClass().getMethod("getStacks").invoke(stacks), blocked);
                ejectHandler(player, stacks.getClass().getMethod("getCosmeticStacks").invoke(stacks), blocked);
                invokeIfPresent(stacks, "update");
            }
            invokeIfPresent(handler, "processSlots");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to reject blocked Curios equipment", error); }
    }

    private static void ejectHandler(ServerPlayer player, Object handler, Predicate<ItemStack> blocked) throws ReflectiveOperationException {
        int count = (int) handler.getClass().getMethod("getSlots").invoke(handler);
        Method get = handler.getClass().getMethod("getStackInSlot", int.class);
        Method set = handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = (ItemStack) get.invoke(handler, slot);
            if (!stack.isEmpty() && blocked.test(stack)) {
                set.invoke(handler, slot, ItemStack.EMPTY);
                returnToInventory(player, stack);
            }
        }
    }

    private static void ejectBlockedAccessories(ServerPlayer player, Predicate<ItemStack> blocked) {
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability == null) return;
            Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
            for (Object container : containers.values()) {
                ejectContainer(player, invokeFirst(container, "getAccessories", "getAccessoryHandler"), blocked);
                ejectContainer(player, invokeFirst(container, "getCosmeticAccessories", "getCosmeticHandler"), blocked);
                try { container.getClass().getMethod("markChanged", boolean.class).invoke(container, true); }
                catch (NoSuchMethodException ignored) {}
            }
            invokeIfPresent(capability, "updateContainers");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to reject blocked Accessories equipment", error); }
    }

    private static void ejectContainer(ServerPlayer player, Object container, Predicate<ItemStack> blocked) throws ReflectiveOperationException {
        int count = (int) container.getClass().getMethod("getContainerSize").invoke(container);
        Method get = container.getClass().getMethod("getItem", int.class);
        Method set = container.getClass().getMethod("setItem", int.class, ItemStack.class);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = (ItemStack) get.invoke(container, slot);
            if (!stack.isEmpty() && blocked.test(stack)) {
                set.invoke(container, slot, ItemStack.EMPTY);
                returnToInventory(player, stack);
            }
        }
    }

    public static CompoundTag captureAll(ServerPlayer player) {
        CompoundTag result = new CompoundTag();
        captureCurios(player, result);
        captureAccessories(player, result);
        return result;
    }

    public static void restoreAll(ServerPlayer player, CompoundTag saved) {
        clearAll(player);
        restoreCurios(player, saved.getCompound("Curios"));
        restoreAccessories(player, saved.getCompound("Accessories"));
    }

    private static ListTag captureHandler(ServerPlayer player, Object handler) throws ReflectiveOperationException {
        ListTag saved = new ListTag();
        int count = (int) handler.getClass().getMethod("getSlots").invoke(handler);
        Method get = handler.getClass().getMethod("getStackInSlot", int.class);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = (ItemStack) get.invoke(handler, slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Stack", stack.save(player.registryAccess()));
            saved.add(entry);
        }
        return saved;
    }

    private static void restoreHandler(ServerPlayer player, Object handler, ListTag saved) throws ReflectiveOperationException {
        Method set = handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
        int count = (int) handler.getClass().getMethod("getSlots").invoke(handler);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < count) set.invoke(handler, slot,
                    ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Stack")));
        }
    }

    private static void captureCurios(ServerPlayer player, CompoundTag output) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return;
            CompoundTag all = new CompoundTag();
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Map.Entry<?, ?> entry : curios.entrySet()) {
                CompoundTag slot = new CompoundTag();
                Object stacks = entry.getValue();
                slot.put("Items", captureHandler(player, stacks.getClass().getMethod("getStacks").invoke(stacks)));
                slot.put("Cosmetics", captureHandler(player, stacks.getClass().getMethod("getCosmeticStacks").invoke(stacks)));
                all.put(String.valueOf(entry.getKey()), slot);
            }
            output.put("Curios", all);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to capture Curios slots", error); }
    }

    private static void restoreCurios(ServerPlayer player, CompoundTag all) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return;
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Map.Entry<?, ?> entry : curios.entrySet()) {
                CompoundTag slot = all.getCompound(String.valueOf(entry.getKey()));
                Object stacks = entry.getValue();
                restoreHandler(player, stacks.getClass().getMethod("getStacks").invoke(stacks), slot.getList("Items", 10));
                restoreHandler(player, stacks.getClass().getMethod("getCosmeticStacks").invoke(stacks), slot.getList("Cosmetics", 10));
                invokeIfPresent(stacks, "update");
            }
            invokeIfPresent(handler, "processSlots");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to restore Curios slots", error); }
    }

    private static void captureAccessories(ServerPlayer player, CompoundTag output) {
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability == null) return;
            CompoundTag all = new CompoundTag();
            Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
            for (Map.Entry<?, ?> entry : containers.entrySet()) {
                CompoundTag slot = new CompoundTag();
                Object container = entry.getValue();
                slot.put("Items", captureContainer(player, invokeFirst(container, "getAccessories", "getAccessoryHandler")));
                slot.put("Cosmetics", captureContainer(player, invokeFirst(container, "getCosmeticAccessories", "getCosmeticHandler")));
                all.put(String.valueOf(entry.getKey()), slot);
            }
            output.put("Accessories", all);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to capture Accessories slots", error); }
    }

    private static ListTag captureContainer(ServerPlayer player, Object container) throws ReflectiveOperationException {
        ListTag saved = new ListTag();
        int count = (int) container.getClass().getMethod("getContainerSize").invoke(container);
        Method get = container.getClass().getMethod("getItem", int.class);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = (ItemStack) get.invoke(container, slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag(); entry.putInt("Slot", slot);
            entry.put("Stack", stack.save(player.registryAccess())); saved.add(entry);
        }
        return saved;
    }

    private static void restoreAccessories(ServerPlayer player, CompoundTag all) {
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability == null) return;
            Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
            for (Map.Entry<?, ?> entry : containers.entrySet()) {
                Object container = entry.getValue(); CompoundTag slot = all.getCompound(String.valueOf(entry.getKey()));
                restoreContainer(player, invokeFirst(container, "getAccessories", "getAccessoryHandler"), slot.getList("Items", 10));
                restoreContainer(player, invokeFirst(container, "getCosmeticAccessories", "getCosmeticHandler"), slot.getList("Cosmetics", 10));
                try { container.getClass().getMethod("markChanged", boolean.class).invoke(container, true); } catch (NoSuchMethodException ignored) {}
            }
            invokeIfPresent(capability, "updateContainers");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to restore Accessories slots", error); }
    }

    private static void restoreContainer(ServerPlayer player, Object container, ListTag saved) throws ReflectiveOperationException {
        Method set = container.getClass().getMethod("setItem", int.class, ItemStack.class);
        int count = (int) container.getClass().getMethod("getContainerSize").invoke(container);
        for (int i = 0; i < saved.size(); i++) { CompoundTag entry = saved.getCompound(i); int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < count) set.invoke(container, slot, ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Stack"))); }
    }

    private static void clearCurios(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return;
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Object stacks : curios.values()) {
                clearHandler(stacks.getClass().getMethod("getStacks").invoke(stacks));
                clearHandler(stacks.getClass().getMethod("getCosmeticStacks").invoke(stacks));
                invokeIfPresent(stacks, "update");
            }
            invokeIfPresent(handler, "processSlots");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to clear Curios slots", error); }
    }

    private static void clearAccessories(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability == null) return;
            Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
            for (Object container : containers.values()) {
                clearAccessoriesContainer(invokeFirst(container, "getAccessories", "getAccessoryHandler"));
                clearAccessoriesContainer(invokeFirst(container, "getCosmeticAccessories", "getCosmeticHandler"));
                try { container.getClass().getMethod("markChanged", boolean.class).invoke(container, true); }
                catch (NoSuchMethodException ignored) {}
            }
            invokeIfPresent(capability, "updateContainers");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to clear Accessories slots", error); }
    }

    private static Object invokeFirst(Object target, String... names) throws ReflectiveOperationException {
        for (String name : names) try { return target.getClass().getMethod(name).invoke(target); }
        catch (NoSuchMethodException ignored) {}
        throw new NoSuchMethodException(String.join("/", names));
    }

    private static void invokeIfPresent(Object target, String name) throws ReflectiveOperationException {
        try { target.getClass().getMethod(name).invoke(target); }
        catch (NoSuchMethodException ignored) {}
    }

    private static void refreshCurios(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return;
            Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
            for (Object stacks : curios.values()) invokeIfPresent(stacks, "update");
            invokeIfPresent(handler, "processSlots");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to refresh Curios slots", error); }
    }

    private static void refreshAccessories(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability == null) return;
            Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
            for (Object container : containers.values()) {
                try { container.getClass().getMethod("markChanged", boolean.class).invoke(container, true); }
                catch (NoSuchMethodException ignored) {}
            }
            invokeIfPresent(capability, "updateContainers");
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to refresh Accessories slots", error); }
    }

    private static void repairWhoopeeCushionAttribute(ServerPlayer player) {
        ResourceLocation cushion = ResourceLocation.fromNamespaceAndPath("artifacts", "whoopee_cushion");
        if (containsFunctionalItem(player, cushion)) return;
        ResourceLocation flatulence = ResourceLocation.fromNamespaceAndPath("artifacts", "flatulence");
        BuiltInRegistries.ATTRIBUTE.getHolder(flatulence).ifPresent(attribute -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && !instance.getModifiers().isEmpty()) instance.removeModifiers();
        });
    }

    private static boolean containsFunctionalItem(ServerPlayer player, ResourceLocation wanted) {
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optional = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler != null) {
                Map<?, ?> curios = (Map<?, ?>) handler.getClass().getMethod("getCurios").invoke(handler);
                for (Object stacks : curios.values()) if (handlerContains(
                        stacks.getClass().getMethod("getStacks").invoke(stacks), wanted)) return true;
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to inspect Curios slots", error); }
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = api.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (capability != null) {
                Map<?, ?> containers = (Map<?, ?>) capability.getClass().getMethod("getContainers").invoke(capability);
                for (Object container : containers.values()) if (containerContains(
                        invokeFirst(container, "getAccessories", "getAccessoryHandler"), wanted)) return true;
            }
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) { Reverie.LOGGER.error("Failed to inspect Accessories slots", error); }
        return false;
    }

    private static boolean handlerContains(Object handler, ResourceLocation wanted) throws ReflectiveOperationException {
        int count = (int) handler.getClass().getMethod("getSlots").invoke(handler);
        Method get = handler.getClass().getMethod("getStackInSlot", int.class);
        for (int slot = 0; slot < count; slot++) if (BuiltInRegistries.ITEM.getKey(
                ((ItemStack) get.invoke(handler, slot)).getItem()).equals(wanted)) return true;
        return false;
    }

    private static boolean containerContains(Object container, ResourceLocation wanted) throws ReflectiveOperationException {
        int count = (int) container.getClass().getMethod("getContainerSize").invoke(container);
        Method get = container.getClass().getMethod("getItem", int.class);
        for (int slot = 0; slot < count; slot++) if (BuiltInRegistries.ITEM.getKey(
                ((ItemStack) get.invoke(container, slot)).getItem()).equals(wanted)) return true;
        return false;
    }

    private static void clearHandler(Object handler) throws ReflectiveOperationException {
        if (handler == null) return;
        Method getSlots = handler.getClass().getMethod("getSlots");
        Method set = handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
        int count = (int) getSlots.invoke(handler);
        for (int slot = 0; slot < count; slot++) set.invoke(handler, slot, ItemStack.EMPTY);
    }

    private static void clearAccessoriesContainer(Object container) throws ReflectiveOperationException {
        if (container == null) return;
        try {
            container.getClass().getMethod("clearContent").invoke(container);
            return;
        } catch (NoSuchMethodException ignored) {}
        Method size = container.getClass().getMethod("getContainerSize");
        Method set = container.getClass().getMethod("setItem", int.class, ItemStack.class);
        int count = (int) size.invoke(container);
        for (int slot = 0; slot < count; slot++) set.invoke(container, slot, ItemStack.EMPTY);
    }
}
