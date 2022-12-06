package me.alex.minesumo.data.instances;

import net.minestom.server.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.LongStream;

public class ArenaPlayer {

    private final Player player;
    private final Map<PlayerStatistics, Object> stats;

    public ArenaPlayer(Player player) {
        this.player = player;
        this.stats = new HashMap<>();
    }

    public Double get(PlayerStatistics data) {
        return data.function.apply(this.stats.get(data));
    }

    public void set(PlayerStatistics data, Object value) {
        Objects.equals(value.getClass(), data.getClass());
        this.stats.put(data, value);
    }

    public enum PlayerStatistics {
        ALL_TIME_HITS(Integer.class, integer -> integer.doubleValue()),
        HIT_HITS(Integer.class, integer -> integer.doubleValue()),
        DEATHS(Integer.class, integer -> integer.doubleValue()),
        AVG_PING(long[].class, longs -> LongStream.of(longs).sum() / longs.length * 1.0D);

        private final Class<?> clazz;
        private final Function<Object, Double> function;

        <T> PlayerStatistics(Class<T> clazz, Function<T, Double> function) {
            this.clazz = clazz;
            this.function = (Function<Object, Double>) function;
        }

        public Function<?, Double> getFunction() {
            return function;
        }

        public Class<?> getClazz() {
            return clazz;
        }
    }

}