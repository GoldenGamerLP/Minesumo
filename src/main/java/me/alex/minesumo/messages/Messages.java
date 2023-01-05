package me.alex.minesumo.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public enum Messages {

    GAME_ENDING("message.game.ending"),
    GAME_WIN("message.game.ending.win"),
    GAME_DRAW("message.game.ending.draw"),
    GAME_STARTING("message.game.starting.in"),
    GAME_STARTING_TITLE("message.game.starting.title"),
    GAME_WAITING("message.game.waiting"),
    GAME_WAITING_PLAYERS("message.game.waiting.players"),
    GAME_TEAM_DEATH("message.game.death.final"),
    GAME_DEATH("message.game.death"),
    GAME_DEATH_PLAYER("message.game.death.player"),

    /*
     * Commands
     */
    GAME_COMMAND_FORCE_START_SUCCESS("message.game.commands.start.success"),
    GAME_COMMAND_FAILURE_PLAYERS("message.game.commands.failure.players"),
    GAME_COMMAND_WRONG_ARENA_STATE("message.game.commands.failure.state"),
    GAME_COMMAND_SET_LIVES("message.game.commands.lives"),

    /*
     * General Messages
     */
    GENERAL_NOT_IN_AN_ARENA("message.general.notInAnArena");

    private final String key;

    Messages(String key) {
        this.key = key;
    }

    public TranslatableComponent toTranslatable(Component... args) {
        return Component.translatable(key).args(args);
    }
}
