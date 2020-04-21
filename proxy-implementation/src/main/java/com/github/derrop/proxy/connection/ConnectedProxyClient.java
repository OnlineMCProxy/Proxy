/*
 * MIT License
 *
 * Copyright (c) derrop and derklaro
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.derrop.proxy.connection;

import com.github.derrop.proxy.MCProxy;
import com.github.derrop.proxy.api.connection.ProtocolDirection;
import com.github.derrop.proxy.api.connection.ProtocolState;
import com.github.derrop.proxy.api.event.EventManager;
import com.github.derrop.proxy.api.events.connection.service.ServiceConnectEvent;
import com.github.derrop.proxy.api.events.connection.service.ServiceDisconnectEvent;
import com.github.derrop.proxy.api.network.Packet;
import com.github.derrop.proxy.api.network.exception.CancelProceedException;
import com.github.derrop.proxy.api.network.wrapper.ProtoBuf;
import com.github.derrop.proxy.api.scoreboard.Scoreboard;
import com.github.derrop.proxy.api.session.ProvidedSessionService;
import com.github.derrop.proxy.api.task.Task;
import com.github.derrop.proxy.api.util.ByteBufUtils;
import com.github.derrop.proxy.api.util.MCCredentials;
import com.github.derrop.proxy.api.util.NetworkAddress;
import com.github.derrop.proxy.basic.BasicServiceConnection;
import com.github.derrop.proxy.connection.cache.PacketCache;
import com.github.derrop.proxy.connection.cache.handler.scoreboard.ScoreboardCache;
import com.github.derrop.proxy.connection.login.ProxyClientLoginListener;
import com.github.derrop.proxy.connection.velocity.PlayerVelocityHandler;
import com.github.derrop.proxy.network.NetworkUtils;
import com.github.derrop.proxy.network.channel.DefaultNetworkChannel;
import com.github.derrop.proxy.network.handler.HandlerEndpoint;
import com.github.derrop.proxy.network.minecraft.MinecraftDecoder;
import com.github.derrop.proxy.network.minecraft.MinecraftEncoder;
import com.github.derrop.proxy.network.wrapper.DefaultProtoBuf;
import com.github.derrop.proxy.protocol.handshake.PacketHandshakingClientSetProtocol;
import com.github.derrop.proxy.protocol.login.client.PacketLoginInLoginRequest;
import com.github.derrop.proxy.protocol.play.client.PacketPlayClientResourcePackStatusResponse;
import com.github.derrop.proxy.protocol.play.server.PacketPlayServerResourcePackSend;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityMetadata;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityTeleport;
import com.github.derrop.proxy.scoreboard.BasicScoreboard;
import com.github.derrop.proxy.task.DefaultTask;
import com.github.derrop.proxy.util.NettyUtils;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConnectedProxyClient extends DefaultNetworkChannel {

    private final MCProxy proxy;
    private final BasicServiceConnection connection;

    private NetworkAddress address;
    private final MinecraftSessionService sessionService = MCProxy.getInstance().getServiceRegistry().getProviderUnchecked(ProvidedSessionService.class).createSessionService();
    private UserAuthentication authentication;
    private MCCredentials credentials;

    private com.github.derrop.proxy.api.entity.player.Player redirector;

    private PacketCache packetCache;
    private Scoreboard scoreboard;

    private int entityId;
    private int dimension;

    private PacketPlayServerEntityMetadata entityMetadata;

    private Consumer<Packet> clientPacketHandler;

    private Runnable disconnectionHandler;

    private boolean globalAccount = true;

    private Map<Predicate<Packet>, Long> blockedPackets = new ConcurrentHashMap<>();

    private Component lastKickReason;

    private PlayerVelocityHandler velocityHandler = new PlayerVelocityHandler(this);

    private long lastAlivePacket = -1;

    private CompletableFuture<Boolean> connectionHandler;

    public ConnectedProxyClient(MCProxy proxy, BasicServiceConnection connection) {
        this.proxy = proxy;
        this.connection = connection;

        this.packetCache = new PacketCache(this);
        this.scoreboard = new BasicScoreboard(connection, (ScoreboardCache) this.packetCache.getHandler(handler -> handler instanceof ScoreboardCache));
    }

    public boolean performMojangLogin(MCCredentials credentials) throws AuthenticationException {
        if (credentials.isOffline()) {
            this.credentials = credentials;
            return true;
        }

        System.out.println("Logging in " + credentials.getEmail() + "...");
        this.authentication = MCProxy.getInstance().getServiceRegistry().getProviderUnchecked(ProvidedSessionService.class).login(credentials.getEmail(), credentials.getPassword());
        this.credentials = credentials;
        System.out.println("Successfully logged in with " + credentials.getEmail() + "!");
        return true;
    }

    public void setAuthentication(UserAuthentication authentication, MCCredentials credentials) {
        this.authentication = authentication;
        this.credentials = credentials;
    }

    public void disconnect() {
        if (super.getWrappedChannel() == null) {
            return;
        }

        super.close();
        this.address = null;
        this.packetCache.reset();

        this.lastKickReason = null;
        this.lastAlivePacket = -1;
        if (this.connectionHandler != null) {
            this.connectionHandler.complete(false);
            this.connectionHandler = null;
        }
        this.velocityHandler = new PlayerVelocityHandler(this);
        this.entityId = -1;
        this.dimension = -1;

        if (MCProxy.getInstance() != null && this.globalAccount) {
            MCProxy.getInstance().getOnlineClients().remove(this);
        }

        if (this.disconnectionHandler != null) {
            this.disconnectionHandler.run();
        }
    }

    public Task<Boolean> connect(NetworkAddress address, NetworkAddress proxy) {
        this.disconnect();

        Task<Boolean> future = new DefaultTask<>();

        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                NetworkUtils.BASE.initChannel(ch);

                if (proxy != null) {
                    ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                }

                ch.pipeline().addAfter(NetworkUtils.LENGTH_DECODER, NetworkUtils.PACKET_DECODER, new MinecraftDecoder(ProtocolDirection.TO_CLIENT, ProtocolState.HANDSHAKING));
                ch.pipeline().addAfter(NetworkUtils.LENGTH_ENCODER, NetworkUtils.PACKET_ENCODER, new MinecraftEncoder(ProtocolDirection.TO_SERVER));
                ch.pipeline().get(HandlerEndpoint.class).setNetworkChannel(ConnectedProxyClient.this);
                ch.pipeline().get(HandlerEndpoint.class).setChannelListener(new ProxyClientLoginListener(ConnectedProxyClient.this));
            }
        };
        ChannelFutureListener listener = future1 -> {
            if (future1.isSuccess()) {
                super.setChannel(future1.channel());
                this.address = address;

                super.write(new PacketHandshakingClientSetProtocol(47, address.getHost(), address.getPort(), 2));
                super.setProtocolState(ProtocolState.LOGIN);
                super.write(new PacketLoginInLoginRequest(this.getAccountName()));
                future.complete(true);
            } else {
                future1.channel().close();
                future.complete(false);
            }
        };

        this.connectionHandler = future;

        new Bootstrap()
                .channel(NettyUtils.getSocketChannelClass())
                .group(NettyUtils.newEventLoopGroup())
                .handler(initializer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                .connect(new InetSocketAddress(address.getHost(), address.getPort()))
                .addListener(listener)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

        return future;
    }

    public void setDisconnectionHandler(Runnable disconnectionHandler) {
        this.disconnectionHandler = disconnectionHandler;
    }

    public void setClientPacketHandler(Consumer<Packet> clientPacketHandler) {
        this.clientPacketHandler = clientPacketHandler;
    }

    public Consumer<Packet> getClientPacketHandler() {
        return clientPacketHandler;
    }

    public void disableGlobal() {
        this.globalAccount = false;
    }

    public MCProxy getProxy() {
        return proxy;
    }

    public BasicServiceConnection getConnection() {
        return this.connection;
    }

    public NetworkAddress getServerAddress() {
        return this.address;
    }

    public UserAuthentication getAuthentication() {
        return authentication;
    }

    public String getAccountName() {
        return this.credentials.isOffline() ? this.credentials.getUsername() : this.authentication.getSelectedProfile().getName();
    }

    public UUID getAccountUUID() {
        return this.credentials.isOffline() ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.credentials.getUsername()).getBytes()) : this.authentication.getSelectedProfile().getId();
    }

    public Component getLastKickReason() {
        return lastKickReason;
    }

    public void setLastKickReason(Component lastKickReason) {
        this.lastKickReason = lastKickReason;
    }

    public PlayerVelocityHandler getVelocityHandler() {
        return velocityHandler;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public PacketCache getPacketCache() {
        return packetCache;
    }

    public MinecraftSessionService getSessionService() {
        return sessionService;
    }

    public MCCredentials getCredentials() {
        return credentials;
    }

    public com.github.derrop.proxy.api.entity.player.Player getRedirector() {
        return redirector;
    }

    public PacketPlayServerEntityMetadata getEntityMetadata() {
        return entityMetadata;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public long getLastAlivePacket() {
        return lastAlivePacket;
    }

    public void setLastAlivePacket(long lastAlivePacket) {
        this.lastAlivePacket = lastAlivePacket;
    }

    public void free() {
        if (this.redirector != null) {
            this.packetCache.handleFree(this.redirector);
        }
        this.redirector = null;
    }

    public void blockPacketUntil(Predicate<Packet> tester, long until) {
        this.blockedPackets.put(tester, until);
    }

    public void redirectPacket(ProtoBuf packet, Packet deserialized) {
        if (!this.isConnected() || super.getProtocolState() != ProtocolState.PLAY) {
            return;
        }
        if (packet == null) {
            return;
        }

        if (deserialized instanceof PacketPlayServerResourcePackSend) {
            super.write(new PacketPlayClientResourcePackStatusResponse(((PacketPlayServerResourcePackSend) deserialized).getHash(), PacketPlayClientResourcePackStatusResponse.Action.ACCEPTED));
            super.write(new PacketPlayClientResourcePackStatusResponse(((PacketPlayServerResourcePackSend) deserialized).getHash(), PacketPlayClientResourcePackStatusResponse.Action.SUCCESSFULLY_LOADED));
            throw CancelProceedException.INSTANCE;
        }

        if (deserialized != null && !this.blockedPackets.isEmpty()) {
            for (Map.Entry<Predicate<Packet>, Long> entry : this.blockedPackets.entrySet()) {
                if (entry.getValue() >= System.currentTimeMillis()) {
                    this.blockedPackets.remove(entry.getKey());
                    continue;
                }

                if (entry.getKey().test(deserialized)) {
                    return;
                }
            }
        }

        this.packetCache.handlePacket(packet, deserialized);
        if (deserialized != null) {
            this.velocityHandler.handlePacket(ProtocolDirection.TO_CLIENT, deserialized);
        }

        if (this.redirector != null) {
            ProtoBuf buf = packet;

            if (deserialized != null) { // rewrite to allow modifications by the packet handlers
                int id = ByteBufUtils.readVarInt(packet);

                buf = new DefaultProtoBuf(packet.getProtocolVersion(), Unpooled.buffer());
                ByteBufUtils.writeVarInt(id, buf);
                deserialized.write(buf, ProtocolDirection.TO_CLIENT, buf.getProtocolVersion());
            }

            this.redirector.sendPacket(buf);

            if (!this.redirector.isConnected()) {
                this.redirector = null;
            }
        }

        if (deserialized instanceof PacketPlayServerEntityMetadata) {
            this.entityMetadata = (PacketPlayServerEntityMetadata) deserialized;
        }
    }

    public void handleClientPacket(Packet packetWrapper) {
        if (this.clientPacketHandler != null) {
            this.clientPacketHandler.accept(packetWrapper);
        }

        this.packetCache.handleClientPacket(packetWrapper);
    }

    public void redirectPackets(com.github.derrop.proxy.api.entity.player.Player con, boolean switched) {
        this.packetCache.send(con, switched);
        this.redirector = con;
        con.sendPacket(new PacketPlayServerEntityTeleport(this.entityId, this.connection.getLocation()));
    }

    public void connectionSuccess() {
        if (this.connectionHandler != null) {
            this.connectionHandler.complete(true);
            this.connectionHandler = null;

            this.proxy.getServiceRegistry().getProviderUnchecked(EventManager.class).callEvent(new ServiceConnectEvent(this.connection));
        }
    }

    public void connectionFailed() {
        if (this.connectionHandler != null) {
            this.connectionHandler.completeExceptionally(new KickedException(GsonComponentSerializer.INSTANCE.serialize(this.lastKickReason)));
            this.connectionHandler = null;
        }
    }

    private boolean receivedServerDisconnect = false;

    public void handleDisconnect(Component reason) {
        if (this.receivedServerDisconnect) {
            return;
        }
        this.receivedServerDisconnect = true;

        System.out.println("Disconnected " + this.getCredentials() + " (" + this.getAccountName() + "#" + this.getAccountUUID() + ") with "
                + LegacyComponentSerializer.legacy().serialize(reason));

        if (this.getRedirector() != null) {
            com.github.derrop.proxy.api.entity.player.Player con = this.getRedirector();
            this.connection.getClient().free();
            con.handleDisconnected(this.connection, reason);
        }

        this.connection.getClient().setLastKickReason(reason);
        this.connection.getClient().connectionFailed();

        this.proxy.getServiceRegistry().getProviderUnchecked(EventManager.class)
                .callEvent(new ServiceDisconnectEvent(this.connection, reason));
    }

}
