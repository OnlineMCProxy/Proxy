package com.github.derrop.proxy.protocol.play.server.world;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PacketPlayServerChunkBulk extends DefinedPacket {

    private int[] x;
    private int[] z;
    private boolean b;
    private PacketPlayServerChunkData.Extracted[] extracted;

    @Override
    public void read(ByteBuf buf) {
        this.b = buf.readBoolean();
        int size = readVarInt(buf);
        this.x = new int[size];
        this.z = new int[size];
        this.extracted = new PacketPlayServerChunkData.Extracted[size];

        for (int i = 0; i < size; i++) {
            this.x[i] = buf.readInt();
            this.z[i] = buf.readInt();
            this.extracted[i] = new PacketPlayServerChunkData.Extracted();
            this.extracted[i].dataLength = buf.readShort() & 65535;


            this.extracted[i].data = new byte[PacketPlayServerChunkData.getArraySize(Integer.bitCount(this.extracted[i].dataLength), this.b, true)];
        }

        for (int i = 0; i < size; i++) {
            buf.readBytes(this.extracted[i].data);
        }
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeBoolean(this.b);
        writeVarInt(this.extracted.length, buf);

        for (int i = 0; i < this.extracted.length; i++) {
            buf.writeInt(this.x[i]);
            buf.writeInt(this.z[i]);
            buf.writeShort(this.extracted[i].dataLength & 65535);
        }

        for (int i = 0; i < this.extracted.length; i++) {
            buf.writeBytes(this.extracted[i].data);
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
    }

}