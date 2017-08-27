package me.lucko.punishmentgui.source;

import me.lucko.helper.terminable.registry.TerminableRegistry;
import me.lucko.punishmentgui.punishment.Punishment;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a source of punishment history (a hook with a banning plugin)
 */
public interface PunishmentSource {

    /**
     * Gets the name of this punishment source
     *
     * @return the source name
     */
    String getName();

    /**
     * Looks up a uuid for a given username
     *
     * @param username the username
     * @return the uuid if found
     */
    CompletableFuture<Optional<UUID>> lookupUuid(String username);

    /**
     * Gets the punishment history for a certain user
     *
     * @param who the user to get history for
     * @return the punishment history for the user. the set is mutable.
     */
    CompletableFuture<Set<Punishment>> getPunishmentHistory(UUID who);

}
