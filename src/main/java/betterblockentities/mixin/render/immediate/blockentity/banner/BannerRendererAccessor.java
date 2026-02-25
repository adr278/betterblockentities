package betterblockentities.mixin.render.immediate.blockentity.banner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BannerRenderer.class)
public interface BannerRendererAccessor {
    @Invoker("submitPatterns")
    static <S> void invokeSubmitPatterns(
            MaterialSet materialSet, PoseStack poseStack, SubmitNodeCollector collector,
            int i, int j, Model<S> model, S state, Material material, boolean bl,
            DyeColor dyeColor, BannerPatternLayers layers, boolean bl2,
            ModelFeatureRenderer.CrumblingOverlay overlay, int k
    ) {
        throw new AssertionError();
    }
}
