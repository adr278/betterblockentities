package betterblockentities.resource.model.models;

import betterblockentities.resource.model.ModelGenerator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.DyeColor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BellModels extends ModelGenerator {
    public static class Model {
        public static void generateBell(Map<String, byte[]> map) {
            JsonObject template = loader.loadTemplate("bell_body_template.json");
            if (template == null) return;
            var elements = loader.readTemplateElements(template);


            String name = "bell_body";
            String texture = "minecraft:entity/bell/bell_body";
            map.put("assets/minecraft/models/block/" + name + ".json",
                    GSON.toJson(makeModelWithParticle("bell", texture, texture, elements)).getBytes(StandardCharsets.UTF_8));
        }
    }

    record Attachment(String name, String model, int northRot) {}

    public static class BlockState {
        public static void generateBellBlockstate(Map<String, byte[]> map) {
            JsonArray multipart = new JsonArray();

            String[] facings = {"north", "east", "south", "west"};

            Attachment[] attachments = {
                    new Attachment("ceiling", "bell_ceiling", 0),
                    new Attachment("floor", "bell_floor", 0),
                    new Attachment("single_wall", "bell_wall", 270),
                    new Attachment("double_wall", "bell_between_walls", 270)
            };

            for (Attachment attachment : attachments) {
                for (String facing : facings) {
                    JsonObject entry = new JsonObject();

                    JsonObject when = new JsonObject();
                    when.addProperty("attachment", attachment.name());
                    when.addProperty("facing", facing);
                    entry.add("when", when);

                    JsonArray apply = new JsonArray();
                    JsonObject model = new JsonObject();
                    model.addProperty("model", "minecraft:block/" + attachment.model());

                    int yRot = getYRotation(facing, attachment.northRot());
                    if (yRot != 0) {
                        model.addProperty("y", yRot);
                    }

                    apply.add(model);
                    entry.add("apply", apply);

                    multipart.add(entry);
                }
            }

            for (String facing : facings) {
                JsonObject entry = new JsonObject();

                JsonObject when = new JsonObject();
                when.addProperty("facing", facing);
                entry.add("when", when);

                JsonObject apply = new JsonObject();
                apply.addProperty("model", "minecraft:block/bell_body");

                int yRot = getYRotation(facing, 0);
                if (yRot != 0) {
                    apply.addProperty("y", yRot);
                }

                entry.add("apply", apply);
                multipart.add(entry);
            }

            JsonObject root = new JsonObject();
            root.add("multipart", multipart);

            map.put(
                    "assets/minecraft/blockstates/bell.json",
                    GSON.toJson(root).getBytes(StandardCharsets.UTF_8)
            );
        }

        private static int getYRotation(String facing, int northRotation) {
            return switch (facing) {
                case "north" -> northRotation;
                case "east"  -> (northRotation + 90) % 360;
                case "south" -> (northRotation + 180) % 360;
                case "west"  -> (northRotation + 270) % 360;
                default -> 0;
            };
        }
    }
}
