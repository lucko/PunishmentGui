package me.lucko.punishmentgui.commands;

import lombok.RequiredArgsConstructor;

import me.lucko.helper.Scheduler;
import me.lucko.helper.utils.Players;
import me.lucko.punishmentgui.PunishmentGuiPlugin;
import me.lucko.punishmentgui.gui.PunishmentGui;
import me.lucko.punishmentgui.punishment.Punishment;
import me.lucko.punishmentgui.source.PunishmentSource;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class PunishCommand implements CommandExecutor {
    private final PunishmentGuiPlugin plugin;

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            return true;
        }

        if (!s.hasPermission("punishmentgui.command.punish")) {
            Players.msg(s, "No permission.");
            return true;
        }

        if (args.length == 0) {
            Players.msg(s, "&c/punish <who>");
            return true;
        }

        Player sender = ((Player) s);
        String target = args[0];

        if (target.equals("reload") && sender.hasPermission("punishmentgui.reload")) {
            plugin.loadConfig();
            Players.msg(sender, "&aConfig reloaded.");
            return true;
        }

        AtomicReference<UUID> uuid = new AtomicReference<>(null);
        try {
            uuid.set(UUID.fromString(target));
        } catch (IllegalArgumentException e) {
            // ignore
        }

        if (uuid.get() == null) {
            Player p = Players.getNullable(target);
            if (p != null) {
                target = p.getName();
                uuid.set(p.getUniqueId());
            }
        }

        String finalTarget = target;
        Scheduler.runAsync(() -> {
            PunishmentSource punishmentSource = plugin.getPunishmentSource();

            if (uuid.get() == null) {
                uuid.set(punishmentSource.lookupUuid(finalTarget).join().orElse(null));
            }

            if (uuid.get() == null) {
                Players.msg(sender, "&cPlayer '" + finalTarget + "' could not be found.");
                return;
            }

            UUID player = uuid.get();

            String punishing = plugin.getBeingPunished().asMap().putIfAbsent(player, sender.getName());
            if (punishing != null && !punishing.equals(sender.getName())) {
                Players.msg(sender, "&c" + finalTarget + " is already being punished by " + punishing);
                return;
            }

            Players.msg(sender, "&cLoading punishment history. Please wait...");
            Set<Punishment> history = punishmentSource.getPunishmentHistory(player).join();
            Scheduler.runSync(() -> {
                if (!sender.isOnline()) {
                    plugin.getBeingPunished().invalidate(player);
                    return;
                }
                new PunishmentGui(plugin, sender, uuid.get(), finalTarget, history).open();
            });
        });

        return true;
    }
}
