package com.github.derrop.proxy.protocol.play.server;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PacketPlayServerScoreboardScore extends DefinedPacket {

    private String itemName;
    /**
     * 0 = create / update, 1 = remove.
     */
    private byte action;
    private String objectiveName;
    private int value;

    /**
     * Destroy packet
     */
    public PacketPlayServerScoreboardScore(String itemName, String objectiveName) {
        this(itemName, (byte) 1, objectiveName, -1);
    }

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        itemName = readString(buf);
        action = buf.readByte();
        objectiveName = readString(buf);
        if (action != 1) {
            value = readVarInt(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        writeString(itemName, buf);
        buf.writeByte(action);
        writeString(objectiveName, buf);
        if (action != 1) {
            writeVarInt(value, buf);
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}