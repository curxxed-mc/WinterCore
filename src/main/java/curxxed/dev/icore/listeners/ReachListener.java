package curxxed.dev.icore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class ReachListener {

    private static final double CUSTOM_REACH_DISTANCE = 25.0;

    public void applyReach(Player player, Player target) {
        // Get the player's eye location and direction
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // Check if the target is within the player's line of sight
        Location targetLocation = target.getLocation();
        Vector toTarget = targetLocation.toVector().subtract(eyeLocation.toVector());
        if (toTarget.normalize().dot(direction) > 0.99) { // Adjust threshold as needed
            double distance = eyeLocation.distance(targetLocation);

            // Simulate damage calculation
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(player, target, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK, 0);
            Bukkit.getPluginManager().callEvent(damageEvent);

            if (!damageEvent.isCancelled()) {
                double damage = damageEvent.getDamage(); // Get the calculated damage
                target.damage(damage, player); // Apply the calculated damage
                player.sendMessage(ChatColor.GREEN + "You hit " + target.getName() + " from " + distance + " blocks away for " + damage + " damage!");
            }
        } else {
            player.sendMessage(ChatColor.RED + target.getName() + " is not in your line of sight.");
        }
    }
}