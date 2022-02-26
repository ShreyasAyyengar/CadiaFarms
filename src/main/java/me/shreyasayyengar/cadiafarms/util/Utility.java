package me.shreyasayyengar.cadiafarms.util;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.database.MySQL;
import me.shreyasayyengar.cadiafarms.objects.CadiaMob;
import org.bukkit.ChatColor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Utility {

    public static String colourise(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void createData(CadiaMob mob) {
        MySQL database = CadiaFarmsPlugin.getInstance().getDatabase();

        try {
            ResultSet resultSet = database.preparedStatement("SELECT COUNT(uuid) FROM cadia_mob_info WHERE uuid = '" + mob.getEntityUUID() + "'").executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) == 0) {
                database.preparedStatement("INSERT INTO cadia_mob_info (uuid, entity_type, inventory, mood_level) " +
                        "VALUES ('" + mob.getEntityUUID() + "', '" + mob.getType() + "', null, 0.5)").executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeMob(UUID entityUUID) {

        try {
            CadiaFarmsPlugin.getInstance().getDatabase().preparedStatement("DELETE FROM cadia_mob_info WHERE `uuid` = '" + entityUUID + "'").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("removed mob method called");

    }

}