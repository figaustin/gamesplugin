package com.etsuni.games.games;

import com.etsuni.games.Games;
import com.etsuni.games.menus.coinflip.CoinflipGameMenu;
import com.etsuni.games.utils.DBUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;


public class Coinflip extends TwoPlayerGame{

    public Player currentPlayerHead;
    public int taskId;
    private final Games plugin;
    private int flipCount = 0;

    public Coinflip(Player player1, Long wager, Games plugin) {
        super(player1, wager, plugin);
        this.plugin = plugin;
    }

    public void addToGamesList(){
        CurrentGames.getInstance().getCoinflipGames().add(this);
    }

    public Boolean start() {
        if(!CurrentGames.getInstance().getCoinflipGames().contains(this)) {
            return false;
        }

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        removeFromList();
        CoinflipGameMenu p1Menu = new CoinflipGameMenu(plugin.getPlayerMenuUtility(player1), plugin, this);
        CoinflipGameMenu p2Menu = new CoinflipGameMenu(plugin.getPlayerMenuUtility(player2), plugin, this);
        p1Menu.open();
        p2Menu.open();

        plugin.getEcon().withdrawPlayer(player2, wager);

        currentPlayerHead = player1;

        int minFlips = plugin.getCoinflipConfig().getInt("game_menu.min_flip_amount");
        int maxFlips = plugin.getCoinflipConfig().getInt("game_menu.max_flip_amount");
        int randomFlips = (int) Math.floor(Math.random() * (maxFlips - minFlips + 1) + minFlips);

        taskId = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if(flipCount <= randomFlips) {
                    if(currentPlayerHead.equals(player1)) {
                        currentPlayerHead = player2;

                    } else if(currentPlayerHead.equals(player2)){
                        currentPlayerHead = player1;
                    }
                    p1Menu.open();
                    p2Menu.open();
                } else {
                    Double winAmount = giveRewards(plugin.getCoinflipConfig()
                            .getInt("settings.tax_amount"), currentPlayerHead, getWager().doubleValue());

                    player1.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getCoinflipConfig().getString("settings.messages.win")
                                    .replace("%winner%", ChatColor.stripColor(currentPlayerHead.getDisplayName()))
                                    .replace("%win_amount%", String.valueOf(winAmount.longValue()))));

                    player2.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getCoinflipConfig().getString("settings.messages.win")
                                    .replace("%winner%", ChatColor.stripColor(currentPlayerHead.getDisplayName()))
                                    .replace("%win_amount%", String.valueOf(winAmount.longValue()))));
                    DBUtils dbUtils = new DBUtils(plugin);

                    if(currentPlayerHead.equals(player1)) {
                        dbUtils.addWinAndProfitToPlayer(player1, "cf", winAmount.longValue());
                        dbUtils.addLossToPlayer(player2, "cf");
                    } else if(currentPlayerHead.equals(player2)) {
                        dbUtils.addWinAndProfitToPlayer(player2, "cf", winAmount.longValue());
                        dbUtils.addLossToPlayer(player1, "cf");
                    }

                    scheduler.cancelTask(taskId);

                    player1.closeInventory();
                    player2.closeInventory();

                    return;
                }
                flipCount++;
            }
        },0, plugin.getCoinflipConfig().getInt("game_menu.flip_frequency"));
        return true;
    }
    public void removeFromList() {
        CurrentGames.getInstance().getCoinflipGames().remove(this);
    }

    public Player getCurrentPlayerHead() {
        return currentPlayerHead;
    }
}
