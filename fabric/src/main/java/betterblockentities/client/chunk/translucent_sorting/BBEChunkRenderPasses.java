package betterblockentities.client.chunk.translucent_sorting;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class BBEChunkRenderPasses {
    public static final TerrainRenderPass TRANSLUCENT =
            new TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, false, true); //we can profile this pass as translucent because it will get caught by the main sorter

    public static final Material TRANSLUCENT_MATERIAL =
            new Material(TRANSLUCENT, AlphaCutoffParameter.TINY, true);

    private BBEChunkRenderPasses() {
    }
}
