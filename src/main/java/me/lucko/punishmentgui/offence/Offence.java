package me.lucko.punishmentgui.offence;

import lombok.Getter;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import me.lucko.helper.utils.Color;
import me.lucko.helper.utils.ImmutableCollectors;
import me.lucko.punishmentgui.punishment.Punishment;
import me.lucko.punishmentgui.punishment.PunishmentType;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Getter
public class Offence {

    // display
    private final ItemStack icon;
    private final String name;
    private final List<String> description;

    // matchers
    private final Set<PunishmentType> searchFor;
    private final Set<Pattern> matching;
    private final Set<Pattern> notMatching;

    // the ladder entries
    private final List<LadderEntry> ladder;

    public Offence(ConfigurationSection section) {
        String icon = section.getString("icon");
        Preconditions.checkArgument(icon != null, "icon not present");
        if (icon.contains("^")) {
            Iterator<String> split = Splitter.on('^').split(icon).iterator();
            Material material = Material.valueOf(split.next());
            short data = Short.parseShort(split.next());
            this.icon = new ItemStack(material, 1, data);
        } else {
            this.icon = new ItemStack(Material.valueOf(icon));
        }

        String name = section.getString("name");
        Preconditions.checkArgument(name != null, "name not present");
        this.name = Color.colorize(name);

        List<String> description = section.getStringList("description");
        Preconditions.checkArgument(description != null && !description.isEmpty(), "description not present");
        this.description = description.stream().map(Color::colorize).collect(ImmutableCollectors.toList());

        List<String> searchFor = section.getStringList("ladder-resolution.search-for");
        Preconditions.checkArgument(searchFor != null && !searchFor.isEmpty(), "searchFor not present");
        this.searchFor = searchFor.stream()
                .map(String::toUpperCase)
                .map(s -> {
                    if (s.endsWith("S")) {
                        return s.substring(0, s.length() - 1);
                    }
                    return s;
                })
                .map(s -> {
                    if (s.equals("WARN")) {
                        return "WARNING";
                    }
                    return s;
                })
                .map(PunishmentType::valueOf)
                .collect(ImmutableCollectors.toSet());

        List<String> matching = section.getStringList("ladder-resolution.matching");
        Preconditions.checkArgument(matching != null && !matching.isEmpty(), "matching not present");
        this.matching = matching.stream()
                .map(s -> s.replace("%", ".*"))
                .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                .collect(ImmutableCollectors.toSet());

        if (section.isSet("ladder-resolution.not-matching")) {
            List<String> notMatching = section.getStringList("ladder-resolution.not-matching");
            Preconditions.checkArgument(notMatching != null, "notMatching not present");
            this.notMatching = notMatching.stream()
                    .map(s -> s.replace("%", ".*"))
                    .map(Pattern::compile)
                    .collect(ImmutableCollectors.toSet());
        } else {
            this.notMatching = ImmutableSet.of();
        }

        ConfigurationSection ladderSection = section.getConfigurationSection("ladder");
        Map<Integer, LadderEntry> ladder = new TreeMap<>(Comparator.naturalOrder());

        for (String key : ladderSection.getKeys(false)) {
            int pos;
            try {
                pos = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                new IllegalArgumentException("Couldn't parse ladder position '" + key + "' from " + this.name, e).printStackTrace();
                continue;
            }

            ConfigurationSection ls = ladderSection.getConfigurationSection(key);
            // permission command output
            String permission = ls.getString("permission", "");
            List<String> command = weakGetStringList(ls, "command").stream().collect(ImmutableCollectors.toList());
            List<String> output = weakGetStringList(ls, "output").stream().map(Color::colorize).collect(ImmutableCollectors.toList());
            ladder.put(pos, new LadderEntry(permission, command, output));
        }

        if (ladder.isEmpty()) {
            throw new IllegalArgumentException("Empty ladder for " + this.name);
        }

        this.ladder = ImmutableList.copyOf(ladder.values());
    }

    public boolean matches(Punishment punishment) {
        if (!searchFor.contains(punishment.getType())) {
            return false;
        }

        boolean match = false;
        for (Pattern matchPattern : matching) {
            if (matchPattern.matcher(punishment.getReason()).matches()) {
                match = true;
                break;
            }
        }

        if (!match) {
            return false;
        }

        boolean notMatch = false;
        for (Pattern notMatchPattern : notMatching) {
            if (notMatchPattern.matcher(punishment.getReason()).matches()) {
                notMatch = true;
                break;
            }
        }

        if (notMatch) {
            return false;
        }

        return true;
    }

    public int getMatchingCount(Collection<Punishment> punishments) {
        return (int) punishments.stream().filter(this::matches).count();
    }

    public LadderEntry getApplicableEntryForHistory(Collection<Punishment> punishments) {
        int ladderIndex = getMatchingCount(punishments);
        if (ladderIndex >= ladder.size()) {
            ladderIndex = ladder.size() - 1;
        }
        return ladder.get(ladderIndex);
    }

    private static List<String> weakGetStringList(ConfigurationSection conf, String key) {
        if (!conf.isSet(key)) {
            return ImmutableList.of();
        }

        if (conf.isString(key)) {
            return ImmutableList.of(conf.getString(key));
        }

        return ImmutableList.copyOf(conf.getStringList(key));
    }
}
