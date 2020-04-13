package com.github.derrop.proxy.connection.cache.packet.entity.spawn;

import com.github.derrop.proxy.api.connection.packet.Packet;

public interface PositionedPacket extends Packet {

    int getEntityId();

    void setX(int x);

    void setY(int y);

    void setZ(int z);

    void setYaw(byte yaw);

    void setPitch(byte pitch);

    int getX();

    int getY();

    int getZ();

    byte getYaw();

    byte getPitch();


}