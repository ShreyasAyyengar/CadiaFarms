package me.shreyasayyengar.cadiafarms.util;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InventoryUtil {

    public static String toBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(inventory.getSize());

            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static Inventory fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    @SuppressWarnings("deprecation")
    public static void openBaseInventory(Player player, UUID mobUUID, Location location) {

        CadiaMob mob = CadiaFarmsPlugin.getInstance().getMobManager().getMobs().stream().filter(c -> c.getEntityUUID().equals(mobUUID)).findFirst().orElse(null);
        assert mob != null;

        Inventory inventory = Bukkit.createInventory(null, 9, Utility.colourise("&6&lCadia Mob Inventory"));
        String moodAsString = CadiaFarmsPlugin.getInstance().getMobManager().getMoodAsString(mobUUID);

        int maxMobsPerChunk = Config.getMaxChunkEntities();
        int currentMobsPerChunk = CadiaFarmsPlugin.getInstance().getMobManager().getEntityCountInChunk(Bukkit.getEntity(mobUUID).getChunk());
        double moodLevel = mob.getMoodLevel();

        ItemStack mood = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta moodMeta = mood.getItemMeta();
        moodMeta.setLocalizedName("cadia.cancel");
//        moodMeta.setDisplayName(Utility.colourise("&6&lMood"));
//        moodMeta.setLore(List.of(
//                "Current mood level: " + CadiaFarmsPlugin.getInstance().getMobManager().getMoodAsString(mobUUID),
//                "(" + (int) (mob.getMoodLevel() * 10) + "/10)"
//        ));
        moodMeta.setDisplayName(Config.getDisplayName(1));
        moodMeta.setLore(Config.getLore(1).stream().map(s -> s.replace("{moodWord}", moodAsString).replace("{moodNumber}", ((int) (moodLevel * 10)) + "")).toList());

        mood.setItemMeta(moodMeta);

        ItemStack openStorage = new ItemStack(Material.CHEST);
        ItemMeta openStorageItemMeta = openStorage.getItemMeta();
        openStorageItemMeta.setLocalizedName("cadia.openbank." + mobUUID);
//        openStorageItemMeta.setDisplayName(Utility.colourise("&6&lOpen Drop Storage"));
        openStorageItemMeta.setDisplayName(Config.getDisplayName(2));
        openStorageItemMeta.setLore(Config.getLore(2));
        openStorage.setItemMeta(openStorageItemMeta);

        ItemStack dropOptions = new ItemStack(Material.BUNDLE);
        ItemMeta dropOptionsItemMeta = dropOptions.getItemMeta();
        dropOptionsItemMeta.setLocalizedName("cadia.droppings." + mobUUID);
//        dropOptionsItemMeta.setDisplayName(Utility.colourise("&6&lDrop Options"));
        dropOptionsItemMeta.setDisplayName(Config.getDisplayName(3));
        dropOptionsItemMeta.setLore(Config.getLore(3));
        dropOptions.setItemMeta(dropOptionsItemMeta);

        ItemStack drop = new ItemStack(Material.BARRIER);
        ItemMeta dropMeta = drop.getItemMeta();
        dropMeta.setLocalizedName("cadia.toggle." + mobUUID);
        dropMeta.setDisplayName(Utility.colourise("&6&lDrop Item"));
        if (mob.doesDrop()) {
            drop = new ItemStack(Material.GREEN_TERRACOTTA);
            dropMeta.setLore(List.of(Utility.colourise("&a&lDrop on Floor: &a&lTrue")));
        } else {
            drop = new ItemStack(Material.RED_TERRACOTTA);
            dropMeta.setLore(List.of(Utility.colourise("&c&lDrop on Floor: &c&lFalse")));
        }
        drop.setItemMeta(dropMeta);


        ItemStack numberInChunks = new ItemStack(Material.SPAWNER);
        ItemMeta numberInChunksItemMeta = numberInChunks.getItemMeta();
        numberInChunksItemMeta.setLocalizedName("cadia.cancel");

//        numberInChunksItemMeta.setDisplayName(Utility.colourise(
//                "&dNumber of Mobs in Chunk: &d" + CadiaFarmsPlugin.getInstance().getMobManager().getEntityCountInChunk(location.getChunk()) + "/" + Config.getMaxChunkEntities()));
        numberInChunksItemMeta.setDisplayName(Config.getDisplayName(5).replace("{current}", String.valueOf(currentMobsPerChunk)).replace("{max}", String.valueOf(maxMobsPerChunk)));
        numberInChunksItemMeta.setLore(Config.getLore(5).stream().map(s -> s.replace("{current}", String.valueOf(currentMobsPerChunk)).replace("{max}", String.valueOf(maxMobsPerChunk))).toList());
        numberInChunks.setItemMeta(numberInChunksItemMeta);

        inventory.setItem(0, mood);
        inventory.setItem(2, openStorage);
        inventory.setItem(4, dropOptions);
        inventory.setItem(6, drop);
        inventory.setItem(8, numberInChunks);

        player.openInventory(inventory);
    }
}
