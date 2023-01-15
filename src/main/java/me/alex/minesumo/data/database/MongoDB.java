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
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.Blocking;

import java.util.logging.Level;
import java.util.logging.Logger;

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

        disableLogging();

        if (isConnected()) {
            log.info("Connected to mongodb!");
        } else {
            log.error("Could not connect to mongodb!");
            throw new IllegalStateException("Could not connect to mongodb!");
        }
    }

    void disableLogging() {
        //Disable logging of "org.mongodb.driver" package
        Logger mongoLogger = Logger.getLogger(mongoClient.getClass().getPackage().getName());
        mongoLogger.setLevel(Level.WARNING);
        mongoLogger.setUseParentHandlers(false);
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
