package com.mongodb.diagnostics.logging;

import org.jetbrains.annotations.NotNull;

public final class Loggers {

    public static Logger getLogger(@NotNull String suffix) {
        return new SilentLogger();
    }

    private static class SilentLogger implements Logger {
        @Override public String getName() { return "silent"; }

        @Override public boolean isTraceEnabled() { return false; }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isInfoEnabled() { return false; }
        @Override public boolean isWarnEnabled() { return false; }
        @Override public boolean isErrorEnabled() { return false; }
        @Override public void trace(String msg) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public void debug(String msg) {}
        @Override public void debug(String msg, Throwable t) {}
        @Override public void info(String msg) {}
        @Override public void info(String msg, Throwable t) {}
        @Override public void warn(String msg) {}
        @Override public void warn(String msg, Throwable t) {}
        @Override public void error(String msg) {}
        @Override public void error(String msg, Throwable t) {}
    }
}