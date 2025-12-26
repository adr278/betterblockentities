package betterblockentities.chunk;

/* minecraft */
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;

public class BBEDefaultMaterials {
    public static final Material SOLID = new Material(BBEDefaultTerrainRenderPasses.SOLID, AlphaCutoffParameter.ZERO, true);
    public static final Material CUTOUT_MIPPED = new Material(BBEDefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.HALF, true);
    public static final Material TRANSLUCENT = new Material(BBEDefaultTerrainRenderPasses.TRANSLUCENT, AlphaCutoffParameter.TINY, true);
    public static final Material TRIPWIRE = new Material(BBEDefaultTerrainRenderPasses.TRANSLUCENT, AlphaCutoffParameter.TINY, true);

    public static Material forChunkLayer(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> SOLID;
            case CUTOUT -> CUTOUT_MIPPED;
            case TRANSLUCENT -> TRANSLUCENT;
            case TRIPWIRE -> TRIPWIRE;
        };
    }
}
