package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.BuiltSectionInfoItemFrameExt;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionAppender;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;

/* mixin */
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BuiltSectionInfo.class)
public class BuiltSectionInfoItemFrameMixin implements BuiltSectionInfoItemFrameExt {
    @Unique private @Nullable ItemFrameSectionAppender itemFrameSectionAppender;

    @Override public @Nullable ItemFrameSectionAppender getItemFrameSectionAppender() {
        return this.itemFrameSectionAppender;
    }

    @Override public void setItemFrameSectionAppender(@Nullable ItemFrameSectionAppender appender) {
        this.itemFrameSectionAppender = appender;
    }
}
