/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class FoliaSchedulerAdapter implements ServerScheduler {
    private static final String GLOBAL_REGION_SCHEDULER = "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler";
    private static final String REGION_SCHEDULER = "io.papermc.paper.threadedregions.scheduler.RegionScheduler";
    private static final String ENTITY_SCHEDULER = "io.papermc.paper.threadedregions.scheduler.EntityScheduler";

    private final Plugin plugin;
    private final BukkitScheduler bukkitScheduler;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.bukkitScheduler = Bukkit.getScheduler();
    }

    public static boolean isSupported() {
        try {
            Class.forName(GLOBAL_REGION_SCHEDULER);
            Class.forName(REGION_SCHEDULER);
            Class.forName(ENTITY_SCHEDULER);
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            Bukkit.class.getMethod("getRegionScheduler");
            Entity.class.getMethod("getScheduler");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public CancellableTask runGlobal(Runnable task) {
        return invokeGlobal("run", task, 0L, 0L, false);
    }

    @Override
    public CancellableTask runGlobalDelayed(Runnable task, long delayTicks) {
        return invokeGlobal("runDelayed", task, delayTicks, 0L, false);
    }

    @Override
    public CancellableTask runGlobalRepeating(Runnable task, long delayTicks, long intervalTicks) {
        return invokeGlobal("runAtFixedRate", task, delayTicks, intervalTicks, true);
    }

    @Override
    public CancellableTask runAsync(Runnable task) {
        return taskHandle(bukkitScheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public CancellableTask runAsyncDelayed(Runnable task, long delayTicks) {
        return taskHandle(bukkitScheduler.runTaskLaterAsynchronously(plugin, task, delayTicks));
    }

    @Override
    public CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long intervalTicks) {
        return taskHandle(bukkitScheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, intervalTicks));
    }

    @Override
    public CancellableTask runEntity(Entity entity, Runnable task) {
        return invokeEntity(entity, "run", task, 0L, 0L, false);
    }

    @Override
    public CancellableTask runEntityDelayed(Entity entity, Runnable task, long delayTicks) {
        return invokeEntity(entity, "runDelayed", task, delayTicks, 0L, false);
    }

    @Override
    public CancellableTask runEntityRepeating(Entity entity, Runnable task, long delayTicks, long intervalTicks) {
        return invokeEntity(entity, "runAtFixedRate", task, delayTicks, intervalTicks, true);
    }

    @Override
    public CancellableTask runRegion(Location location, Runnable task) {
        return invokeRegion(location, "run", task, 0L, 0L, false);
    }

    @Override
    public CancellableTask runRegionDelayed(Location location, Runnable task, long delayTicks) {
        return invokeRegion(location, "runDelayed", task, delayTicks, 0L, false);
    }

    @Override
    public CancellableTask runRegionRepeating(Location location, Runnable task, long delayTicks, long intervalTicks) {
        return invokeRegion(location, "runAtFixedRate", task, delayTicks, intervalTicks, true);
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private CancellableTask invokeGlobal(String method, Runnable task, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Consumer<Object> consumer = ignored -> task.run();

            Object scheduledTask;
            if (repeating) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class, long.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer, delay, period);
            } else if (delay > 0) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer, delay);
            } else {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer);
            }

            return foliaTaskHandle(scheduledTask);
        } catch (Throwable ignored) {
            return fallbackSync(task, delay, period, repeating);
        }
    }

    private CancellableTask invokeEntity(Entity entity, String method, Runnable task, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Entity.class.getMethod("getScheduler").invoke(entity);
            Consumer<Object> consumer = ignored -> task.run();
            Runnable retired = () -> {
            };

            Object scheduledTask;
            if (repeating) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer, retired, delay, period);
            } else if (delay > 0) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class, Runnable.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer, retired, delay);
            } else {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Consumer.class, Runnable.class);
                scheduledTask = m.invoke(scheduler, plugin, consumer, retired);
            }

            return foliaTaskHandle(scheduledTask);
        } catch (Throwable ignored) {
            return fallbackSync(task, delay, period, repeating);
        }
    }

    private CancellableTask invokeRegion(Location location, String method, Runnable task, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Consumer<Object> consumer = ignored -> task.run();

            Object scheduledTask;
            if (repeating) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Location.class, Consumer.class, long.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, location, consumer, delay, period);
            } else if (delay > 0) {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Location.class, Consumer.class, long.class);
                scheduledTask = m.invoke(scheduler, plugin, location, consumer, delay);
            } else {
                Method m = scheduler.getClass().getMethod(method, Plugin.class, Location.class, Consumer.class);
                scheduledTask = m.invoke(scheduler, plugin, location, consumer);
            }

            return foliaTaskHandle(scheduledTask);
        } catch (Throwable ignored) {
            return fallbackSync(task, delay, period, repeating);
        }
    }

    private CancellableTask fallbackSync(Runnable task, long delay, long period, boolean repeating) {
        if (repeating) {
            return taskHandle(bukkitScheduler.runTaskTimer(plugin, task, delay, period));
        }

        if (delay > 0) {
            return taskHandle(bukkitScheduler.runTaskLater(plugin, task, delay));
        }

        return taskHandle(bukkitScheduler.runTask(plugin, task));
    }

    private static CancellableTask foliaTaskHandle(Object scheduledTask) {
        if (scheduledTask == null) {
            return () -> {
            };
        }

        return () -> {
            try {
                scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
            } catch (Throwable ignored) {
            }
        };
    }

    private static CancellableTask taskHandle(org.bukkit.scheduler.BukkitTask task) {
        return task::cancel;
    }
}
