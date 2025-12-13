package betterblockentities.resource.model.models;

/* local */
import betterblockentities.resource.model.ModelGenerator;

/* minecraft */
import net.minecraft.world.item.DyeColor;

/* gson */
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/* java/misc */
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BannerModels extends ModelGenerator {
    public static class Model {
        public static void generateBannerBaseStanding(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("banner_standing_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "standing_banner";
            String texture = "minecraft:entity/banner_base";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("banner", texture, "minecraft:block/oak_planks", elements)).getBytes(StandardCharsets.UTF_8));
        }

        public static void generateBannerBaseWall(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("banner_wall_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "wall_banner";
            String texture = "minecraft:entity/banner_base";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("banner", texture, "minecraft:block/oak_planks", elements)).getBytes(StandardCharsets.UTF_8));
        }

        public static void generateBannerCanvasStanding(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("banner_standing_canvas_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "standing_canvas";
            String texture = "minecraft:entity/banner_base";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("banner", texture, "minecraft:block/oak_planks", elements)).getBytes(StandardCharsets.UTF_8));
        }

        public static void generateBannerCanvasWall(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("banner_wall_canvas_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);

            String name = "wall_canvas";
            String texture = "minecraft:entity/banner_base";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("banner", texture, "minecraft:block/oak_planks", elements)).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class BlockState {
        public static void generateBannerBlockstates(Map<String, byte[]> map) {
            generateStandingBlockStates(map);
            generateWallBlockStates(map);
        }

        public static void generateStandingBlockStates(Map<String, byte[]> map) {
            for (DyeColor color : DyeColor.values()) {
                String baseName = color.getName() + "_banner";

                JsonArray multipart = new JsonArray();

                multipart.add(makeBannerPart("standing_banner"));
                multipart.add(makeBannerPart("standing_canvas"));

                JsonObject root = new JsonObject();
                root.add("multipart", multipart);

                map.put("assets/minecraft/blockstates/" + baseName + ".json",
                        GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
            }
        }

        public static void generateWallBlockStates(Map<String, byte[]> map) {
            for (DyeColor color : DyeColor.values()) {
                String baseName = color.getName() + "_wall_banner";

                JsonArray multipart = new JsonArray();

                String[] models = { "wall_banner", "wall_canvas" };

                String[] facings = { "north", "south", "west", "east" };
                int[] rotations = { 180, 0, 90, 270 };

                for (String model : models) {
                    for (int i = 0; i < facings.length; i++) {

                        JsonObject whenObj = new JsonObject();
                        whenObj.addProperty("facing", facings[i]);

                        JsonObject applyObj = new JsonObject();
                        applyObj.addProperty("model", "block/" + model);
                        applyObj.addProperty("y", rotations[i]);

                        JsonObject part = new JsonObject();
                        part.add("when", whenObj);
                        part.add("apply", applyObj);

                        multipart.add(part);
                    }
                }

                JsonObject root = new JsonObject();
                root.add("multipart", multipart);

                map.put("assets/minecraft/blockstates/" + baseName + ".json",
                        GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
            }
        }

        private static JsonObject makeBannerPart(String model) {
            JsonObject apply = new JsonObject();
            apply.addProperty("model", "minecraft:block/" + model);

            JsonObject part = new JsonObject();
            part.add("apply", apply);
            return part;
        }
    }
}
