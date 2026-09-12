package dev.shuncha.headfireworkpaper;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * /headfirework ... コマンドの処理。
 * Fabric MOD版の/headfirework configコマンド体系に合わせている。
 *
 * 権限について: このコマンド自体はplugin.ymlで誰でも実行できる権限(headfirework.use、
 * デフォルトtrue)に紐付けている。そのうえで、管理者専用のサブコマンド(config / gui)
 * だけをここで明示的にheadfirework.admin権限チェックしている。myface / mygui / testhead
 * は参加者本人が使うためのコマンドなので、admin権限は要求しない。
 */
public class HeadFireworkCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SHAPES = List.of("small_ball", "large_ball", "star", "creeper", "burst");
    private static final List<String> DIRECTIONS = List.of("north", "east", "south", "west");
    private static final List<String> SUBCOMMANDS = List.of(
            "scale", "display_duration", "animation_duration", "fade_duration", "facing", "show");

    private final HeadFireworkPaperPlugin plugin;

    public HeadFireworkCommand(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("testhead")) {
            return handleTestHead(sender, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("myface")) {
            return handleMyFace(sender, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("mygui")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
                return true;
            }
            plugin.getGuiListener().openPersonal(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (!sender.hasPermission("headfirework.admin")) {
                sender.sendMessage(ChatColor.RED + "このコマンドを使う権限がありません。");
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
                return true;
            }
            plugin.getGuiListener().open(player);
            return true;
        }

        HeadFireworkConfig config = plugin.getHeadFireworkConfig();

        if (args.length == 0 || !args[0].equalsIgnoreCase("config")) {
            sender.sendMessage(ChatColor.RED + "使用法: /headfirework config <項目> <値> / /headfirework gui / "
                    + "/headfirework myface <方角> / /headfirework mygui / /headfirework testhead <pitch> <roll> <yaw>");
            return true;
        }

        if (!sender.hasPermission("headfirework.admin")) {
            sender.sendMessage(ChatColor.RED + "このコマンドを使う権限がありません。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "項目を指定してください: " + String.join(", ", SUBCOMMANDS));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "scale" -> handleScale(sender, config, args);
            case "display_duration" -> handleIntSetting(sender, args, "表示時間",
                    config::setDisplayDuration, config::resetDisplayDuration);
            case "animation_duration" -> handleIntSetting(sender, args, "拡大アニメーション時間",
                    config::setAnimationDuration, config::resetAnimationDuration);
            case "fade_duration" -> handleIntSetting(sender, args, "フェードアウト時間",
                    config::setFadeDuration, config::resetFadeDuration);
            case "facing" -> handleFacing(sender, config, args);
            case "show" -> showConfig(sender, config);
            default -> sender.sendMessage(ChatColor.RED + "不明な項目です: " + sub);
        }

        config.save();
        return true;
    }

    /**
     * デバッグ用コマンド: /headfirework testhead <pitch> <roll> <yaw>
     * 再ビルドせずにその場で任意の角度の頭を出して見た目を確認できる。
     */
    private boolean handleTestHead(CommandSender sender, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "使用法: /headfirework testhead <pitch> <roll> <yaw> (角度は度数法)");
            return true;
        }
        try {
            float pitch = Float.parseFloat(args[1]);
            float roll = Float.parseFloat(args[2]);
            float yaw = Float.parseFloat(args[3]);
            plugin.getFireworkListener().spawnDebugHead(player, pitch, roll, yaw);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "角度は数値で指定してください。");
        }
        return true;
    }

    /**
     * 参加者本人が、自分の花火に映る顔の向きを設定するコマンド。管理者権限は不要。
     * /headfirework myface <north|south|east|west> … 向きを設定
     * /headfirework myface show                     … 現在の自分の設定を表示
     * /headfirework myface reset                    … 個人設定を削除し、サーバーのデフォルトに戻す
     */
    private boolean handleMyFace(CommandSender sender, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        HeadFireworkConfig config = plugin.getHeadFireworkConfig();

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用法: /headfirework myface <north|south|east|west> / show / reset");
            return true;
        }

        String value = args[1].toLowerCase();

        if (value.equals("show")) {
            String current = config.getPlayerFacing(player.getName())
                    .map(f -> f.name().toLowerCase())
                    .orElse("未設定(サーバーのデフォルト値「" + config.getFacing().name().toLowerCase() + "」を使用中)");
            player.sendMessage(ChatColor.YELLOW + "あなたの花火の顔の向き設定: " + ChatColor.AQUA + current);
            return true;
        }
        if (value.equals("reset")) {
            config.resetPlayerFacing(player.getName());
            config.save();
            player.sendMessage(ChatColor.GREEN + "顔の向き設定をサーバーのデフォルト値に戻しました。");
            return true;
        }

        BlockFace face = switch (value) {
            case "north" -> BlockFace.NORTH;
            case "east" -> BlockFace.EAST;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            default -> null;
        };
        if (face == null) {
            sender.sendMessage(ChatColor.RED + "不明な方角です: " + args[1]
                    + " (使用可能: " + String.join(", ", DIRECTIONS) + " / show / reset)");
            return true;
        }
        config.setPlayerFacing(player.getName(), face);
        config.save();
        player.sendMessage(ChatColor.GREEN + "あなたの花火に映る顔の向きを" + value + "に設定しました。");
        return true;
    }

    private void handleScale(CommandSender sender, HeadFireworkConfig config, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "使用法: /headfirework config scale <形状> <値>");
            return;
        }
        HeadFireworkConfig.ExplosionShape shape = parseShape(args[2]);
        if (shape == null) {
            sender.sendMessage(ChatColor.RED + "不明な形状です: " + args[2] + " (使用可能: " + String.join(", ", SHAPES) + ")");
            return;
        }
        if (args.length < 4) {
            config.resetScale(shape);
            sender.sendMessage(ChatColor.GREEN + args[2] + "のサイズをデフォルトに戻しました。");
            return;
        }
        try {
            double value = Double.parseDouble(args[3]);
            config.setScale(shape, value);
            sender.sendMessage(ChatColor.GREEN + args[2] + "のサイズを" + value + "に設定しました。");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数値を指定してください: " + args[3]);
        }
    }

    private void handleIntSetting(CommandSender sender, String[] args, String label,
                                   java.util.function.IntConsumer setter, Runnable reset) {
        if (args.length < 3) {
            reset.run();
            sender.sendMessage(ChatColor.GREEN + label + "をデフォルトに戻しました。");
            return;
        }
        try {
            int value = Integer.parseInt(args[2]);
            setter.accept(value);
            sender.sendMessage(ChatColor.GREEN + label + "を" + value + "tickに設定しました。");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "整数を指定してください: " + args[2]);
        }
    }

    private void handleFacing(CommandSender sender, HeadFireworkConfig config, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "使用法: /headfirework config facing <north|south|east|west>");
            return;
        }
        BlockFace face = switch (args[2].toLowerCase()) {
            case "north" -> BlockFace.NORTH;
            case "east" -> BlockFace.EAST;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            default -> null;
        };
        if (face == null) {
            sender.sendMessage(ChatColor.RED + "不明な方角です: " + args[2] + " (使用可能: " + String.join(", ", DIRECTIONS) + ")");
            return;
        }
        config.setFacing(face);
        sender.sendMessage(ChatColor.GREEN + "顔の向きを" + args[2] + "に設定しました。");
    }

    private void showConfig(CommandSender sender, HeadFireworkConfig config) {
        sender.sendMessage(ChatColor.YELLOW + "=== HeadFirework 現在の設定 ===");
        for (HeadFireworkConfig.ExplosionShape shape : HeadFireworkConfig.ExplosionShape.values()) {
            sender.sendMessage(ChatColor.GRAY + "  " + shape.name().toLowerCase() + ": " + config.getScale(shape));
        }
        sender.sendMessage(ChatColor.GRAY + "  display_duration: " + config.getDisplayDuration() + " tick");
        sender.sendMessage(ChatColor.GRAY + "  animation_duration: " + config.getAnimationDuration() + " tick");
        sender.sendMessage(ChatColor.GRAY + "  fade_duration: " + config.getFadeDuration() + " tick");
        sender.sendMessage(ChatColor.GRAY + "  facing: " + config.getFacing().name().toLowerCase());
    }

    private HeadFireworkConfig.ExplosionShape parseShape(String value) {
        return switch (value.toLowerCase()) {
            case "small_ball" -> HeadFireworkConfig.ExplosionShape.SMALL_BALL;
            case "large_ball" -> HeadFireworkConfig.ExplosionShape.LARGE_BALL;
            case "star" -> HeadFireworkConfig.ExplosionShape.STAR;
            case "creeper" -> HeadFireworkConfig.ExplosionShape.CREEPER;
            case "burst" -> HeadFireworkConfig.ExplosionShape.BURST;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("myface");
            options.add("mygui");
            if (sender.hasPermission("headfirework.admin")) {
                options.add("config");
                options.add("gui");
                options.add("testhead");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("config")) {
                options.addAll(SUBCOMMANDS);
            } else if (args[0].equalsIgnoreCase("myface")) {
                options.addAll(DIRECTIONS);
                options.add("show");
                options.add("reset");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("scale")) options.addAll(SHAPES);
            if (args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("facing")) options.addAll(DIRECTIONS);
        }
        return options;
    }
}
