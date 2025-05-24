package me.chromisek.chromiumGUI.commands;

import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DirectGuiCommand implements CommandExecutor {

    private final ChromiumGUI plugin;

    public DirectGuiCommand(ChromiumGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        Player player = (Player) sender;

        plugin.getGUIManager().openMainGUI(player);
        // Můžeš přidat i zprávu pro hráče, pokud chceš
        player.sendMessage("§a§lChromium §7» §fOpening main panel...");
        return true;
    }
}