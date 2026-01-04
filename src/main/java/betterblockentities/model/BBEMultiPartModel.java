package betterblockentities.model;

/* local */
import betterblockentities.mixin.minecraft.ModelPartAccessor;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.util.RandomSource;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BBEMultiPartModel implements BlockStateModel {
    private final List<BlockStateModel> models = new ArrayList<>();

    /* pairs of model and key, will only be populated if type Model constructor is called */
    private final Map<String, BlockStateModel> pairs = new HashMap<>();

    /* construct models from geometry class Model and passed sprite. assumes that all ModelParts shares the same PoseStack */
    public BBEMultiPartModel(Model model, TextureAtlasSprite sprite, PoseStack stack) {
        ModelPart root = model.root();
        generateMeshModel(root, sprite, stack);
    }
    public BBEMultiPartModel(ModelPart root, TextureAtlasSprite sprite, PoseStack stack) {
        generateMeshModel(root, sprite, stack);
    }
    private void generateMeshModel(ModelPart root, TextureAtlasSprite sprite, PoseStack stack) {
        ModelPartAccessor modelAcc = (ModelPartAccessor)(Object)(root);

        /* maybe we cant split up the root like this, we might need to pass the root itself to toBakedQuads{...} to avoid stack issues */
        modelAcc.getChildren().forEach((key, part) -> {
            List<BakedQuad> outputQuads = new ArrayList<>();
            List<BlockModelPart> blockParts = new ArrayList<>();

            /* if there are any nested children in this part, this function should be able to traverse all of them */
            ModelPartWrapper.toBakedQuadsWithTransforms(part, outputQuads, sprite, stack);

            /* TODO: fix culling */
            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (BakedQuad quad : outputQuads) {
                builder.addUnculledFace(quad);
            }
            QuadCollection collection = builder.build();

            /* TODO: fix particle sprite */
            SimpleModelWrapper blockPart = new SimpleModelWrapper(collection, true, null);

            blockParts.add(blockPart);
            constructSingleVariants(blockParts);

            createModelPairs(key);
        });
    }
    private void createModelPairs(String key) {
        pairs.put(key, models.getLast());
    }

    /* construct models from BlockModelParts (wraps each BlockModelPart in a BlockStateModel{SingleVariant}) */
    public BBEMultiPartModel(List<BlockModelPart> parts) {
        constructSingleVariants(parts);
    }
    private void constructSingleVariants(List<BlockModelPart> parts) {
        for (BlockModelPart variant : parts) {
            var model = new SingleVariant(variant);
            models.add(model);
        }
    }

    public Map<String, BlockStateModel> getPairs() {
        return pairs;
    }

    @Override
    public void collectParts(RandomSource randomSource, List<BlockModelPart> list) {
        long l = randomSource.nextLong();

        if (this.models.isEmpty()) return;

        for(BlockStateModel blockStateModel : this.models) {
            randomSource.setSeed(l);
            blockStateModel.collectParts(randomSource, list);
        }
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return null;
    }
}
