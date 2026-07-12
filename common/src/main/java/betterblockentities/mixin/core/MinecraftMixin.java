package betterblockentities.mixin.core;

/* local */
import betterblockentities.platform.GlobalScope;
import betterblockentities.render.AltRenderDispatcher;
import betterblockentities.client.tasks.ManagerTasks;

/* minecraft */
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
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
    @Shadow @Final private ItemModelResolver itemModelResolver;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private AtlasManager atlasManager;
    @Shadow @Final private PlayerSkinRenderCache playerSkinRenderCache;
    @Shadow @Final private ReloadableResourceManager resourceManager;

    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;registerReloadListeners(Lnet/minecraft/server/packs/resources/ReloadableResourceManager;)V"
            )
    )
    void bbe$registerDispatchListener(Gui instance, ReloadableResourceManager resourceManager, Operation<Void> original) {
        GlobalScope.altRenderDispatcher = new AltRenderDispatcher(
                this.font,
                this.modelManager.entityModels(),
                new BlockModelResolver(this.modelManager),
                this.itemModelResolver,
                this.entityRenderDispatcher,
                this.atlasManager,
                this.playerSkinRenderCache
        );

        original.call(instance, resourceManager);
    }

    @WrapOperation(method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/GameRenderer.extract(Lnet/minecraft/client/DeltaTracker;Z)V"
            )
    )
    private void bbe$pollManagerQueue(GameRenderer instance, DeltaTracker deltaTracker, boolean advanceGameTime, Operation<Void> original) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        ManagerTasks.process(partialTicks);

        original.call(instance, deltaTracker, advanceGameTime);
    }
}
