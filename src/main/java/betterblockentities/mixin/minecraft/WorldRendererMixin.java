package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.BetterBlockEntities;

/* minecraft */
import betterblockentities.model.BBEGeometryRegistry;
import betterblockentities.model.BBEMultiPartModel;
import betterblockentities.util.ModelTransform;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;

/* mixin */
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    @Inject(at = @At("HEAD"), method = "cullTerrain", remap = false)
    private void captureFrustum(Camera camera, Frustum frustum, boolean bl, CallbackInfo ci) {
        BetterBlockEntities.curFrustum = frustum;
    }
}
