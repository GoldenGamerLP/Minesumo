package me.alex.minesumo.map;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.entities.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class MapSelection {

    //Scenarios
    //1. Map with size bigger
    //2. Map with a party and create new one
    //3. Next map with free space

    private final List<MapConfig> mapConfigs;
    private final MapCreator creator;

    public MapSelection(Minesumo min) {
        this.mapConfigs = min.getSchematicLoader().getLoadedMapConfigs();
        this.creator = min.getMapCreator();
    }

    public void addPlayersToQueue(List<Player> players) {
        List<MapConfig> possibleConfigs = mapConfigs.stream().filter(getMapWithSize(players.size())).toList();
        if (possibleConfigs.size() == 0) {
            players.forEach(player -> player.sendMessage("No map found"));
            return;
        }

        List<ArenaImpl> possibleArenas = MinecraftServer.getInstanceManager().getInstances().stream()
                .filter(instance -> instance instanceof ArenaImpl)
                .map(instance -> (ArenaImpl) instance)
                .filter(arena -> arena.getState() == ArenaImpl.ArenaState.WAITING_FOR_PLAYERS)
                .filter(arena -> possibleConfigs.contains(arena.getConfig()))
                .filter(arena -> arena.getPlayers().size() + players.size() <= arena.getMaxPlayers())
                .toList();
        //If no arenas found create a random one from possibleCfgs
        //Else add players to the arena
        if (!possibleArenas.isEmpty()) {
            queuePlayers(possibleArenas.get(0), players);
        } else {
            MapConfig cfg = possibleConfigs.get((int) (Math.random() * possibleConfigs.size()));
            creator.getMap(cfg).thenAccept(arena -> queuePlayers(arena, players));
        }


    }

    private Predicate<MapConfig> getMapWithSize(int size) {
        return mapConfig -> mapConfig.getSpawnPositions().size()
                * mapConfig.getPlayerPerSpawnPosition() >= size;
    }

    private void queuePlayers(Instance instance, List<Player> queue) {
        Iterator<Player> iterator = queue.iterator();

        instance.scheduler().scheduleTask(() -> {
            if (!iterator.hasNext()) return;


            Player player = iterator.next();
            if (!player.isOnline()) return;
            player.setInstance(instance);
        }, TaskSchedule.tick(1), TaskSchedule.tick(1));
    }
}
