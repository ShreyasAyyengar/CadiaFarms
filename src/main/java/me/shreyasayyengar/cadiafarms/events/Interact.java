package me.shreyasayyengar.cadiafarms.events;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.InventoryUtil;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.sql.SQLException;

public class Interact implements Listener {

    @EventHandler
    private void onPlayerInteract(PlayerInteractAtEntityEvent event) throws SQLException {

        Player player = event.getPlayer();
        player.sendMessage(event.getRightClicked().getUniqueId().toString());

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

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null) return;

            if (item.getType().name().contains("SPAWN_EGG")) {

                SpawnEggMeta itemMeta = (SpawnEggMeta) item.getItemMeta();
                if (itemMeta.getLocalizedName().contains("cadiamob")) {

                    EntityType type = EntityType.valueOf(itemMeta.getLocalizedName().split("\\.")[1]);
                    LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(event.getClickedBlock().getLocation().add(.5, 1, .5), type);
                    entity.setCustomNameVisible(false);
                    entity.setCustomName(null);
                    event.setCancelled(true);
                    player.getInventory().getItemInMainHand().subtract();

                    new CadiaMob(entity);
                }
            }
        }
    }

    // TODO: manage chunk numbers

}
