package me.chromisek.chromiumGUI;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ServerStatsTask extends BukkitRunnable {
    
    private double currentTPS;
    private long usedMemory;
    private long maxMemory;
    private long startTime;
    
    public ServerStatsTask() {
        this.startTime = System.currentTimeMillis();
        updateStats();
    }
    
    @Override
    public void run() {
        updateStats();
        
        // Refresh all open GUIs on main thread
        Bukkit.getScheduler().runTask(ChromiumGUI.getInstance(), () -> {
            ChromiumGUI.getInstance().getGUIManager().refreshAllGUIs();
        });
    }
    
    private void updateStats() {
        // Calculate TPS (simplified method)
        try {
            this.currentTPS = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        } catch (Exception e) {
            // Fallback if getTPS() is not available
            this.currentTPS = 20.0;
        }
        
        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        this.maxMemory = runtime.maxMemory();
        this.usedMemory = runtime.totalMemory() - runtime.freeMemory();
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