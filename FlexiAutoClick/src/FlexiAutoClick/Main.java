package FlexiAutoClick;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    public ConfigManager configManager;
    public UpdateChecker updateChecker;
    public Map<UUID, PlayerCache> autoclickAtivo = new HashMap<>();

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.carregarConfigs();

        updateChecker = new UpdateChecker(this);
        updateChecker.iniciarVerificacaoPeriodica();

        new AutoClickTask(this).iniciarTaskGlobal();

        getLogger().info("FlexiAutoClick habilitado! Servidor rodando: " + Bukkit.getBukkitVersion().split("-")[0]);
    }

    @Override
    public void onDisable() {
        autoclickAtivo.clear();
        getLogger().info("FlexiAutoClick desabilitado!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.msg("Mensagens.apenas-jogadores"));
            return true;
        }

        Player p = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("autoclick.admin")) {
                p.sendMessage(configManager.msg("Mensagens.sem-permissao"));
                return true;
            }
            configManager.recarregarConfigs();
            p.sendMessage(configManager.msg("Mensagens.reload"));
            return true;
        }

        if (!p.hasPermission("autoclick.use")) {
            p.sendMessage(configManager.msg("Mensagens.sem-permissao"));
            return true;
        }

        if (autoclickAtivo.containsKey(p.getUniqueId())) {
            autoclickAtivo.remove(p.getUniqueId());
            p.sendMessage(configManager.msg("Mensagens.desativado"));
            configManager.tocarSom(p, configManager.getConfig().getString("som-desativado"));
        } else {
            configManager.ativarAutoClick(p, autoclickAtivo);
        }

        return true;
    }
}
