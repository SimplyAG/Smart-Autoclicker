package com.simplyag.autoclicker.integration;

import com.simplyag.autoclicker.ui.ControlPanelScreen;
//? if >=1.17 {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//?} else {
/*import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
*///?}

/**
 * Adds the control panel as this mod's config button in Mod Menu. Mod Menu's API package was
 * io.github.prospector before 1.17; versions without a Mod Menu build leave this class out (remap.gradle).
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ControlPanelScreen::new;
    }
}
