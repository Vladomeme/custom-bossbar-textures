package net.cbt.main.mixin;

import com.google.common.collect.Maps;
import net.cbt.main.CBTClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin implements CBTTransmitter {

	@Unique
	private CBTClient cbtClient;

	@Final @Shadow
	public final Map<UUID, ClientBossBar> bossBars = Maps.newLinkedHashMap();

	@Inject(method = "handlePacket", at = @At(value = "HEAD"))
	private void handlePacket(BossBarS2CPacket packet, CallbackInfo ci) {
		packet.accept(new BossBarS2CPacket.Consumer(){

			@Override
			public void add(UUID uuid, Text name, float percent, BossBar.Color color, BossBar.Style style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
				bossBars.put(uuid, new ClientBossBar(uuid, name, percent, color, style, darkenSky, dragonMusic, thickenFog));
				cbtClient.eventManager.addCondition(name);
			}

			@Override
			public void remove(UUID uuid) {
				cbtClient.eventManager.removeCondition(bossBars.get(uuid).getName());
				bossBars.remove(uuid);
			}

			@Override
			public void updateProgress(UUID uuid, float percent) {
				bossBars.get(uuid).setPercent(percent);
			}

			@Override
			public void updateName(UUID uuid, Text name) {
				bossBars.get(uuid).setName(name);
			}

			@Override
			public void updateStyle(UUID id, BossBar.Color color, BossBar.Style style) {
				ClientBossBar clientBossBar = bossBars.get(id);
				clientBossBar.setColor(color);
				clientBossBar.setStyle(style);
			}

			@Override
			public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
				ClientBossBar clientBossBar = bossBars.get(uuid);
				clientBossBar.setDarkenSky(darkenSky);
				clientBossBar.setDragonMusic(dragonMusic);
				clientBossBar.setThickenFog(thickenFog);
			}
		});
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
