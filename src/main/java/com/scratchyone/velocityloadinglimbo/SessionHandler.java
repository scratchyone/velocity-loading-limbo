package com.scratchyone.velocityloadinglimbo;

import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.concurrent.TimeUnit;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;

public class SessionHandler implements LimboSessionHandler {
  private RegisteredServer server;
  private LimboPlayer player;
  private Boolean connected;

  public SessionHandler(RegisteredServer server) {
    this.server = server;
  }

  @Override
  public void onDisconnect() {
    this.connected = false;
  }

  private void tick() {
    if (!this.connected) {
      return;
    }

    this.player
        .getProxyPlayer()
        .showTitle(
            Title.title(
                Component.text("Server loading...").color(TextColor.fromHexString("#5234eb")),
                Component.text("")));

    this.server
        .ping(
            PingOptions.builder().build(),
            this.player.getProxyPlayer().getVirtualHost().map((h) -> h.getHostString()).orElse(""))
        .whenComplete(
            (ping, exception) -> {
              if (exception != null) {
                this.player
                    .getScheduledExecutor()
                    .schedule(this::tick, 1000, TimeUnit.MILLISECONDS);
              } else {
                this.player
                    .getScheduledExecutor()
                    .execute(
                        () -> {
                          this.player
                              .getScheduledExecutor()
                              .schedule(
                                  () -> {
                                    this.player.getProxyPlayer().resetTitle();
                                    this.player.disconnect(this.server);
                                  },
                                  1000,
                                  TimeUnit.MILLISECONDS);
                        });
              }
            });
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.connected = true;
    this.player = player;
    this.player.disableFalling();
    this.player.setGameMode(GameMode.SPECTATOR);
    server.setWorldTime(8);
    // this.player
    //     .getScheduledExecutor()
    //     .schedule(this::tick, CONFIG.checkInterval, TimeUnit.MILLISECONDS);
    this.player
        .getProxyPlayer()
        .showTitle(
            Title.title(
                Component.text("Server loading...").color(TextColor.fromHexString("#5234eb")),
                Component.text("")));
    // this.player
    //     .getProxyPlayer()
    //     .showBossBar(
    //         BossBar.bossBar(
    //             Component.text("Server is starting... Please wait..."),
    //             0.5F,
    //             Color.PURPLE,
    //             Overlay.NOTCHED_6));

    this.player.getScheduledExecutor().schedule(this::tick, 1000, TimeUnit.MILLISECONDS);
  }
}
