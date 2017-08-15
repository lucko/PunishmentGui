package me.lucko.punishmentgui.offence;

import lombok.Getter;

import com.google.common.collect.ImmutableList;

import org.bukkit.entity.Player;

import java.util.List;

@Getter
public class LadderEntry {

    private final String requiredPermission;
    private final List<String> commands;
    private final List<String> response;

    public LadderEntry(String requiredPermission, List<String> commands, List<String> response) {
        if (requiredPermission.isEmpty()) {
            requiredPermission = null;
        }

        this.requiredPermission = requiredPermission;
        this.commands = ImmutableList.copyOf(commands);
        this.response = ImmutableList.copyOf(response);
    }

    public boolean isAuthorized(Player player) {
        return requiredPermission == null || player.hasPermission(requiredPermission);
    }
}
