package me.lucko.punishmentgui;

import lombok.Getter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;

import me.lucko.helper.plugin.ExtendedJavaPlugin;
import me.lucko.helper.plugin.ap.Plugin;
import me.lucko.helper.plugin.ap.PluginDependency;
import me.lucko.helper.sql.SqlProvider;
import me.lucko.punishmentgui.commands.PunishCommand;
import me.lucko.punishmentgui.offence.Offence;
import me.lucko.punishmentgui.source.LiteBansPunishmentSource;
import me.lucko.punishmentgui.source.PunishmentSource;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(name = "PunishmentGui", version = "1.0.0", authors = "Luck", depends = {
        @PluginDependency(value = "helper", soft = true),
        @PluginDependency(value = "helper-sql", soft = true)
})
public class PunishmentGuiPlugin extends ExtendedJavaPlugin {

    private YamlConfiguration configuration;

    @Getter
    private Set<Offence> offences;

    @Getter
    private SqlProvider sql;

    @Getter
    private PunishmentSource punishmentSource;

    @Getter
    private Cache<UUID, String> beingPunished = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    @Override
    public void enable() {

        // load sql
        sql = getService(SqlProvider.class);
        if (sql == null) {
            throw new RuntimeException("SqlProvider not present");
        }

        // load config
        loadConfig();

        // load source
        // try to get from a service first, otherwise, try the built in handlers
        punishmentSource = getService(PunishmentSource.class);
        if (punishmentSource != null) {
            getLogger().info("Using external punishment source: " + punishmentSource.getName());
        } else {
            String source = configuration.getString("source", "").toLowerCase();
            switch (source) {
                case "litebans":
                    getLogger().info("Using LiteBans punishment source");
                    punishmentSource = new LiteBansPunishmentSource(
                            sql,
                            configuration.getConfigurationSection("litebans-source")
                    );
                    break;
                default:
                    throw new RuntimeException("Unknown punishment source: '" + source + "'.");
            }
        }

        registerCommand(new PunishCommand(this), "punish");
    }

    public void loadConfig() {
        this.configuration = loadConfig("config.yml");

        Set<Offence> offences = new LinkedHashSet<>();
        ConfigurationSection offencesSection = configuration.getConfigurationSection("offences");
        for (String key : offencesSection.getKeys(false)) {
            ConfigurationSection offenceSection = offencesSection.getConfigurationSection(key);
            try {
                offences.add(new Offence(offenceSection));
            } catch (Exception e) {
                getLogger().info("Unable to load offence: " + key);
                e.printStackTrace();
            }
        }

        this.offences = ImmutableSet.copyOf(offences);
    }
}
