package me.chromisek.chromiumGUI.commands;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChromiumCommand implements CommandExecutor, TabCompleter {
    
    private final ChromiumGUI plugin;
    
    public ChromiumCommand(ChromiumGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "gui":
                openMainGUI(player);
                break;
                
            case "reload":
                if (!player.hasPermission("chromium.admin")) {
                    player.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                reloadPlugin(player);
                break;
                
            case "version":
            case "ver":
                sendVersionInfo(player);
                break;
                
            case "help":
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void openMainGUI(Player player) {
        plugin.getGUIManager().openMainGUI(player);
        player.sendMessage("§a§lChromium §7» §fOpening main panel...");
    }
    
    private void reloadPlugin(Player player) {
        try {
            // Reload ChromiumCore config
            ChromiumCore.getInstance().getGeneralConfig().reloadConfig();
            
            // Close all open GUIs
            plugin.getGUIManager().closeAllGUIs();
            
            player.sendMessage("§a§lChromium §7» §fConfiguration reloaded successfully!");
            ChromiumCore.getInstance().getLogger().info("Configuration reloaded by " + player.getName());
            
        } catch (Exception e) {
            player.sendMessage("§c§lChromium §7» §fError while reloading configuration!");
            ChromiumCore.getInstance().getLogger().severe("Error reloading config: " + e.getMessage());
        }
    }
    
    private void sendVersionInfo(Player player) {
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lChromium Framework §7- §fVersion Information");
        player.sendMessage("");
        player.sendMessage("§f├ §eChromiumCore: §a" + ChromiumCore.getInstance().getDescription().getVersion());
        player.sendMessage("§f├ §eChromiumGUI: §a" + plugin.getDescription().getVersion());
        player.sendMessage("§f└ §eAuthor: §fchromisek");
        player.sendMessage("");
        player.sendMessage("§7Server: §f" + player.getServer().getVersion());
        player.sendMessage("§7API: §f" + player.getServer().getBukkitVersion());
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lChromium Framework §7- §fHelp Menu");
        player.sendMessage("");
        player.sendMessage("§f├ §e/chromium gui §7- §fOpen main control panel");
        player.sendMessage("§f├ §e/chromium version §7- §fShow version information");
        player.sendMessage("§f├ §e/chromium reload §7- §fReload configuration §c(Admin)");
        player.sendMessage("§f└ §e/chromium help §7- §fShow this help menu");
        player.sendMessage("");
        player.sendMessage("§7Aliases: §f/cgui, /gui §7(for GUI only)");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("gui", "version", "help", "reload");
            String input = args[0].toLowerCase();
            
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(input)) {
                    // Check permissions for admin commands
                    if (subcommand.equals("reload") && !sender.hasPermission("chromium.admin")) {
                        continue;
                    }
                    completions.add(subcommand);
                }
            }
        }
        
        return completions;
    }
}