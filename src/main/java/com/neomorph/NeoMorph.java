package com.neomorph;

import com.neomorph.ability.AbilityRegistry;
import com.neomorph.ability.AbilityTickHandler;
import com.neomorph.command.MorphCommand;
import com.neomorph.command.UnmorphCommand;
import com.neomorph.gui.GUIListener;
import com.neomorph.gui.MorphGUI;
import com.neomorph.listener.AbilityListener;
import com.neomorph.listener.DisguisePersistListener;
import com.neomorph.listener.MorphListener;
import com.neomorph.morph.MorphManager;
import de.luisagrether.idisguise.iDisguise;
import org.bukkit.plugin.java.JavaPlugin;

public class NeoMorph extends JavaPlugin {

    private MorphManager morphManager;
    private AbilityRegistry abilityRegistry;
    private MorphGUI morphGUI;
    private iDisguise disguiseEngine;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        disguiseEngine = new iDisguise(this);
        disguiseEngine.init();
        getLogger().info("iDisguise engine initialized (embedded, by Luisa Grether).");

        abilityRegistry = new AbilityRegistry();
        getLogger().info("Registered " + abilityRegistry.getAllAbilities().size() + " mob morphs.");

        morphManager = new MorphManager(this, abilityRegistry);

        String guiTitle = getConfig().getString("gui-title", "NeoMorph");
        morphGUI = new MorphGUI(morphManager, guiTitle);

        MorphCommand morphCommand = new MorphCommand(morphManager, morphGUI);
        getCommand("morph").setExecutor(morphCommand);
        getCommand("morph").setTabCompleter(morphCommand);
        getCommand("unmorph").setExecutor(new UnmorphCommand(morphManager));

        getServer().getPluginManager().registerEvents(new GUIListener(this, morphManager, morphGUI), this);
        getServer().getPluginManager().registerEvents(new MorphListener(this, morphManager), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this, morphManager), this);
        getServer().getPluginManager().registerEvents(new DisguisePersistListener(morphManager), this);

        AbilityTickHandler tickHandler = new AbilityTickHandler(this, morphManager);
        tickHandler.runTaskTimer(this, 20L, 1L);

        getLogger().info("NeoMorph v" + getDescription().getVersion() + " enabled!");
        getLogger().info("The ultimate morph experience is ready.");
    }

    @Override
    public void onDisable() {
        if (morphManager != null) {
            morphManager.unmorphAll();
        }
        if (disguiseEngine != null) {
            disguiseEngine.shutdown();
        }
        getLogger().info("NeoMorph disabled. All morphs reverted.");
    }

    public MorphManager getMorphManager() {
        return morphManager;
    }

    public AbilityRegistry getAbilityRegistry() {
        return abilityRegistry;
    }

    public iDisguise getDisguiseEngine() {
        return disguiseEngine;
    }
}
