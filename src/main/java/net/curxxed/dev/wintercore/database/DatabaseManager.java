package net.curxxed.dev.wintercore.database;

import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.curxxed.dev.wintercore.database.cache.TagCacheService;
import net.curxxed.dev.wintercore.database.mongo.MongoConnection;
import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.database.service.IdentityService;
import net.curxxed.dev.wintercore.database.service.CurrencyService;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.database.service.ProfileService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.database.cache.RankCacheService;

public final class DatabaseManager implements AutoCloseable {

    private final MongoConnection mongo;
    private final ProfileRepository profiles;

    @Getter private final RankCacheService rankCache;
    @Getter private final TagCacheService tagCache;

    @Getter private final ProfileService profileService;
    @Getter private final CurrencyService currencyService;
    @Getter private final IdentityService identityService;
    @Getter private final ModerationService moderationService;

    @Getter private final ProfileRepository profileRepository;

    public DatabaseManager(WinterCore plugin) {
        this.mongo = new MongoConnection(plugin);
        this.profiles = new ProfileRepository(mongo.collection("profiles"));

        this.rankCache = new RankCacheService(plugin, profiles);
        this.tagCache = new TagCacheService(plugin, profiles);

        this.identityService = new IdentityService(plugin, profiles);
        this.profileService = new ProfileService(plugin, profiles, rankCache, tagCache);
        this.currencyService = new CurrencyService(plugin, profiles);
        this.moderationService = new ModerationService(plugin, profiles, identityService);

        this.profileRepository = profiles;
    }

    @Override
    public void close() {
        mongo.close();
    }

    public MongoDatabase getMongoDatabase() {
        return mongo.getDatabase();
    }

    public void ping() {
        mongo.ping();
    }
}
