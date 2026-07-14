package betterblockentities.client.chunk.util;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;

/* minecraft */
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* java/misc */
import org.joml.Vector3f;
import java.util.Arrays;

public class QuadLighter {
    private static final float MINECRAFT_LIGHT_POWER = 0.6F;
    private static final float MINECRAFT_AMBIENT_LIGHT = 0.4F;
    private static final int LIGHTMAP_TEXEL_CENTER_OFFSET = 0x00080008;

    private static final Vector3f Light0_Direction = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3f Light1_Direction = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();

    private LevelSlice slice;

    public void prepare(LevelSlice slice) {
        this.slice = slice;
    }

    /**
     * Mirrors vanillas entity lighting behavior from {@code entity.vsh} and {@code light.glsl}:
     *
     * vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
     *
     * minecraft_mix_light_separate(vec2 light, vec4 color) ->
     *      lightAccum = min(1.0, (max(dot(light0, normal), 0.0) + max(dot(light1, normal), 0.0)) * 0.6 + 0.4);
     *
     */
    private static float computeDirectionalLight(MutableQuadViewImpl quad) {
        Vector3f normal = quad.faceNormal();

        float light0 = Math.max(0.0F, Light0_Direction.dot(normal));
        float light1 = Math.max(0.0F, Light1_Direction.dot(normal));

        return Math.min(1.0F, (light0 + light1) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);
    }

    public void shadeEntityQuad(BlockPos pos, BlockState state, boolean emissive, MutableQuadViewImpl quad, QuadLightData out) {
        int light = (emissive ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(this.slice, state, pos))
                // Sodium filters terrain lightmaps, so we offset this to match entity lighting.
                + LIGHTMAP_TEXEL_CENTER_OFFSET;
        float brightness = computeDirectionalLight(quad);

        //write brightness/ao
        Arrays.fill(out.br, brightness);

        //apply per vertex lightmap (its uniform in this case)
        for (int vertex = 0; vertex < BBEEmitter.QUAD_VERTICES; ++vertex) {
            quad.lightmap(vertex, light);
        }
    }
}
