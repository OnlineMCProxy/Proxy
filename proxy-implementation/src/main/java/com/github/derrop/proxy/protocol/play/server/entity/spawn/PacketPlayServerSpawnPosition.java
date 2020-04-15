package com.github.derrop.proxy.protocol.play.server.entity.spawn;

import com.github.derrop.proxy.api.location.BlockPos;
import io.netty.buffer.ByteBuf;
import lombok.*;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
@ToString
@AllArgsConstructor
public class PacketPlayServerSpawnPosition extends DefinedPacket {

    private BlockPos spawnPosition;

    @Override
    public void read(ByteBuf buf) {
        this.spawnPosition = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(this.spawnPosition.toLong());
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
    }
}