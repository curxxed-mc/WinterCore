package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Arrays;

@CommandInfo(
        name = "about",
        aliases = {"wintercore", "ver", "version"},
        description = "Displays information about the WinterCore plugin.",
        usage = "/about",
        permission = {"wintercore.command.about"}
)
public class AboutCommand extends BaseCommand {

    public AboutCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        PluginDescriptionFile desc = plugin.getDescription();

        String description = desc.getDescription();

        // Use a fallback description if the plugin.yml is missing one.
        if (description == null || description.isEmpty()) {
            description = msg("about.default-description", "The core plugin for your server network.");
        }

        sendList(sender, "about.info", Arrays.asList(
                "&7&m----------------------------------------------------",
                " &b&l{name} &fv{version}",
                "  &7Author: &f{authors}",
                "  &7Description: &f{description}",
                "&7&m----------------------------------------------------"
        ), "{name}", desc.getName(),
                "{version}", desc.getVersion(),
                "{authors}", String.join(", ", desc.getAuthors()),
                "{description}", description);
    }
}

