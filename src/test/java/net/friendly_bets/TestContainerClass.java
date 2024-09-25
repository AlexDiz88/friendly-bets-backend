package net.friendly_bets;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestContainerClass {
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.0");
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;


    @BeforeAll
    public void setup() {
        String uri = mongoDBContainer.getConnectionString();
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase("testDB");
        collection = database.getCollection("testCollection");

        // Insert sample data
        Document doc1 = new Document("title", "Inception").append("imdb", new Document("rating", 8.8));
        Document doc2 = new Document("title", "The Room").append("imdb", new Document("rating", 3.7));
        Document doc3 = new Document("title", "The Dark Knight").append("imdb", new Document("rating", 9.0));

        collection.insertMany(List.of(doc1, doc2, doc3));
    }

    @Test
    void testMoviesWithHighRating() {
        List<Document> resultDocuments = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(Filters.gt("imdb.rating", 7))
                .projection(new Document("title", 1).append("_id", 0))
                .limit(5)
                .iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                System.out.println(doc.toJson());
                resultDocuments.add(doc);
            }
        }

        assertEquals(2, resultDocuments.size());
        for (Document doc : resultDocuments) {
            assertTrue(doc.containsKey("title"));
            assertFalse(doc.containsKey("_id"));
        }
    }

    @AfterAll
    public void cleanup() {
        mongoClient.close();
    }
}
