package net.curxxed.dev.wintercore.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

public final class Tasks {

    private final Plugin plugin;

    public Tasks(Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask sync(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, guarded(task));
    }

    public BukkitTask async(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, guarded(task));
    }

    public BukkitTask later(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, guarded(task), delayTicks);
    }

    public BukkitTask laterAsync(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, guarded(task), delayTicks);
    }

    public BukkitTask timer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, guarded(task), delayTicks, periodTicks);
    }

    public BukkitTask timerAsync(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, guarded(task), delayTicks, periodTicks);
    }

    public <T> Future<T> callSync(Callable<T> task) {
        return Bukkit.getScheduler().callSyncMethod(plugin, task);
    }

    public void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private Runnable guarded(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Scheduled task failed", throwable);
            }
        };
    }
}
