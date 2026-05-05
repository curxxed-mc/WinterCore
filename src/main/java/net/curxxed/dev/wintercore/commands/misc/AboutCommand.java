package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

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
            description = "The core plugin for your server network.";
        }

        sender.sendMessage(CC.translate("&7&m----------------------------------------------------"));
        sender.sendMessage(CC.translate(" &b&l" + desc.getName() + " &fv" + desc.getVersion()));
        sender.sendMessage(CC.translate("  &7Authors: &f" + String.join(", ", desc.getAuthors())));
        sender.sendMessage(CC.translate("  &7Description: &f" + description));
        sender.sendMessage(CC.translate("&7&m----------------------------------------------------"));
    }
}

