package io.reactivex.netty.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ExamplesEnvironment {

    private static final ConcurrentHashMap<Class, ExamplesEnvironment> envs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class, ExamplesEnvironment> overrideEnvs = new ConcurrentHashMap<>();

    private volatile SocketAddress serverAddress;
    private final Logger logger;
    private Class<?> exampleClass;

    public ExamplesEnvironment(Class<?> exampleClass, Logger logger) {
        this.exampleClass = exampleClass;
        this.logger = logger;
    }

    private ExamplesEnvironment(Class<?> exampleClass) {
        this(exampleClass, LoggerFactory.getLogger(exampleClass));
    }

    public Logger getLogger() {
        return logger;
    }

    public void registerServerAddress(SocketAddress server) {
        serverAddress = server;
    }

    public SocketAddress getServerAddress(Class<?> serverClass, String[] args) {
        String host = "127.0.0.1";

        if(null != args && args.length > 0) {
            String portAsStr = args[0];
            try {
                int port = Integer.parseInt(portAsStr);
                if (args.length > 1) {
                    host = args[1];
                }

                return new InetSocketAddress(host, port);
            } catch (NumberFormatException e) {
                printUsageAndExit();
            }
        }

        invokeExample(serverClass, new String[] {"false"});

        ExamplesEnvironment serverEnv = getRegisteredEnvironment(serverClass);

        if (null == serverEnv) {
            throw new RuntimeException("No environment registered for the server: " + serverClass.getName());
        } else if (null == serverEnv.serverAddress) {
            throw new RuntimeException("Failed to start the server: " + serverClass.getName());
        }

        return serverEnv.serverAddress;
    }

    public static ExamplesEnvironment newEnvironment(Class<?> exampleClass) {
        ExamplesEnvironment override = overrideEnvs.get(exampleClass);
        if (null != override) {
            return override;
        }
        ExamplesEnvironment env = new ExamplesEnvironment(exampleClass);
        envs.put(exampleClass, env);
        return env;
    }

    public static void overrideEnvironment(Class<?> exampleClass, ExamplesEnvironment examplesEnvironment) {
        overrideEnvs.put(exampleClass, examplesEnvironment);
    }

    public static ExamplesEnvironment getRegisteredEnvironment(Class<?> exampleClass) {
        return envs.get(exampleClass);
    }

    public static void invokeExample(Class<?> exampleClass, String[] params) {
        try {
            Method method = exampleClass.getMethod("main", String[].class);
            method.invoke(null, (Object) params);
        } catch (IllegalAccessException e) {
            System.err.println("Failed to invoke main method on the example.");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Failed to get the main method on the example. ");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Failed to invoke main method on the example. ");
            e.printStackTrace();
        }
    }

    public boolean shouldWaitForShutdown(String[] args) {
        return args.length == 0;
    }

    private void printUsageAndExit() {
        System.out.println("Usage: java " + exampleClass.getName() + " [<server port> [<server host>]]");
        System.exit(-1);
    }
}
