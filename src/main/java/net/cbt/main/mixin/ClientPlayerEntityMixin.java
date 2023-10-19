package net.cbt.main.mixin;

import net.cbt.main.CBTClient;
import net.cbt.main.CBTConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

	@Inject(method = "tick", at = @At(value = "TAIL"))
	private void tick(CallbackInfo ci) {
		if (CBTConfig.INSTANCE.enabled) {
			CBTClient.bossbarManager.tick();
		}
	}
}
