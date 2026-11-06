package com.simplyag.autoclicker.ui.panel;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * One row in the control panel. Labels come from "smart-autoclicker.option.&lt;id&gt;",
 * hover help from "smart-autoclicker.option.&lt;id&gt;.tooltip".
 */
public sealed interface Setting {
    String P = "smart-autoclicker.option.";
    BooleanSupplier ALWAYS = () -> true;

    String id();

    /** False greys the row out, e.g. "Fixed Delay" while timing is Perfect. */
    BooleanSupplier enabled();

    default Component label() {
        return Component.translatable(P + id());
    }

    default Component tooltip() {
        return Component.translatable(P + id() + ".tooltip");
    }

    record Toggle(String id, BooleanSupplier get, Consumer<Boolean> set, BooleanSupplier enabled) implements Setting {
        public Toggle(String id, BooleanSupplier get, Consumer<Boolean> set) {
            this(id, get, set, ALWAYS);
        }
    }

    record Range(String id, int min, int max, int step, IntSupplier get, IntConsumer set, IntFunction<String> format,
                 BooleanSupplier enabled) implements Setting {
        public Range(String id, int min, int max, int step, IntSupplier get, IntConsumer set, IntFunction<String> format) {
            this(id, min, max, step, get, set, format, ALWAYS);
        }

        /** Snaps a 0..1 slider position to a valid value. */
        public int valueAt(double fraction) {
            double raw = min + Mth.clamp(fraction, 0.0, 1.0) * (max - min);
            int snapped = min + (int) Math.round((raw - min) / step) * step;
            return Mth.clamp(snapped, min, max);
        }

        public double fraction() {
            return max == min ? 0 : (double) (get.getAsInt() - min) / (max - min);
        }
    }

    record Choice<E extends Enum<E>>(String id, E[] values, Supplier<E> get, Consumer<E> set,
                                     BooleanSupplier enabled) implements Setting {
        public Choice(String id, E[] values, Supplier<E> get, Consumer<E> set) {
            this(id, values, get, set, ALWAYS);
        }

        public Component valueLabel() {
            return Component.translatable(P + id + "." + get.get().name().toLowerCase());
        }

        public void cycle(int direction) {
            int next = Math.floorMod(get.get().ordinal() + direction, values.length);
            set.accept(values[next]);
        }
    }

    record Hotkey(String id, Supplier<KeyMapping> key, BooleanSupplier enabled) implements Setting {
        public Hotkey(String id, Supplier<KeyMapping> key) {
            this(id, key, ALWAYS);
        }
    }
}
