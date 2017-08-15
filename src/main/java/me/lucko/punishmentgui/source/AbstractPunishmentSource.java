package me.lucko.punishmentgui.source;

import lombok.experimental.Delegate;

import me.lucko.helper.terminable.TerminableRegistry;

abstract class AbstractPunishmentSource implements PunishmentSource {

    @Delegate
    private final TerminableRegistry registry = TerminableRegistry.create();

}
