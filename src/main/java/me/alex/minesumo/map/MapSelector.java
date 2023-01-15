package me.alex.minesumo.map;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MapSelector {

    public static final Predicate<MapConfig> ALL_MAPS;

    static {
        ALL_MAPS = mapConfig -> true;
    }

    private final SchematicHandler handler;
    private final MapCreator mapCreator;
    private final ThreadLocalRandom localRandom;

    public MapSelector(Minesumo minesumo) {
        this.handler = minesumo.getSchematicLoader();
        this.mapCreator = minesumo.getMapCreator();

        this.localRandom = ThreadLocalRandom.current();
    }

    /**
     * Returns a MapConfig which has been selected by the arguments
     *
     * @param selection         The selection on the map
     * @param selectionStrategy The strategy to select the map if multiple are present
     * @return A MapConfig or none if nothing was found.
     */
    public Optional<MapConfig> selectMap(Predicate<MapConfig> selection, MapSelectionStrategy selectionStrategy) {
        List<MapConfig> configs = handler.getLoadedMapConfigs();
        if (configs.size() == 0) return Optional.empty();

        Stream<MapConfig> stream = configs.stream().filter(selection);

        return switch (selectionStrategy) {
            case ANY_RESULT -> stream.findAny();
            case RANDOM_RESULT -> {
                List<MapConfig> config = stream.toList();
                int nextRandom = localRandom.nextInt(config.size());
                yield Optional.ofNullable(config.get(nextRandom));
            }
            case FIRST_RESULT -> stream.findFirst();
        };
    }

    /**
     * Returns an arena which is ready to be used. <b>Party -> Save the uuid of the instance and set the instance from the player to the uuid</b>
     *
     * @param mapConfig The map where to play.
     * @param state     The state of the given arena on which the player wants to play.
     * @return A CompletableFuture which may be load the complete map.
     */
    public CompletableFuture<ArenaImpl> getAvailableMap(MapConfig mapConfig, ArenaImpl.ArenaState state) {
        List<ArenaImpl> arenaImpls = MinecraftServer.getInstanceManager()
                .getInstances()
                .stream()
                .filter(instance -> instance instanceof ArenaImpl)
                .map(instance -> (ArenaImpl) instance)
                .filter(arena -> arena.getMapConfig().equals(mapConfig))
                .filter(arena -> arena.getState().equals(state))
                .toList();

        return arenaImpls.size() == 0 ? mapCreator.getMap(mapConfig) : CompletableFuture.completedFuture(arenaImpls.get(0));
    }

    /**
     * Returns an arena. Practical for multi-player joins
     *
     * @param uuid The uuid of the arena
     * @return A CompletableFuture of the Arena
     */
    public CompletableFuture<ArenaImpl> getAvailableMap(UUID uuid) {
        ArenaImpl arenaImpl = (ArenaImpl) MinecraftServer.getInstanceManager().getInstance(uuid);
        return CompletableFuture.completedFuture(arenaImpl);
    }

    public void queueArena(Player player, ArenaImpl impl) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> player.setInstance(impl, impl.getMapConfig().getSpectatorPosition()));
    }

    public enum MapSelectionStrategy {
        ANY_RESULT,
        RANDOM_RESULT,
        FIRST_RESULT
    }
}
