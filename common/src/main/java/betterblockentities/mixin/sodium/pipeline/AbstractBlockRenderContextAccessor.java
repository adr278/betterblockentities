package betterblockentities.mixin.sodium.pipeline;

/* sodium */
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(AbstractBlockRenderContext.class)
public interface AbstractBlockRenderContextAccessor {
    @Accessor("slice")
    LevelSlice getSlice();

    @Accessor("pos")
    BlockPos getPos();

    @Accessor("state")
    BlockState getState();

    @Accessor("quadLightData")
    QuadLightData getQuadLightData();
}
