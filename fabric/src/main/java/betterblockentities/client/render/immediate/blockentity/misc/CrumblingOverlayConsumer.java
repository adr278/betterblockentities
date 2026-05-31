package betterblockentities.client.render.immediate.blockentity.misc;

/* local */
import betterblockentities.mixin.render.immediate.blockentity.VertexMultiConsumerDoubleAccessor;

/* minecraft */
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/* mojang */
import com.mojang.blaze3d.vertex.VertexConsumer;

/* java/misc */
import org.jspecify.annotations.NonNull;

public class CrumblingOverlayConsumer {
    public record CrumblingOnlyBufferSource(MultiBufferSource delegate) implements MultiBufferSource {
        @Override public @NonNull VertexConsumer getBuffer(final RenderType renderType) {
            if (!renderType.affectsCrumbling()) {
                return NoopVertexConsumer.INSTANCE;
            }

            final VertexConsumer vertexConsumer = this.delegate.getBuffer(renderType);
            if (vertexConsumer instanceof VertexMultiConsumerDoubleAccessor doubleConsumer) {
                return doubleConsumer.getFirst();
            }

            return vertexConsumer;
        }
    }

    public enum NoopVertexConsumer implements VertexConsumer {
        INSTANCE;

        @Override public @NonNull VertexConsumer addVertex(final float x, final float y, final float z) {
            return this;
        }

        @Override public @NonNull VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv(final float u, final float v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv1(final int u, final int v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv2(final int u, final int v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setNormal(final float normalX, final float normalY, final float normalZ) {
            return this;
        }
    }
}
