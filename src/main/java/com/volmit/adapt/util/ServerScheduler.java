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

public interface ServerScheduler {
    CancellableTask runGlobal(Runnable task);

    CancellableTask runGlobalDelayed(Runnable task, long delayTicks);

    CancellableTask runGlobalRepeating(Runnable task, long delayTicks, long intervalTicks);

    CancellableTask runAsync(Runnable task);

    CancellableTask runAsyncDelayed(Runnable task, long delayTicks);

    CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long intervalTicks);

    CancellableTask runEntity(Entity entity, Runnable task);

    CancellableTask runEntityDelayed(Entity entity, Runnable task, long delayTicks);

    CancellableTask runEntityRepeating(Entity entity, Runnable task, long delayTicks, long intervalTicks);

    CancellableTask runRegion(Location location, Runnable task);

    CancellableTask runRegionDelayed(Location location, Runnable task, long delayTicks);

    CancellableTask runRegionRepeating(Location location, Runnable task, long delayTicks, long intervalTicks);

    boolean isFolia();

    static ServerScheduler create(Plugin plugin) {
        if (FoliaSchedulerAdapter.isSupported()) {
            return new FoliaSchedulerAdapter(plugin);
        }

        return new BukkitSchedulerAdapter(plugin, Bukkit.getScheduler());
    }
}
