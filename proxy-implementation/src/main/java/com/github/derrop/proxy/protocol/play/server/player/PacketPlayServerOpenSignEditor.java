package com.github.derrop.proxy.protocol.play.server.player;

import com.github.derrop.proxy.api.connection.ProtocolDirection;
import com.github.derrop.proxy.api.location.Location;
import com.github.derrop.proxy.api.network.Packet;
import com.github.derrop.proxy.api.network.wrapper.ProtoBuf;
import com.github.derrop.proxy.protocol.ProtocolIds;
import org.jetbrains.annotations.NotNull;

public class PacketPlayServerOpenSignEditor implements Packet {

    private Location location;

    public PacketPlayServerOpenSignEditor(Location location) {
        this.location = location;
    }

    public PacketPlayServerOpenSignEditor() {
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void read(@NotNull ProtoBuf protoBuf, @NotNull ProtocolDirection direction, int protocolVersion) {
        this.location = protoBuf.readLocation();
    }

    @Override
    public void write(@NotNull ProtoBuf protoBuf, @NotNull ProtocolDirection direction, int protocolVersion) {
        protoBuf.writeLocation(this.location);
    }

    @Override
    public int getId() {
        return ProtocolIds.ToClient.Play.OPEN_SIGN_EDITOR;
    }
}
