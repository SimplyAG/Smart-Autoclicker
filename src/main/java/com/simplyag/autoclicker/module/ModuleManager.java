package com.simplyag.autoclicker.module;

import com.simplyag.autoclicker.config.SmartConfig;
import com.simplyag.autoclicker.util.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns every module, the presets, and the current {@link Session}. */
public final class ModuleManager {
    public static final AutoAttackModule AUTO_ATTACK = new AutoAttackModule();
    public static final AutoEatModule AUTO_EAT = new AutoEatModule();
    public static final HoldAttackModule HOLD_ATTACK = new HoldAttackModule();
    public static final HoldUseModule HOLD_USE = new HoldUseModule();
    public static final AutoWalkModule AUTO_WALK = new AutoWalkModule();
    public static final MobSweepModule MOB_SWEEP = new MobSweepModule();
    public static final ElytraCruiseModule ELYTRA_CRUISE = new ElytraCruiseModule();

    public static final List<Module> ALL = List.of(
            AUTO_ATTACK, AUTO_EAT, HOLD_ATTACK, HOLD_USE, AUTO_WALK, MOB_SWEEP, ELYTRA_CRUISE);

    /**
     * Modules that can't run at the same time. Auto Attack, Hold Left and Hold Right fight over the
     * mouse buttons (you can't swing while blocking or eating, and holding left never re-attacks);
     * Hold Right also eats whatever is in hand, which is Auto Eat's job; Mob Sweep would steer Auto
     * Walk in zigzags; and while flying, Elytra Cruise needs the mouse and your heading to itself.
     * Auto Eat pauses the others while it eats, so it goes with everything except Hold Right.
     */
    private static final Map<Module, Set<Module>> CONFLICTS = Map.of(
            AUTO_ATTACK, Set.of(HOLD_ATTACK, HOLD_USE, ELYTRA_CRUISE),
            AUTO_EAT, Set.of(HOLD_USE),
            HOLD_ATTACK, Set.of(AUTO_ATTACK, HOLD_USE, MOB_SWEEP, ELYTRA_CRUISE),
            HOLD_USE, Set.of(AUTO_ATTACK, AUTO_EAT, HOLD_ATTACK, MOB_SWEEP, ELYTRA_CRUISE),
            AUTO_WALK, Set.of(MOB_SWEEP, ELYTRA_CRUISE),
            MOB_SWEEP, Set.of(HOLD_ATTACK, HOLD_USE, AUTO_WALK, ELYTRA_CRUISE),
            ELYTRA_CRUISE, Set.of(AUTO_ATTACK, HOLD_ATTACK, HOLD_USE, AUTO_WALK, MOB_SWEEP));

    /** Mob Sweep only exists to widen Auto Attack, so it brings Auto Attack along. */
    private static final Map<Module, Module> REQUIRES = Map.of(MOB_SWEEP, AUTO_ATTACK);

    public static boolean conflicts(Module a, Module b) {
        return CONFLICTS.getOrDefault(a, Set.of()).contains(b);
    }

    /** Modules that can't run alongside this one, in sidebar order. */
    public static List<Module> conflictsOf(Module module) {
        return ALL.stream().filter(m -> conflicts(module, m)).toList();
    }

    public static @Nullable Module requirementOf(Module module) {
        return REQUIRES.get(module);
    }

    /** Modules that need this one to run (Mob Sweep for Auto Attack). */
    public static List<Module> dependentsOf(Module module) {
        return ALL.stream().filter(m -> REQUIRES.get(m) == module).toList();
    }

