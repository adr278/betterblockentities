package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameTerrainEmitter;
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/* java */
import java.util.HashSet;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

public final class BBEEntityRenderer {
    private final BBEEmitter emitter;
    private final HashSet<Long> emittedSectionsForCurrentMeshingPass = new HashSet<>(16);

    public BBEEntityRenderer(BlockRenderer sodiumBlockRenderer) { this.emitter = new BBEEmitter(sodiumBlockRenderer); }

    public void beginMeshingPass() { this.emittedSectionsForCurrentMeshingPass.clear(); }

    public void endMeshingPass() { this.emittedSectionsForCurrentMeshingPass.clear(); }

    public void emit(BlockPos supportPos) {
        if (!ConfigCache.optimizeItemFrames) return;

        int sectionX = SectionPos.blockToSectionCoord(supportPos.getX());
        int sectionY = SectionPos.blockToSectionCoord(supportPos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(supportPos.getZ());
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        if (!this.emittedSectionsForCurrentMeshingPass.add(sectionKey)) return;

        ItemFrameTerrainEmitter.emitForSection(supportPos, this.emitter);
    }
}
