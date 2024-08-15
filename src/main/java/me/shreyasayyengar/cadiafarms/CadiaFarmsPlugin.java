package me.shreyasayyengar.cadiafarms;

import me.shreyasayyengar.cadiafarms.commands.CadiaBaseCommand;
import me.shreyasayyengar.cadiafarms.database.MySQL;
import me.shreyasayyengar.cadiafarms.events.ChunkLoad;
import me.shreyasayyengar.cadiafarms.events.Click;
import me.shreyasayyengar.cadiafarms.events.Interact;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import me.shreyasayyengar.cadiafarms.util.CadiaMobManager;
import me.shreyasayyengar.cadiafarms.util.Config;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

public final class CadiaFarmsPlugin extends JavaPlugin {

    private final CadiaMobManager mobManager = new CadiaMobManager();
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
        Stream.of(
                new Interact(),
                new Click(),
                new ChunkLoad()
        ).forEach(event -> getServer().getPluginManager().registerEvents(event, this));
    }

    private void registerCommands() {
        //noinspection ConstantConditions
        this.getCommand("cadiafarm").setExecutor(new CadiaBaseCommand());
    }

    private void initMySQL() {
        this.database = new MySQL(Config.getSQL("username"), Config.getSQL("password"), Config.getSQL("database"), Config.getSQL("host"), Integer.parseInt(Config.getSQL("port")));

        this.database.preparedStatementBuilder("create table if not exists cadia_mob_info(" +
                "    uuid        varchar(36) null," +
                "    entity_type tinyblob    null," +
                "    inventory   longtext    null," +
                "    drop_floor  boolean     default false," +
                "    mood_level  double      null" +
                ");").executeUpdate();
    }

    private void loadExistingMobs() {

        try {
            ResultSet resultSet = database.preparedStatement("select * from cadia_mob_info").executeQuery();

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                mobManager.unloadedWaitlist.add(uuid);
            }

        } catch (SQLException e) {
            getLogger().severe("There was a problem loading existing mobs from the database! Please contact the developer!");
            this.getServer().getPluginManager().disablePlugin(this);
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
