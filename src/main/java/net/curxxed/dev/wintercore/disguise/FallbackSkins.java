package net.curxxed.dev.wintercore.disguise;

import java.util.concurrent.ThreadLocalRandom;

public final class FallbackSkins {

    private static final String[] NAMES = {
            "Notch", "jeb_", "Dinnerbone", "Grumm",
            "MHF_Steve", "MHF_Alex", "ANDERZ", "AntVenom",
            "Aureylian", "Avidya", "BadBoyHalo", "Baj",
            "BdoubleO100", "BebopVox", "BlameTC", "Bopogamel",
            "CaptainSparklez", "Chaosflo44", "ConCrafter", "Cyclone",
            "DanTDM", "Dner", "Docm77", "Dream",
            "EvilSeph", "GeminiTay", "GeorgeNotFound", "GermanLetsPlay",
            "Goodtimes", "Grian", "Gronkh", "Guude",
            "Herr_Bergmann", "Honeydew", "InTheLittleWood", "JL2579",
            "JoeHills", "KarlJacobs", "Keralis", "KrisJelbring",
            "LPmitKev", "MCGamer", "MHF_ArrowDown", "MHF_ArrowLeft",
            "MHF_ArrowRight", "MHF_ArrowUp", "MHF_Blaze", "MHF_Cactus",
            "MHF_Cake", "MHF_CaveSpider", "MHF_Chest", "MHF_Chicken",
            "MHF_CoconutB", "MHF_CoconutG", "MHF_Cow", "MHF_Creeper",
            "MHF_Enderman", "MHF_Exclamation", "MHF_Ghast", "MHF_Golem",
            "MHF_Herobrine", "MHF_LavaSlime", "MHF_Melon", "MHF_MushroomCow",
            "MHF_OakLog", "MHF_Ocelot", "MHF_Pig", "MHF_PigZombie",
            "MHF_Present1", "MHF_Present2", "MHF_Pumpkin", "MHF_Question",
            "MHF_Sheep", "MHF_Skeleton", "MHF_Slime", "MHF_Spider",
            "MHF_Squid", "MHF_TNT", "MHF_TNT2", "MHF_Villager",
            "MHF_WSkeleton", "MHF_Zombie", "Marc_IRL", "MumboJumbo",
            "Nebris", "Pahimar", "Paluten", "Papaplatte",
            "PauseUnpause", "PearlescentMoon", "Ph1LzA", "ProfMobius",
            "Pyrao", "Quackity", "Ranboo", "Rendog",
            "Sapnap", "Searge", "SethBling", "Sevadus",
    };

    private FallbackSkins() {
    }

    public static String random() {
        return NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
    }
}
