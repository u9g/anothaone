package dev.u9g.neustoragegui;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        System.setProperty("devauth.enabled", "true");
        System.setProperty("mixin.debug.verbose", "true");
        System.setProperty("mixin.debug.export", "true");
        System.setProperty("mixin.dumpTargetOnFailure", "true");
        MixinExtrasBootstrap.init();
    }
}
