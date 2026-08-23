package net.curxxed.dev.wintercore.database.service;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class CurrencyService {

    private static final String BALANCE_FIELD = "currency";

    private final WinterCore plugin;
    private final ProfileRepository profiles;

    public CurrencyService(WinterCore plugin, ProfileRepository profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public long getBalanceSync(UUID uuid) {
        return Math.max(0L, profiles.getLong(uuid, BALANCE_FIELD, 0L));
    }

    public void getBalance(UUID uuid, Consumer<Long> callback) {
        plugin.getTasks().async(() -> {
            long balance = safeBalance(uuid);
            if (callback != null) {
                plugin.getTasks().sync(() -> callback.accept(balance));
            }
        });
    }

    public void setBalance(UUID uuid, long balance, Consumer<Long> callback) {
        long normalized = Math.max(0L, balance);
        plugin.getTasks().async(() -> {
            try {
                profiles.upsertField(uuid, BALANCE_FIELD, normalized);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not set currency balance for " + uuid, e);
            }

            if (callback != null) {
                plugin.getTasks().sync(() -> callback.accept(safeBalance(uuid)));
            }
        });
    }

    public void deposit(UUID uuid, long amount, Consumer<Long> callback) {
        if (amount <= 0L) {
            getBalance(uuid, callback);
            return;
        }

        plugin.getTasks().async(() -> {
            try {
                profiles.inc(uuid, BALANCE_FIELD, amount);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not deposit currency for " + uuid, e);
            }

            if (callback != null) {
                plugin.getTasks().sync(() -> callback.accept(safeBalance(uuid)));
            }
        });
    }

    public void withdraw(UUID uuid, long amount, Consumer<Boolean> callback) {
        if (amount <= 0L) {
            if (callback != null) {
                plugin.getTasks().sync(() -> callback.accept(true));
            }
            return;
        }

        plugin.getTasks().async(() -> {
            boolean success = false;
            try {
                success = profiles.decrementIfEnough(uuid, BALANCE_FIELD, amount);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not withdraw currency for " + uuid, e);
            }

            boolean result = success;
            if (callback != null) {
                plugin.getTasks().sync(() -> callback.accept(result));
            }
        });
    }

    public void transfer(UUID from, UUID to, long amount, Consumer<Boolean> callback) {
        if (from == null || to == null || amount <= 0L) {
            complete(callback, false);
            return;
        }
        if (from.equals(to)) {
            complete(callback, true);
            return;
        }

        plugin.getTasks().async(() -> {
            boolean success = false;
            boolean debited = false;
            try {
                debited = profiles.decrementIfEnough(from, BALANCE_FIELD, amount);
                if (debited) {
                    profiles.inc(to, BALANCE_FIELD, amount);
                    success = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not transfer currency from " + from + " to " + to, e);
                if (debited) {
                    try {
                        profiles.inc(from, BALANCE_FIELD, amount);
                    } catch (Exception rollbackFailure) {
                        e.addSuppressed(rollbackFailure);
                        plugin.getLogger().log(Level.SEVERE, "Could not restore currency to " + from, rollbackFailure);
                    }
                }
            }

            boolean result = success;
            complete(callback, result);
        });
    }

    private void complete(Consumer<Boolean> callback, boolean result) {
        if (callback != null) {
            plugin.getTasks().sync(() -> callback.accept(result));
        }
    }

    private long safeBalance(UUID uuid) {
        try {
            return getBalanceSync(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch currency balance for " + uuid, e);
            return 0L;
        }
    }
}
