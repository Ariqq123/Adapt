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

import com.volmit.adapt.Adapt;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class J {
    private static List<Runnable> afterStartup = new ArrayList<>();
    private static List<Runnable> afterStartupAsync = new ArrayList<>();
    private static boolean started = false;

    private static final AtomicInteger LEGACY_TASK_ID = new AtomicInteger(1);
    private static final Map<Integer, CancellableTask> LEGACY_TASKS = new ConcurrentHashMap<>();
    private static volatile ServerScheduler scheduler;

    public static void dofor(int a, Function<Integer, Boolean> c, int ch, Consumer<Integer> d) {
        for (int i = a; c.apply(i); i += ch) {
            c.apply(i);
        }
    }

    public static boolean doif(Supplier<Boolean> c, Runnable g) {
        if (c.get()) {
            g.run();
            return true;
        }

        return false;
    }

    public static void a(Runnable a) {
        MultiBurst.burst.lazy(a);
    }

    public static <T> Future<T> a(Callable<T> a) {
        return MultiBurst.burst.getService().submit(a);
    }

    public static void attemptAsync(NastyRunnable r) {
        J.a(() -> J.attempt(r));
    }

    public static <R> R attemptResult(NastyFuture<R> r, R onError) {
        try {
            return r.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return onError;
    }

    public static <T, R> R attemptFunction(NastyFunction<T, R> r, T param, R onError) {
        try {
            return r.run(param);
        } catch (Throwable e) {
            Adapt.verbose("Failed to run function: " + e.getMessage());
        }

        return onError;
    }

    public static boolean sleep(long ms) {
        return J.attempt(() -> Thread.sleep(ms));
    }

    public static boolean attempt(NastyRunnable r) {
        return attemptCatch(r) == null;
    }

    public static Throwable attemptCatch(NastyRunnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            return e;
        }

        return null;
    }

    public static <T> T attempt(Supplier<T> t, T i) {
        try {
            return t.get();
        } catch (Throwable e) {
            return i;
        }
    }

    /**
     * Dont call this unless you know what you are doing!
     */
    public static void executeAfterStartupQueue() {
        if (started) {
            return;
        }

        started = true;

        for (Runnable r : afterStartup) {
            s(r);
        }

        for (Runnable r : afterStartupAsync) {
            a(r);
        }

        afterStartup = null;
        afterStartupAsync = null;
    }

    /**
     * Schedule a sync task to be run right after startup. If the server has already
     * started ticking, it will simply run it in a sync task.
     */
    public static void ass(Runnable r) {
        if (started) {
            s(r);
        } else {
            afterStartup.add(r);
        }
    }

    /**
     * Schedule an async task to be run right after startup. If the server has
     * already started ticking, it will simply run it in an async task.
     */
    public static void asa(Runnable r) {
        if (started) {
            a(r);
        } else {
            afterStartupAsync.add(r);
        }
    }

    public static CancellableTask sh(Runnable r) {
        return scheduler().runGlobal(r);
    }

    public static CancellableTask sh(Runnable r, long delayTicks) {
        return scheduler().runGlobalDelayed(r, delayTicks);
    }

    public static CancellableTask srh(Runnable r, long intervalTicks) {
        return scheduler().runGlobalRepeating(r, 0, intervalTicks);
    }

    public static CancellableTask ah(Runnable r) {
        return scheduler().runAsync(r);
    }

    public static CancellableTask ah(Runnable r, long delayTicks) {
        return scheduler().runAsyncDelayed(r, delayTicks);
    }

    public static CancellableTask arh(Runnable r, long intervalTicks) {
        return scheduler().runAsyncRepeating(r, 0, intervalTicks);
    }

    public static CancellableTask eh(Entity entity, Runnable task) {
        return scheduler().runEntity(entity, task);
    }

    public static CancellableTask eh(Entity entity, Runnable task, long delayTicks) {
        return scheduler().runEntityDelayed(entity, task, delayTicks);
    }

    public static CancellableTask erh(Entity entity, Runnable task, long intervalTicks) {
        return scheduler().runEntityRepeating(entity, task, 0, intervalTicks);
    }

    public static CancellableTask rh(Location location, Runnable task) {
        return scheduler().runRegion(location, task);
    }

    public static CancellableTask rh(Location location, Runnable task, long delayTicks) {
        return scheduler().runRegionDelayed(location, task, delayTicks);
    }

    public static CancellableTask rrh(Location location, Runnable task, long intervalTicks) {
        return scheduler().runRegionRepeating(location, task, 0, intervalTicks);
    }

    /**
     * Queue a sync task
     */
    public static void s(Runnable r) {
        sh(r);
    }

    /**
     * Queue a sync task
     */
    public static void s(Runnable r, int delay) {
        sh(r, delay);
    }

    /**
     * Cancel a sync repeating task
     */
    public static void csr(int id) {
        cancelLegacyTask(id);
    }

    /**
     * Start a sync repeating task
     */
    public static int sr(Runnable r, int interval) {
        return registerLegacyTask(srh(r, interval));
    }

    /**
     * Start a sync repeating task for a limited amount of ticks
     */
    public static void sr(Runnable r, int interval, int intervals) {
        FinalInteger fi = new FinalInteger(0);

        new SR() {
            @Override
            public void run() {
                fi.add(1);
                r.run();

                if (fi.get() >= intervals) {
                    cancel();
                }
            }
        };
    }

    /**
     * Call an async task delayed
     */
    public static void a(Runnable r, int delay) {
        ah(r, delay);
    }

    /**
     * Cancel an async repeat task
     */
    public static void car(int id) {
        cancelLegacyTask(id);
    }

    /**
     * Start an async repeat task
     */
    public static int ar(Runnable r, int interval) {
        return registerLegacyTask(arh(r, interval));
    }

    /**
     * Start an async repeating task for a limited time
     */
    public static void ar(Runnable r, int interval, int intervals) {
        FinalInteger fi = new FinalInteger(0);

        new AR() {
            @Override
            public void run() {
                fi.add(1);
                r.run();

                if (fi.get() >= intervals) {
                    cancel();
                }
            }
        };
    }

    private static int registerLegacyTask(CancellableTask task) {
        int id = LEGACY_TASK_ID.getAndIncrement();
        LEGACY_TASKS.put(id, task);
        return id;
    }

    private static void cancelLegacyTask(int id) {
        CancellableTask task = LEGACY_TASKS.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private static ServerScheduler scheduler() {
        ServerScheduler current = scheduler;
        if (current != null) {
            return current;
        }

        synchronized (J.class) {
            if (scheduler == null) {
                scheduler = ServerScheduler.create(Adapt.instance);
            }

            return scheduler;
        }
    }
}
