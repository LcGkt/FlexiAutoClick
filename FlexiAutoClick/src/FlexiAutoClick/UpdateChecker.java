package FlexiAutoClick;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final Main plugin;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
    }

    public void iniciarVerificacaoPeriodica() {
        int intervaloMinutos = plugin.getConfig().getInt("check-update-minutes", 30);
        long intervaloTicks = intervaloMinutos * 60L * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                verificarAtualizacao();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, intervaloTicks);
    }

    private void verificarAtualizacao() {
        if (!plugin.getConfig().getBoolean("update", true)) return;

        try {
            String currentVersion = plugin.getDescription().getVersion().trim();
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=128065");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String latestVersion = reader.readLine().trim();
                if (isNewerVersion(latestVersion, currentVersion)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.hasPermission("autoclick.admin")) {
                                p.sendMessage("§c[FlexiAutoClick] §eNova versão disponível!");
                                p.sendMessage("§eSua versão: §f" + currentVersion + " §e| Nova versão: §f" + latestVersion);
                                p.sendMessage("§eBaixe em: §fhttps://www.spigotmc.org/resources/flexiautoclick.128065/");
                                plugin.configManager.tocarSom(p, "LEVEL_UP");
                            }
                        }
                    });
                } else plugin.getLogger().info("Você está utilizando a versão mais recente (" + currentVersion + ").");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Não foi possível verificar atualizações: " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("[^0-9]+");
        String[] currentParts = current.split("[^0-9]+");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestNum = i < latestParts.length ? parseInt(latestParts[i]) : 0;
            int currentNum = i < currentParts.length ? parseInt(currentParts[i]) : 0;
            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
