package com.simplyag.autoclicker.gametest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test clients run in the background while the PC is in use: the game window must never take focus.
 * Minecraft up to 26.2 creates its window with GLFW, which has no environment switch for that, so ask
 * GLFW right before the window is made. From 26.3 the window comes from SDL; the build sets SDL's own
 * switches instead, and this injection simply finds nothing to hook (require = 0).
 */
@Mixin(Window.class)
abstract class QuietWindowMixin {
	private static final int GLFW_FOCUSED = 0x00020001;
	private static final int GLFW_FOCUS_ON_SHOW = 0x0002000C;

	@Inject(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"),
		require = 0
	)
	private void quiettest$noFocus(CallbackInfo ci) {
		try {
			// GLFW isn't on the test compile classpath, so call it reflectively.
			var hint = Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwWindowHint", int.class, int.class);
			hint.invoke(null, GLFW_FOCUSED, 0);
			hint.invoke(null, GLFW_FOCUS_ON_SHOW, 0);
		} catch (ReflectiveOperationException ignored) {
			// Not a GLFW build; nothing to do.
		}
	}
}
