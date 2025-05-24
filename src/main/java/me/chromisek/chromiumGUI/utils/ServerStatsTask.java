package me.chromisek.chromiumGUI.utils;

import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

public class ServerStatsTask extends BukkitRunnable {
    
    private double currentTPS;
    private long usedMemory;
    private long maxMemory;
    private final long startTime;
    private boolean sparkAvailable = false;
    
    public ServerStatsTask() {
        this.startTime = System.currentTimeMillis();

        // Check if Spark plugin is available
        Plugin sparkPlugin = Bukkit.getPluginManager().getPlugin("spark");
        sparkAvailable = (sparkPlugin != null && sparkPlugin.isEnabled());

        if (sparkAvailable) {
            ChromiumGUI.getInstance().getLogger().info("Spark plugin detected! Using Spark for TPS metrics.");
        } else {
            ChromiumGUI.getInstance().getLogger().warning("Spark plugin not found or not enabled. TPS metrics will be estimated.");
        }
        
        updateStats();
    }
    
    @Override
    public void run() {
        updateStats();

        Bukkit.getScheduler().runTask(ChromiumGUI.getInstance(), () -> {
            ChromiumGUI.getInstance().getGUIManager().refreshAllGUIs();
        });
    }
    
    private void updateStats() {
        // Get TPS information
        try {
            if (sparkAvailable) {
                // Get TPS from Spark plugin
                this.currentTPS = getSparkTPS();
            } else {
                // Fallback - try using server's native methods
                this.currentTPS = getNativeTPS();
            }
        } catch (Exception e) {
            // Fallback if any method fails
            ChromiumGUI.getInstance().getLogger().warning("Failed to get TPS: " + e.getMessage());
            this.currentTPS = 20.0;
        }
        
        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        this.maxMemory = runtime.maxMemory();
        this.usedMemory = runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Gets TPS from native server methods (Paper or Spigot)
     * @return The current TPS value (1-minute average)
     */
    private double getNativeTPS() {
        try {
            // Try Paper's method first
            try {
                return Math.min(20.0, Bukkit.getServer().getTPS()[0]);
            } catch (Exception e) {
                // Try Spigot's method
                try {
                    Object server = Bukkit.getServer();
                    Object spigotServer = server.getClass().getMethod("spigot").invoke(server);
                    double[] tps = (double[]) spigotServer.getClass().getMethod("getTPS").invoke(spigotServer);
                    return Math.min(20.0, tps[0]);
                } catch (Exception ex) {
                    ChromiumGUI.getInstance().getLogger().warning("Failed to get native TPS: " + ex.getMessage());
                    return 20.0;
                }
            }
        } catch (Exception e) {
            ChromiumGUI.getInstance().getLogger().warning("Failed to get native TPS: " + e.getMessage());
            return 20.0;
        }
    }

    /**
     * Gets TPS from Spark plugin using reflection
     * @return The current TPS value (1-minute average)
     */
    private double getSparkTPS() {
        try {
            // Use reflection to access Spark API without hard dependency
            Class<?> sparkClass = Class.forName("me.lucko.spark.api.Spark");

            // Get the Spark service from Bukkit services manager
            Object sparkApi = Bukkit.getServicesManager().getRegistration(sparkClass).getProvider();

            // Check if we got a valid API object
            if (sparkApi == null) {
                ChromiumGUI.getInstance().getLogger().warning("Could not get Spark API from ServicesManager");
                return getNativeTPS();
            }

            // First try to get TPS directly if available
            try {
                // Some versions have direct TPS method
                Object tpsData = sparkClass.getMethod("tps").invoke(sparkApi);
                if (tpsData != null) {
                    // Get the mean value from tpsData
                    double tpsMean = (double) tpsData.getClass().getMethod("mean").invoke(tpsData);
                    return Math.min(20.0, tpsMean);
                }
            } catch (Exception e) {
                // TPS method not available, try MSPT method
            }

            // Use MSPT to calculate TPS
            // Get the statistic window class
            Class<?> statisticWindowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$MillisPerTick");

            // Get the MSPT method
            Object msptStat = sparkClass.getMethod("mspt").invoke(sparkApi);
            if (msptStat == null) {
                ChromiumGUI.getInstance().getLogger().warning("Spark MSPT statistic not available");
                return getNativeTPS();
            }

            // Get the minutes constant for polling (MINUTES_1)
            Object minutesWindow = statisticWindowClass.getField("MINUTES_1").get(null);

            // Poll the statistic
            Object msptInfo = msptStat.getClass().getMethod("poll", Class.forName("me.lucko.spark.api.statistic.StatisticWindow")).invoke(msptStat, minutesWindow);

            if (msptInfo == null) {
                ChromiumGUI.getInstance().getLogger().warning("Could not poll MSPT data from Spark");
                return getNativeTPS();
            }

            // Get the mean value
            double msptMean = (double) msptInfo.getClass().getMethod("mean").invoke(msptInfo);

            // Convert MSPT to TPS (MSPT = Milliseconds Per Tick)
            // TPS = 1000 / MSPT (capped at 20)
            return Math.min(20.0, 1000.0 / Math.max(msptMean, 50.0));

        } catch (Exception e) {
            ChromiumGUI.getInstance().getLogger().warning("Failed to get TPS from Spark: " + e.getMessage());
            return getNativeTPS(); // Fall back to native methods
        }
    }
    
    public double getTPS() {
        return currentTPS;
    }
    
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