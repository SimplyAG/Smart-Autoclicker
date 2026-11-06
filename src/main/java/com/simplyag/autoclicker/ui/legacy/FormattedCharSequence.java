package com.simplyag.autoclicker.ui.legacy;

//? if <1.16.2 {
/*/^*
 * Stand-in for Minecraft's FormattedCharSequence (from 1.16.2): one line of wrapped text, kept as a string with the
 * game's formatting codes. Only compiled for Minecraft before 1.16.2; {@link com.simplyag.autoclicker.ui.Gfx} makes
 * and draws these, so the rest of the mod uses the same line type on every version.
 ^/
public final class FormattedCharSequence {
    public final String text;

    public FormattedCharSequence(String text) {
        this.text = text;
    }
}
*///?}
