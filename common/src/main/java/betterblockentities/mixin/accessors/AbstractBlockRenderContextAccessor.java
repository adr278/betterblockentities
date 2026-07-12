package betterblockentities.mixin.accessors;

/* minecraft */
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/* java/misc */
import org.jetbrains.annotations.Nullable;

@Pseudo
@Mixin(AbstractBlockRenderContext.class)
public interface AbstractBlockRenderContextAccessor {
    @Accessor("level")
    BlockAndTintGetter bbe$getLevel();

    @Accessor("random")
    RandomSource bbe$getRandom();

    @Accessor("state")
    BlockState bbe$getState();

    @Accessor("state")
    void bbe$setState(BlockState state);

    @Accessor("pos")
    BlockPos bbe$getPos();

    @Accessor("pos")
    void bbe$setPos(BlockPos pos);

    @Accessor("slice")
    LevelSlice bbe$getSlice();

    @Accessor("slice")
    void bbe$setSlice(LevelSlice slice);

    @Invoker("getForEmitting")
    MutableQuadViewImpl bbe$getEmitter();

    @Invoker("prepareAoInfo")
    void bbe$prepareAoInfo(boolean modelAo);

    @Invoker("prepareCulling")
    void bbe$prepareCulling(boolean enableCulling);

    @Invoker("isFaceCulled")
    boolean bbe$isFaceCulled(@Nullable Direction face);

    @Accessor("quadLightData")
    QuadLightData bbe$quadLightData();
}
