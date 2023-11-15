package net.cbt.main.mixin;

import net.cbt.main.CBTClient;
import net.cbt.main.CBTConfig;
import net.cbt.main.bossbar.BossBarManager;
import net.cbt.main.events.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin implements CBTTransmitter {

	@Final @Shadow
	private BossBarHud bossBarHud;

	@Unique
	private CBTClient cbtClient;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void init(MinecraftClient client, ItemRenderer itemRenderer, CallbackInfo ci) {
		if (CBTConfig.INSTANCE.enabled) {
			cbtClient.bossbarManager = new BossBarManager(client);
			cbtClient.eventManager = new EventManager(cbtClient);
			cbtClient.bossbarManager.setEventManager(cbtClient.eventManager);
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/BossBarHud;render(Lnet/minecraft/client/gui/DrawContext;)V"))
	private void render(BossBarHud instance, DrawContext context) {
		if (!CBTConfig.INSTANCE.enabled) {
			this.bossBarHud.render(context);
			return;
		}
		cbtClient.bossbarManager.setBossBars(instance);
		cbtClient.bossbarManager.render(context);
	}

	@Override
	public CBTClient cbt$getClient() {
		return this.cbtClient;
	}

	@Override
	public void cbt$setClient(CBTClient client) {
		this.cbtClient = client;
	}
}
