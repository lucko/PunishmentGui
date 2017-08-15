package me.lucko.punishmentgui.punishment;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class SimplePunishment implements Punishment {

    private final UUID punished;
    private final PunishmentType type;
    private final long time;
    private final String reason;

    @Override
    public int compareTo(Punishment other) {
        int i = punished.compareTo(other.getPunished());
        if (i != 0) {
            return i;
        }

        i = Long.compare(time, other.getTime());
        if (i != 0) {
            return i;
        }

        i = type.compareTo(other.getType());
        if (i != 0) {
            return i;
        }

        return reason.compareToIgnoreCase(other.getReason());
    }
}
