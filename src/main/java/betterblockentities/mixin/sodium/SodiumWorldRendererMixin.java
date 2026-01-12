package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;

@Pseudo
@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererMixin {
    @Inject(method = "extractBlockEntity", at = @At("HEAD"), cancellable = true)
    private void extractBlockEntity(BlockEntity blockEntity, PoseStack poseStack, Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!BlockEntityManager.shouldRender(blockEntity)) ci.cancel();
    }
}