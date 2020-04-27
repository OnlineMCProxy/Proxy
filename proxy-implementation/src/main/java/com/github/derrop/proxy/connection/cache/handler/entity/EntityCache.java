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
package com.github.derrop.proxy.connection.cache.handler.entity;

import com.github.derrop.proxy.api.entity.player.Player;
import com.github.derrop.proxy.api.network.Packet;
import com.github.derrop.proxy.api.network.PacketSender;
import com.github.derrop.proxy.connection.cache.CachedPacket;
import com.github.derrop.proxy.connection.cache.PacketCache;
import com.github.derrop.proxy.connection.cache.PacketCacheHandler;
import com.github.derrop.proxy.connection.cache.handler.PlayerInfoCache;
import com.github.derrop.proxy.protocol.ProtocolIds;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityDestroy;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityEquipment;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityMetadata;
import com.github.derrop.proxy.protocol.play.server.entity.PacketPlayServerEntityTeleport;
import com.github.derrop.proxy.protocol.play.server.entity.spawn.PacketPlayServerNamedEntitySpawn;
import com.github.derrop.proxy.protocol.play.server.entity.spawn.PacketPlayServerSpawnEntity;
import com.github.derrop.proxy.protocol.play.server.entity.spawn.PacketPlayServerSpawnLivingEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EntityCache implements PacketCacheHandler {

    private final Map<Integer, CachedEntity> entities = new ConcurrentHashMap<>();

    private PacketCache packetCache;

    @Override
    public int[] getPacketIDs() {
        return new int[]{
                ProtocolIds.ToClient.Play.NAMED_ENTITY_SPAWN,
                ProtocolIds.ToClient.Play.ENTITY_DESTROY,
                ProtocolIds.ToClient.Play.ENTITY_TELEPORT,
                ProtocolIds.ToClient.Play.ENTITY_EQUIPMENT,
                ProtocolIds.ToClient.Play.SPAWN_ENTITY_LIVING,
                ProtocolIds.ToClient.Play.SPAWN_ENTITY,
                ProtocolIds.ToClient.Play.ENTITY_METADATA
        };
    }

    @Override
    public void cachePacket(PacketCache packetCache, CachedPacket newPacket) {
        this.packetCache = packetCache;

        Packet packet = newPacket.getDeserializedPacket();

        if (packet instanceof PacketPlayServerEntityTeleport) {

            PacketPlayServerEntityTeleport teleport = (PacketPlayServerEntityTeleport) packet;

            if (this.entities.containsKey(teleport.getEntityId())) {
                this.entities.get(teleport.getEntityId()).updateLocation(
                        teleport.getX(), teleport.getY(), teleport.getZ(),
                        teleport.getYaw(), teleport.getPitch()
                );
            }

        } else if (packet instanceof PacketPlayServerNamedEntitySpawn) {

            PacketPlayServerNamedEntitySpawn spawnPlayer = (PacketPlayServerNamedEntitySpawn) packet;

            this.entities.put(spawnPlayer.getEntityId(), new CachedEntity(spawnPlayer));

        } else if (packet instanceof PacketPlayServerSpawnLivingEntity) {

            PacketPlayServerSpawnLivingEntity spawnMob = (PacketPlayServerSpawnLivingEntity) packet;

            this.entities.put(spawnMob.getEntityId(), new CachedEntity(spawnMob));

        } else if (packet instanceof PacketPlayServerSpawnEntity) {

            PacketPlayServerSpawnEntity spawnObject = (PacketPlayServerSpawnEntity) packet;

            this.entities.put(spawnObject.getEntityId(), new CachedEntity(spawnObject));

        } else if (packet instanceof PacketPlayServerEntityMetadata) {

            PacketPlayServerEntityMetadata metadata = (PacketPlayServerEntityMetadata) packet;
            if (this.entities.containsKey(metadata.getEntityId())) {
                this.entities.get(metadata.getEntityId()).updateMetadata(metadata);
            }

        } else if (packet instanceof PacketPlayServerEntityDestroy) {

            PacketPlayServerEntityDestroy destroyEntities = (PacketPlayServerEntityDestroy) packet;
            for (int entityId : destroyEntities.getEntityIds()) {
                this.entities.remove(entityId);
            }

        } else if (packet instanceof PacketPlayServerEntityEquipment) {

            PacketPlayServerEntityEquipment equipment = (PacketPlayServerEntityEquipment) packet;
            if (this.entities.containsKey(equipment.getEntityId())) {
                this.entities.get(equipment.getEntityId()).setEquipmentSlot(equipment.getSlot(), equipment.getItem());
            }

        }
    }

    @Override
    public void sendCached(PacketSender con) {
        if (this.packetCache == null) {
            return;
        }
        PlayerInfoCache infoCache = (PlayerInfoCache) this.packetCache.getHandler(handler -> handler instanceof PlayerInfoCache);

        for (CachedEntity entity : this.entities.values()) {
            entity.spawn(infoCache, con);
        }
    }

    @Override
    public void onClientSwitch(Player con) {
        if (this.entities.isEmpty()) {
            return;
        }

        Set<Integer> entityIdSet = new HashSet<>(this.entities.keySet());

        int[] entityIds = new int[entityIdSet.size()];
        int i = 0;
        for (Integer entityId : entityIdSet) {
            entityIds[i++] = entityId;
        }

        con.sendPacket(new PacketPlayServerEntityDestroy(entityIds));
    }
}