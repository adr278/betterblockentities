package betterblockentities.chunk;

/* minecraft */
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public class BBEDefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(ChunkSectionLayer.SOLID, false, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(ChunkSectionLayer.CUTOUT, false, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, true, true);
}
