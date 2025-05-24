package me.chromisek.chromiumGUI.utils;

import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ServerStatsTask extends BukkitRunnable {

    public ServerStatsTask() {
        // Konstruktor může být prázdný, pokud už nedělá detekci Sparku atd.
        // Nebo zde můžeš logovat, že task byl spuštěn.
        ChromiumGUI.getInstance().getLogger().info("ServerStatsTask pro GUI refresh spuštěn.");
    }

    @Override
    public void run() {
        // Jen zajistí refresh GUI na hlavním vlákně
        Bukkit.getScheduler().runTask(ChromiumGUI.getInstance(), () -> {
            // Pojistka pro případ, že by se plugin vypínal
            if (ChromiumGUI.getInstance() != null && ChromiumGUI.getInstance().isEnabled() && ChromiumGUI.getInstance().getGUIManager() != null) {
                ChromiumGUI.getInstance().getGUIManager().refreshAllGUIs();
            }
        });
    }

    // Metody getTPS(), getUsedMemoryMB() atd. odsud můžeš odstranit,
    // protože tyto hodnoty budeme brát z PAPI placeholderů přímo v GUIManageru.
}