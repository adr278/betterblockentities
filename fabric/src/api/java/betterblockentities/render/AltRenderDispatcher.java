package betterblockentities.render;

/* local */
import betterblockentities.data.RegistrationInfo;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* google */
import com.google.common.collect.ImmutableMap;

/* java/misc */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/* annotations */
import org.jetbrains.annotations.Nullable;

public class AltRenderDispatcher implements ResourceManagerReloadListener {
    private Map<BlockEntityType<?>, List<AltRenderer<?, ?>>> renderers = ImmutableMap.of();
    private final Map<AltBlockEntityRenderState, AltRenderer<?, ?>> stateRendererPairs = new HashMap<>();

    private final Font font;
    private final Supplier<EntityModelSet> entityModelSet;
    private Vec3 cameraPos = Vec3.ZERO;
    private final Supplier<BlockRenderDispatcher> blockRenderDispatcher;
    private final Supplier<ItemRenderer> itemRenderer;
    private final Supplier<EntityRenderDispatcher> entityRenderer;

    public AltRenderDispatcher(
            final Font font,
            final Supplier<EntityModelSet> entityModelSet,
            final Supplier<BlockRenderDispatcher> blockRenderDispatcher,
            final Supplier<ItemRenderer> itemRenderer,
            final Supplier<EntityRenderDispatcher> entityRenderDispatcher
    ) {
        this.blockRenderDispatcher = blockRenderDispatcher;
        this.itemRenderer = itemRenderer;
        this.entityRenderer = entityRenderDispatcher;
        this.font = font;
        this.entityModelSet = entityModelSet;
    }

    @SuppressWarnings("unchecked")
    public <E extends BlockEntity, S extends AltBlockEntityRenderState> List<AltRenderer<E, S>> getRenderers(final E blockEntity) {
        return (List<AltRenderer<E, S>>) (List<?>) this.renderers.getOrDefault(blockEntity.getType(), List.of());
    }

    @SuppressWarnings("unchecked")
    public <S extends AltBlockEntityRenderState> List<AltRenderer<?, S>> getRenderers(final S state) {
        BlockEntityType<?> type = state.blockEntityType();
        if (type == null) {
            return List.of();
        }

        return (List<AltRenderer<?, S>>) (List<?>) this.renderers.getOrDefault(type, List.of());
    }

    public void prepare(final Vec3 cameraPos) {
        this.cameraPos = cameraPos;
    }

    /* no logging in the hotpath, we just throw, an error here is unexpected and the implementing dev is not at fault most likely */
    public <E extends BlockEntity, S extends AltBlockEntityRenderState> List<S> tryExtractRenderStates(final E blockEntity, final float partialTicks, final @Nullable BlockDestructionProgress breakProgress) {
        List<AltRenderer<E, S>> renderers = this.getRenderers(blockEntity);

        if (renderers.isEmpty()) {
            return List.of();
        } else if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return List.of();
        }

        Vec3 cameraPosition = this.cameraPos;
        List<S> states = new ArrayList<>();

        for (AltRenderer<E, S> renderer : renderers) {
            RegistrationInfo regInfo = AltRenderers.forRenderer(renderer);
            if (regInfo == null) {
                throw new RuntimeException("RegistrationInfo for a registered AltRenderer was null!");
            }

            if (!renderer.shouldRender(blockEntity, this.cameraPos)) {
                continue;
            }

            S state = renderer.createRenderState();

            addStateRendererPair(state, renderer);

            renderer.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
            states.add(state);
        }
        return states;
    }

    /* no logging in the hotpath, we just throw, an error here is unexpected and the implementing dev is not at fault most likely */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <S extends AltBlockEntityRenderState> void submit(final S state, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final Camera camera, final int light, final int overlay) {
        AltRenderer renderer = stateRendererPairs.get(state);

        if (renderer == null) {
            throw new RuntimeException("Could not map this AltBlockEntityRenderState to a registered AltRenderer -> " + state);
        }

        try {
            renderer.submit(state, poseStack, vertexConsumers, camera, light, overlay);
        } catch (Exception e) {
            throw new RuntimeException("An exception was caught inside a registered AltRenderer -> ", e);
        }
    }

    private <E extends BlockEntity, S extends AltBlockEntityRenderState> void addStateRendererPair(final S state, final AltRenderer<E, S> renderer) {
        stateRendererPairs.put(state, renderer);
    }

    public  void clearStateRendererPairs() {
        stateRendererPairs.clear();
    }

    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {
        AltRendererProvider.Context context = new AltRendererProvider.Context(
                this,
                this.blockRenderDispatcher.get(),
                this.itemRenderer.get(),
                this.entityRenderer.get(),
                this.entityModelSet.get(),
                this.font
        );
        this.renderers = AltRenderers.createAltEntityRenderers(context);
    }
}