    public static @Nullable Module byId(String id) {
        return ALL.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * Makes a saved module list runnable: drops unknown ids and anything that conflicts with an
     * earlier entry, and adds requirements (or drops the module when its requirement can't fit).
     */
    public static List<String> normalize(List<String> ids) {
        List<Module> kept = new ArrayList<>();
        for (String id : ids) {
            Module module = byId(id);
            if (module == null || kept.contains(module)) continue;
            if (kept.stream().noneMatch(k -> conflicts(module, k))) kept.add(module);
        }
        for (Module module : List.copyOf(kept)) {
            Module required = REQUIRES.get(module);
            if (required == null || kept.contains(required)) continue;
            if (kept.stream().noneMatch(k -> conflicts(required, k))) {
                kept.add(required);
            } else {
                kept.remove(module);
            }
        }
        return new ArrayList<>(kept.stream().map(Module::id).toList());
    }

    /**
     * Makes room for a module that's starting: stops whatever conflicts with it and starts what it needs.
     *
     * @return the modules that were stopped, so the toast can say so
     */
    static List<Module> prepareStart(Module module) {
        List<Module> stopped = new ArrayList<>();
        Module required = REQUIRES.get(module);
        boolean wasStoppingAll = stoppingAll;
        stoppingAll = true; // a swap, not the end of the session
        for (Module other : ALL) {
            if (other.isRunning() && (conflicts(module, other) || required != null && conflicts(required, other))) {
                other.stop(false);
                stopped.add(other);
            }
        }
        stoppingAll = wasStoppingAll;
        if (required != null) required.start(false);
        return stopped;
    }

    /** A module stopped: anything that needed it stops too. */
    static void afterStop(Module module) {
        dependentsOf(module).forEach(d -> d.stop(false));
    }

    /** A named combination of modules. Which modules it includes is stored in the config and editable. */
    public record Preset(String id, Item icon) {
        public Component name() {
            return Component.translatable("smart-autoclicker.preset." + id);
        }

        public Component description() {
            return Component.translatable("smart-autoclicker.preset." + id + ".desc");
        }

        public List<Module> modules() {
            List<String> ids = SmartConfig.get().presets.getOrDefault(id, List.of());
            return ALL.stream().filter(m -> ids.contains(m.id())).toList();
        }

        public boolean includes(Module module) {
            return SmartConfig.get().presets.getOrDefault(id, List.of()).contains(module.id());
        }

        /**
         * Adds or removes a module. Adding swaps out anything it conflicts with and brings its
         * requirement; removing also removes whatever needed it.
         */
        public void setIncluded(Module module, boolean included) {
            List<String> ids = SmartConfig.get().presets.computeIfAbsent(id, k -> new ArrayList<>());
            ids.remove(module.id());
            if (included) {
                ids.removeIf(other -> {
                    Module m = byId(other);
                    return m != null && conflicts(module, m);
                });
                ids.add(module.id());
                Module required = REQUIRES.get(module);
                if (required != null && !ids.contains(required.id())) setIncluded(required, true);
            } else {
                dependentsOf(module).forEach(d -> ids.remove(d.id()));
            }
        }

        /** Modules that adding this one would take out of the preset. */
        public List<Module> wouldReplace(Module module) {
            Module required = REQUIRES.get(module);
            return modules().stream().filter(m -> m != module && (conflicts(module, m)
                    || required != null && !includes(required) && conflicts(required, m))).toList();
        }

        public boolean isRunning() {
            List<Module> modules = modules();
            return !modules.isEmpty() && modules.stream().allMatch(Module::isRunning);
        }
    }

    public static final List<Preset> PRESETS = List.of(
            new Preset("mob_grinder", Items.DIAMOND_SWORD),
            new Preset("sweep_grinder", Items.NETHERITE_SWORD),
            new Preset("afk_miner", Items.DIAMOND_PICKAXE),
            new Preset("elytra_trip", Items.ELYTRA));

    private static @Nullable Session session;
    private static boolean stoppingAll;

    private ModuleManager() {
    }

    public static boolean anyRunning() {
        for (Module module : ALL) {
            if (module.isRunning()) return true;
        }
        return false;
    }

    public static @Nullable Session session() {
        return session;
    }

    /** Runs every running module. Called at the end of each client tick. */
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (anyRunning()) stopAll(null);
            return;
        }
        for (Module module : ALL) {
            if (module.isRunning()) {
                module.tick(mc);
            }
        }
    }

    public static void applyPreset(Preset preset) {
        stoppingAll = true;
        for (Module module : ALL) {
            if (!preset.modules().contains(module)) module.stop(false);
        }
        stoppingAll = false;
        for (Module module : preset.modules()) {
            module.start(false);
        }
        Notifier.info(new ItemStack(preset.icon()), Component.translatable("smart-autoclicker.toast.preset"), preset.name());
    }

    /**
     * Stops everything.
     *
     * @param reason shown as an alert; null stops quietly (e.g. on disconnect)
     */
    public static void stopAll(@Nullable Component reason) {
        if (!anyRunning()) return;
        Minecraft mc = Minecraft.getInstance();
        Component summary = session != null && mc.player != null ? session.summary(mc.player) : null;
        stoppingAll = true;
        for (Module module : ALL) {
            module.stop(false);
        }
        stoppingAll = false;
        session = null;
        if (reason != null) {
            Notifier.alert(new ItemStack(Items.BARRIER), reason, summary, true);
        }
    }

    /** User pressed Stop All: a calm summary instead of an alarm. */
    public static void stopAllByUser() {
        if (!anyRunning()) {
            Notifier.info(new ItemStack(Items.CLOCK), Component.translatable("smart-autoclicker.toast.nothing_running"), null);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Component summary = session != null && mc.player != null ? session.summary(mc.player) : null;
        stoppingAll = true;
        for (Module module : ALL) {
            module.stop(false);
        }
        stoppingAll = false;
        session = null;
        Notifier.info(new ItemStack(Items.CLOCK), Component.translatable("smart-autoclicker.toast.stopped_all"), summary);
    }

    static void beforeModuleStart() {
        Minecraft mc = Minecraft.getInstance();
        if (session == null && mc.player != null) {
            session = new Session(mc.player);
        }
    }

    static void afterModuleStop() {
        if (!stoppingAll && !anyRunning()) {
            Minecraft mc = Minecraft.getInstance();
            if (session != null && mc.player != null && session.elapsedMillis() > 60_000) {
                Notifier.info(new ItemStack(Items.CLOCK), Component.translatable("smart-autoclicker.toast.session_over"), session.summary(mc.player));
            }
            session = null;
        }
    }
}
