package FlexiAutoClick;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void carregarConfigs() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void recarregarConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        salvarMessages();
    }

    private void salvarMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String msg(String path) {
        return messages.getString(path, "Mensagem não encontrada: " + path).replace("&", "§");
    }

    public void tocarSom(Player p, String som) {
        try {
            if (som == null || som.isEmpty()) return;
            Sound s = Sound.valueOf(som.toUpperCase());
            p.playSound(p.getLocation(), s, 1f, 1f);
        } catch (Exception e) {
            plugin.getLogger().warning("Som inválido ou não compatível: " + som);
        }
    }

    public void ativarAutoClick(Player p, Map<java.util.UUID, PlayerCache> autoclickAtivo) {
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
}
