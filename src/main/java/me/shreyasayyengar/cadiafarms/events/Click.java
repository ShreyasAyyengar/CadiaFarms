package me.shreyasayyengar.cadiafarms.events;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.Config;
import me.shreyasayyengar.cadiafarms.util.InventoryUtil;
import me.shreyasayyengar.cadiafarms.util.Utility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class Click implements Listener {

    @EventHandler
    private void onClick(InventoryClickEvent e) {

        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getItemMeta() == null) return;

        String localizedName = e.getCurrentItem().getItemMeta().getLocalizedName();

        if (!localizedName.contains("cancelexempt")) {
            if (localizedName.contains("cadia")) {
                e.setCancelled(true);
            }
        }

        if (localizedName.contains("cadia.openbank")) {
            String[] split = localizedName.split("\\.");
            UUID uuid = UUID.fromString(split[2]);

            for (CadiaMob mob : CadiaFarmsPlugin.getInstance().getMobManager().getMobs()) {
                if (mob.getEntityUUID().equals(uuid)) {
                    player.openInventory(mob.getBukkitInventory());
                }
            }
        }

        if (localizedName.contains("cadia.droppings")) {
            String[] split = localizedName.split("\\.");
            UUID uuid = UUID.fromString(split[2]);

            for (CadiaMob mob : CadiaFarmsPlugin.getInstance().getMobManager().getMobs()) {
                if (mob.getEntityUUID().equals(uuid)) {

                    Inventory dropItems = Bukkit.createInventory(null, 54, Utility.colourise("&cDrop Items"));

                    Config.getTiedPercentages(mob.getType()).forEach((material, weight) -> {
                        ItemStack itemStack = new ItemStack(material);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(Utility.colourise("&f1 &8✕ &c" + material.name()));
                        itemMeta.setLore(List.of("", Utility.colourise("&fChance&7: " + weight + "%")));
                        itemMeta.setLocalizedName("cadia.cancel");
                        itemStack.setItemMeta(itemMeta);
                        dropItems.addItem(itemStack);
                    });

                    player.openInventory(dropItems);
                }
            }
        }

        if (localizedName.contains("cadia.toggle")) {
            String[] split = localizedName.split("\\.");
            UUID uuid = UUID.fromString(split[2]);

            for (CadiaMob mob : CadiaFarmsPlugin.getInstance().getMobManager().getMobs()) {
                if (mob.getEntityUUID().equals(uuid)) {
                    mob.setDropOnFloor(!mob.doesDrop());
                    break;
                }
            }

            InventoryUtil.openBaseInventory(player, uuid, Bukkit.getEntity(uuid).getLocation());
        }

    }
}
