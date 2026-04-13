package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.BuiltSectionInfoItemFrameExt;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionAppender;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionUploadRegistry;
import betterblockentities.client.chunk.pipeline.itemframe.RenderSectionItemFrameExt;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;

/* mixin */
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSection.class)
public class RenderSectionItemFrameMixin implements RenderSectionItemFrameExt {
    @Unique private @Nullable ItemFrameSectionAppender itemFrameSectionAppender;

    @Override public @Nullable ItemFrameSectionAppender getItemFrameSectionAppender() {
        return this.itemFrameSectionAppender;
    }

    @Override public void setItemFrameSectionAppender(@Nullable ItemFrameSectionAppender appender) {
        this.itemFrameSectionAppender = appender;
    }

    @Inject(method = "setInfo", at = @At("HEAD"))
    private void clearItemFramesWhenInfoClears(
            @Nullable BuiltSectionInfo info,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) {
            clearSectionData();
            return;
        }

        if (info != null) return;
        clearSectionData();
    }

    @Inject(method = "setInfo", at = @At("TAIL"))
    private void updateItemFramesWhenInfoSets(
            @Nullable BuiltSectionInfo info,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) {
            clearSectionData();
            return;
        }

        if (info == null) return;

        ItemFrameSectionAppender appender = ((BuiltSectionInfoItemFrameExt) info).getItemFrameSectionAppender();
        this.itemFrameSectionAppender = appender;

        RenderSection section = (RenderSection) (Object) this;
        ItemFrameSectionUploadRegistry.update(
                section.getChunkX(),
                section.getChunkY(),
                section.getChunkZ(),
                appender
        );
        ItemFrameRuntimeHelper.onSectionUploaded(appender);
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void clearItemFramesOnDelete(CallbackInfo ci) { clearSectionData(); }

    @Unique private void clearSectionData() {
        this.itemFrameSectionAppender = null;

        RenderSection section = (RenderSection) (Object) this;
        ItemFrameSectionUploadRegistry.clear(
                section.getChunkX(),
                section.getChunkY(),
                section.getChunkZ()
        );
    }
}
