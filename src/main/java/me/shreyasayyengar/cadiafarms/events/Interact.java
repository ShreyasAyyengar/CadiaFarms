package me.shreyasayyengar.cadiafarms.events;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.Config;
import me.shreyasayyengar.cadiafarms.util.InventoryUtil;
import me.shreyasayyengar.cadiafarms.util.Utility;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

public class Interact implements Listener {

    @EventHandler
    private void onPlayerInteract(PlayerInteractAtEntityEvent event) {

        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.HAND) {
            if (event.getRightClicked() instanceof LivingEntity entity) {
                CadiaFarmsPlugin.getInstance().getMobManager().getMobs().forEach(cadiaMob -> {

                    if (cadiaMob.getEntityUUID().equals(entity.getUniqueId())) {

                        if (entity instanceof Animals animal) {
                            if (animal.isBreedItem(player.getInventory().getItemInMainHand().getType())) {
                                event.setCancelled(true);
                                animal.setBreed(false);

                                player.getInventory().getItemInMainHand().subtract();
                                cadiaMob.plusMood();

                            } else
                                InventoryUtil.openBaseInventory(player, cadiaMob.getEntityUUID(), entity.getLocation());
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType().name().contains("SPAWN_EGG")) {

                if (CadiaFarmsPlugin.getInstance().getMobManager().getEntityCountInChunk(player.getLocation().getChunk()) >= Config.getMaxChunkEntities()) {
                    player.sendMessage(Utility.colourise("&cYou can't spawn more than &l" + Config.getMaxChunkEntities() + "&r&c mobs in a chunk."));
                    event.setCancelled(true);
                    return;
                }

                SpawnEggMeta itemMeta = (SpawnEggMeta) item.getItemMeta();
                if (itemMeta.getLocalizedName().contains("cadiamob")) {
                    event.setCancelled(true);

                    EntityType type = EntityType.valueOf(itemMeta.getLocalizedName().split("\\.")[1]);
                    player.getInventory().getItemInMainHand().subtract();
                    //noinspection ConstantConditions
                    Ageable entity = (Ageable) player.getWorld().spawnEntity(event.getClickedBlock().getLocation().add(.5, 1.5, .5), type);
                    entity.setAdult();
                    entity.setCustomNameVisible(false);
                    entity.setCustomName(null);
                    new CadiaMob(entity);
                }
            }
        }
    }

    // TODO: manage chunk numbers

}
