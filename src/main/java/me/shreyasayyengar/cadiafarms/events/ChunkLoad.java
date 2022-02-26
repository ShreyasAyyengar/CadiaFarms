package me.shreyasayyengar.cadiafarms.events;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.CadiaMobManager;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChunkLoad implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        CadiaMobManager mobManager = CadiaFarmsPlugin.getInstance().getMobManager();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : chunk.getEntities()) {
                    UUID uuid = entity.getUniqueId();

                    if (mobManager.toLoadWaitlist.contains(uuid)) {
                        try {
                            new CadiaMob(uuid);
                        } catch (IOException | SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    for (CadiaMob mob : mobManager.getMobs()) {
                        if (mob.getEntityUUID().equals(uuid)) {
                            mob.load();
                        }
                    }
                }
            }
        }.runTaskLater(CadiaFarmsPlugin.getInstance(), 20);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        CadiaMobManager mobManager = CadiaFarmsPlugin.getInstance().getMobManager();

        for (Entity entity : chunk.getEntities()) {
            UUID uuid = entity.getUniqueId();

            for (CadiaMob mob : CadiaFarmsPlugin.getInstance().getMobManager().getMobs()) {
                if (mob.getEntityUUID().equals(uuid)) {
                    mob.unload();
                }
            }
        }
    }

    public List<Entity> findByChunk(World world, Chunk chunk) {
        return world.getEntities()
                .stream()
                .filter(
                        entity -> entity.getChunk().getChunkKey() == chunk.getChunkKey())
                .collect(Collectors.toList());
    }
}
