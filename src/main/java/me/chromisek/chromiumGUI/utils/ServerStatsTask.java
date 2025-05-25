package me.chromisek.chromiumGUI.utils;

import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ServerStatsTask extends BukkitRunnable {

    public ServerStatsTask() {

        ChromiumGUI.getInstance().getLogger().info("ServerStatsTask pro GUI refresh spuštěn.");
    }

    @Override
    public void run() {

        Bukkit.getScheduler().runTask(ChromiumGUI.getInstance(), () -> {

            if (ChromiumGUI.getInstance() != null && ChromiumGUI.getInstance().isEnabled() && ChromiumGUI.getInstance().getGUIManager() != null) {
                ChromiumGUI.getInstance().getGUIManager().refreshAllGUIs();
            }
        });
    }


}