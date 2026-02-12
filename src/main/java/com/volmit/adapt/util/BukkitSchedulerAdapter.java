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

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulerAdapter implements ServerScheduler {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitSchedulerAdapter(Plugin plugin, BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public CancellableTask runGlobal(Runnable task) {
        return wrap(scheduler.runTask(plugin, task));
    }

    @Override
    public CancellableTask runGlobalDelayed(Runnable task, long delayTicks) {
        return wrap(scheduler.runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public CancellableTask runGlobalRepeating(Runnable task, long delayTicks, long intervalTicks) {
        return wrap(scheduler.runTaskTimer(plugin, task, delayTicks, intervalTicks));
    }

    @Override
    public CancellableTask runAsync(Runnable task) {
        return wrap(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public CancellableTask runAsyncDelayed(Runnable task, long delayTicks) {
        return wrap(scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks));
    }

    @Override
    public CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long intervalTicks) {
        return wrap(scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, intervalTicks));
    }

    @Override
    public CancellableTask runEntity(Entity entity, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public CancellableTask runEntityDelayed(Entity entity, Runnable task, long delayTicks) {
        return runGlobalDelayed(task, delayTicks);
    }

    @Override
    public CancellableTask runEntityRepeating(Entity entity, Runnable task, long delayTicks, long intervalTicks) {
        return runGlobalRepeating(task, delayTicks, intervalTicks);
    }

    @Override
    public CancellableTask runRegion(Location location, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public CancellableTask runRegionDelayed(Location location, Runnable task, long delayTicks) {
        return runGlobalDelayed(task, delayTicks);
    }

    @Override
    public CancellableTask runRegionRepeating(Location location, Runnable task, long delayTicks, long intervalTicks) {
        return runGlobalRepeating(task, delayTicks, intervalTicks);
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private static CancellableTask wrap(BukkitTask task) {
        return task::cancel;
    }
}
