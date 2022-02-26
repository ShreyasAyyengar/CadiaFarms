package me.shreyasayyengar.cadiafarms.commands;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import me.shreyasayyengar.cadiafarms.util.Config;
import me.shreyasayyengar.cadiafarms.util.Utility;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CadiaBaseCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            if (args.length <= 0) {
                sendUsage(player);
                return false;
            }

            System.out.println(args[0].toLowerCase());
            switch (args[0]) {
                case "give" -> {
                    if (args.length != 4) {
                        sendUsage(player);
                    }

                    EntityType type;
                    Player target = player.getServer().getPlayer(args[2]);
                    int amount;

                    try {
                        amount = Integer.parseInt(args[3]);
                        type = EntityType.valueOf(args[1].toUpperCase());
                    } catch (NumberFormatException e) {
                        player.sendMessage(Utility.colourise("&cAmount must be an number!."));
                        return false;
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utility.colourise("&cInvalid entity type, here is a list of supported types:"));
                        Config.getRegisteredEntityTypes().forEach(registeredType -> player.sendMessage(Utility.colourise(" - &7" + registeredType.toString())));
                        return false;
                    }
                    if (target == null) {
                        player.sendMessage(Utility.colourise("&cPlayer with the name " + args[2] + " not found."));
                        return false;
                    }

                    CadiaFarmsPlugin.getInstance().getMobManager().giveCadiaMobEgg(target, type, amount);

                }
                case "clear", "remove" -> {
                    if (args.length != 2) {
                        sendUsage(player);
                        return false;
                    }

                    Chunk chunk = player.getLocation().getChunk();
                    EntityType type;
                    try {
                        type = EntityType.valueOf(args[1].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utility.colourise("&cInvalid entity type, here is a list of supported types:"));
                        Config.getRegisteredEntityTypes().forEach(registeredType -> player.sendMessage(Utility.colourise(" - &7" + registeredType.toString())));
                        return false;
                    }

                    CadiaFarmsPlugin.getInstance().getMobManager().clearMobs(type, chunk);

                }
                case "reload" -> {
                    CadiaFarmsPlugin.getInstance().reloadConfig();
                    player.sendMessage(Utility.colourise("&aConfiguration & Plugin reloaded!"));
                    return false;
                }
            }
        }

        return false;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Utility.colourise("&cUsage: /cadia give <type|mob> <player> <amount>"));
        player.sendMessage(Utility.colourise("&cUsage: /cadia clear <type|mob>"));
        player.sendMessage(Utility.colourise("&cUsage: /cadia reload"));
    }
}
