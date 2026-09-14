package dev.shuncha.headfireworkpaper;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * headfirework.admin権限を持つプレイヤー(OP)がサーバーに参加したとき、
 * 新しいバージョンが公開されていればチャットで知らせるリスナー。
 *
 * サーバーコンソールのログとは別に、実際にプレイしているOPにもゲーム内で
 * 気づいてもらうための通知。UpdateCheckerの非同期チェックが起動時に完了していない
 * (サーバー起動直後にOPが参加した)場合は何も表示されない点に注意。
 */
public class UpdateNotifyListener implements Listener {

    private final HeadFireworkPaperPlugin plugin;

    public UpdateNotifyListener(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("headfirework.admin")) {
            return;
        }

        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null || !checker.isUpdateAvailable()) {
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();
        player.sendMessage(ChatColor.GOLD + "[HeadFirework] " + ChatColor.YELLOW
                + "新しいバージョン " + ChatColor.AQUA + checker.getLatestVersion() + ChatColor.YELLOW
                + " が公開されています(現在: v" + currentVersion + ")。");
        if (checker.getLatestReleaseUrl() != null) {
            player.sendMessage(ChatColor.GOLD + "[HeadFirework] " + ChatColor.YELLOW
                    + "ダウンロード: " + ChatColor.AQUA + checker.getLatestReleaseUrl());
        }
    }
}
