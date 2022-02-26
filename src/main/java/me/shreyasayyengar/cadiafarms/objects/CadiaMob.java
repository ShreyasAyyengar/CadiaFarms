package me.shreyasayyengar.cadiafarms.objects;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.util.InventoryUtil;
import me.shreyasayyengar.cadiafarms.util.Utility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CadiaMob {

    public static final Collection<CadiaMob> UNLOADED_MOBS = new HashSet<>();

    private final CadiaFarmsPlugin plugin = CadiaFarmsPlugin.getInstance();
    private final Collection<BukkitTask> tasks = new HashSet<>();
    private final double drop5Quota = 0.6;

    private UUID entityUUID;
    private EntityType entityType;
    private Entity bukkitEntity;
    private RandomWeightCollection<Material> drops;
    private Inventory bukkitInventory;
    private boolean dropOnFloor = false;
    private boolean loaded = true;
    private ResultSet resultSet;
    private boolean previouslyFailedQuota = false;

    private double moodLevel;

    private CadiaMob() {
        plugin.getMobManager().getMobs().add(this);
    }

    public CadiaMob(UUID entityUUID) throws IOException, SQLException {
        this();
        this.resultSet = plugin.getDatabase().preparedStatement("SELECT * FROM cadia_mob_info WHERE uuid = '" + entityUUID.toString() + "'").executeQuery();
        resultSet.next();

        this.entityUUID = entityUUID;
        this.entityType = EntityType.valueOf(resultSet.getString("entity_type"));
        this.moodLevel = resultSet.getDouble("mood_level");
        this.dropOnFloor = resultSet.getBoolean("drop_floor");
        this.bukkitInventory = InventoryUtil.fromBase64(resultSet.getString("inventory"));
//        this.entityType = EntityType.valueOf(plugin.getConfig().getString(path + "entity-type"));
//        this.moodLevel = plugin.getConfig().getDouble(path + "mood-level");
//        this.bukkitInventory = InventoryUtil.fromBase64(plugin.getConfig().getString(path + "inventory"));
        setDrops();
        runTasks();
    }

    public CadiaMob(LivingEntity entity) {
        this();
        this.entityUUID = entity.getUniqueId();
        this.entityType = entity.getType();
        this.moodLevel = 0.5;
        this.bukkitInventory = Bukkit.createInventory(null, 54, "Mob Bank");
        Utility.createData(this);
        setDrops();
        runTasks();
    }

    private void setDrops() {
        this.bukkitInventory.setMaxStackSize(32);
        this.drops = CadiaFarmsPlugin.getInstance().getMobManager().getDrops(this.entityType);
    }

    private void runTasks() {
        if (!this.loaded) return;

        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!CadiaMob.this.loaded) return;
                if (Bukkit.getEntity(entityUUID) == null || Bukkit.getEntity(entityUUID).isDead()) {
                    plugin.getMobManager().getMobs().remove(CadiaMob.this);
                    removeEntity();
                }
            }
        }.runTaskTimer(CadiaFarmsPlugin.getInstance(), 0, 20));


        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!CadiaMob.this.loaded) return;

                if (previouslyFailedQuota) {
                    dropItem();
                    previouslyFailedQuota = false;
                    return;
                }

                if (moodLevel >= drop5Quota) {
                    dropItem();
                    return;
                }

                previouslyFailedQuota = true;
            }

        }.runTaskTimer(CadiaFarmsPlugin.getInstance(), 1, 100));

        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!CadiaMob.this.loaded) return;
                if (moodLevel > 0.7) return;

                Color color = Color.GRAY;

                if (moodLevel <= 0.5) {
                    color = Color.fromBGR(0, 0, 181);
                }

                if (moodLevel > 0.5) {
                    color = Color.fromBGR(255, 223, 13);
                }

                Location location = Bukkit.getEntity(entityUUID).getLocation();
                for (int degree = 0; degree < 360; degree++) {
                    double radians = Math.toRadians(degree);
                    double x = Math.cos(radians);
                    double z = Math.sin(radians);
                    location.add(x, 0, z);
                    location.getWorld().spawnParticle(Particle.REDSTONE.builder().color(Color.RED).count(1).particle(), location.clone().add(0, 1, 0), 2, new Particle.DustOptions(color, 1));
                    location.subtract(x, 0, z);
                }
            }
        }.runTaskTimer(CadiaFarmsPlugin.getInstance(), 1, 3000L));

        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (!CadiaMob.this.loaded) return;

                if (moodLevel > 0) {
                    moodLevel -= 0.1;
                }
            }
        }.runTaskTimerAsynchronously(CadiaFarmsPlugin.getInstance(), 12000, 12000));
    }

    private void dropItem() {
        if (drops != null) {
            ItemStack drop = new ItemStack(drops.next());
            if (dropOnFloor) {
                Bukkit.getEntity(entityUUID).getWorld().dropItem(Bukkit.getEntity(entityUUID).getLocation(), drop);
            } else {

                // Handles full inventory
                HashMap<Integer, ItemStack> integerItemStackHashMap = bukkitInventory.addItem(drop);
                if (!integerItemStackHashMap.isEmpty()) {
                    for (ItemStack itemStack : integerItemStackHashMap.values()) {
                        Bukkit.getEntity(entityUUID).getWorld().dropItem(Bukkit.getEntity(entityUUID).getLocation(), itemStack);
                    }
                    return;
                }

                // handles max stack size
                int typeCounter = 0;
                for (ItemStack stack : bukkitInventory.getStorageContents()) {
                    if (stack == null) return;

                    if (stack.getType() == drop.getType()) {
                        typeCounter += stack.getAmount() + 1;
                        if (typeCounter >= 32) {
                            Bukkit.getEntity(entityUUID).getWorld().dropItem(Bukkit.getEntity(entityUUID).getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    private void removeEntity() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }

        plugin.getMobManager().getMobs().remove(this);

        Utility.removeMob(entityUUID);
    }

    public void serialise() {
        try {
            plugin.getDatabase().preparedStatement("UPDATE cadia_mob_info SET entity_type = '" + entityType + "', mood_level = '" + moodLevel + "', inventory = '" + InventoryUtil.toBase64(bukkitInventory) + "' WHERE uuid = '" + entityUUID + "'").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        this.loaded = true;
    }

    public void unload() {
        this.loaded = false;
    }


    // --- Getters

    public UUID getEntityUUID() {
        return this.entityUUID;
    }

    public void plusMood() {
        if (moodLevel < 1.0) {
            moodLevel += 0.1;
        }
    }

    public double getMoodLevel() {
        return moodLevel;
    }

    public boolean doesDrop() {
        return dropOnFloor;
    }

    public void setDropOnFloor(boolean dropOnFloor) {
        this.dropOnFloor = dropOnFloor;
    }


    public Inventory getBukkitInventory() {
        return bukkitInventory;
    }

    public EntityType getType() {
        return entityType;
    }

    public RandomWeightCollection<Material> getDrops() {
        return drops;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
