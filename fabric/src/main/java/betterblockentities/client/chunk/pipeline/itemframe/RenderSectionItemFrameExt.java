package betterblockentities.client.chunk.pipeline.itemframe;

/* annotations */
import org.jspecify.annotations.Nullable;

public interface RenderSectionItemFrameExt {
    @Nullable ItemFrameSectionAppender getItemFrameSectionAppender();
    void setItemFrameSectionAppender(@Nullable ItemFrameSectionAppender appender);
}
