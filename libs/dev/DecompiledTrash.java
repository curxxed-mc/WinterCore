package net.curxxed.dev.wintercore.disguise.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.iCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.NMSUtils;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class DefaultDisguiseHandler extends DisguiseHandler {
    private final Map<UUID, String> disguiseRanks = new ConcurrentHashMap<>();

    private final DisguiseRegistry disguiseRegistry;

    public DefaultDisguiseHandler(iCore plugin, DisguiseRegistry disguiseRegistry) {
        super(plugin);
        this.disguiseRegistry = disguiseRegistry;
    }

    public DisguiseCallback disguise(Player player, String rank, String name, String skin) throws Exception {
        // Byte code:
        //   0: aload_1
        //   1: ifnull -> 13
        //   4: aload_1
        //   5: invokeinterface isOnline : ()Z
        //   10: ifne -> 17
        //   13: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.ERROR : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   16: areturn
        //   17: aload_3
        //   18: invokestatic getPlayerExact : (Ljava/lang/String;)Lorg/bukkit/entity/Player;
        //   21: astore #5
        //   23: aload #5
        //   25: ifnull -> 51
        //   28: aload #5
        //   30: invokeinterface getName : ()Ljava/lang/String;
        //   35: aload_1
        //   36: invokeinterface getName : ()Ljava/lang/String;
        //   41: invokevirtual equals : (Ljava/lang/Object;)Z
        //   44: ifne -> 51
        //   47: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.GLOBAL_PLAYER_FOUND : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   50: areturn
        //   51: invokestatic getServerVersion : ()Ljava/lang/String;
        //   54: astore #6
        //   56: aload_1
        //   57: invokeinterface isFlying : ()Z
        //   62: istore #7
        //   64: aload_1
        //   65: invokeinterface getAllowFlight : ()Z
        //   70: istore #8
        //   72: aload_1
        //   73: invokestatic getEntityPlayer : (Lorg/bukkit/entity/Player;)Ljava/lang/Object;
        //   76: astore #9
        //   78: aload #9
        //   80: invokevirtual getClass : ()Ljava/lang/Class;
        //   83: ldc 'getProfile'
        //   85: iconst_0
        //   86: anewarray java/lang/Class
        //   89: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   92: aload #9
        //   94: iconst_0
        //   95: anewarray java/lang/Object
        //   98: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   101: checkcast com/mojang/authlib/GameProfile
        //   104: astore #10
        //   106: new com/google/gson/JsonObject
        //   109: dup
        //   110: invokespecial <init> : ()V
        //   113: astore #11
        //   115: aload #11
        //   117: ldc 'name'
        //   119: aload_1
        //   120: invokeinterface getName : ()Ljava/lang/String;
        //   125: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   128: aload #11
        //   130: ldc 'uuid'
        //   132: aload_1
        //   133: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   138: invokevirtual toString : ()Ljava/lang/String;
        //   141: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   144: new com/google/gson/JsonArray
        //   147: dup
        //   148: invokespecial <init> : ()V
        //   151: astore #12
        //   153: aload #10
        //   155: invokevirtual getProperties : ()Lcom/mojang/authlib/properties/PropertyMap;
        //   158: invokevirtual entries : ()Ljava/util/Collection;
        //   161: aload #12
        //   163: <illegal opcode> accept : (Lcom/google/gson/JsonArray;)Ljava/util/function/Consumer;
        //   168: invokeinterface forEach : (Ljava/util/function/Consumer;)V
        //   173: aload #11
        //   175: ldc 'properties'
        //   177: aload #12
        //   179: invokevirtual add : (Ljava/lang/String;Lcom/google/gson/JsonElement;)V
        //   182: new net/curxxed/dev/wintercore/disguise/player/DisguiseData
        //   185: dup
        //   186: aload_2
        //   187: aload_3
        //   188: aload #4
        //   190: aload #11
        //   192: invokestatic currentTimeMillis : ()J
        //   195: invokespecial <init> : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gson/JsonObject;J)V
        //   198: astore #13
        //   200: aload_0
        //   201: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   204: invokevirtual getDisguiseDataMap : ()Ljava/util/Map;
        //   207: aload_1
        //   208: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   213: aload #13
        //   215: invokeinterface put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        //   220: pop
        //   221: aload_0
        //   222: getfield disguiseRanks : Ljava/util/Map;
        //   225: aload_1
        //   226: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   231: aload_2
        //   232: invokeinterface put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        //   237: pop
        //   238: aconst_null
        //   239: astore #14
        //   241: aload_0
        //   242: aload #4
        //   244: <illegal opcode> get : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Ljava/lang/String;)Ljava/util/function/Supplier;
        //   249: invokestatic supplyAsync : (Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;
        //   252: astore #15
        //   254: aload #15
        //   256: invokevirtual get : ()Ljava/lang/Object;
        //   259: checkcast net/curxxed/dev/wintercore/utils/SkinFetcher$SkinProperty
        //   262: astore #14
        //   264: goto -> 269
        //   267: astore #15
        //   269: ldc 'ewogICJ0aW1lc3RhbXAiIDogMTU5OTkxNzE1OTc4NiwKICAicHJvZmlsZUlkIiA6ICJhZDI1N2Q0ZmJmZjc0YWRhOTY3ZDM0YWZjM2Q5NTcyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGYWNlU2xhcF8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQzYjA2YzM4NTA0ZmZjMDIyOWI5NDkyMTQ3YzY5ZmNmNTlmZDJlZDc4ODVmNzg1MDIxNTJmNzdiNGQ1MGRlMSIKICAgIH0KICB9Cn0='
        //   271: astore #15
        //   273: ldc 'ICq7KLYfdYPI4v3aFxEvpYadhFoYptjKtEhybC4vFnHd081JHiLTuSIqtYPwpqCSkIG+ooUrUMJ/Qka+ieKuOqefmQ+03apVmCeQVnqcYVMyzJTvp69q1Q1TPlc7G/tLgtyF+Ct/E6u/kZ6Dc494VsuXQj6wfLg7+yqqb2Y9PAr2Np91x0AbKithM1vOqvXAcvZRGILp/BAhZ817myXa/CkrvTxFEbiXbD8isWw+tIXLlPi+3Ck5r6KS3tHBGH7/IeY2WM7DN5/vRATfkKGo2F+H6s8IB9t/2bIWG39TKmxYg6wX0daa/FkpEhXb7O61HvhOnpmewKs0b40sK+E5+IC+tx9SlDLsFFeTALjpc2qwOOQ25ITFN4EgdHaP9bO4PGrcIHB7lz7fIRwJSxxHAsxfqc5nzRogy3cXFvsa8pByPGSSdvNzysYN2wGOyIaY+oMXPCfrnGVuno1cJk4L/8noGCX9pLRUd/Ow2WSjTl6zaIfgiEa4d7JWdxdL9/+UQja6oKoQldbMpRTwQPL5uyGbkrirPMNud1s1qaBVrrDUDQoJM0XrYxSF+TtUWRd3kWTN7x7QWdh+8hFECB9H5Kl6k0TyLTSAJkFbKE6aKSLXnSPW7Rb7F/6D3/NRFuDKLDm1exdKBRG3qr0ThB1LhOSE8nOOztETDoPkZJEwWho='
        //   275: astore #16
        //   277: aload #14
        //   279: ifnull -> 296
        //   282: aload #14
        //   284: getfield value : Ljava/lang/String;
        //   287: astore #15
        //   289: aload #14
        //   291: getfield signature : Ljava/lang/String;
        //   294: astore #16
        //   296: aload #10
        //   298: invokevirtual getProperties : ()Lcom/mojang/authlib/properties/PropertyMap;
        //   301: invokevirtual clear : ()V
        //   304: aload #10
        //   306: invokevirtual getProperties : ()Lcom/mojang/authlib/properties/PropertyMap;
        //   309: ldc 'textures'
        //   311: new com/mojang/authlib/properties/Property
        //   314: dup
        //   315: ldc 'textures'
        //   317: aload #15
        //   319: aload #16
        //   321: invokespecial <init> : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   324: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Z
        //   327: pop
        //   328: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   331: aload_0
        //   332: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   335: aload_1
        //   336: <illegal opcode> run : (Lorg/bukkit/entity/Player;)Ljava/lang/Runnable;
        //   341: invokeinterface runTask : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;
        //   346: pop
        //   347: aload_0
        //   348: ldc_w 'PacketPlayOutPlayerInfo'
        //   351: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   354: astore #17
        //   356: aload_0
        //   357: ldc_w 'PacketPlayOutPlayerInfo$EnumPlayerInfoAction'
        //   360: invokevirtual doesClassExists : (Ljava/lang/String;)Z
        //   363: ifeq -> 376
        //   366: aload_0
        //   367: ldc_w 'PacketPlayOutPlayerInfo$EnumPlayerInfoAction'
        //   370: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   373: goto -> 383
        //   376: aload_0
        //   377: ldc_w 'EnumPlayerInfoAction'
        //   380: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   383: astore #18
        //   385: aload #17
        //   387: iconst_2
        //   388: anewarray java/lang/Class
        //   391: dup
        //   392: iconst_0
        //   393: aload #18
        //   395: aastore
        //   396: dup
        //   397: iconst_1
        //   398: ldc_w java/lang/Iterable
        //   401: aastore
        //   402: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   405: astore #19
        //   407: aload #18
        //   409: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   412: iconst_4
        //   413: aaload
        //   414: astore #20
        //   416: aload #19
        //   418: iconst_2
        //   419: anewarray java/lang/Object
        //   422: dup
        //   423: iconst_0
        //   424: aload #20
        //   426: aastore
        //   427: dup
        //   428: iconst_1
        //   429: aload #9
        //   431: invokestatic singleton : (Ljava/lang/Object;)Ljava/util/Set;
        //   434: aastore
        //   435: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   438: astore #21
        //   440: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   443: invokeinterface iterator : ()Ljava/util/Iterator;
        //   448: astore #22
        //   450: aload #22
        //   452: invokeinterface hasNext : ()Z
        //   457: ifeq -> 483
        //   460: aload #22
        //   462: invokeinterface next : ()Ljava/lang/Object;
        //   467: checkcast org/bukkit/entity/Player
        //   470: astore #23
        //   472: aload_0
        //   473: aload #23
        //   475: aload #21
        //   477: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   480: goto -> 450
        //   483: aload_0
        //   484: ldc_w 'PacketPlayOutEntityDestroy'
        //   487: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   490: astore #22
        //   492: aload #22
        //   494: iconst_1
        //   495: anewarray java/lang/Class
        //   498: dup
        //   499: iconst_0
        //   500: ldc_w [I
        //   503: aastore
        //   504: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   507: iconst_1
        //   508: anewarray java/lang/Object
        //   511: dup
        //   512: iconst_0
        //   513: iconst_1
        //   514: newarray int
        //   516: dup
        //   517: iconst_0
        //   518: aload #9
        //   520: invokevirtual getClass : ()Ljava/lang/Class;
        //   523: ldc_w 'getId'
        //   526: iconst_0
        //   527: anewarray java/lang/Class
        //   530: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   533: aload #9
        //   535: iconst_0
        //   536: anewarray java/lang/Object
        //   539: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   542: checkcast java/lang/Integer
        //   545: invokevirtual intValue : ()I
        //   548: iastore
        //   549: aastore
        //   550: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   553: astore #23
        //   555: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   558: invokeinterface iterator : ()Ljava/util/Iterator;
        //   563: astore #24
        //   565: aload #24
        //   567: invokeinterface hasNext : ()Z
        //   572: ifeq -> 598
        //   575: aload #24
        //   577: invokeinterface next : ()Ljava/lang/Object;
        //   582: checkcast org/bukkit/entity/Player
        //   585: astore #25
        //   587: aload_0
        //   588: aload #25
        //   590: aload #23
        //   592: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   595: goto -> 565
        //   598: aload_0
        //   599: aload #10
        //   601: ldc 'name'
        //   603: aload_3
        //   604: invokevirtual changeField : (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
        //   607: aload_0
        //   608: aload #9
        //   610: ldc_w 'displayName'
        //   613: aload_3
        //   614: invokevirtual changeField : (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
        //   617: aload #18
        //   619: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   622: iconst_0
        //   623: aaload
        //   624: astore #24
        //   626: aload #19
        //   628: iconst_2
        //   629: anewarray java/lang/Object
        //   632: dup
        //   633: iconst_0
        //   634: aload #24
        //   636: aastore
        //   637: dup
        //   638: iconst_1
        //   639: aload #9
        //   641: invokestatic singleton : (Ljava/lang/Object;)Ljava/util/Set;
        //   644: aastore
        //   645: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   648: astore #25
        //   650: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   653: invokeinterface iterator : ()Ljava/util/Iterator;
        //   658: astore #26
        //   660: aload #26
        //   662: invokeinterface hasNext : ()Z
        //   667: ifeq -> 693
        //   670: aload #26
        //   672: invokeinterface next : ()Ljava/lang/Object;
        //   677: checkcast org/bukkit/entity/Player
        //   680: astore #27
        //   682: aload_0
        //   683: aload #27
        //   685: aload #25
        //   687: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   690: goto -> 660
        //   693: aload_0
        //   694: ldc_w 'PacketPlayOutNamedEntitySpawn'
        //   697: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   700: astore #26
        //   702: aload_0
        //   703: aload #26
        //   705: iconst_1
        //   706: invokevirtual getConstructorWithParameterExact : (Ljava/lang/Class;I)Ljava/lang/reflect/Constructor;
        //   709: iconst_1
        //   710: anewarray java/lang/Object
        //   713: dup
        //   714: iconst_0
        //   715: aload_0
        //   716: aload #9
        //   718: aload_0
        //   719: ldc_w 'EntityHuman'
        //   722: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   725: invokevirtual safeCastTo : (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;
        //   728: aastore
        //   729: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   732: astore #27
        //   734: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   737: invokeinterface iterator : ()Ljava/util/Iterator;
        //   742: astore #28
        //   744: aload #28
        //   746: invokeinterface hasNext : ()Z
        //   751: ifeq -> 777
        //   754: aload #28
        //   756: invokeinterface next : ()Ljava/lang/Object;
        //   761: checkcast org/bukkit/entity/Player
        //   764: astore #29
        //   766: aload_0
        //   767: aload #29
        //   769: aload #27
        //   771: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   774: goto -> 744
        //   777: aload #6
        //   779: ldc_w '1_8'
        //   782: invokevirtual contains : (Ljava/lang/CharSequence;)Z
        //   785: ifeq -> 839
        //   788: iconst_4
        //   789: anewarray java/lang/Integer
        //   792: dup
        //   793: iconst_0
        //   794: iconst_0
        //   795: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   798: aastore
        //   799: dup
        //   800: iconst_1
        //   801: iconst_1
        //   802: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   805: aastore
        //   806: dup
        //   807: iconst_2
        //   808: iconst_2
        //   809: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   812: aastore
        //   813: dup
        //   814: iconst_3
        //   815: iconst_3
        //   816: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   819: aastore
        //   820: invokestatic of : ([Ljava/lang/Object;)Ljava/util/stream/Stream;
        //   823: aload_0
        //   824: aload #9
        //   826: <illegal opcode> accept : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Ljava/lang/Object;)Ljava/util/function/Consumer;
        //   831: invokeinterface forEach : (Ljava/util/function/Consumer;)V
        //   836: goto -> 1063
        //   839: aload_0
        //   840: ldc_w 'EnumItemSlot'
        //   843: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   846: astore #28
        //   848: aload #28
        //   850: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   853: astore #29
        //   855: aload #29
        //   857: arraylength
        //   858: istore #30
        //   860: iconst_0
        //   861: istore #31
        //   863: iload #31
        //   865: iload #30
        //   867: if_icmpge -> 1063
        //   870: aload #29
        //   872: iload #31
        //   874: aaload
        //   875: astore #32
        //   877: aload_0
        //   878: ldc_w 'PacketPlayOutEntityEquipment'
        //   881: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   884: astore #33
        //   886: aload #33
        //   888: iconst_3
        //   889: anewarray java/lang/Class
        //   892: dup
        //   893: iconst_0
        //   894: getstatic java/lang/Integer.TYPE : Ljava/lang/Class;
        //   897: aastore
        //   898: dup
        //   899: iconst_1
        //   900: aload #28
        //   902: aastore
        //   903: dup
        //   904: iconst_2
        //   905: aload_0
        //   906: ldc_w 'ItemStack'
        //   909: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   912: aastore
        //   913: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   916: iconst_3
        //   917: anewarray java/lang/Object
        //   920: dup
        //   921: iconst_0
        //   922: aload #9
        //   924: invokevirtual getClass : ()Ljava/lang/Class;
        //   927: ldc_w 'getId'
        //   930: iconst_0
        //   931: anewarray java/lang/Class
        //   934: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   937: aload #9
        //   939: iconst_0
        //   940: anewarray java/lang/Object
        //   943: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   946: checkcast java/lang/Integer
        //   949: invokevirtual intValue : ()I
        //   952: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   955: aastore
        //   956: dup
        //   957: iconst_1
        //   958: aload #32
        //   960: aastore
        //   961: dup
        //   962: iconst_2
        //   963: aload_0
        //   964: aload #9
        //   966: invokevirtual getClass : ()Ljava/lang/Class;
        //   969: ldc_w 'getEquipment'
        //   972: iconst_1
        //   973: anewarray java/lang/Class
        //   976: dup
        //   977: iconst_0
        //   978: aload #28
        //   980: aastore
        //   981: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   984: aload #9
        //   986: iconst_1
        //   987: anewarray java/lang/Object
        //   990: dup
        //   991: iconst_0
        //   992: aload #32
        //   994: aastore
        //   995: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   998: aload_0
        //   999: ldc_w 'ItemStack'
        //   1002: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   1005: invokevirtual safeCastTo : (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;
        //   1008: aastore
        //   1009: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   1012: astore #34
        //   1014: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   1017: invokeinterface iterator : ()Ljava/util/Iterator;
        //   1022: astore #35
        //   1024: aload #35
        //   1026: invokeinterface hasNext : ()Z
        //   1031: ifeq -> 1057
        //   1034: aload #35
        //   1036: invokeinterface next : ()Ljava/lang/Object;
        //   1041: checkcast org/bukkit/entity/Player
        //   1044: astore #36
        //   1046: aload_0
        //   1047: aload #36
        //   1049: aload #34
        //   1051: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   1054: goto -> 1024
        //   1057: iinc #31, 1
        //   1060: goto -> 863
        //   1063: aload_1
        //   1064: invokeinterface getInventory : ()Lorg/bukkit/inventory/PlayerInventory;
        //   1069: invokeinterface getHeldItemSlot : ()I
        //   1074: istore #28
        //   1076: aload_0
        //   1077: aload_1
        //   1078: aload #23
        //   1080: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   1083: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   1086: aload_0
        //   1087: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1090: aload_0
        //   1091: aload_1
        //   1092: iload #7
        //   1094: iload #8
        //   1096: iload #28
        //   1098: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;ZZI)Ljava/lang/Runnable;
        //   1103: invokeinterface runTask : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;
        //   1108: pop
        //   1109: aload_0
        //   1110: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1113: invokevirtual getRankManager : ()Lnet/curxxed/dev/wintercore/rank/RankManager;
        //   1116: invokevirtual getRanksSection : ()Lorg/bukkit/configuration/ConfigurationSection;
        //   1119: aload_2
        //   1120: invokeinterface getConfigurationSection : (Ljava/lang/String;)Lorg/bukkit/configuration/ConfigurationSection;
        //   1125: ifnull -> 1158
        //   1128: aload_0
        //   1129: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1132: invokevirtual getRankManager : ()Lnet/curxxed/dev/wintercore/rank/RankManager;
        //   1135: invokevirtual getRanksSection : ()Lorg/bukkit/configuration/ConfigurationSection;
        //   1138: aload_2
        //   1139: invokeinterface getConfigurationSection : (Ljava/lang/String;)Lorg/bukkit/configuration/ConfigurationSection;
        //   1144: ldc_w 'prefix'
        //   1147: ldc_w ''
        //   1150: invokeinterface getString : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
        //   1155: goto -> 1161
        //   1158: ldc_w ''
        //   1161: astore #29
        //   1163: aload_0
        //   1164: aload_0
        //   1165: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1168: invokevirtual getRankManager : ()Lnet/curxxed/dev/wintercore/rank/RankManager;
        //   1171: invokevirtual getRanksSection : ()Lorg/bukkit/configuration/ConfigurationSection;
        //   1174: aload_2
        //   1175: invokeinterface getConfigurationSection : (Ljava/lang/String;)Lorg/bukkit/configuration/ConfigurationSection;
        //   1180: ifnull -> 1213
        //   1183: aload_0
        //   1184: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1187: invokevirtual getRankManager : ()Lnet/curxxed/dev/wintercore/rank/RankManager;
        //   1190: invokevirtual getRanksSection : ()Lorg/bukkit/configuration/ConfigurationSection;
        //   1193: aload_2
        //   1194: invokeinterface getConfigurationSection : (Ljava/lang/String;)Lorg/bukkit/configuration/ConfigurationSection;
        //   1199: ldc_w 'name-color'
        //   1202: ldc_w '&f'
        //   1205: invokeinterface getString : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
        //   1210: goto -> 1216
        //   1213: ldc_w '&f'
        //   1216: invokespecial extractFirstColorChar : (Ljava/lang/String;)C
        //   1219: istore #30
        //   1221: new java/lang/StringBuilder
        //   1224: dup
        //   1225: invokespecial <init> : ()V
        //   1228: ldc_w '&'
        //   1231: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   1234: iload #30
        //   1236: invokevirtual append : (C)Ljava/lang/StringBuilder;
        //   1239: invokevirtual toString : ()Ljava/lang/String;
        //   1242: astore #31
        //   1244: aload_0
        //   1245: getfield disguiseRegistry : Lnet/curxxed/dev/wintercore/disguise/DisguiseRegistry;
        //   1248: aload_1
        //   1249: aload_3
        //   1250: aload_2
        //   1251: aload #31
        //   1253: aload #29
        //   1255: invokevirtual setDisguiseInfo : (Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   1258: aload_0
        //   1259: getfield disguiseRegistry : Lnet/curxxed/dev/wintercore/disguise/DisguiseRegistry;
        //   1262: aload_1
        //   1263: aload_0
        //   1264: aload_1
        //   1265: <illegal opcode> accept : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;)Ljava/util/function/Consumer;
        //   1270: invokevirtual getEffectiveColor : (Lorg/bukkit/entity/Player;Ljava/util/function/Consumer;)V
        //   1273: aload_0
        //   1274: aload_1
        //   1275: aload_3
        //   1276: invokespecial updateTabListAndTeam : (Lorg/bukkit/entity/Player;Ljava/lang/String;)V
        //   1279: new com/google/gson/JsonObject
        //   1282: dup
        //   1283: invokespecial <init> : ()V
        //   1286: astore #32
        //   1288: aload #32
        //   1290: ldc 'name'
        //   1292: aload_3
        //   1293: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   1296: aload #32
        //   1298: ldc_w 'rank'
        //   1301: aload_2
        //   1302: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   1305: aload #32
        //   1307: ldc_w 'skinValue'
        //   1310: aload #15
        //   1312: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   1315: aload #32
        //   1317: ldc_w 'skinSignature'
        //   1320: aload #16
        //   1322: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   1325: aload #32
        //   1327: ldc_w 'color'
        //   1330: aload #31
        //   1332: invokevirtual addProperty : (Ljava/lang/String;Ljava/lang/String;)V
        //   1335: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   1338: aload_0
        //   1339: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1342: aload_0
        //   1343: aload_1
        //   1344: aload #32
        //   1346: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;Lcom/google/gson/JsonObject;)Ljava/lang/Runnable;
        //   1351: invokeinterface runTaskAsynchronously : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;
        //   1356: pop
        //   1357: new net/curxxed/dev/wintercore/events/PlayerDisguiseEvent
        //   1360: dup
        //   1361: aload_1
        //   1362: aload_1
        //   1363: invokeinterface getName : ()Ljava/lang/String;
        //   1368: aload_3
        //   1369: aload_2
        //   1370: invokespecial <init> : (Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   1373: astore #33
        //   1375: invokestatic getPluginManager : ()Lorg/bukkit/plugin/PluginManager;
        //   1378: aload #33
        //   1380: invokeinterface callEvent : (Lorg/bukkit/event/Event;)V
        //   1385: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.SUCCESS : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   1388: areturn
        // Line number table:
        //   Java source line number -> byte code offset
        //   #41	-> 0
        //   #42	-> 13
        //   #44	-> 17
        //   #45	-> 23
        //   #46	-> 47
        //   #48	-> 51
        //   #49	-> 56
        //   #50	-> 64
        //   #51	-> 72
        //   #52	-> 78
        //   #55	-> 106
        //   #56	-> 115
        //   #57	-> 128
        //   #58	-> 144
        //   #59	-> 153
        //   #67	-> 173
        //   #69	-> 182
        //   #70	-> 200
        //   #72	-> 221
        //   #75	-> 238
        //   #77	-> 241
        //   #78	-> 254
        //   #81	-> 264
        //   #79	-> 267
        //   #82	-> 269
        //   #83	-> 273
        //   #84	-> 277
        //   #85	-> 282
        //   #86	-> 289
        //   #88	-> 296
        //   #89	-> 304
        //   #92	-> 328
        //   #101	-> 347
        //   #102	-> 356
        //   #103	-> 370
        //   #104	-> 380
        //   #105	-> 385
        //   #106	-> 407
        //   #107	-> 416
        //   #108	-> 440
        //   #109	-> 472
        //   #110	-> 480
        //   #111	-> 483
        //   #112	-> 492
        //   #113	-> 520
        //   #114	-> 555
        //   #115	-> 587
        //   #116	-> 595
        //   #117	-> 598
        //   #118	-> 607
        //   #119	-> 617
        //   #120	-> 626
        //   #121	-> 650
        //   #122	-> 682
        //   #123	-> 690
        //   #124	-> 693
        //   #125	-> 702
        //   #126	-> 722
        //   #127	-> 734
        //   #128	-> 766
        //   #129	-> 774
        //   #130	-> 777
        //   #131	-> 788
        //   #144	-> 839
        //   #145	-> 848
        //   #146	-> 877
        //   #147	-> 886
        //   #148	-> 924
        //   #149	-> 1014
        //   #150	-> 1046
        //   #151	-> 1054
        //   #145	-> 1057
        //   #154	-> 1063
        //   #155	-> 1076
        //   #156	-> 1083
        //   #164	-> 1109
        //   #165	-> 1132
        //   #166	-> 1163
        //   #167	-> 1187
        //   #166	-> 1216
        //   #168	-> 1221
        //   #170	-> 1244
        //   #172	-> 1258
        //   #187	-> 1273
        //   #188	-> 1279
        //   #189	-> 1288
        //   #190	-> 1296
        //   #191	-> 1305
        //   #192	-> 1315
        //   #193	-> 1325
        //   #195	-> 1335
        //   #196	-> 1357
        //   #197	-> 1375
        //   #198	-> 1385
        // Local variable table:
        //   start	length	slot	name	descriptor
        //   254	10	15	future	Ljava/util/concurrent/CompletableFuture;
        //   472	8	23	online	Lorg/bukkit/entity/Player;
        //   587	8	25	online2	Lorg/bukkit/entity/Player;
        //   682	8	27	online3	Lorg/bukkit/entity/Player;
        //   766	8	29	online4	Lorg/bukkit/entity/Player;
        //   1046	8	36	online5	Lorg/bukkit/entity/Player;
        //   886	171	33	packetPlayOutEntityEquipment	Ljava/lang/Class;
        //   1014	43	34	packetPlayOutEntityEquipmentInitialized	Ljava/lang/Object;
        //   877	180	32	constant	Ljava/lang/Object;
        //   848	215	28	enumSlotsClass	Ljava/lang/Class;
        //   0	1389	0	this	Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;
        //   0	1389	1	player	Lorg/bukkit/entity/Player;
        //   0	1389	2	rank	Ljava/lang/String;
        //   0	1389	3	name	Ljava/lang/String;
        //   0	1389	4	skin	Ljava/lang/String;
        //   23	1366	5	check	Lorg/bukkit/entity/Player;
        //   56	1333	6	version	Ljava/lang/String;
        //   64	1325	7	flying	Z
        //   72	1317	8	allowFlight	Z
        //   78	1311	9	entityPlayer	Ljava/lang/Object;
        //   106	1283	10	gameProfile	Lcom/mojang/authlib/GameProfile;
        //   115	1274	11	data	Lcom/google/gson/JsonObject;
        //   153	1236	12	properties	Lcom/google/gson/JsonArray;
        //   200	1189	13	disguiseData	Lnet/curxxed/dev/wintercore/disguise/player/DisguiseData;
        //   241	1148	14	skinProperty	Lnet/curxxed/dev/wintercore/utils/SkinFetcher$SkinProperty;
        //   273	1116	15	value	Ljava/lang/String;
        //   277	1112	16	signature	Ljava/lang/String;
        //   356	1033	17	packetPlayOutPlayerInfo	Ljava/lang/Class;
        //   385	1004	18	enumPlayerInfoAction	Ljava/lang/Class;
        //   407	982	19	constructor	Ljava/lang/reflect/Constructor;
        //   416	973	20	removePlayerEnum	Ljava/lang/Object;
        //   440	949	21	packetPlayOutPlayerInfoRemoveInitialized	Ljava/lang/Object;
        //   492	897	22	packetPlayOutEntityDestroy	Ljava/lang/Class;
        //   555	834	23	packetPlayOutEntityDestroyInitialized	Ljava/lang/Object;
        //   626	763	24	addPlayerEnum	Ljava/lang/Object;
        //   650	739	25	packetPlayOutPlayerInfoAddInitialized	Ljava/lang/Object;
        //   702	687	26	packetPlayOutNamedEntitySpawn	Ljava/lang/Class;
        //   734	655	27	packetPlayOutNamedEntitySpawnInitialized	Ljava/lang/Object;
        //   1076	313	28	held	I
        //   1163	226	29	rawPrefix	Ljava/lang/String;
        //   1221	168	30	colorChar	C
        //   1244	145	31	colorCode	Ljava/lang/String;
        //   1288	101	32	disguiseJson	Lcom/google/gson/JsonObject;
        //   1375	14	33	event	Lnet/curxxed/dev/wintercore/events/PlayerDisguiseEvent;
        // Local variable type table:
        //   start	length	slot	name	signature
        //   254	10	15	future	Ljava/util/concurrent/CompletableFuture<Lnet/curxxed/dev/wintercore/utils/SkinFetcher$SkinProperty;>;
        //   886	171	33	packetPlayOutEntityEquipment	Ljava/lang/Class<*>;
        //   848	215	28	enumSlotsClass	Ljava/lang/Class<*>;
        //   356	1033	17	packetPlayOutPlayerInfo	Ljava/lang/Class<*>;
        //   385	1004	18	enumPlayerInfoAction	Ljava/lang/Class<*>;
        //   407	982	19	constructor	Ljava/lang/reflect/Constructor<*>;
        //   492	897	22	packetPlayOutEntityDestroy	Ljava/lang/Class<*>;
        //   702	687	26	packetPlayOutNamedEntitySpawn	Ljava/lang/Class<*>;
        // Exception table:
        //   from	to	target	type
        //   241	264	267	java/lang/Exception
    }

    public DisguiseCallback unDisguise(Player player, boolean save) throws Exception {
        // Byte code:
        //   0: aload_0
        //   1: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   4: invokevirtual getDisguiseDataMap : ()Ljava/util/Map;
        //   7: aload_1
        //   8: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   13: invokeinterface get : (Ljava/lang/Object;)Ljava/lang/Object;
        //   18: checkcast net/curxxed/dev/wintercore/disguise/player/DisguiseData
        //   21: astore_3
        //   22: aload_3
        //   23: ifnonnull -> 66
        //   26: aload_0
        //   27: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   30: invokevirtual getNameTagAdapter : ()Lnet/curxxed/dev/wintercore/nametags/NameTagAdapter;
        //   33: aload_1
        //   34: invokeinterface resetNameTag : (Lorg/bukkit/entity/Player;)V
        //   39: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   42: aload_0
        //   43: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   46: aload_0
        //   47: aload_1
        //   48: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;)Ljava/lang/Runnable;
        //   53: ldc2_w 100
        //   56: invokeinterface runTaskLaterAsynchronously : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;J)Lorg/bukkit/scheduler/BukkitTask;
        //   61: pop
        //   62: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.NOT_DISGUISED : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   65: areturn
        //   66: aload_3
        //   67: invokevirtual getInfo : ()Lcom/google/gson/JsonObject;
        //   70: astore #4
        //   72: aload_1
        //   73: invokeinterface isFlying : ()Z
        //   78: istore #5
        //   80: aload_1
        //   81: invokeinterface getAllowFlight : ()Z
        //   86: istore #6
        //   88: aload_1
        //   89: invokestatic getEntityPlayer : (Lorg/bukkit/entity/Player;)Ljava/lang/Object;
        //   92: astore #7
        //   94: aload #7
        //   96: invokevirtual getClass : ()Ljava/lang/Class;
        //   99: ldc 'getProfile'
        //   101: iconst_0
        //   102: anewarray java/lang/Class
        //   105: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   108: aload #7
        //   110: iconst_0
        //   111: anewarray java/lang/Object
        //   114: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   117: checkcast com/mojang/authlib/GameProfile
        //   120: astore #8
        //   122: aconst_null
        //   123: astore #9
        //   125: aconst_null
        //   126: astore #10
        //   128: aload #4
        //   130: ldc 'properties'
        //   132: invokevirtual has : (Ljava/lang/String;)Z
        //   135: ifeq -> 234
        //   138: aload #4
        //   140: ldc 'properties'
        //   142: invokevirtual get : (Ljava/lang/String;)Lcom/google/gson/JsonElement;
        //   145: invokevirtual getAsJsonArray : ()Lcom/google/gson/JsonArray;
        //   148: astore #11
        //   150: aload #11
        //   152: invokevirtual size : ()I
        //   155: ifle -> 234
        //   158: aload #11
        //   160: iconst_0
        //   161: invokevirtual get : (I)Lcom/google/gson/JsonElement;
        //   164: invokevirtual getAsJsonObject : ()Lcom/google/gson/JsonObject;
        //   167: ldc_w 'value'
        //   170: invokevirtual has : (Ljava/lang/String;)Z
        //   173: ifeq -> 234
        //   176: aload #11
        //   178: iconst_0
        //   179: invokevirtual get : (I)Lcom/google/gson/JsonElement;
        //   182: invokevirtual getAsJsonObject : ()Lcom/google/gson/JsonObject;
        //   185: ldc_w 'signature'
        //   188: invokevirtual has : (Ljava/lang/String;)Z
        //   191: ifeq -> 234
        //   194: aload #11
        //   196: iconst_0
        //   197: invokevirtual get : (I)Lcom/google/gson/JsonElement;
        //   200: invokevirtual getAsJsonObject : ()Lcom/google/gson/JsonObject;
        //   203: ldc_w 'value'
        //   206: invokevirtual get : (Ljava/lang/String;)Lcom/google/gson/JsonElement;
        //   209: invokevirtual getAsString : ()Ljava/lang/String;
        //   212: astore #9
        //   214: aload #11
        //   216: iconst_0
        //   217: invokevirtual get : (I)Lcom/google/gson/JsonElement;
        //   220: invokevirtual getAsJsonObject : ()Lcom/google/gson/JsonObject;
        //   223: ldc_w 'signature'
        //   226: invokevirtual get : (Ljava/lang/String;)Lcom/google/gson/JsonElement;
        //   229: invokevirtual getAsString : ()Ljava/lang/String;
        //   232: astore #10
        //   234: aload #9
        //   236: ifnull -> 276
        //   239: aload #10
        //   241: ifnull -> 276
        //   244: aload #8
        //   246: invokevirtual getProperties : ()Lcom/mojang/authlib/properties/PropertyMap;
        //   249: invokevirtual clear : ()V
        //   252: aload #8
        //   254: invokevirtual getProperties : ()Lcom/mojang/authlib/properties/PropertyMap;
        //   257: ldc 'textures'
        //   259: new com/mojang/authlib/properties/Property
        //   262: dup
        //   263: ldc 'textures'
        //   265: aload #9
        //   267: aload #10
        //   269: invokespecial <init> : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   272: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Z
        //   275: pop
        //   276: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   279: aload_0
        //   280: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   283: aload_1
        //   284: <illegal opcode> run : (Lorg/bukkit/entity/Player;)Ljava/lang/Runnable;
        //   289: invokeinterface runTask : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;
        //   294: pop
        //   295: aload_0
        //   296: ldc_w 'PacketPlayOutPlayerInfo'
        //   299: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   302: astore #11
        //   304: aload_0
        //   305: ldc_w 'PacketPlayOutPlayerInfo$EnumPlayerInfoAction'
        //   308: invokevirtual doesClassExists : (Ljava/lang/String;)Z
        //   311: ifeq -> 324
        //   314: aload_0
        //   315: ldc_w 'PacketPlayOutPlayerInfo$EnumPlayerInfoAction'
        //   318: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   321: goto -> 331
        //   324: aload_0
        //   325: ldc_w 'EnumPlayerInfoAction'
        //   328: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   331: astore #12
        //   333: aload #11
        //   335: iconst_2
        //   336: anewarray java/lang/Class
        //   339: dup
        //   340: iconst_0
        //   341: aload #12
        //   343: aastore
        //   344: dup
        //   345: iconst_1
        //   346: ldc_w java/lang/Iterable
        //   349: aastore
        //   350: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   353: astore #13
        //   355: aload #12
        //   357: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   360: iconst_4
        //   361: aaload
        //   362: astore #14
        //   364: aload #13
        //   366: iconst_2
        //   367: anewarray java/lang/Object
        //   370: dup
        //   371: iconst_0
        //   372: aload #14
        //   374: aastore
        //   375: dup
        //   376: iconst_1
        //   377: aload #7
        //   379: invokestatic singleton : (Ljava/lang/Object;)Ljava/util/Set;
        //   382: aastore
        //   383: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   386: astore #15
        //   388: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   391: invokeinterface iterator : ()Ljava/util/Iterator;
        //   396: astore #16
        //   398: aload #16
        //   400: invokeinterface hasNext : ()Z
        //   405: ifeq -> 431
        //   408: aload #16
        //   410: invokeinterface next : ()Ljava/lang/Object;
        //   415: checkcast org/bukkit/entity/Player
        //   418: astore #17
        //   420: aload_0
        //   421: aload #17
        //   423: aload #15
        //   425: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   428: goto -> 398
        //   431: aload_0
        //   432: ldc_w 'PacketPlayOutEntityDestroy'
        //   435: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   438: astore #16
        //   440: aload #16
        //   442: iconst_1
        //   443: anewarray java/lang/Class
        //   446: dup
        //   447: iconst_0
        //   448: ldc_w [I
        //   451: aastore
        //   452: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   455: iconst_1
        //   456: anewarray java/lang/Object
        //   459: dup
        //   460: iconst_0
        //   461: iconst_1
        //   462: newarray int
        //   464: dup
        //   465: iconst_0
        //   466: aload #7
        //   468: invokevirtual getClass : ()Ljava/lang/Class;
        //   471: ldc_w 'getId'
        //   474: iconst_0
        //   475: anewarray java/lang/Class
        //   478: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   481: aload #7
        //   483: iconst_0
        //   484: anewarray java/lang/Object
        //   487: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   490: checkcast java/lang/Integer
        //   493: invokevirtual intValue : ()I
        //   496: iastore
        //   497: aastore
        //   498: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   501: astore #17
        //   503: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   506: invokeinterface iterator : ()Ljava/util/Iterator;
        //   511: astore #18
        //   513: aload #18
        //   515: invokeinterface hasNext : ()Z
        //   520: ifeq -> 546
        //   523: aload #18
        //   525: invokeinterface next : ()Ljava/lang/Object;
        //   530: checkcast org/bukkit/entity/Player
        //   533: astore #19
        //   535: aload_0
        //   536: aload #19
        //   538: aload #17
        //   540: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   543: goto -> 513
        //   546: aload #4
        //   548: ldc 'name'
        //   550: invokevirtual get : (Ljava/lang/String;)Lcom/google/gson/JsonElement;
        //   553: invokevirtual getAsString : ()Ljava/lang/String;
        //   556: astore #18
        //   558: aload_0
        //   559: aload #8
        //   561: ldc 'name'
        //   563: aload #18
        //   565: invokevirtual changeField : (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
        //   568: aload_0
        //   569: aload #7
        //   571: ldc_w 'displayName'
        //   574: aload #18
        //   576: invokevirtual changeField : (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
        //   579: iload_2
        //   580: ifne -> 653
        //   583: new net/curxxed/dev/wintercore/events/PlayerUnDisguiseEvent
        //   586: dup
        //   587: aload_1
        //   588: aload_3
        //   589: invokevirtual getName : ()Ljava/lang/String;
        //   592: aload #18
        //   594: aload_3
        //   595: invokevirtual getRank : ()Ljava/lang/String;
        //   598: invokespecial <init> : (Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   601: astore #19
        //   603: invokestatic getPluginManager : ()Lorg/bukkit/plugin/PluginManager;
        //   606: aload #19
        //   608: invokeinterface callEvent : (Lorg/bukkit/event/Event;)V
        //   613: aload_0
        //   614: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   617: invokevirtual getNameTagAdapter : ()Lnet/curxxed/dev/wintercore/nametags/NameTagAdapter;
        //   620: aload_1
        //   621: invokeinterface resetNameTag : (Lorg/bukkit/entity/Player;)V
        //   626: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   629: aload_0
        //   630: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   633: aload_0
        //   634: aload_1
        //   635: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;)Ljava/lang/Runnable;
        //   640: ldc2_w 100
        //   643: invokeinterface runTaskLaterAsynchronously : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;J)Lorg/bukkit/scheduler/BukkitTask;
        //   648: pop
        //   649: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.SUCCESS : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   652: areturn
        //   653: aload #12
        //   655: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   658: iconst_0
        //   659: aaload
        //   660: astore #19
        //   662: aload #13
        //   664: iconst_2
        //   665: anewarray java/lang/Object
        //   668: dup
        //   669: iconst_0
        //   670: aload #19
        //   672: aastore
        //   673: dup
        //   674: iconst_1
        //   675: aload #7
        //   677: invokestatic singleton : (Ljava/lang/Object;)Ljava/util/Set;
        //   680: aastore
        //   681: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   684: astore #20
        //   686: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   689: invokeinterface iterator : ()Ljava/util/Iterator;
        //   694: astore #21
        //   696: aload #21
        //   698: invokeinterface hasNext : ()Z
        //   703: ifeq -> 729
        //   706: aload #21
        //   708: invokeinterface next : ()Ljava/lang/Object;
        //   713: checkcast org/bukkit/entity/Player
        //   716: astore #22
        //   718: aload_0
        //   719: aload #22
        //   721: aload #20
        //   723: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   726: goto -> 696
        //   729: aload_0
        //   730: ldc_w 'PacketPlayOutNamedEntitySpawn'
        //   733: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   736: astore #21
        //   738: aload_0
        //   739: aload #21
        //   741: iconst_1
        //   742: invokevirtual getConstructorWithParameterExact : (Ljava/lang/Class;I)Ljava/lang/reflect/Constructor;
        //   745: iconst_1
        //   746: anewarray java/lang/Object
        //   749: dup
        //   750: iconst_0
        //   751: aload_0
        //   752: aload #7
        //   754: aload_0
        //   755: ldc_w 'EntityHuman'
        //   758: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   761: invokevirtual safeCastTo : (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;
        //   764: aastore
        //   765: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   768: astore #22
        //   770: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   773: invokeinterface iterator : ()Ljava/util/Iterator;
        //   778: astore #23
        //   780: aload #23
        //   782: invokeinterface hasNext : ()Z
        //   787: ifeq -> 813
        //   790: aload #23
        //   792: invokeinterface next : ()Ljava/lang/Object;
        //   797: checkcast org/bukkit/entity/Player
        //   800: astore #24
        //   802: aload_0
        //   803: aload #24
        //   805: aload #22
        //   807: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   810: goto -> 780
        //   813: invokestatic getServerVersion : ()Ljava/lang/String;
        //   816: astore #23
        //   818: aload #23
        //   820: ldc_w '1_8'
        //   823: invokevirtual contains : (Ljava/lang/CharSequence;)Z
        //   826: ifeq -> 880
        //   829: iconst_4
        //   830: anewarray java/lang/Integer
        //   833: dup
        //   834: iconst_0
        //   835: iconst_0
        //   836: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   839: aastore
        //   840: dup
        //   841: iconst_1
        //   842: iconst_1
        //   843: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   846: aastore
        //   847: dup
        //   848: iconst_2
        //   849: iconst_2
        //   850: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   853: aastore
        //   854: dup
        //   855: iconst_3
        //   856: iconst_3
        //   857: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   860: aastore
        //   861: invokestatic of : ([Ljava/lang/Object;)Ljava/util/stream/Stream;
        //   864: aload_0
        //   865: aload #7
        //   867: <illegal opcode> accept : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Ljava/lang/Object;)Ljava/util/function/Consumer;
        //   872: invokeinterface forEach : (Ljava/util/function/Consumer;)V
        //   877: goto -> 1104
        //   880: aload_0
        //   881: ldc_w 'EnumItemSlot'
        //   884: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   887: astore #24
        //   889: aload #24
        //   891: invokevirtual getEnumConstants : ()[Ljava/lang/Object;
        //   894: astore #25
        //   896: aload #25
        //   898: arraylength
        //   899: istore #26
        //   901: iconst_0
        //   902: istore #27
        //   904: iload #27
        //   906: iload #26
        //   908: if_icmpge -> 1104
        //   911: aload #25
        //   913: iload #27
        //   915: aaload
        //   916: astore #28
        //   918: aload_0
        //   919: ldc_w 'PacketPlayOutEntityEquipment'
        //   922: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   925: astore #29
        //   927: aload #29
        //   929: iconst_3
        //   930: anewarray java/lang/Class
        //   933: dup
        //   934: iconst_0
        //   935: getstatic java/lang/Integer.TYPE : Ljava/lang/Class;
        //   938: aastore
        //   939: dup
        //   940: iconst_1
        //   941: aload #24
        //   943: aastore
        //   944: dup
        //   945: iconst_2
        //   946: aload_0
        //   947: ldc_w 'ItemStack'
        //   950: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   953: aastore
        //   954: invokevirtual getConstructor : ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;
        //   957: iconst_3
        //   958: anewarray java/lang/Object
        //   961: dup
        //   962: iconst_0
        //   963: aload #7
        //   965: invokevirtual getClass : ()Ljava/lang/Class;
        //   968: ldc_w 'getId'
        //   971: iconst_0
        //   972: anewarray java/lang/Class
        //   975: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   978: aload #7
        //   980: iconst_0
        //   981: anewarray java/lang/Object
        //   984: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   987: checkcast java/lang/Integer
        //   990: invokevirtual intValue : ()I
        //   993: invokestatic valueOf : (I)Ljava/lang/Integer;
        //   996: aastore
        //   997: dup
        //   998: iconst_1
        //   999: aload #28
        //   1001: aastore
        //   1002: dup
        //   1003: iconst_2
        //   1004: aload_0
        //   1005: aload #7
        //   1007: invokevirtual getClass : ()Ljava/lang/Class;
        //   1010: ldc_w 'getEquipment'
        //   1013: iconst_1
        //   1014: anewarray java/lang/Class
        //   1017: dup
        //   1018: iconst_0
        //   1019: aload #24
        //   1021: aastore
        //   1022: invokevirtual getMethod : (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
        //   1025: aload #7
        //   1027: iconst_1
        //   1028: anewarray java/lang/Object
        //   1031: dup
        //   1032: iconst_0
        //   1033: aload #28
        //   1035: aastore
        //   1036: invokevirtual invoke : (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        //   1039: aload_0
        //   1040: ldc_w 'ItemStack'
        //   1043: invokevirtual getNMSClass : (Ljava/lang/String;)Ljava/lang/Class;
        //   1046: invokevirtual safeCastTo : (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;
        //   1049: aastore
        //   1050: invokevirtual newInstance : ([Ljava/lang/Object;)Ljava/lang/Object;
        //   1053: astore #30
        //   1055: invokestatic getOnlinePlayers : ()Ljava/util/Collection;
        //   1058: invokeinterface iterator : ()Ljava/util/Iterator;
        //   1063: astore #31
        //   1065: aload #31
        //   1067: invokeinterface hasNext : ()Z
        //   1072: ifeq -> 1098
        //   1075: aload #31
        //   1077: invokeinterface next : ()Ljava/lang/Object;
        //   1082: checkcast org/bukkit/entity/Player
        //   1085: astore #32
        //   1087: aload_0
        //   1088: aload #32
        //   1090: aload #30
        //   1092: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   1095: goto -> 1065
        //   1098: iinc #27, 1
        //   1101: goto -> 904
        //   1104: aload_1
        //   1105: invokeinterface getInventory : ()Lorg/bukkit/inventory/PlayerInventory;
        //   1110: invokeinterface getHeldItemSlot : ()I
        //   1115: istore #24
        //   1117: aload_0
        //   1118: aload_1
        //   1119: aload #17
        //   1121: invokevirtual sendPacket : (Lorg/bukkit/entity/Player;Ljava/lang/Object;)V
        //   1124: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   1127: aload_0
        //   1128: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1131: aload_0
        //   1132: aload_1
        //   1133: iload #5
        //   1135: iload #6
        //   1137: iload #24
        //   1139: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;ZZI)Ljava/lang/Runnable;
        //   1144: invokeinterface runTask : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;
        //   1149: pop
        //   1150: new net/curxxed/dev/wintercore/events/PlayerUnDisguiseEvent
        //   1153: dup
        //   1154: aload_1
        //   1155: aload_3
        //   1156: invokevirtual getName : ()Ljava/lang/String;
        //   1159: aload #18
        //   1161: aload_3
        //   1162: invokevirtual getRank : ()Ljava/lang/String;
        //   1165: invokespecial <init> : (Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
        //   1168: astore #25
        //   1170: invokestatic getPluginManager : ()Lorg/bukkit/plugin/PluginManager;
        //   1173: aload #25
        //   1175: invokeinterface callEvent : (Lorg/bukkit/event/Event;)V
        //   1180: aload_0
        //   1181: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1184: invokevirtual getDisguiseDataMap : ()Ljava/util/Map;
        //   1187: aload_1
        //   1188: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   1193: invokeinterface remove : (Ljava/lang/Object;)Ljava/lang/Object;
        //   1198: pop
        //   1199: aload_0
        //   1200: getfield disguiseRanks : Ljava/util/Map;
        //   1203: aload_1
        //   1204: invokeinterface getUniqueId : ()Ljava/util/UUID;
        //   1209: invokeinterface remove : (Ljava/lang/Object;)Ljava/lang/Object;
        //   1214: pop
        //   1215: invokestatic getScheduler : ()Lorg/bukkit/scheduler/BukkitScheduler;
        //   1218: aload_0
        //   1219: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1222: aload_0
        //   1223: aload_1
        //   1224: <illegal opcode> run : (Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;Lorg/bukkit/entity/Player;)Ljava/lang/Runnable;
        //   1229: ldc2_w 100
        //   1232: invokeinterface runTaskLaterAsynchronously : (Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;J)Lorg/bukkit/scheduler/BukkitTask;
        //   1237: pop
        //   1238: aload_0
        //   1239: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1242: invokevirtual getNameTagAdapter : ()Lnet/curxxed/dev/wintercore/nametags/NameTagAdapter;
        //   1245: ifnull -> 1264
        //   1248: aload_0
        //   1249: getfield plugin : Lnet/curxxed/dev/wintercore/plugin/WinterCore;
        //   1252: invokevirtual getNameTagAdapter : ()Lnet/curxxed/dev/wintercore/nametags/NameTagAdapter;
        //   1255: aload_1
        //   1256: invokeinterface resetNameTag : (Lorg/bukkit/entity/Player;)V
        //   1261: goto -> 1295
        //   1264: getstatic java/lang/System.out : Ljava/io/PrintStream;
        //   1267: new java/lang/StringBuilder
        //   1270: dup
        //   1271: invokespecial <init> : ()V
        //   1274: ldc_w '[WinterCore] NameTagAdapter is null! Cannot reset name tag for '
        //   1277: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   1280: aload_1
        //   1281: invokeinterface getName : ()Ljava/lang/String;
        //   1286: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   1289: invokevirtual toString : ()Ljava/lang/String;
        //   1292: invokevirtual println : (Ljava/lang/String;)V
        //   1295: getstatic net/curxxed/dev/wintercore/disguise/callback/DisguiseCallback.SUCCESS : Lnet/curxxed/dev/wintercore/disguise/callback/DisguiseCallback;
        //   1298: areturn
        // Line number table:
        //   Java source line number -> byte code offset
        //   #203	-> 0
        //   #204	-> 22
        //   #205	-> 26
        //   #206	-> 39
        //   #207	-> 62
        //   #209	-> 66
        //   #210	-> 72
        //   #211	-> 80
        //   #212	-> 88
        //   #213	-> 94
        //   #216	-> 122
        //   #217	-> 128
        //   #218	-> 138
        //   #219	-> 150
        //   #220	-> 194
        //   #221	-> 214
        //   #224	-> 234
        //   #225	-> 244
        //   #226	-> 252
        //   #230	-> 276
        //   #239	-> 295
        //   #240	-> 304
        //   #241	-> 318
        //   #242	-> 328
        //   #243	-> 333
        //   #244	-> 355
        //   #245	-> 364
        //   #246	-> 388
        //   #247	-> 420
        //   #248	-> 428
        //   #249	-> 431
        //   #250	-> 440
        //   #251	-> 468
        //   #252	-> 503
        //   #253	-> 535
        //   #254	-> 543
        //   #255	-> 546
        //   #256	-> 558
        //   #257	-> 568
        //   #259	-> 579
        //   #260	-> 583
        //   #261	-> 603
        //   #262	-> 613
        //   #263	-> 626
        //   #264	-> 649
        //   #267	-> 653
        //   #268	-> 662
        //   #269	-> 686
        //   #270	-> 718
        //   #271	-> 726
        //   #272	-> 729
        //   #273	-> 738
        //   #274	-> 758
        //   #275	-> 770
        //   #276	-> 802
        //   #277	-> 810
        //   #278	-> 813
        //   #279	-> 818
        //   #280	-> 829
        //   #293	-> 880
        //   #294	-> 889
        //   #295	-> 918
        //   #296	-> 927
        //   #297	-> 965
        //   #298	-> 1055
        //   #299	-> 1087
        //   #300	-> 1095
        //   #294	-> 1098
        //   #303	-> 1104
        //   #304	-> 1117
        //   #305	-> 1124
        //   #312	-> 1150
        //   #313	-> 1170
        //   #314	-> 1180
        //   #316	-> 1199
        //   #318	-> 1215
        //   #319	-> 1238
        //   #320	-> 1248
        //   #322	-> 1264
        //   #324	-> 1295
        // Local variable table:
        //   start	length	slot	name	descriptor
        //   150	84	11	properties	Lcom/google/gson/JsonArray;
        //   420	8	17	online	Lorg/bukkit/entity/Player;
        //   535	8	19	online2	Lorg/bukkit/entity/Player;
        //   603	50	19	event	Lnet/curxxed/dev/wintercore/events/PlayerUnDisguiseEvent;
        //   718	8	22	online3	Lorg/bukkit/entity/Player;
        //   802	8	24	online4	Lorg/bukkit/entity/Player;
        //   1087	8	32	online5	Lorg/bukkit/entity/Player;
        //   927	171	29	packetPlayOutEntityEquipment	Ljava/lang/Class;
        //   1055	43	30	packetPlayOutEntityEquipmentInitialized	Ljava/lang/Object;
        //   918	180	28	constant	Ljava/lang/Object;
        //   889	215	24	enumSlotsClass	Ljava/lang/Class;
        //   0	1299	0	this	Lnet/curxxed/dev/wintercore/disguise/impl/DefaultDisguiseHandler;
        //   0	1299	1	player	Lorg/bukkit/entity/Player;
        //   0	1299	2	save	Z
        //   22	1277	3	disguiseData	Lnet/curxxed/dev/wintercore/disguise/player/DisguiseData;
        //   72	1227	4	profileData	Lcom/google/gson/JsonObject;
        //   80	1219	5	flying	Z
        //   88	1211	6	allowFlight	Z
        //   94	1205	7	entityPlayer	Ljava/lang/Object;
        //   122	1177	8	gameProfile	Lcom/mojang/authlib/GameProfile;
        //   125	1174	9	value	Ljava/lang/String;
        //   128	1171	10	signature	Ljava/lang/String;
        //   304	995	11	packetPlayOutPlayerInfo	Ljava/lang/Class;
        //   333	966	12	enumPlayerInfoAction	Ljava/lang/Class;
        //   355	944	13	constructor	Ljava/lang/reflect/Constructor;
        //   364	935	14	removePlayerEnum	Ljava/lang/Object;
        //   388	911	15	packetPlayOutPlayerInfoRemoveInitialized	Ljava/lang/Object;
        //   440	859	16	packetPlayOutEntityDestroy	Ljava/lang/Class;
        //   503	796	17	packetPlayOutEntityDestroyInitialized	Ljava/lang/Object;
        //   558	741	18	name	Ljava/lang/String;
        //   662	637	19	addPlayerEnum	Ljava/lang/Object;
        //   686	613	20	packetPlayOutPlayerInfoAddInitialized	Ljava/lang/Object;
        //   738	561	21	packetPlayOutNamedEntitySpawn	Ljava/lang/Class;
        //   770	529	22	packetPlayOutNamedEntitySpawnInitialized	Ljava/lang/Object;
        //   818	481	23	version	Ljava/lang/String;
        //   1117	182	24	held	I
        //   1170	129	25	event2	Lnet/curxxed/dev/wintercore/events/PlayerUnDisguiseEvent;
        // Local variable type table:
        //   start	length	slot	name	signature
        //   927	171	29	packetPlayOutEntityEquipment	Ljava/lang/Class<*>;
        //   889	215	24	enumSlotsClass	Ljava/lang/Class<*>;
        //   304	995	11	packetPlayOutPlayerInfo	Ljava/lang/Class<*>;
        //   333	966	12	enumPlayerInfoAction	Ljava/lang/Class<*>;
        //   355	944	13	constructor	Ljava/lang/reflect/Constructor<*>;
        //   440	859	16	packetPlayOutEntityDestroy	Ljava/lang/Class<*>;
        //   738	561	21	packetPlayOutNamedEntitySpawn	Ljava/lang/Class<*>;
    }

    public DisguiseCallback disguise(Player player, String targetName) throws Exception {
        String version = NMSUtils.getServerVersion();
        if (!version.startsWith("v1_7") && !version.startsWith("v1_8"))
            return DisguiseCallback.ERROR;
        if (player == null || !player.isOnline())
            return DisguiseCallback.NOT_ONLINE;
        if (targetName.equalsIgnoreCase(player.getName()))
            return DisguiseCallback.SAME_NAME;
        Player check = Bukkit.getPlayerExact(targetName);
        if (check != null && !check.getName().equals(player.getName()))
            return DisguiseCallback.GLOBAL_PLAYER_FOUND;
        SkinFetcher.SkinProperty skinProperty = fetchSkinData(targetName);
        if (skinProperty == null)
            return DisguiseCallback.ERROR;
        if (!this.plugin.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            Object entityPlayer = NMSUtils.getEntityPlayer(player);
            GameProfile gameProfile = (GameProfile)entityPlayer.getClass().getMethod("getProfile", new Class[0]).invoke(entityPlayer, new Object[0]);
            JsonObject data = new JsonObject();
            data.addProperty("name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            JsonArray properties = new JsonArray();
            gameProfile.getProperties().entries().forEach(entry -> {
                JsonObject object = new JsonObject();
                object.addProperty("key", (String)entry.getKey());
                object.addProperty("value-name", ((Property)entry.getValue()).getName());
                object.addProperty("value", ((Property)entry.getValue()).getValue());
                object.addProperty("signature", ((Property)entry.getValue()).getSignature());
                properties.add((JsonElement)object);
            });
            data.add("properties", (JsonElement)properties);
            this.plugin.getDisguiseDataMap().put(player.getUniqueId(), new DisguiseData("", player.getName(), player.getName(), data, System.currentTimeMillis()));
        }
        return disguise(player, "", targetName, targetName);
    }

    public DisguiseCallback undisguise(Player player) throws Exception {
        String version = NMSUtils.getServerVersion();
        if (!version.startsWith("v1_7") && !version.startsWith("v1_8"))
            return DisguiseCallback.ERROR;
        if (player == null || !player.isOnline())
            return DisguiseCallback.NOT_ONLINE;
        return unDisguise(player, true);
    }

    private char extractFirstColorChar(String input) {
        if (input == null)
            return 'f';
        for (int i = 0; i < input.length() - 1; i++) {
            char c = input.charAt(i);
            char code = input.charAt(i + 1);
            if ((c == '|| c == '&') && ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F')))
            return code;
        }
        return 'f';
    }

    private void updateTabListAndTeam(Player player, String name) {
        this.disguiseRegistry.getEffectiveColor(player, color -> {
            if (player == null || !player.isOnline())
                return;
            char colorChar = extractFirstColorChar(color);
            ChatColor chatColor = ChatColor.getByChar(colorChar);
            if (chatColor == null)
                chatColor = ChatColor.WHITE;
            if (this.plugin.getNameTagAdapter() != null)
                this.plugin.getNameTagAdapter().setNameTag(player, "", chatColor);
        });
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getNameTagAdapter().resetNameTag(player);
        Bukkit.getScheduler().runTaskLaterAsynchronously((Plugin)this.plugin, () -> this.plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
    }
}
