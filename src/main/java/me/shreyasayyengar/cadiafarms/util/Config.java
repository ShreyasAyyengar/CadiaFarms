package me.shreyasayyengar.cadiafarms.util;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Config {

    private static CadiaFarmsPlugin main;

    public static void init(CadiaFarmsPlugin main) {

        main.getLogger().info("Initializing config.yml ...");
        Config.main = main;
        main.getConfig().options().configuration();
        main.saveDefaultConfig();
        main.reloadConfig();

        assignDrops();
    }

    private static void assignDrops() {
        for (String key : main.getConfig().getConfigurationSection("mob-drops").getKeys(false)) {

            EntityType type;
            Material material;

            try {
                type = EntityType.valueOf(key);
            } catch (IllegalArgumentException e) {
                main.getLogger().warning("Invalid mob type: " + key);
                return;
            }

            for (String drop : main.getConfig().getStringList("mob-drops." + key)) {
                try {

                    String[] data = drop.split("%");

                    material = Material.valueOf(data[0]);
                    int chance = Integer.parseInt(data[1]);

                    main.getLogger().info("Adding drop: " + material + " with chance: " + chance + "%");

                    CadiaFarmsPlugin.getInstance().getMobManager().addDrop(type, material, chance);
                } catch (IllegalArgumentException e) {
                    main.getLogger().warning("Invalid material: " + drop);
                    return;
                }
            }
        }
    }

    public static Collection<ItemStack> getEntityDrops(EntityType type) {

        Collection<ItemStack> drops = new HashSet<>();
        for (String key : main.getConfig().getConfigurationSection("mob-drops").getKeys(false)) {
            if (key.equalsIgnoreCase(type.name())) {
                for (String drop : main.getConfig().getStringList("mob-drops." + key)) {
                    String[] data = drop.split("%");

                    ItemStack stack = new ItemStack(Material.valueOf(data[0]));
                    ItemMeta itemMeta = stack.getItemMeta();
                    itemMeta.setLore(List.of(Utility.colourise("&7Drop Chance: &a" + data[1])));
                    stack.setItemMeta(itemMeta);
                    drops.add(stack);
                }
            }

        }
        return drops;
    }

    public static Map<Material, Double> getTiedPercentages(EntityType type) {

        Map<Material, Double> tiedPercentages = new TreeMap<>();
        for (String key : main.getConfig().getConfigurationSection("mob-drops").getKeys(false)) {
            if (key.equalsIgnoreCase(type.name())) {
                for (String drop : main.getConfig().getStringList("mob-drops." + key)) {
                    String[] data = drop.split("%");

                    double percentage = Double.parseDouble(data[1]);
                    Material material = Material.valueOf(data[0]);

                    tiedPercentages.put(material, percentage);
                }
            }

        }
        return tiedPercentages;
    }

    public static String getSQL(String key) {
        return main.getConfig().getString("MySQL." + key);
    }

    public static int getMaxChunkEntities() {
        return main.getConfig().getInt("max-mobs-per-chunk");
    }
}
