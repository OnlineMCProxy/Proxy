package com.github.derrop.proxy.protocol.play.server.scoreboard;

import com.github.derrop.proxy.protocol.ProtocolIds;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.DefinedPacket;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PacketPlayServerScoreboardDisplay extends DefinedPacket {

    /**
     * 0 = list, 1 = side, 2 = below.
     */
    private byte position;
    private String name;

    @Override
    public void read(@NotNull ByteBuf buf) {
        position = buf.readByte();
        name = readString(buf);
    }

    @Override
    public void write(@NotNull ByteBuf buf) {
        buf.writeByte(position);
        writeString(name, buf);
    }

    @Override
    public int getId() {
        return ProtocolIds.ToClient.Play.SCOREBOARD_DISPLAY_OBJECTIVE;
    }
}
