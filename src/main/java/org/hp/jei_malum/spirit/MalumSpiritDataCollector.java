package org.hp.jei_malum.spirit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class MalumSpiritDataCollector {
    private static final String SPIRIT_DATA_PATH = "spirit_data/entity";
    private static final String MALUM_SPIRIT_DATA_ROOT = "data/malum/spirit_data/entity/";
    private static final String MALUM_MOD_CLASS = "com.sammy.malum.MalumMod";
    private static final String MALUM = "malum";

    private MalumSpiritDataCollector() {
    }

    public static List<SpiritHarvestRecipe> collectRecipes() {
        Map<ResourceLocation, ParsedEntitySpiritData> entityData = new HashMap<>();
        Set<ResourceLocation> entitiesWithoutSpirits = new HashSet<>();

        parseMalumModResources(entityData, entitiesWithoutSpirits);
        parseClientResources(entityData, entitiesWithoutSpirits);

        addDefaultFallbacks(entityData, entitiesWithoutSpirits);

        Map<ResourceLocation, List<SpiritDropEntry>> entriesBySpirit = new LinkedHashMap<>();
        entityData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addEntriesForEntity(entriesBySpirit, entry.getKey(), entry.getValue()));

        return entriesBySpirit.entrySet().stream()
                .map(entry -> createRecipe(entry.getKey(), entry.getValue()))
                .filter(recipe -> !recipe.spiritStack().isEmpty())
                .sorted(Comparator.comparing(recipe -> recipe.spiritId().toString()))
                .toList();
    }

    private static void parseClientResources(Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        resourceManager.listResources(SPIRIT_DATA_PATH, id -> id.getPath().endsWith(".json")).values()
                .forEach(resource -> parseResource(resource, entityData, entitiesWithoutSpirits));
    }

    private static void parseMalumModResources(Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        Path malumPath = getMalumCodeSourcePath();
        if (malumPath == null) {
            return;
        }

        try {
            if (Files.isDirectory(malumPath)) {
                parseMalumDirectory(malumPath, entityData, entitiesWithoutSpirits);
            } else {
                parseMalumJar(malumPath, entityData, entitiesWithoutSpirits);
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getMalumCodeSourcePath() {
        try {
            Class<?> malumClass = Class.forName(MALUM_MOD_CLASS, false, Thread.currentThread().getContextClassLoader());
            CodeSource codeSource = malumClass.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URL location = codeSource.getLocation();
            URI uri = location.toURI();
            return Path.of(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void parseMalumDirectory(Path malumPath, Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        Path spiritDataPath = malumPath.resolve(MALUM_SPIRIT_DATA_ROOT);
        if (!Files.isDirectory(spiritDataPath)) {
            return;
        }

        try (Stream<Path> files = Files.walk(spiritDataPath)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            parseReader(reader, entityData, entitiesWithoutSpirits);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static void parseMalumJar(Path malumPath, Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        try (JarFile jarFile = new JarFile(malumPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(MALUM_SPIRIT_DATA_ROOT))
                    .filter(entry -> entry.getName().endsWith(".json"))
                    .forEach(entry -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                            parseReader(reader, entityData, entitiesWithoutSpirits);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static void parseResource(Resource resource, Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        try (BufferedReader reader = resource.openAsReader()) {
            parseReader(reader, entityData, entitiesWithoutSpirits);
        } catch (Exception ignored) {
        }
    }

    private static void parseReader(BufferedReader reader, Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
        ResourceLocation entityId = ResourceLocation.tryParse(object.getAsJsonPrimitive("registry_name").getAsString());
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            return;
        }

        if (object.has("no_spirits") && object.get("no_spirits").getAsBoolean()) {
            entityData.remove(entityId);
            entitiesWithoutSpirits.add(entityId);
            return;
        }

        if (!object.has("primary_type") || object.getAsJsonPrimitive("primary_type").getAsString().equals("none")) {
            return;
        }

        List<ParsedSpiritStack> spirits = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("spirits")) {
            JsonObject spiritObject = element.getAsJsonObject();
            ResourceLocation spiritType = parseSpiritType(spiritObject.getAsJsonPrimitive("spirit").getAsString());
            int count = spiritObject.has("count") ? spiritObject.getAsJsonPrimitive("count").getAsInt() : 1;
            if (spiritType != null && count > 0) {
                spirits.add(new ParsedSpiritStack(spiritType, count));
            }
        }

        if (!spirits.isEmpty()) {
            entityData.put(entityId, new ParsedEntitySpiritData(spirits, false));
            entitiesWithoutSpirits.remove(entityId);
        }
    }

    private static void addDefaultFallbacks(Map<ResourceLocation, ParsedEntitySpiritData> entityData, Set<ResourceLocation> entitiesWithoutSpirits) {
        BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(entry -> entry.getValue().canSummon())
                .forEach(entry -> {
                    ResourceLocation entityId = entry.getKey().location();
                    if (entityData.containsKey(entityId) || entitiesWithoutSpirits.contains(entityId)) {
                        return;
                    }

                    List<ParsedSpiritStack> fallback = getFallbackSpirits(entry.getValue());
                    if (!fallback.isEmpty()) {
                        entityData.put(entityId, new ParsedEntitySpiritData(fallback, true));
                    }
                });
    }

    private static List<ParsedSpiritStack> getFallbackSpirits(EntityType<?> entityType) {
        if (getDefaultMaxHealth(entityType) >= 60F) {
            return List.of(new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "eldritch"), 4));
        }

        MobCategory category = entityType.getCategory();
        return switch (category) {
            case MONSTER -> List.of(new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "wicked"), 1));
            case CREATURE -> List.of(new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "sacred"), 1));
            case AMBIENT -> List.of(new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "aerial"), 1));
            case AXOLOTLS -> List.of(
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "aqueous"), 2),
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "sacred"), 1)
            );
            case UNDERGROUND_WATER_CREATURE -> List.of(
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "aqueous"), 1),
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "earthen"), 1)
            );
            case WATER_CREATURE -> List.of(
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "aqueous"), 1),
                    new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "sacred"), 1)
            );
            case WATER_AMBIENT -> List.of(new ParsedSpiritStack(ResourceLocation.fromNamespaceAndPath(MALUM, "aqueous"), 1));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private static double getDefaultMaxHealth(EntityType<?> entityType) {
        if (!LivingEntity.class.isAssignableFrom(entityType.getBaseClass()) || !DefaultAttributes.hasSupplier(entityType)) {
            return 0;
        }
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType).getBaseValue(Attributes.MAX_HEALTH);
    }

    private static void addEntriesForEntity(Map<ResourceLocation, List<SpiritDropEntry>> entriesBySpirit, ResourceLocation entityId, ParsedEntitySpiritData data) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        List<ItemStack> spawnEggs = getSpawnEggs(entityType);

        for (ParsedSpiritStack spirit : data.spirits()) {
            ResourceLocation spiritItemId = spiritTypeToItemId(spirit.spiritType());
            entriesBySpirit.computeIfAbsent(spiritItemId, key -> new ArrayList<>())
                    .add(new SpiritDropEntry(
                            entityId,
                            entityType,
                            entityType.getDescription(),
                            spawnEggs,
                            spirit.count(),
                            data.fallback()
                    ));
        }
    }

    private static ResourceLocation spiritTypeToItemId(ResourceLocation spiritType) {
        return ResourceLocation.fromNamespaceAndPath(spiritType.getNamespace(), spiritType.getPath() + "_spirit");
    }

    private static ResourceLocation parseSpiritType(String spirit) {
        if (spirit.indexOf(':') < 0) {
            return ResourceLocation.fromNamespaceAndPath(MALUM, spirit);
        }
        return ResourceLocation.tryParse(spirit);
    }

    private static List<ItemStack> getSpawnEggs(EntityType<?> entityType) {
        SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);
        if (spawnEgg == null) {
            return List.of();
        }
        return List.of(new ItemStack(spawnEgg));
    }

    private static SpiritHarvestRecipe createRecipe(ResourceLocation spiritId, List<SpiritDropEntry> entries) {
        Item spiritItem = BuiltInRegistries.ITEM.get(spiritId);
        ItemStack spiritStack = BuiltInRegistries.ITEM.containsKey(spiritId) ? new ItemStack(spiritItem) : ItemStack.EMPTY;
        List<SpiritDropEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparingInt(SpiritDropEntry::count).reversed()
                        .thenComparing(entry -> entry.entityId().toString()))
                .toList();
        return new SpiritHarvestRecipe(spiritId, spiritStack, sortedEntries);
    }

    private record ParsedEntitySpiritData(List<ParsedSpiritStack> spirits, boolean fallback) {
    }

    private record ParsedSpiritStack(ResourceLocation spiritType, int count) {
    }
}


