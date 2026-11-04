package com.simplyag.autoclicker.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.simplyag.autoclicker.ui.ControlPanelScreen;

/** Adds the control panel as this mod's config button in Mod Menu. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ControlPanelScreen::new;
    }
}
