package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionAppender;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionBuildBridge;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionRegistry;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* sodium */
import net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* java */
import java.util.ArrayList;
import java.util.List;

@Mixin(FabricLevelRenderHooks.class)
public class FabricLevelRenderHooksMixin {
    @Inject(method = "retrieveChunkMeshAppenders", at = @At("RETURN"), cancellable = true)
    private void retrieveItemFrameAppenders(
            Level level,
            BlockPos pos,
            CallbackInfoReturnable<List<?>> cir
    ) {
        ItemFrameSectionAppender appender = null;

        if (ItemFrameEligibility.optimizationEnabled()) {
            SectionPos sectionPos = SectionPos.of(pos);

            if (ItemFrameSectionRegistry.hasFramesForSection(sectionPos)
                    || ItemFrameRuntimeHelper.shouldCollectLiveFramesDuringSectionCapture()) {
                appender = ItemFrameSectionAppender.capture(level, pos);
            }
        }

        ItemFrameSectionBuildBridge.publish(pos, appender);

        if (appender == null) return;

        List<?> current = cir.getReturnValue();
        if (current == null || current.isEmpty()) {
            cir.setReturnValue(List.of(appender));
            return;
        }

        ArrayList<Object> merged = new ArrayList<>(current.size() + 1);
        merged.addAll(current);
        merged.add(appender);
        cir.setReturnValue(List.copyOf(merged));
    }
}
