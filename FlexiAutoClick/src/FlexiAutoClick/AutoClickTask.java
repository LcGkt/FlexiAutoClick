package FlexiAutoClick;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class AutoClickTask {

    private final Main plugin;

    public AutoClickTask(Main plugin) {
        this.plugin = plugin;
    }

    public void iniciarTaskGlobal() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerCache cache : new ArrayList<>(plugin.autoclickAtivo.values())) {
                    Player p = cache.player;
                    if (p == null || !p.isOnline()) {
                        plugin.autoclickAtivo.remove(cache.player.getUniqueId());
                        continue;
                    }

                    if (cache.hitarea) hitarea(p, cache.range, cache.dano);
                    else hitdirect(p, cache.range, cache.dano);
                }
            }
        }.runTaskTimer(plugin, 0L, plugin.getConfig().getInt("ticksporclick"));
    }

    private void hitarea(Player p, int range, double danoBase) {
        Location loc = p.getLocation();

        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                if (loc.distanceSquared(e.getLocation()) <= range * range) {
                    LivingEntity target = (LivingEntity) e;
                    target.damage(calculateVanillaDano(p), p);
                }
            }
        }
    }

    private void hitdirect(Player p, int range, double danoBase) {
        Location loc = p.getLocation();

        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                if (loc.distanceSquared(e.getLocation()) <= range * range) {
                    LivingEntity target = (LivingEntity) e;
                    target.damage(calculateVanillaDano(p), p);
                    break;
                }
            }
        }
    }

    private double calculateVanillaDano(Player p) {
        ItemStack item = p.getInventory().getItemInHand(); // 1.8 usa getItemInHand()

        if (item == null || item.getType() == Material.AIR) {
            return 1.0;
        }

        Material type = item.getType();
        double baseDano;

        // Switch clássico do Java 7/1.8
        switch (type) {
            // Espadas
            case WOOD_SWORD: baseDano = 4.0; break;
            case STONE_SWORD: baseDano = 5.0; break;
            case IRON_SWORD: baseDano = 6.0; break;
            case DIAMOND_SWORD: baseDano = 7.0; break;
            case GOLD_SWORD: baseDano = 4.0; break;
            // Machados
            case WOOD_AXE: baseDano = 7.0; break;
            case STONE_AXE: baseDano = 9.0; break;
            case IRON_AXE: baseDano = 9.0; break;
            case DIAMOND_AXE: baseDano = 9.0; break;
            case GOLD_AXE: baseDano = 7.0; break;
            default: baseDano = 1.0; break;
        }

        int sharpnessLevel = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        baseDano += sharpnessLevel;

        return baseDano;
    }
}
