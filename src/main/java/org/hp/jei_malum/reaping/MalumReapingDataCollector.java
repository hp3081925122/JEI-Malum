package org.hp.jei_malum.reaping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class MalumReapingDataCollector {
    private static final String REAPING_DATA_PATH = "reaping_data";
    private static final String MALUM_REAPING_DATA_ROOT = "data/malum/reaping_data/";
    private static final String MALUM_MOD_CLASS = "com.sammy.malum.MalumMod";

    private MalumReapingDataCollector() {
    }

    public static List<ReapingDropsRecipe> collectRecipes() {
        Map<ResourceLocation, List<ParsedReapingDrop>> entityData = new HashMap<>();

        parseMalumModResources(entityData);
        parseClientResources(entityData);

        Map<ResourceLocation, List<ReapingDropEntry>> entriesByDrop = new LinkedHashMap<>();
        entityData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addEntriesForEntity(entriesByDrop, entry.getKey(), entry.getValue()));

        return entriesByDrop.entrySet().stream()
                .map(entry -> createRecipe(entry.getKey(), entry.getValue()))
                .filter(recipe -> !recipe.dropStack().isEmpty())
                .sorted(Comparator.comparing(recipe -> recipe.dropId().toString()))
                .toList();
    }

    private static void parseClientResources(Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        resourceManager.listResources(REAPING_DATA_PATH, id -> id.getPath().endsWith(".json")).values()
                .forEach(resource -> parseResource(resource, entityData));
    }

    private static void parseMalumModResources(Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        Path malumPath = getMalumCodeSourcePath();
        if (malumPath == null) {
            return;
        }

        try {
            if (Files.isDirectory(malumPath)) {
                parseMalumDirectory(malumPath, entityData);
            } else {
                parseMalumJar(malumPath, entityData);
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

    private static void parseMalumDirectory(Path malumPath, Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        Path reapingDataPath = malumPath.resolve(MALUM_REAPING_DATA_ROOT);
        if (!Files.isDirectory(reapingDataPath)) {
            return;
        }

        try (Stream<Path> files = Files.walk(reapingDataPath)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            parseReader(reader, entityData);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static void parseMalumJar(Path malumPath, Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        try (JarFile jarFile = new JarFile(malumPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(MALUM_REAPING_DATA_ROOT))
                    .filter(entry -> entry.getName().endsWith(".json"))
                    .forEach(entry -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                            parseReader(reader, entityData);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static void parseResource(Resource resource, Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        try (BufferedReader reader = resource.openAsReader()) {
            parseReader(reader, entityData);
        } catch (Exception ignored) {
        }
    }

    private static void parseReader(BufferedReader reader, Map<ResourceLocation, List<ParsedReapingDrop>> entityData) {
        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
        ResourceLocation entityId = ResourceLocation.tryParse(object.getAsJsonPrimitive("registry_name").getAsString());
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            return;
        }

        List<ParsedReapingDrop> drops = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("drops")) {
            JsonObject dropObject = element.getAsJsonObject();
            if (!dropObject.has("ingredient")) {
                continue;
            }

            Ingredient ingredient = Ingredient.CODEC.parse(JsonOps.INSTANCE, dropObject.get("ingredient")).getOrThrow();
            float chance = dropObject.has("chance") ? dropObject.getAsJsonPrimitive("chance").getAsFloat() : 1F;
            int min = dropObject.has("min") ? dropObject.getAsJsonPrimitive("min").getAsInt() : 1;
            int max = dropObject.has("max") ? dropObject.getAsJsonPrimitive("max").getAsInt() : Math.max(min, 1);
            for (ItemStack stack : ingredient.getItems()) {
                ResourceLocation dropId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (BuiltInRegistries.ITEM.containsKey(dropId)) {
                    drops.add(new ParsedReapingDrop(dropId, chance, min, max));
                }
            }
        }

        if (!drops.isEmpty()) {
            entityData.put(entityId, drops);
        }
    }

    private static void addEntriesForEntity(Map<ResourceLocation, List<ReapingDropEntry>> entriesByDrop, ResourceLocation entityId, List<ParsedReapingDrop> drops) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        List<ItemStack> spawnEggs = getSpawnEggs(entityType);

        for (ParsedReapingDrop drop : drops) {
            entriesByDrop.computeIfAbsent(drop.dropId(), key -> new ArrayList<>())
                    .add(new ReapingDropEntry(
                            entityId,
                            entityType,
                            entityType.getDescription(),
                            spawnEggs,
                            drop.chance(),
                            drop.min(),
                            drop.max()
                    ));
        }
    }

    private static List<ItemStack> getSpawnEggs(EntityType<?> entityType) {
        SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);
        if (spawnEgg == null) {
            return List.of();
        }
        return List.of(new ItemStack(spawnEgg));
    }

    private static ReapingDropsRecipe createRecipe(ResourceLocation dropId, List<ReapingDropEntry> entries) {
        Item dropItem = BuiltInRegistries.ITEM.get(dropId);
        ItemStack dropStack = BuiltInRegistries.ITEM.containsKey(dropId) ? new ItemStack(dropItem) : ItemStack.EMPTY;
        List<ReapingDropEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparingInt(ReapingDropEntry::max).reversed()
                        .thenComparing(Comparator.comparingInt(ReapingDropEntry::min).reversed())
                        .thenComparing(Comparator.comparingDouble(ReapingDropEntry::chance).reversed())
                        .thenComparing(entry -> entry.entityId().toString()))
                .toList();
        return new ReapingDropsRecipe(dropId, dropStack, sortedEntries);
    }

    private record ParsedReapingDrop(ResourceLocation dropId, float chance, int min, int max) {
    }
}
