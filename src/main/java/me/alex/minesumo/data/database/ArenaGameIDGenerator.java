package me.alex.minesumo.data.database;

import me.alex.minesumo.Minesumo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaGameIDGenerator {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final StatisticDB dbStatsHandler;

    public ArenaGameIDGenerator(Minesumo minesumo) {
        this.dbStatsHandler = minesumo.getStatsHandler();
    }

    private String getRandomArenaUID() {
        StringBuilder sb = new StringBuilder();
        //Random Zahlen und nummern
        for (int i = 0; i < 6; i++) {
            int randomChar = random.nextInt(36);
            if (randomChar < 26) {
                sb.append((char) (randomChar + 'a'));
            } else {
                sb.append((char) (randomChar - 26 + '0'));
            }
        }
        return sb.toString();
    }

    /**
     * Returns a not used Arena UID
     *
     * @return A safe UID in form of a String
     */
    public CompletableFuture<String> getSafeArenaUID() {
        return CompletableFuture.supplyAsync(() -> {
            String rndm;
            do {
                rndm = getRandomArenaUID();
            } while (this.dbStatsHandler
                    .getArenaDB()
                    .countDocuments(this.dbStatsHandler.arenaFilter(rndm)) != 0);

            return rndm;
        });
    }

}
