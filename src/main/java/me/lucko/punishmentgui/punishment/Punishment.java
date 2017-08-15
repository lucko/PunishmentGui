package me.lucko.punishmentgui.punishment;

import java.util.UUID;

/**
 * Represents an individual punishment
 */
public interface Punishment extends Comparable<Punishment> {

    /**
     * Gets the uuid of the player who was punished
     *
     * @return the uuid of the punished player
     */
    UUID getPunished();

    /**
     * Gets the type of punishment
     *
     * @return the punishment type
     */
    PunishmentType getType();

    /**
     * Gets the time of the punishment in epoch seconds.
     *
     * @return the punishment time
     */
    long getTime();

    /**
     * Gets the punishment reason
     *
     * @return the punishment reason
     */
    String getReason();

}
