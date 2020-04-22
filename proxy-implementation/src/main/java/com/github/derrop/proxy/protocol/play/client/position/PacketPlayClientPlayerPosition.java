package com.github.derrop.proxy.protocol.play.client.position;

import com.github.derrop.proxy.api.connection.ProtocolDirection;
import com.github.derrop.proxy.api.location.Location;
import com.github.derrop.proxy.api.network.Packet;
import com.github.derrop.proxy.api.network.wrapper.ProtoBuf;
import com.github.derrop.proxy.protocol.ProtocolIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PacketPlayClientPlayerPosition implements Packet {

    private boolean onGround;

    public PacketPlayClientPlayerPosition(boolean onGround) {
        this.onGround = onGround;
    }

    public PacketPlayClientPlayerPosition() {
    }

    public Location getLocation(@Nullable Location before) {
        throw new UnsupportedOperationException("Not supported in PacketPlayClientPlayerPosition");
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isMoving() {
        return this instanceof PacketPlayClientPosition || this instanceof PacketPlayClientPositionLook;
    }

    public boolean isRotating() {
        return this instanceof PacketPlayClientLook || this instanceof PacketPlayClientPositionLook;
    }

    public static PacketPlayClientPlayerPosition create(@Nullable Location before, @NotNull Location after) {
        boolean pos = before == null || before.getX() != after.getX() || before.getY() != after.getY() || before.getZ() != after.getZ();
        boolean look = before == null || before.getYaw() != after.getYaw() || before.getPitch() != after.getPitch();
        if (pos && look) {
            return new PacketPlayClientPositionLook(after);
        }
        if (pos) {
            return new PacketPlayClientPosition(after);
        }
        if (look) {
            return new PacketPlayClientLook(after);
        }
        return null;
    }

    @Override
    public void read(@NotNull ProtoBuf protoBuf, @NotNull ProtocolDirection direction, int protocolVersion) {
        this.onGround = protoBuf.readUnsignedByte() != 0;
    }

    @Override
    public void write(@NotNull ProtoBuf protoBuf, @NotNull ProtocolDirection direction, int protocolVersion) {
        protoBuf.writeByte(this.onGround ? 1 : 0);
    }

    @Override
    public int getId() {
        return ProtocolIds.FromClient.Play.FLYING;
    }
}
