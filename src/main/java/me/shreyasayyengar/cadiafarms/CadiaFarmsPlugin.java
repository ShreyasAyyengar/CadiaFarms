package me.shreyasayyengar.cadiafarms;

import me.shreyasayyengar.cadiafarms.commands.GiveCommand;
import me.shreyasayyengar.cadiafarms.database.MySQL;
import me.shreyasayyengar.cadiafarms.events.Click;
import me.shreyasayyengar.cadiafarms.events.Interact;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.CadiaMobManager;
import me.shreyasayyengar.cadiafarms.util.Config;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public final class CadiaFarmsPlugin extends JavaPlugin {

    private final CadiaMobManager mobManager = new CadiaMobManager();
    private final Collection<UUID> unLoadedCadiaMobs = new ArrayList<>();
    private MySQL database;

    public static CadiaFarmsPlugin getInstance() {
        return JavaPlugin.getPlugin(CadiaFarmsPlugin.class);
    }

    @Override
    public void onEnable() {
        Config.init(this);
        registerEvents();
        registerCommands();
        initMySQL();
        loadExistingMobs();
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(new Interact(), this);
        this.getServer().getPluginManager().registerEvents(new Click(), this);
    }

    private void registerCommands() {
        this.getCommand("give").setExecutor(new GiveCommand());
    }

    private void initMySQL() {
        this.database = new MySQL(Config.getSQL("username"), Config.getSQL("password"), Config.getSQL("database"), Config.getSQL("host"), Integer.parseInt(Config.getSQL("port")));

        try {
            this.database.preparedStatement("create table if not exists cadia_mob_info(" +
                    "    uuid        varchar(36) null," +
                    "    entity_type tinyblob    null," +
                    "    inventory   longtext    null," +
                    "    drop_floor  boolean     default false," +
                    "    mood_level  double      null" +
                    ");").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadExistingMobs() {

        try {
            ResultSet resultSet = database.preparedStatement("select * from cadia_mob_info").executeQuery();

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                new CadiaMob(uuid);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDisable() {
        mobManager.getMobs().forEach(CadiaMob::serialise);
    }

    public CadiaMobManager getMobManager() {
        return mobManager;
    }

    public MySQL getDatabase() {
        return database;
    }

}
