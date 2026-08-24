package net.friendly_bets.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Full-database snapshot in mongorestore directory layout, packed as zip:
 * {@code <db>/<collection>.bson} + {@code <db>/<collection>.metadata.json}.
 */
@Component
public class MongoDatabaseDumpWriter {

    private final MongoTemplate mongoTemplate;

    public MongoDatabaseDumpWriter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public byte[] writeZip() throws IOException {
        MongoDatabase database = mongoTemplate.getDb();
        String dbName = database.getName();
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            for (Document collectionInfo : database.listCollections()) {
                String type = collectionInfo.getString("type");
                if (type != null && !"collection".equals(type)) {
                    continue;
                }
                String name = collectionInfo.getString("name");
                if (name == null || name.startsWith("system.")) {
                    continue;
                }
                MongoCollection<RawBsonDocument> collection = database.getCollection(name, RawBsonDocument.class);
                zip.putNextEntry(new ZipEntry(dbName + "/" + name + ".bson"));
                try (MongoCursor<RawBsonDocument> cursor = collection.find().iterator()) {
                    while (cursor.hasNext()) {
                        zip.write(rawBytes(cursor.next()));
                    }
                }
                zip.closeEntry();

                List<Document> indexes = database.getCollection(name).listIndexes().into(new ArrayList<>());
                Document metadata = new Document()
                        .append("collectionName", name)
                        .append("type", "collection")
                        .append("indexes", indexes);
                zip.putNextEntry(new ZipEntry(dbName + "/" + name + ".metadata.json"));
                zip.write(metadata.toJson().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return zipBytes.toByteArray();
    }

    private static byte[] rawBytes(RawBsonDocument raw) {
        ByteBuffer buffer = raw.getByteBuffer().asNIO();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
