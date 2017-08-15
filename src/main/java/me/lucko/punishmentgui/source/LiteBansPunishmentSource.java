package me.lucko.punishmentgui.source;

import me.lucko.helper.Scheduler;
import me.lucko.helper.sql.DatabaseCredentials;
import me.lucko.helper.sql.HelperDataSource;
import me.lucko.helper.sql.SqlProvider;
import me.lucko.punishmentgui.punishment.Punishment;
import me.lucko.punishmentgui.punishment.PunishmentType;
import me.lucko.punishmentgui.punishment.SimplePunishment;

import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LiteBansPunishmentSource extends AbstractPunishmentSource implements PunishmentSource {

    private final HelperDataSource sql;
    private final String tablePrefix;

    public LiteBansPunishmentSource(SqlProvider sqlProvider, ConfigurationSection config) {
        ConfigurationSection sqlConfig = config.getConfigurationSection("sql");

        sql = sqlProvider.getDataSource(DatabaseCredentials.fromConfig(sqlConfig));
        tablePrefix = sqlConfig.getString("table-prefix", "litebans_");
    }

    @Override
    public String getName() {
        return "LiteBans";
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookupUuid(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = sql.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT `uuid` FROM " + tablePrefix + "history WHERE `name`=? ORDER BY `date` DESC LIMIT 1")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            try {
                                return Optional.of(UUID.fromString(rs.getString("uuid")));
                            } catch (IllegalArgumentException e) {
                                // ignore
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, Scheduler.async());
    }

    @Override
    public CompletableFuture<Set<Punishment>> getPunishmentHistory(UUID who) {
        return CompletableFuture.supplyAsync(() -> {
            Set<Punishment> ret = new HashSet<>();

            try (Connection connection = sql.getConnection()) {
                retrieveData(ret, who, tablePrefix + "warnings", PunishmentType.WARNING, connection);
                retrieveData(ret, who, tablePrefix + "kicks", PunishmentType.KICK, connection);
                retrieveData(ret, who, tablePrefix + "mutes", PunishmentType.MUTE, connection);
                retrieveData(ret, who, tablePrefix + "bans", PunishmentType.BAN, connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return ret;
        }, Scheduler.async());
    }

    private static void retrieveData(Set<Punishment> accumulator, UUID who, String table, PunishmentType type, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT `reason`, `time` FROM " + table + " WHERE uuid=?")) {
            ps.setString(1, who.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String reason = rs.getString("reason");
                    long date = rs.getLong("time");
                    accumulator.add(new SimplePunishment(who, type, date / 1000L, reason));
                }
            }
        }
    }
}
