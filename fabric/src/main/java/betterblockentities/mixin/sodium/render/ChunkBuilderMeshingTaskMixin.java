package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.chunk.pipeline.itemframe.BuiltSectionInfoItemFrameExt;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionAppender;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionBuildBridge;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;

/* mixin */
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* java */
import java.util.List;

@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
    @Shadow @Final private ChunkRenderContext renderContext;

    @Inject(method = "execute", at = @At("HEAD"))
    private void beginItemFrameSectionCapture(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) {
            ItemFrameSectionBuildBridge.begin(null);
            return;
        }

        ItemFrameSectionBuildBridge.begin(resolveItemFrameAppender());
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void attachItemFrameSectionData(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) {
            ItemFrameSectionBuildBridge.take();
            return;
        }

        ItemFrameSectionAppender appender = ItemFrameSectionBuildBridge.take();
        ChunkBuildOutput output = cir.getReturnValue();

        if (output == null) return;
        ((BuiltSectionInfoItemFrameExt) output.info).setItemFrameSectionAppender(appender);
    }

    @Unique private @Nullable ItemFrameSectionAppender resolveItemFrameAppender() {
        ItemFrameSectionAppender appender = findItemFrameAppender(this.renderContext.getRenderers());
        if (appender != null) {
            return appender;
        }

        return ItemFrameSectionBuildBridge.consume(this.renderContext.getOrigin());
    }

    @Unique private static @Nullable ItemFrameSectionAppender findItemFrameAppender(List<?> renderers) {
        for (Object renderer : renderers) {
            if (renderer instanceof ItemFrameSectionAppender appender) {
                return appender;
            }
        }

        return null;
    }
}
