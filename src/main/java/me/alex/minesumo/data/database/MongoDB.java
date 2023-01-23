package me.alex.minesumo.data.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.entities.MinesumoMainConfig;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.Blocking;

@Slf4j
public class MongoDB {

    @Getter
    private final MongoClient mongoClient;

    public MongoDB(Minesumo minesumo) {
        MinesumoMainConfig cfg = minesumo.getConfig();
        ConnectionString connectionString = new ConnectionString(cfg.getMongoDB());

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .build();

        this.mongoClient = MongoClients.create(settings);

        if (isConnected()) {
            log.info("Connected to mongodb!");
        } else {
            log.error("Could not connect to mongodb!");
            throw new IllegalStateException("Could not connect to mongodb!");
        }
    }

    public void close() {
        this.mongoClient.close();
    }

    //Check if the database is connected
    @Blocking
    public boolean isConnected() {
        return this.mongoClient != null && this.mongoClient.getDatabase("admin")
                .runCommand(Document.parse("{ping:1}"))
                .size() != 0;
    }
}
