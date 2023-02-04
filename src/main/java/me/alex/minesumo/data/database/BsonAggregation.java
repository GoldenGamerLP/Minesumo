package me.alex.minesumo.data.database;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;

public final class BsonAggregation {
    public static Bson RANKING = Aggregates.project(Projections.fields(
            Projections.include("lastName", "playerID"),
            Projections.computed("ratio", new Document("$cond", Arrays.asList(
                    new Document("$and", Arrays.asList(
                            new Document("$gte", Arrays.asList("$kills", 1)),
                            new Document("$gte", Arrays.asList("$deaths", 2))
                    )),
                    new Document("$divide", Arrays.asList("$kills", "$deaths")),
                    0.0
            )))
    ));

    public static Bson SORT_KD = new Document("$sort", new Document("ratio", -1));

    public static Bson INDEX_FROM_KD(double kd) {
        return new Document("$group", new Document()
                .append("_id", null)
                .append("count", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$lt", Arrays.asList("$ratio", kd)),
                                1,
                                0
                        )))
                )
        );
    }
}
