package com.volmit.adapt.util;

import com.volmit.adapt.Adapt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Routes work to the appropriate scheduler intent.
 */
public final class ThreadContext {
    private static final Runnable NOOP = () -> {
    };

    private ThreadContext() {
    }

    public static boolean isGlobalThread() {
        Object state = invokeStatic(Bukkit.class, "isGlobalTickThread");
        if (state instanceof Boolean b) {
            return b;
        }

        Object primary = invokeStatic(Bukkit.class, "isPrimaryThread");
        return primary instanceof Boolean b && b;
    }

    public static void entity(Entity entity, Runnable action) {
        if (entity == null) {
            global(action);
            return;
        }

        Object scheduler = invoke(entity, "getScheduler");
        if (scheduler != null && (tryEntityExecute(scheduler, action) || tryRunConsumer(scheduler, action))) {
            return;
        }

        global(action);
    }

    public static void region(Location location, Runnable action) {
        if (location == null) {
            global(action);
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            global(action);
            return;
        }

        Object regionScheduler = invoke(Bukkit.getServer(), "getRegionScheduler");
        if (regionScheduler != null) {
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;

            if (tryInvoke(regionScheduler, "execute", plugin(), world, chunkX, chunkZ, action)
                    || tryRunRegionConsumer(regionScheduler, world, chunkX, chunkZ, action)) {
                return;
            }
        }

        global(action);
    }

    public static void global(Runnable action) {
        Object globalScheduler = invoke(Bukkit.getServer(), "getGlobalRegionScheduler");
        if (globalScheduler != null && (tryInvoke(globalScheduler, "execute", plugin(), action) || tryRunConsumer(globalScheduler, action))) {
            return;
        }

        J.s(action);
    }

    public static void async(Runnable action) {
        J.a(action);
    }

    private static Plugin plugin() {
        return Adapt.instance;
    }

    private static boolean tryEntityExecute(Object scheduler, Runnable action) {
        return tryInvoke(scheduler, "execute", plugin(), action, NOOP, 0L)
                || tryInvoke(scheduler, "execute", plugin(), action, NOOP)
                || tryInvoke(scheduler, "execute", plugin(), action);
    }

    private static boolean tryRunConsumer(Object scheduler, Runnable action) {
        Consumer<Object> run = ignored -> action.run();
        return tryInvoke(scheduler, "run", plugin(), run, NOOP)
                || tryInvoke(scheduler, "run", plugin(), run);
    }

    private static boolean tryRunRegionConsumer(Object scheduler, World world, int chunkX, int chunkZ, Runnable action) {
        Consumer<Object> run = ignored -> action.run();
        return tryInvoke(scheduler, "run", plugin(), world, chunkX, chunkZ, run);
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            if (method.getParameterCount() != args.length) {
                continue;
            }

            if (!matches(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                return method.invoke(target, args);
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    private static Object invokeStatic(Class<?> type, String methodName, Object... args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            if (method.getParameterCount() != args.length) {
                continue;
            }

            if (!matches(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                return method.invoke(null, args);
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean tryInvoke(Object target, String methodName, Object... args) {
        if (target == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            if (method.getParameterCount() != args.length) {
                continue;
            }

            if (!matches(method.getParameterTypes(), args)) {
                continue;
            }

            try {
                method.invoke(target, args);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        return false;
    }

    private static boolean matches(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }

            Class<?> expected = wrap(parameterTypes[i]);
            if (!expected.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }

        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
