package com.simplyag.autoclicker.ui.panel;

import com.simplyag.autoclicker.util.Compat;
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
public interface Setting {
    String P = "smart-autoclicker.option.";
    BooleanSupplier ALWAYS = () -> true;

    String id();

    /** False greys the row out, e.g. "Fixed Delay" while timing is Perfect. */
    BooleanSupplier enabled();

    default Component label() {
        return Compat.translatable(P + id());
    }

    default Component tooltip() {
        return Compat.translatable(P + id() + ".tooltip");
    }

    // Plain classes rather than records: Minecraft 1.14 - 1.16 runs on Java 8. Accessors keep the record-style names.

    final class Toggle implements Setting {
        private final String id;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private final BooleanSupplier enabled;

        public Toggle(String id, BooleanSupplier get, Consumer<Boolean> set, BooleanSupplier enabled) {
            this.id = id;
            this.get = get;
            this.set = set;
            this.enabled = enabled;
        }

        public Toggle(String id, BooleanSupplier get, Consumer<Boolean> set) {
            this(id, get, set, ALWAYS);
        }

        @Override
        public String id() {
            return id;
        }

        public BooleanSupplier get() {
            return get;
        }

        public Consumer<Boolean> set() {
            return set;
        }

        @Override
        public BooleanSupplier enabled() {
            return enabled;
        }
    }

    final class Range implements Setting {
        private final String id;
        private final int min;
        private final int max;
        private final int step;
        private final IntSupplier get;
        private final IntConsumer set;
        private final IntFunction<String> format;
        private final BooleanSupplier enabled;

        public Range(String id, int min, int max, int step, IntSupplier get, IntConsumer set, IntFunction<String> format,
                     BooleanSupplier enabled) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.format = format;
            this.enabled = enabled;
        }

        public Range(String id, int min, int max, int step, IntSupplier get, IntConsumer set, IntFunction<String> format) {
            this(id, min, max, step, get, set, format, ALWAYS);
        }

        @Override
        public String id() {
            return id;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        public int step() {
            return step;
        }

        public IntSupplier get() {
            return get;
        }

        public IntConsumer set() {
            return set;
        }

        public IntFunction<String> format() {
            return format;
        }

        @Override
        public BooleanSupplier enabled() {
            return enabled;
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

    final class Choice<E extends Enum<E>> implements Setting {
        private final String id;
        private final E[] values;
        private final Supplier<E> get;
        private final Consumer<E> set;
        private final BooleanSupplier enabled;

        public Choice(String id, E[] values, Supplier<E> get, Consumer<E> set, BooleanSupplier enabled) {
            this.id = id;
            this.values = values;
            this.get = get;
            this.set = set;
            this.enabled = enabled;
        }

        public Choice(String id, E[] values, Supplier<E> get, Consumer<E> set) {
            this(id, values, get, set, ALWAYS);
        }

        @Override
        public String id() {
            return id;
        }

        public E[] values() {
            return values;
        }

        public Supplier<E> get() {
            return get;
        }

        public Consumer<E> set() {
            return set;
        }

        @Override
        public BooleanSupplier enabled() {
            return enabled;
        }

        public Component valueLabel() {
            return Compat.translatable(P + id + "." + get.get().name().toLowerCase());
        }

        public void cycle(int direction) {
            int next = Math.floorMod(get.get().ordinal() + direction, values.length);
            set.accept(values[next]);
        }
    }

    final class Hotkey implements Setting {
        private final String id;
        private final Supplier<KeyMapping> key;
        private final BooleanSupplier enabled;

        public Hotkey(String id, Supplier<KeyMapping> key, BooleanSupplier enabled) {
            this.id = id;
            this.key = key;
            this.enabled = enabled;
        }

        public Hotkey(String id, Supplier<KeyMapping> key) {
            this(id, key, ALWAYS);
        }

        @Override
        public String id() {
            return id;
        }

        public Supplier<KeyMapping> key() {
            return key;
        }

        @Override
        public BooleanSupplier enabled() {
            return enabled;
        }
    }
}
