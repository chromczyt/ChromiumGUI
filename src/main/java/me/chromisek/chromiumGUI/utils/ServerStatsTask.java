package me.chromisek.chromiumGUI.utils;

import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ServerStatsTask extends BukkitRunnable {

    private double currentTPS = -1.0; // Výchozí hodnota značící "nedostupné"
    private long usedMemory;
    private long maxMemory;
    private final long startTime;
    private boolean sparkApiAvailable = false;
    private Object sparkApiInstance = null;
    private Class<?> sparkApiClass = null;

    public ServerStatsTask() {
        this.startTime = System.currentTimeMillis();

        Plugin sparkPlugin = Bukkit.getPluginManager().getPlugin("spark");
        if (sparkPlugin != null && sparkPlugin.isEnabled()) {
            try {
                this.sparkApiClass = Class.forName("me.lucko.spark.api.Spark");
                this.sparkApiInstance = Bukkit.getServicesManager().getRegistration(this.sparkApiClass).getProvider();
                if (this.sparkApiInstance != null) {
                    this.sparkApiAvailable = true;
                    ChromiumGUI.getInstance().getLogger().info("Spark plugin detected! Using Spark for TPS metrics.");
                } else {
                    ChromiumGUI.getInstance().getLogger().warning("Spark plugin detected, but could not get Spark API provider from ServicesManager. TPS via Spark will not be available.");
                }
            } catch (ClassNotFoundException e) {
                ChromiumGUI.getInstance().getLogger().warning("Spark API class (me.lucko.spark.api.Spark) not found. Spark TPS will not be used.");
            } catch (NoClassDefFoundError e) {
                ChromiumGUI.getInstance().getLogger().warning("Spark API class definition not found (NoClassDefFoundError). Spark TPS will not be used. Is Spark fully loaded and compatible?");
            } catch (Exception e) {
                ChromiumGUI.getInstance().getLogger().warning("An unexpected error occurred while initializing Spark API access: " + e.getMessage());
            }
        }

        if (!this.sparkApiAvailable) {
            ChromiumGUI.getInstance().getLogger().warning("Spark plugin not found or not enabled/initialized correctly. TPS metrics will not be available. Please install Spark for TPS monitoring: https://www.spigotmc.org/resources/spark.57242/");
        }
        updateStats(); // První aktualizace statistik
    }

    @Override
    public void run() {
        updateStats();
        Bukkit.getScheduler().runTask(ChromiumGUI.getInstance(), () -> {
            if (ChromiumGUI.getInstance() != null && ChromiumGUI.getInstance().getGUIManager() != null) {
                ChromiumGUI.getInstance().getGUIManager().refreshAllGUIs();
            }
        });
    }

    private void updateStats() {
        if (sparkApiAvailable && sparkApiInstance != null) {
            this.currentTPS = getSparkTPSValue();
        } else {
            this.currentTPS = -1.0; // Indikuje, že TPS není dostupné bez Sparku
        }

        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        this.maxMemory = runtime.maxMemory();
        this.usedMemory = runtime.totalMemory() - runtime.freeMemory();
    }

    private double getSparkTPSValue() {
        try {
            // Možnost 1: Získat TPS přímo z objektu Tps (např. 1-minutový průměr)
            try {
                Object tpsDataObject = this.sparkApiClass.getMethod("tps").invoke(this.sparkApiInstance);
                if (tpsDataObject != null) {
                    double tpsAverage = (double) tpsDataObject.getClass().getMethod("oneMinuteAverage").invoke(tpsDataObject);
                    return Math.min(20.0, Math.max(0.0, tpsAverage)); // Ochrana pro validní rozsah
                }
            } catch (NoSuchMethodException nsme) {
                ChromiumGUI.getInstance().getLogger().info("Spark: tps().oneMinuteAverage() not available, trying MSPT method for TPS.");
            } catch (Exception e) {
                ChromiumGUI.getInstance().getLogger().warning("Error getting TPS averages from Spark: " + e.getMessage() + ". Trying MSPT for TPS.");
            }

            // Možnost 2: Vypočítat TPS z MSPT
            Object msptStatisticObject = this.sparkApiClass.getMethod("mspt").invoke(this.sparkApiInstance);
            if (msptStatisticObject == null) {
                ChromiumGUI.getInstance().getLogger().warning("Spark MSPT statistic not available.");
                return -1.0;
            }

            Class<?> statisticWindowInterface = Class.forName("me.lucko.spark.api.statistic.StatisticWindow");
            Class<?> msptWindowEnumClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$MillisPerTick");
            Object minutes1WindowConstant = msptWindowEnumClass.getField("MINUTES_1").get(null);

            Object aggregateDataObject = msptStatisticObject.getClass().getMethod("poll", statisticWindowInterface).invoke(msptStatisticObject, minutes1WindowConstant);
            if (aggregateDataObject == null) {
                ChromiumGUI.getInstance().getLogger().warning("Could not poll MSPT data from Spark.");
                return -1.0;
            }

            double meanMspt = (double) aggregateDataObject.getClass().getMethod("mean").invoke(aggregateDataObject);

            if (meanMspt <= 0) {
                return 20.0; // Pokud je MSPT neplatné, raději ukažme plné TPS než chybu
            }
            return Math.min(20.0, Math.max(0.0, 1000.0 / meanMspt)); // Ochrana pro validní rozsah

        } catch (Exception e) {
            ChromiumGUI.getInstance().getLogger().warning("Failed to get TPS from Spark via reflection: " + e.getMessage());
            // e.printStackTrace(); // Pro detailní ladění můžeš dočasně odkomentovat
            return -1.0; // Selhalo získání TPS ze Sparku
        }
    }

    /**
     * Vrací aktuální TPS. Pokud je hodnota -1.0, TPS není dostupné (např. chybí Spark).
     * @return Aktuální TPS nebo -1.0 pokud není dostupné.
     */
    public double getTPS() {
        return currentTPS;
    }

    // ... zbytek metod (getUsedMemoryMB, atd.) ...
    public long getUsedMemoryMB() {
        return usedMemory / 1024 / 1024;
    }

    public long getMaxMemoryMB() {
        return maxMemory / 1024 / 1024;
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public String getFormattedUptime() {
        long totalSeconds = getUptimeSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public double getMemoryUsagePercent() {
        return ((double) usedMemory / maxMemory) * 100.0;
    }

    public String getMemoryUsageFormatted() {
        return String.format("%.1f%%", getMemoryUsagePercent());
    }
}