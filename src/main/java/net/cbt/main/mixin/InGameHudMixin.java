package net.cbt.main.mixin;

import net.cbt.main.CBTClient;
import net.cbt.main.CBTConfig;
import net.cbt.main.bossbar.BossBarManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Final @Shadow
	private BossBarHud bossBarHud;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void init(MinecraftClient client, ItemRenderer itemRenderer, CallbackInfo ci) {
		if (CBTConfig.INSTANCE.enabled) {
			CBTClient.bossbarManager = new BossBarManager(client);
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/BossBarHud;render(Lnet/minecraft/client/util/math/MatrixStack;)V"))
	private void render(BossBarHud instance, MatrixStack matrices) {
		if (!CBTConfig.INSTANCE.enabled) {
			this.bossBarHud.render(matrices);
			return;
		}
		CBTClient.bossbarManager.setBossBars(instance);
		CBTClient.bossbarManager.render(matrices);
	}
}
