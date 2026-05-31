package betterblockentities.mixin.render.texture;

/* local */
import betterblockentities.client.chunk.pipeline.TerrainSpriteAwareVertexConsumer;

/* minecraft */
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* mojang */
import com.mojang.blaze3d.vertex.VertexConsumer;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteCoordinateExpander.class)
public abstract class SpriteCoordinateExpanderMixin {
    @Shadow @Final private VertexConsumer delegate;
    @Shadow @Final private TextureAtlasSprite sprite;

    @Inject(method = "setUv", at = @At("HEAD"))
    private void captureSourceSprite(final float u, final float v, final CallbackInfoReturnable<VertexConsumer> cir) {
        if (this.delegate instanceof TerrainSpriteAwareVertexConsumer terrainConsumer) {
            terrainConsumer.setSourceSprite(this.sprite);
        }
    }

    @Inject(method = "addVertex(FFFIFFIIFFF)V", at = @At("HEAD"))
    private void captureSourceSpriteFast(
            final float x,
            final float y,
            final float z,
            final int argb,
            final float u,
            final float v,
            final int overlay,
            final int light,
            final float normalX,
            final float normalY,
            final float normalZ,
            final CallbackInfo ci
    ) {
        if (this.delegate instanceof TerrainSpriteAwareVertexConsumer terrainConsumer) {
            terrainConsumer.setSourceSprite(this.sprite);
        }
    }
}
