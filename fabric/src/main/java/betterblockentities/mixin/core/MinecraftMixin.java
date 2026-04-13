package betterblockentities.mixin.core;

/* local */
import betterblockentities.render.AltRenderDispatcher;
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.pipeline.itemframe.MapAtlasManager;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;
import betterblockentities.client.resource.ClientReloaderRegister;
import betterblockentities.client.tasks.ManagerTasks;

/* fabric */
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

/* minecraft */
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final public Font font;
    @Shadow @Final private ModelManager modelManager;
    @Shadow @Final private BlockModelResolver blockModelResolver;
    @Shadow @Final private ItemModelResolver itemModelResolver;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private AtlasManager atlasManager;
    @Shadow @Final private PlayerSkinRenderCache playerSkinRenderCache;
    @Shadow @Final private TextureManager textureManager;

    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "com/mojang/blaze3d/vertex/Tesselator.init ()V"
            )
    )
    void registerDispatchListener(Operation<Void> original) {
        MapAtlasManager.initialize(this.textureManager);

        BBE.GlobalScope.altRenderDispatcher = new AltRenderDispatcher(
                this.font,
                this.modelManager.entityModels(),
                this.blockModelResolver,
                this.itemModelResolver,
                this.entityRenderDispatcher,
                this.atlasManager,
                this.playerSkinRenderCache
        );

        ClientReloaderRegister.register(
                AltRenderDispatcher.RELOAD_LISTENER_ID,
                BBE.GlobalScope.altRenderDispatcher,
                ResourceReloaderKeys.Client.FONTS,
                ResourceReloaderKeys.Client.MODELS,
                ResourceReloaderKeys.Client.TEXTURES
        );

        original.call();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void pollManagerQueue(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Level level = mc.level;
        if (level == null || !level.isClientSide()) return;

        ItemFrameRuntimeHelper.tickBootstrapPasses();
        ManagerTasks.process();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void bootstrapItemFramesOnLevelSet(ClientLevel level, CallbackInfo ci) {
        ItemFrameRuntimeHelper.onLevelSet(level);
    }
}
