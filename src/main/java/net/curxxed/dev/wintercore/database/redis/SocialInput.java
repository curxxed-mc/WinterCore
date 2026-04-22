package net.curxxed.dev.wintercore.database.redis;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SocialInput implements Listener {

    private final WinterCore plugin;
    private final RedisSocials socials;

    private static final Map<UUID, SocialPrompt> awaiting = new ConcurrentHashMap<>();
    private static final Map<String, Validator> VALIDATORS = new ConcurrentHashMap<>();

    public SocialInput(WinterCore plugin, RedisSocials socials) {
        this.plugin = plugin;
        this.socials = socials;

        registerValidators();
    }

    public boolean waitForInput(Player player, String platform) {
        UUID uuid = player.getUniqueId();

        if (awaiting.containsKey(uuid)) {
            SocialPrompt current = awaiting.get(uuid);
            player.sendMessage(CC.translate("&cYou already have an active input pending. Please type your "
                    + current.platform + " info first."));
            return false;
        }

        awaiting.put(uuid, new SocialPrompt(platform, socials));
        player.sendMessage(CC.translate("&eType your &b" + platform + " &einfo in chat:"));
        return true;
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!awaiting.containsKey(uuid)) return;

        final SocialPrompt prompt = awaiting.remove(uuid);
        final String rawInput = event.getMessage();
        event.setCancelled(true);

        final String input = normalize(rawInput);

        Validator validator = VALIDATORS.get(prompt.platform);

        if (validator == null || !validator.validate(input)) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(CC.translate("&cInvalid " + prompt.platform + " link.")));
            return;
        }
        prompt.socials.setSocialLink(uuid, prompt.platform, input);

        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(CC.translate("&aUpdated your " + prompt.platform + " link to: &f" + input)));
    }

    private void registerValidators() {

        VALIDATORS.put("discord", input -> input.matches("^[a-zA-Z0-9._]{2,32}$") // username
                || input.contains("discord.gg/")
                || input.contains("discord.com/invite/"));

        VALIDATORS.put("youtube", input -> input.contains("youtube.com/@"));

        VALIDATORS.put("twitter", input -> input.startsWith("twitter.com")
                || input.startsWith("x.com")
                || input.contains("twitter.com/")
                || input.contains("x.com/"));
    }

    private interface Validator {
        boolean validate(String input);
    }

    private String normalize(String input) {
        return input
                .trim()
                .replace("https://", "")
                .replace("http://", "")
                .replaceAll("/+$", ""); // remove trailing slashes
    }

    private static final class SocialPrompt {
        private final String platform;
        private final RedisSocials socials;

        private SocialPrompt(String platform, RedisSocials socials) {
            this.platform = platform.toLowerCase();
            this.socials = socials;
        }
    }
}
