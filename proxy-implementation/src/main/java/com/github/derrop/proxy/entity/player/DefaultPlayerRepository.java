package com.github.derrop.proxy.entity.player;

import com.github.derrop.proxy.MCProxy;
import com.github.derrop.proxy.api.entity.player.Player;
import com.github.derrop.proxy.api.connection.ServiceConnection;
import com.github.derrop.proxy.api.entity.player.OfflinePlayer;
import com.github.derrop.proxy.api.repository.PlayerRepository;
import com.github.derrop.proxy.storage.OfflinePlayerStorage;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultPlayerRepository implements PlayerRepository {

    private final MCProxy proxy;
    private final OfflinePlayerStorage storage;

    public DefaultPlayerRepository(MCProxy proxy) {
        this.proxy = proxy;
        this.storage = new OfflinePlayerStorage(proxy.getServiceRegistry());
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return this.proxy.getOnlineClients().stream().map(ServiceConnection::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Player getOnlinePlayer(String name) {
        return this.proxy.getOnlineClients().stream().map(ServiceConnection::getPlayer).filter(Objects::nonNull).filter(connection -> connection.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public Player getOnlinePlayer(UUID uniqueId) {
        return this.proxy.getOnlineClients().stream().map(ServiceConnection::getPlayer).filter(Objects::nonNull).filter(connection -> connection.getUniqueId().equals(uniqueId)).findFirst().orElse(null);
    }

    @Override
    public void updateOfflinePlayer(OfflinePlayer offlinePlayer) {
        this.storage.updateOfflinePlayer(offlinePlayer);
    }

    @Override
    public Collection<? extends OfflinePlayer> getOfflinePlayers() {
        return this.storage.getOfflinePlayers();
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String name) {
        return this.storage.getOfflinePlayer(name);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID uniqueId) {
        return this.storage.getOfflinePlayer(uniqueId);
    }
}
