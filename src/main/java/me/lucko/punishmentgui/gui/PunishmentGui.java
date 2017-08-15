package me.lucko.punishmentgui.gui;

import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.menu.scheme.MenuScheme;
import me.lucko.punishmentgui.PunishmentGuiPlugin;
import me.lucko.punishmentgui.offence.LadderEntry;
import me.lucko.punishmentgui.offence.Offence;
import me.lucko.punishmentgui.punishment.Punishment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PunishmentGui extends Gui {

    private static final List<Integer> SLOTS = new MenuScheme()
            .maskEmpty(1)
            .mask("011111110")
            .mask("011111110")
            .mask("011111110")
            .mask("011111110")
            .maskEmpty(1)
            .getMaskedIndexesImmutable();

    private static final List<Integer> PAGES = new MenuScheme()
            .maskEmpty(5)
            .mask("010000010")
            .getMaskedIndexesImmutable();

    private final PunishmentGuiPlugin plugin;
    private final UUID target;
    private final String targetName;
    private final Set<Punishment> history;
    private int page;

    public PunishmentGui(PunishmentGuiPlugin plugin, Player player, UUID target, String targetName, Set<Punishment> history) {
        super(player, 6, "&3&l✦ &2Punish &7-> &2" + targetName);
        this.plugin = plugin;
        this.target = target;
        this.targetName = targetName;
        this.history = history;
        this.page = 1;
    }

    @Override
    public void redraw() {
        // get available slots
        List<Integer> slots = new ArrayList<>(SLOTS);

        // work out the values to display on this page
        List<List<Offence>> pages = divideList(plugin.getOffences(), slots.size());
        List<Offence> page = pages.isEmpty() ? new ArrayList<>() : pages.get(this.page - 1);

        // place prev/next page buttons
        if (this.page == 1) {
            // can't go back further
            removeItem(PAGES.get(0));
        } else {
            setItem(PAGES.get(0), ItemStackBuilder.of(Material.ARROW)
                    .name("&e<&e&m--")
                    .lore("&fSwitch to the previous page.")
                    .lore("")
                    .lore("&7Currently viewing page &e" + this.page + "&7/&e" + pages.size())
                    .build(() -> {
                        this.page = this.page - 1;
                        redraw();
                    }));
        }

        if (this.page >= pages.size()) {
            // can't go forward a page
            removeItem(PAGES.get(1));
        } else {
            setItem(PAGES.get(1), ItemStackBuilder.of(Material.ARROW)
                    .name("&e&m--&e>")
                    .lore("&fSwitch to the next page.")
                    .lore("")
                    .lore("&7Currently viewing page &e" + this.page + "&7/&e" + pages.size())
                    .build(() -> {
                        this.page = this.page + 1;
                        redraw();
                    }));
        }

        // remove previous items
        if (!isFirstDraw()) {
            slots.forEach(this::removeItem);
        }

        // place the actual items
        for (Offence offence : page) {
            int index = slots.remove(0);

            int matchingCount = offence.getMatchingCount(history);
            LadderEntry entry = offence.getApplicableEntryForHistory(history);

            setItem(index, ItemStackBuilder.of(new ItemStack(offence.getIcon()))
                    .name(offence.getName())
                    .amount(Math.max(1, matchingCount))
                    .apply(isb -> {
                        for (String lore : offence.getDescription()) {
                            isb.lore(lore);
                        }

                        if (entry.isAuthorized(getPlayer())) {
                            isb.lore("")
                                    .lore("&7&m---------------")
                                    .lore("&bClick to apply the following punishment.")
                                    .lore("&aLadder #: &f" + (matchingCount + 1))
                                    .lore("&aCommands:");
                            for (String command : entry.getCommands()) {
                                String cmd = command.replace("{offence_count}", Integer.toString(matchingCount + 1));
                                isb.lore("&a- &f/" + cmd);
                            }
                            isb.lore("&7&m---------------");
                        } else {
                            isb.lore("&7&m---------------")
                                    .lore("&cYou are not able to apply")
                                    .lore("&cfuther punishments on this ladder.")
                                    .lore("")
                                    .lore("&cPlease contact a more senior")
                                    .lore("&cstaff member.")
                                    .lore("&7&m---------------");
                        }
                    })
                    .build(() -> {
                        if (!entry.isAuthorized(getPlayer())) {
                            return;
                        }

                        for (String command : entry.getCommands()) {
                            String cmd = command
                                    .replace("{offence_count}", Integer.toString(matchingCount + 1))
                                    .replace("{name}", target.toString());

                            Bukkit.dispatchCommand(getPlayer(), cmd);
                        }

                        for (String response : entry.getResponse()) {
                            String resp = response
                                    .replace("{offence_count}", Integer.toString(matchingCount + 1))
                                    .replace("{name}", targetName.toString());

                            getPlayer().sendMessage(resp);
                        }

                        plugin.getBeingPunished().invalidate(target);
                        close();
                    }));
        }
    }

    private static <T> List<List<T>> divideList(Iterable<T> source, int size) {
        List<List<T>> lists = new ArrayList<>();
        Iterator<T> it = source.iterator();
        while (it.hasNext()) {
            List<T> subList = new ArrayList<>();
            for (int i = 0; it.hasNext() && i < size; i++) {
                subList.add(it.next());
            }
            lists.add(subList);
        }
        return lists;
    }
}
