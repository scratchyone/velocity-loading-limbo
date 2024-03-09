package com.scratchyone.velocityloadinglimbo;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Objects;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

@Plugin(
    id = "velocityloadinglimbo",
    name = "Velocity Loading Limbo",
    version = "0.1.0-SNAPSHOT",
    description = "Puts players into a limbo server if the destination server refuses connection",
    authors = {"scratchyone"})
public class LimboLoadingVelocityPlugin {

  private final ProxyServer server;
  private final Logger logger;
  private final LimboFactory factory;

  private Limbo limbo;

  public static final PlainTextComponentSerializer SERIALIZER =
      PlainTextComponentSerializer.builder().flattener(ComponentFlattener.basic()).build();

  @Inject
  public LimboLoadingVelocityPlugin(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
    this.factory =
        (LimboFactory)
            this.server
                .getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    // Do some operation demanding access to the Velocity API here.
    // For instance, we could register an event:
    // server.getEventManager().register(this, new PluginListener());
    VirtualWorld world =
        this.factory.createVirtualWorld(Dimension.OVERWORLD, 0.0, 100.0, 0.0, 0.0F, 0.0F);
    this.limbo = this.factory.createLimbo(world);
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onKick(KickedFromServerEvent event) {
    logger.info("kicked: {}", event.kickedDuringServerConnect());
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
    logger.info("registering...");
    event.setOnKickCallback(
        kickEvent -> {
          Component kickReason =
              kickEvent.getServerKickReason().isPresent()
                  ? kickEvent.getServerKickReason().get()
                  : Component.empty();
          String kickMessage =
              Objects.requireNonNullElse(SERIALIZER.serialize(kickReason), "unknown");

          logger.info("Kick reason: {}", kickReason);
          logger.info("Kick message: {}", kickMessage.isEmpty());

          if (kickMessage.isEmpty())
            this.limbo.spawnPlayer(
                kickEvent.getPlayer(), new SessionHandler(kickEvent.getServer()));

          // if (CONFIG.debug) {
          // LimboReconnect.getLogger().info("Component: {}", kickReason);
          // LimboReconnect.getLogger().info("Kick message: {}", kickMessage);
          // LimboReconnect.getLogger()
          // .info("Match: {}", kickMessage.matches(CONFIG.triggerMessage));
          // }
          //

          if (kickMessage.matches("")) {
            return true;
          } else {
            return false;
          }
        });
  }
}
