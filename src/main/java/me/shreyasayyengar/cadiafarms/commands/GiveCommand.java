package me.shreyasayyengar.cadiafarms.commands;

import me.shreyasayyengar.cadiafarms.CadiaFarmsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GiveCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        CadiaFarmsPlugin.getInstance().getMobManager().giveCadiaMobEgg(((Player) sender), EntityType.COW);

        return false;
    }
}
