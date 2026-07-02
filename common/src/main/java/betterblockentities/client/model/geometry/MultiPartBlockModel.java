package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.mixin.model.modelpart.ModelPartAccessor;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiPartBlockModel implements BakedModel {
    private final List<SimpleBakedModel> models = new ArrayList<>();
    private final Map<String, BakedModel> pairs = new HashMap<>();

    public MultiPartBlockModel(ModelPart root, TextureAtlasSprite sprite, PoseStack stack) {
        generateMeshModel(root, sprite, stack);
    }

    public MultiPartBlockModel(SimpleBakedModel parts) {
        constructSingleVariants(parts);
    }

    @SuppressWarnings("resource")
    private void generateMeshModel(ModelPart root, TextureAtlasSprite sprite, PoseStack stack) {
        ModelPartAccessor modelAcc = (ModelPartAccessor)(Object)root;
        if (modelAcc == null) {
            GlobalScope.LOGGER.error("Failed to invoke accessor on root model part with sprite {}", sprite.contents().name());
            return;
        }

        Map<String, ModelPart> children = modelAcc.getChildren();
        if (children.isEmpty()) {
            GlobalScope.LOGGER.error("Root model part with sprite {} has no children, skipping!", sprite.contents().name());
            return;
        }

        children.forEach((key, part) -> {
            List<BakedQuad> quads = new ArrayList<>();
            bakePartToQuads(part, quads, sprite, stack);

            Map<Direction, List<BakedQuad>> culledQuads = new HashMap<>();
            SimpleBakedModel bakedModel = new SimpleBakedModel(quads, culledQuads, false, false, false, sprite, null, null);

            constructSingleVariants(bakedModel);
            createModelPairs(key);
        });
    }

    /**
     * if there are any nested children in this part, this should traverse all of them
     */
    private void bakePartToQuads(ModelPart part, List<BakedQuad> outputQuads, TextureAtlasSprite sprite, PoseStack stack) {
        ModelUtility.toBakedQuads(part, outputQuads, sprite, stack);
    }

    private void constructSingleVariants(SimpleBakedModel model) {
        models.add(model);
    }

    private void createModelPairs(String key) {
        pairs.put(key, models.getLast());
    }

    public Map<String, BakedModel> getPairs() {
        return pairs;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return null;
    }

    @Override
    public ItemTransforms getTransforms() {
        return null;
    }

    @Override
    public ItemOverrides getOverrides() {
        return null;
    }
}
