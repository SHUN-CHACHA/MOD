package dev.shuncha.headfireworkpaper;

import org.bukkit.plugin.java.JavaPlugin;

public class HeadFireworkPaperPlugin extends JavaPlugin {

    private HeadFireworkConfig headFireworkConfig;
    private RecipeManager recipeManager;
    private FireworkListener fireworkListener;
    private HeadFireworkGuiListener guiListener;

    @Override
    public void onEnable() {
        headFireworkConfig = new HeadFireworkConfig(this);
        headFireworkConfig.load();

        recipeManager = new RecipeManager(this);
        getServer().getPluginManager().registerEvents(recipeManager, this);
        fireworkListener = new FireworkListener(this);
        getServer().getPluginManager().registerEvents(fireworkListener, this);
        guiListener = new HeadFireworkGuiListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        HeadFireworkCommand command = new HeadFireworkCommand(this);
        getCommand("headfirework").setExecutor(command);
        getCommand("headfirework").setTabCompleter(command);

        getLogger().info("HeadFirework(Paper版)を有効化しました。");
    }

    @Override
    public void onDisable() {
        if (headFireworkConfig != null) {
            headFireworkConfig.save();
        }
    }

    public HeadFireworkConfig getHeadFireworkConfig() {
        return headFireworkConfig;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public FireworkListener getFireworkListener() {
        return fireworkListener;
    }

    public HeadFireworkGuiListener getGuiListener() {
        return guiListener;
    }
}