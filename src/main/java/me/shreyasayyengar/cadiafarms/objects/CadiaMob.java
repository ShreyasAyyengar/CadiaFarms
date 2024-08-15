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
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CadiaMob {

    private final CadiaFarmsPlugin plugin = CadiaFarmsPlugin.getInstance();
    private final Collection<BukkitTask> tasks = new HashSet<>();
    private final double drop5Quota = 0.8;

    private Entity bukkitEntity;
    private UUID entityUUID;
    private EntityType entityType;

    private RandomWeightCollection<Material> drops;
    private Inventory bukkitInventory;

    private boolean dropOnFloor = false;
    private boolean loaded = true;
    private boolean previouslyFailedQuota = false;

    private double moodLevel;

    private CadiaMob() {
        plugin.getMobManager().getMobs().add(this);
    }

    public CadiaMob(UUID entityUUID) throws IOException, SQLException {
        this();
        ResultSet resultSet = plugin.getDatabase().preparedStatement("SELECT * FROM cadia_mob_info WHERE uuid = '" + entityUUID.toString() + "'").executeQuery();
        resultSet.next();

        this.bukkitEntity = Bukkit.getEntity(entityUUID);
        this.entityUUID = bukkitEntity.getUniqueId();
        this.entityType = bukkitEntity.getType();
        this.moodLevel = resultSet.getDouble("mood_level");
        this.dropOnFloor = resultSet.getBoolean("drop_floor");
        this.bukkitInventory = InventoryUtil.fromBase64(resultSet.getString("inventory"));
        setDrops();
        runTasks();
    }

    public CadiaMob(LivingEntity entity) {
        this();
        this.bukkitEntity = entity;
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

        // Entity Dead or Unloaded Task
        BukkitTask deadTask = Bukkit.getScheduler().runTaskTimer(CadiaFarmsPlugin.getInstance(), () -> {
            if (!CadiaMob.this.loaded) return;
            if (bukkitEntity.isDead()) {
                plugin.getMobManager().getMobs().remove(this);
                removeEntity();
            }
        }, 0, 20);

        // Entity Drop Task
        BukkitTask dropTask = Bukkit.getScheduler().runTaskTimer(CadiaFarmsPlugin.getInstance(), () -> {
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
        }, 1, 100);

        // Entity Calm Particle Task
        BukkitTask calmParticleTask = Bukkit.getScheduler().runTaskTimer(CadiaFarmsPlugin.getInstance(), () -> {
            if (!CadiaMob.this.loaded) return;
            if (moodLevel > 0.7) return;
            if (moodLevel > 0.5) {
                spawnCircleParticle(Color.fromBGR(255, 223, 13));
            }
        }, 1, 3000);

        // Entity Angry Particle Task
        BukkitTask angryParticleTask = Bukkit.getScheduler().runTaskTimer(CadiaFarmsPlugin.getInstance(), () -> {

            if (moodLevel <= 0.5) {
                spawnCircleParticle(Color.fromBGR(0, 0, 181));
            }

        }, 1, 160);

        // Entity Update Name Task
        BukkitTask updateNameTask = Bukkit.getScheduler().runTaskTimer(CadiaFarmsPlugin.getInstance(), () -> {

            if (!CadiaMob.this.loaded) return;

            if (!bukkitEntity.isCustomNameVisible()) {
                bukkitEntity.setCustomNameVisible(true);
            }

            String moodAsString = CadiaFarmsPlugin.getInstance().getMobManager().getMoodAsString(entityUUID);
            bukkitEntity.setCustomName(Utility.colourise("&a&lCow &8- " + moodAsString));

        }, 1, 20);

        // Entity Mood Decrease Task
        BukkitTask moodTask = Bukkit.getScheduler().runTaskTimerAsynchronously(CadiaFarmsPlugin.getInstance(), () -> {
            if (!CadiaMob.this.loaded) return;

            if (moodLevel > 0) {
                moodLevel -= 0.1;
            }
        }, 12000, 12000);

        tasks.addAll(List.of(deadTask, dropTask, calmParticleTask, angryParticleTask, updateNameTask, moodTask));
    }

    private void dropItem() {
        if (drops != null) {
            ItemStack drop = new ItemStack(drops.next());
            if (dropOnFloor) {
                bukkitEntity.getWorld().dropItem(bukkitEntity.getLocation(), drop);
            } else {

                // Handles full inventory
                HashMap<Integer, ItemStack> integerItemStackHashMap = bukkitInventory.addItem(drop);
                if (!integerItemStackHashMap.isEmpty()) {
                    for (ItemStack itemStack : integerItemStackHashMap.values()) {
                        bukkitEntity.getWorld().dropItem(bukkitEntity.getLocation(), itemStack);
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
                            bukkitEntity.getWorld().dropItem(bukkitEntity.getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    private void removeEntity() {
        tasks.forEach(BukkitTask::cancel);
        plugin.getMobManager().getMobs().remove(this);

        CadiaFarmsPlugin.getInstance().getDatabase().preparedStatementBuilder("DELETE FROM cadia_mob_info WHERE uuid = '" + entityUUID + "'").executeUpdate();
    }

    private void spawnCircleParticle(Color color) {
        Location location = bukkitEntity.getLocation();
        for (int degree = 0; degree < 360; degree++) {
            double radians = Math.toRadians(degree);
            double x = Math.cos(radians);
            double z = Math.sin(radians);

            double red = color.getRed() / 255D;
            double green = color.getGreen() / 255D;
            double blue = color.getBlue() / 255D;

            location.add(x, 0, z);
            location.getWorld().spawnParticle(Particle.SPELL_MOB, location.clone().add(0, 1, 0), 0, red, green, blue, 1);
            location.subtract(x, 0, z);
        }
    }

    public void serialise() {
        plugin.getDatabase().preparedStatementBuilder("UPDATE cadia_mob_info SET entity_type = '" + entityType + "', mood_level = '" + moodLevel + "', inventory = '" + InventoryUtil.toBase64(bukkitInventory) + "' WHERE uuid = '" + entityUUID + "'").executeUpdate();
    }

    public void load() {
        this.loaded = true;
    }

    public void unload() {
        this.loaded = false;
    }

    // --- Getters

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

    public UUID getEntityUUID() {
        return this.entityUUID;
    }
}
