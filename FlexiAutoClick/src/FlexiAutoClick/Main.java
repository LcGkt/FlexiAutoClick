package FlexiAutoClick;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {

    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    private Map<UUID, PlayerCache> autoclickAtivo = new HashMap<>();
    private String versaoServidor;

    @Override
    public void onEnable() {
        versaoServidor = Bukkit.getBukkitVersion().split("-")[0]; // ex: 1.8.8
        carregarConfigs();
        getLogger().info("FlexiAutoClick habilitado! Servidor rodando: " + versaoServidor);
        verificarAtualizacao();
        iniciarTaskGlobal();
    }

    @Override
    public void onDisable() {
        autoclickAtivo.clear();
        getLogger().info("FlexiAutoClick desabilitado!");
    }

    private void carregarConfigs() {
        saveDefaultConfig();
        config = getConfig();

        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void salvarMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String msg(String path) {
        return messages.getString(path, "Mensagem não encontrada: " + path).replace("&", "§");
    }

    // Tocar sons compatíveis de 1.8 até 1.21
    private void tocarSom(Player p, String som) {
        try {
            if (som == null || som.isEmpty()) return;
            Sound s;

            if (versaoServidor.startsWith("1.8") || versaoServidor.startsWith("1.9") ||
                versaoServidor.startsWith("1.10") || versaoServidor.startsWith("1.11") ||
                versaoServidor.startsWith("1.12")) {

                // Mapear sons novos para sons 1.8 compatíveis
                switch (som.toUpperCase()) {
                    case "ENTITY_PLAYER_ATTACK_STRONG":
                    case "ENTITY_PLAYER_ATTACK_KNOCKBACK":
                    case "ENTITY_PLAYER_ATTACK_CRIT":
                    case "ENTITY_PLAYER_ATTACK_SWEEP":
                        s = Sound.ORB_PICKUP;
                        break;
                    case "CLICK":
                        s = Sound.CLICK;
                        break;
                    default:
                        s = Sound.valueOf(som.toUpperCase());
                }
            } else {
                s = Sound.valueOf(som.toUpperCase());
            }

            p.playSound(p.getLocation(), s, 1f, 1f);
        } catch (Exception e) {
            getLogger().warning("Som inválido ou não compatível no config.yml: " + som);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("Mensagens.apenas-jogadores"));
            return true;
        }

        Player p = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("autoclick.admin")) {
                p.sendMessage(msg("Mensagens.sem-permissao"));
                return true;
            }

            reloadConfig();
            config = getConfig();
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            salvarMessages();
            p.sendMessage(msg("Mensagens.reload"));
            return true;
        }

        if (!p.hasPermission("autoclick.use")) {
            p.sendMessage(msg("Mensagens.sem-permissao"));
            return true;
        }

        if (autoclickAtivo.containsKey(p.getUniqueId())) {
            desativarAutoClick(p);
        } else {
            ativarAutoClick(p);
        }

        return true;
    }

    private void ativarAutoClick(Player p) {
        List<String> mundosPermitidos = config.getStringList("worlds");
        String mundoAtual = p.getWorld().getName().toLowerCase();
        boolean mundoPermitido = mundosPermitidos.stream().anyMatch(w -> w.equalsIgnoreCase(mundoAtual));

        if (!mundoPermitido) {
            p.sendMessage(msg("Mensagens.mundo-bloqueado").replace("{world}", p.getWorld().getName()));
            tocarSom(p, config.getString("som-desativado"));
            return;
        }

        PlayerCache cache = new PlayerCache(
                p,
                config.getInt("range"),
                config.getDouble("dano"),
                config.getBoolean("hitarea")
        );
        autoclickAtivo.put(p.getUniqueId(), cache);

        p.sendMessage(msg("Mensagens.ativado"));
        tocarSom(p, config.getString("som-ativado"));
    }

    private void desativarAutoClick(Player p) {
        autoclickAtivo.remove(p.getUniqueId());
        p.sendMessage(msg("Mensagens.desativado"));
        tocarSom(p, config.getString("som-desativado"));
    }

    private void iniciarTaskGlobal() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerCache cache : new ArrayList<>(autoclickAtivo.values())) {
                    Player p = cache.player;
                    if (p == null || !p.isOnline()) {
                        autoclickAtivo.remove(cache.player.getUniqueId());
                        continue;
                    }

                    if (cache.hitarea) {
                        hitarea(p, cache.range, cache.dano);
                    } else {
                        hitdirect(p, cache.range, cache.dano);
                    }
                }
            }
        }.runTaskTimer(this, 0L, config.getInt("ticksporclick"));
    }

    private void hitarea(Player p, int range, double dano) {
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                ((LivingEntity) e).damage(dano, p);
            }
        }
    }

    private void hitdirect(Player p, int range, double dano) {
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                ((LivingEntity) e).damage(dano, p);
                break;
            }
        }
    }

    private void verificarAtualizacao() {
        if (!config.getBoolean("update", true)) return;

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String currentVersion = getDescription().getVersion();
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=128065");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latestVersion = reader.readLine().trim();
                    if (!latestVersion.equals(currentVersion)) {
                        getLogger().warning("===============================================");
                        getLogger().warning("Existe uma nova versão do FlexiAutoClick!");
                        getLogger().warning("Sua versão: " + currentVersion + " | Nova versão: " + latestVersion);
                        getLogger().warning("Baixe em: https://www.spigotmc.org/resources/flexiautoclick.128065/");
                        getLogger().warning("===============================================");
                    } else {
                        getLogger().info("Você está utilizando a versão mais recente do FlexiAutoClick.");
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Não foi possível verificar atualizações: " + e.getMessage());
            }
        });
    }

    private static class PlayerCache {
        Player player;
        int range;
        double dano;
        boolean hitarea;

        PlayerCache(Player player, int range, double dano, boolean hitarea) {
            this.player = player;
            this.range = range;
            this.dano = dano;
            this.hitarea = hitarea;
        }
    }
}
