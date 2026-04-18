package net.curxxed.dev.wintercore.database;

import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.curxxed.dev.wintercore.database.cache.TagCacheService;
import net.curxxed.dev.wintercore.database.mongo.MongoConnectionManager;
import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.database.service.IdentityService;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.database.service.ProfileService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.database.cache.RankCacheService;

public final class DatabaseManager implements AutoCloseable {

    @Getter
    private static DatabaseManager instance;

    private final MongoConnectionManager mongo;
    private final ProfileRepository profiles;

    @Getter private final RankCacheService rankCache;
    @Getter private final TagCacheService tagCache;

    @Getter private final ProfileService profileService;
    @Getter private final IdentityService identityService;
    @Getter private final ModerationService moderationService;

    public DatabaseManager(WinterCore plugin) {
        this.mongo = new MongoConnectionManager(plugin);
        this.profiles = new ProfileRepository(mongo.collection("profiles"));

        this.rankCache = new RankCacheService(plugin, profiles);
        this.tagCache = new TagCacheService(plugin, profiles);

        this.identityService = new IdentityService(plugin, profiles);
        this.profileService = new ProfileService(plugin, profiles, rankCache, tagCache);
        this.moderationService = new ModerationService(plugin, profiles, identityService);
    }

    public static DatabaseManager init(WinterCore plugin) {
        instance = new DatabaseManager(plugin);
        return instance;
    }

    @Override
    public void close() {
        mongo.close();
    }

    public MongoDatabase getMongoDatabase() {
        return mongo.getDatabase();
    }
}