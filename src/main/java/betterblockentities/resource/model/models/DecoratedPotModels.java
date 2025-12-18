package betterblockentities.resource.model.models;

import betterblockentities.resource.model.ModelGenerator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.DyeColor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DecoratedPotModels extends ModelGenerator {
    public static class Model {
        public static void generateSides(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("decorated_pot_sides_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "decorated_pot_sides";
            String texture = "minecraft:entity/decorated_pot/decorated_pot_side";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("side", texture, "minecraft:block/terracotta", elements)).getBytes(StandardCharsets.UTF_8));
        }

        public static void generateTop(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("decorated_pot_top_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "decorated_pot_top";
            String texture = "minecraft:entity/decorated_pot/decorated_pot_base";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("pot", texture, "minecraft:block/terracotta", elements)).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class BlockState {

        public static void generateDecoratedPotBlockstate(Map<String, byte[]> map) {
            JsonArray multipart = new JsonArray();

            String[] facings = {"north", "east", "south", "west"};

            String[] models = {
                    "block/decorated_pot_sides",
                    "block/decorated_pot_top"
            };

            for (String modelName : models) {
                for (String facing : facings) {
                    JsonObject entry = new JsonObject();

                    // when
                    JsonObject when = new JsonObject();
                    when.addProperty("facing", facing);
                    entry.add("when", when);

                    // apply
                    JsonObject apply = new JsonObject();
                    apply.addProperty("model", modelName);

                    int yRot = switch (facing) {
                        case "east"  -> 90;
                        case "south" -> 180;
                        case "west"  -> 270;
                        default -> 0;
                    };

                    if (yRot != 0) {
                        apply.addProperty("y", yRot);
                    }

                    entry.add("apply", apply);
                    multipart.add(entry);
                }
            }

            JsonObject root = new JsonObject();
            root.add("multipart", multipart);

            map.put(
                    "assets/minecraft/blockstates/decorated_pot.json",
                    GSON.toJson(root).getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
