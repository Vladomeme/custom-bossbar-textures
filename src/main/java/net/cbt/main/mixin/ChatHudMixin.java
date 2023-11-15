package net.cbt.main.mixin;

import net.cbt.main.CBTClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin implements CBTTransmitter {

	@Unique
	private CBTClient cbtClient;

	@Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
			at = @At(value = "HEAD"))
	private void addMessage(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci) {
		cbtClient.eventManager.chatCondition(message);
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
