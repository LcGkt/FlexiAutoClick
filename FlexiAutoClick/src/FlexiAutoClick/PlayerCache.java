package FlexiAutoClick;

import org.bukkit.entity.Player;

public class PlayerCache {
    public final Player player;
    public final int range;
    public final double dano;
    public final boolean hitarea;

    public PlayerCache(Player player, int range, double dano, boolean hitarea) {
        this.player = player;
        this.range = range;
        this.dano = dano;
        this.hitarea = hitarea;
    }
}
