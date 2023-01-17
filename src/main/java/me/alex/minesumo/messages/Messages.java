package me.alex.minesumo.messages;

import net.kyori.adventure.text.Component;

public enum Messages {

    GAME_ENDING("message.game.ending", true),
    GAME_END_SUMMARY_SELF("message.game.ending.summary", true),
    GAME_WIN("message.game.ending.win", true),
    GAME_DRAW("message.game.ending.draw", true),
    GAME_STARTING("message.game.starting.in", false),
    GAME_STARTING_TITLE("message.game.starting.title", false),
    GAME_WAITING("message.game.waiting", true),
    GAME_WAITING_PLAYERS("message.game.waiting.players", false),
    GAME_TEAM_DEATH("message.game.death.final", true),
    GAME_DEATH("message.game.death", true),
    GAME_DEATH_PLAYER("message.game.death.player", true),
    GAME_TEAM_ENTRY("message.game.team", false),

    /*
     * Commands
     */
    GAME_COMMAND_FORCE_START_SUCCESS("message.game.commands.start.success", true),
    GAME_COMMAND_FAILURE_PLAYERS("message.game.commands.failure.players", true),
    GAME_COMMAND_WRONG_ARENA_STATE("message.game.commands.failure.state", true),
    GAME_COMMAND_SET_LIVES("message.game.commands.lives", true),
    GAME_COMMAND_GAME_ID("message.game.commands.gameid", true),

    /*
     * General Messages
     */
    GENERAL_NOT_IN_AN_ARENA("message.general.notInAnArena", true),
    GENERAL_NOT_FOUND("message.general.notfound", true),
    GENERAL_STATS_PLAYER("message.general.stats.player", true),
    GENERAL_STATS_ARENA("message.general.stats.arena", true),
    GENERAL_STATS_ARENA_HISTORY("message.general.stats.killAndDeathEntry", false);

    private final String key;
    private final Boolean prefix;

    Messages(String key, Boolean prefix) {
        this.key = key;
        this.prefix = prefix;
    }

    public Component toTranslatable(Component... args) {
        if (prefix) return MinesumoMessages.PREFIX.append(Component.translatable(key, args));
        return Component.translatable(key, args);
    }
}
