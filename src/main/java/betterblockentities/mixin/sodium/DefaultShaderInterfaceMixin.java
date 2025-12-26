package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;

/* mojang */
import com.mojang.blaze3d.textures.GpuSampler;

/* sodium */
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformBool;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(DefaultShaderInterface.class)
public class DefaultShaderInterfaceMixin {
    @Shadow @Final private GlUniformBool uniformRGSS;

    /*
        disable RGSS texture filtering for our render passes. see DefaultTerrainRenderPasses
        mixin for more context to as why we are doing this
    */
    @Inject(method = "setupState", at = @At("TAIL"))
    public void disableRGSS(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, CallbackInfo ci) {
        if (pass == BBEDefaultTerrainRenderPasses.SOLID         ||
            pass == BBEDefaultTerrainRenderPasses.CUTOUT        ||
            pass == BBEDefaultTerrainRenderPasses.TRANSLUCENT
        ) {
            this.uniformRGSS.setBool(false);
        }
    }
}
