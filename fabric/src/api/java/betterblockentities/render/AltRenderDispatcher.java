package betterblockentities.render;

/* local */
import betterblockentities.data.RegistrationInfo;

/* minecraft */
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;
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
import org.joml.Quaternionf;

public class AltRenderDispatcher implements ResourceManagerReloadListener {
    private Map<BlockEntityType<?>, List<AltRenderer<?>>> renderers = ImmutableMap.of();
    private final Font font;
    private final EntityModelSet entityModelSet;
    public Level level;
    public Camera camera;
    public HitResult cameraHitResult;
    private final Supplier<BlockRenderDispatcher> blockRenderDispatcher;
    private final Supplier<ItemRenderer> itemRenderer;
    private final Supplier<EntityRenderDispatcher> entityRenderer;

    public AltRenderDispatcher(
            Font font,
            EntityModelSet entityModelSet,
            Supplier<BlockRenderDispatcher> supplier,
            Supplier<ItemRenderer> supplier2,
            Supplier<EntityRenderDispatcher> supplier3
    ) {
        this.itemRenderer = supplier2;
        this.entityRenderer = supplier3;
        this.font = font;
        this.entityModelSet = entityModelSet;
        this.blockRenderDispatcher = supplier;
    }

    @Nullable
    public List<AltRenderer<?>> getRenderers(BlockEntity blockEntity) {
        return this.renderers.get(blockEntity.getType());
    }

    public void prepare(Level level, Camera camera, HitResult hitResult) {
        if (this.level != level) {
            this.setLevel(level);
        }

        this.camera = camera;
        this.cameraHitResult = hitResult;
    }

    public void render(BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        List<AltRenderer<?>> altRenderers = this.getRenderers(blockEntity);

        for (AltRenderer<?> altRenderer : altRenderers) {
            if (altRenderer != null) {
                if (blockEntity.hasLevel() && blockEntity.getType().isValid(blockEntity.getBlockState())) {
                    if (altRenderer.shouldRender(blockEntity, this.camera.getPosition())) {
                        tryRender(blockEntity, () -> setupAndRender(altRenderer, blockEntity, f, poseStack, multiBufferSource));
                    }
                }
            }
        }
    }

    private static void setupAndRender(
            AltRenderer<?> AltRenderer, BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource
    ) {
        Level level = blockEntity.getLevel();
        int i;
        if (level != null) {
            i = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
        } else {
            i = 15728880;
        }

        AltRenderer.render(blockEntity, f, poseStack, multiBufferSource, i, OverlayTexture.NO_OVERLAY);
    }

    public boolean renderItem(BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j) {
        List<AltRenderer<?>> altRenderers = this.getRenderers(blockEntity);

        for (AltRenderer<?> altRenderer : altRenderers) {
            if (altRenderer == null) {
                return true;
            } else {
                tryRender(blockEntity, () -> altRenderer.render(blockEntity, 0.0F, poseStack, multiBufferSource, i, j));
                return false;
            }
        }
        return true;
    }

    private static void tryRender(BlockEntity blockEntity, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable var5) {
            CrashReport crashReport = CrashReport.forThrowable(var5, "Rendering Block Entity");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block Entity Details");
            blockEntity.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    public void setLevel(@Nullable Level level) {
        this.level = level;
        if (level == null) {
            this.camera = null;
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        AltRendererProvider.Context context = new  AltRendererProvider.Context(
                this,
                (BlockRenderDispatcher)this.blockRenderDispatcher.get(),
                (ItemRenderer)this.itemRenderer.get(),
                (EntityRenderDispatcher)this.entityRenderer.get(),
                this.entityModelSet,
                this.font
        );
        this.renderers = AltRenderers.createAltEntityRenderers(context);
    }
}
