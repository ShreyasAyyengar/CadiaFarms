package me.shreyasayyengar.cadiafarms.objects;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.util.InventoryUtil;
import me.shreyasayyengar.cadiafarms.util.Utility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class CadiaMob {

    private final CadiaFarmsPlugin plugin = CadiaFarmsPlugin.getInstance();
    private final Collection<BukkitTask> tasks = new ArrayList<>();
    private final double drop5Quota = 0.6;

    private UUID entityUUID;
    private EntityType entityType;
    private Entity bukkitEntity;
    private RandomWeightCollection<Material> drops;
    private Inventory bukkitInventory;
    private boolean dropOnFloor = false;
    private ResultSet resultSet;
    private boolean previouslyFailedQuota = false;

    private double moodLevel;

    public CadiaMob() {
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

        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                boolean load = Bukkit.getWorld("onexp").getChunkAt(new Location((Bukkit.getWorld("onexp")), 234, 0, 83)).load();
                System.out.println(load + " <- chunk loaded");
                System.out.println(new Location((Bukkit.getWorld("onexp")), 234, 0, 83).getChunk().isLoaded());

                System.out.println(Bukkit.getEntity(entityUUID));

                if (Bukkit.getEntity(entityUUID) == null || Bukkit.getEntity(entityUUID).isDead()) {
                    plugin.getMobManager().getMobs().remove(CadiaMob.this);
                    removeEntity();
                }
            }
        }.runTaskTimer(CadiaFarmsPlugin.getInstance(), 20L, 20L));


        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {

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

        }.runTaskTimer(CadiaFarmsPlugin.getInstance(), 1, 100L));

        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (moodLevel > 0) {
                    moodLevel -= 0.1;
                }
            }
        }.runTaskTimerAsynchronously(CadiaFarmsPlugin.getInstance(), 12000L, 12000L));
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

    public UUID getEntityUUID() {
        return this.entityUUID;
    }

    public void plusMood() {
        this.moodLevel = moodLevel + 0.1;
    }

    public double getMoodLevel() {
        return moodLevel;
    }

    public void setDropOnFloor(boolean dropOnFloor) {
        this.dropOnFloor = dropOnFloor;
    }

    public boolean doesDrop() {
        return dropOnFloor;
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
}
