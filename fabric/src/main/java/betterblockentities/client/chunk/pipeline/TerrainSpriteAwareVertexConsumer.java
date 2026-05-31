package betterblockentities.client.chunk.pipeline;

/* minecraft */
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface TerrainSpriteAwareVertexConsumer {
    void setSourceSprite(TextureAtlasSprite sourceSprite);
}
