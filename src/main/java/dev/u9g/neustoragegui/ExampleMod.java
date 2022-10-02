package dev.u9g.neustoragegui;

import dev.u9g.configlib.M;
import dev.u9g.configlib.config.MyModConfigEditor;
import dev.u9g.configlib.config.ScreenElementWrapper;
import net.fabricmc.api.ModInitializer;
import net.legacyfabric.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.legacyfabric.fabric.api.command.v2.CommandRegistrar;
import net.legacyfabric.fabric.api.command.v2.lib.sponge.CommandResult;
import net.legacyfabric.fabric.api.command.v2.lib.sponge.spec.CommandSpec;

public class ExampleMod implements ModInitializer {
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("Hello Fabric world!");
		CommandRegistrar.EVENT.register((manager, dedicated) -> {
			manager.register(CommandSpec.builder().executor((src, args) -> {
				// Done so this is run after we send the command result of success
				// this is needed so closing the chat screen doesn't capture the mouse cursor before we open the screen
				// instead we capture the mouse pointer after we close the chat screen
				M.C.submit(() -> {
					M.C.openScreen(new ScreenElementWrapper(new MyModConfigEditor(PrisonsModConfig.INSTANCE)));
				});
				return CommandResult.success();
			}).build(), "prisonsmod");
		});

		long[] lastLongUpdate = {0};

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			long currentTime = System.currentTimeMillis();
			boolean longUpdate = false;
			if (currentTime - lastLongUpdate[0] > 1000) {
				longUpdate = true;
				lastLongUpdate[0] = currentTime;
			}

			if (longUpdate) {
				StorageOverlay.getInstance().markDirty();
			}
		});
	}
}
