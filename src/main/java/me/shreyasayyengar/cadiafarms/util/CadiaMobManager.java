package me.shreyasayyengar.cadiafarms.util;

import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.objects.RandomWeightCollection;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.material.SpawnEgg;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CadiaMobManager {

    public final Collection<UUID> unloadedWaitlist = new HashSet<>();
    private final List<CadiaMob> mobs = new ArrayList<>();
    private final Map<EntityType, RandomWeightCollection<Material>> randomisdDrops = new HashMap<>();

    @SuppressWarnings("deprecation")
    public void giveCadiaMobEgg(Player player, EntityType type, int amount) {

        ItemStack stack = new SpawnEgg(type).toItemStack(amount);
        SpawnEggMeta itemMeta = (SpawnEggMeta) stack.getItemMeta();
        itemMeta.setDisplayName(WordUtils.capitalize(type.name().toLowerCase()) + " Spawn Egg");
        itemMeta.setLocalizedName("cadiamob." + type.name() + ".cancelexempt");
        stack.setItemMeta(itemMeta);

        HashMap<Integer, ItemStack> integerItemStackHashMap = player.getInventory().addItem(stack);
        if (!integerItemStackHashMap.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), integerItemStackHashMap.get(0));
        }
        player.sendMessage(Utility.colourise("&bSpawn egg given!"));
    }

    public RandomWeightCollection<Material> getDrops(EntityType type) {
        return randomisdDrops.get(type);
    }

    public void addDrop(EntityType type, Material material, int weight) {
        if (!randomisdDrops.containsKey(type)) {
            randomisdDrops.put(type, new RandomWeightCollection<>());
        }
        randomisdDrops.get(type).add(weight, material);
    }

    public String getMobMood(UUID mobUUID) {

        String mood = Utility.colourise("&7Failed to retrieve mood");
        for (CadiaMob mob : mobs) {
            if (mob.getEntityUUID().equals(mobUUID)) {

                if (mob.getMoodLevel() <= 0.5) {
                    mood = Utility.colourise("&4&lANGRY");
                }

                if (mob.getMoodLevel() > 0.5 && mob.getMoodLevel() <= 0.7) {
                    mood = Utility.colourise("&b&lCALM");
                }

                if (mob.getMoodLevel() > 0.7) {
                    mood = Utility.colourise("&a&lHAPPY");
                }
            }
        }

        return mood;
    }

    public List<CadiaMob> getMobs() {
        return mobs;
    }

    public void clearMobs(EntityType type, Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            for (CadiaMob mob : mobs) {
                if (mob.getEntityUUID().equals(entity.getUniqueId())) {
                    if (mob.getType().equals(type)) {
                        entity.remove();
                    }
                }
            }
        }
    }

    public int getEntityCountInChunk(Chunk chunk) {
        AtomicInteger amount = new AtomicInteger();

        for (Entity entity : chunk.getEntities()) {
            mobs.forEach(cadiaMob -> {
                if (cadiaMob.getEntityUUID().equals(entity.getUniqueId())) {
                    amount.getAndIncrement();
                }
            });
        }

        return amount.get();
    }

}