package net.curxxed.dev.wintercore.commands.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define the properties of a command class.
 * This allows for command registration without using the plugin.yml.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {

    /**
     * The primary name of the command (e.g., "fly").
     * @return The command name.
     */
    String name();

    /**
     * An array of alternative names for the command (e.g., ["flight", "f"]).
     * @return The command aliases.
     */
    String[] aliases() default {};

    /**
     * The permission node required to execute this command.
     * @return The permission node.
     */
    String[] permission() default {};

    /**
     * A brief description of what the command does.
     * @return The command description.
     */
    String description() default "";

    /**
     * The proper usage syntax for the command (e.g., "/fly [player]").
     * @return The command usage message.
     */
    String usage() default "";

    /**
     * If set to true, the command's execute method will be run on an asynchronous thread.
     * This is useful for commands that perform database lookups or other long-running tasks.
     * @return True if the command should be run asynchronously.
     */
    boolean async() default false;

    boolean inGameOnly() default false;
}
