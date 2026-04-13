package betterblockentities.client.chunk.pipeline.itemframe;

/* java/misc */
import org.jspecify.annotations.Nullable;

public interface BuiltSectionInfoItemFrameExt {
    @Nullable ItemFrameSectionAppender getItemFrameSectionAppender();
    void setItemFrameSectionAppender(@Nullable ItemFrameSectionAppender appender);
}
