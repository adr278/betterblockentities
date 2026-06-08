package betterblockentities.mixin.core;

/* local */
import betterblockentities.render.AltRenderDispatcher;
import betterblockentities.client.BBE;
import betterblockentities.client.tasks.ManagerTasks;

/* minecraft */
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
    @Shadow @Final private ReloadableResourceManager resourceManager;
    @Shadow public abstract BlockRenderDispatcher getBlockRenderer();
    @Shadow public abstract ItemRenderer getItemRenderer();
    @Shadow public abstract EntityRenderDispatcher getEntityRenderDispatcher();
    @Shadow @Final private EntityModelSet entityModels;

    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "com/mojang/blaze3d/vertex/Tesselator.init ()V"
            )
    )
    void registerDispatchListener(Operation<Void> original) {
        BBE.GlobalScope.altRenderDispatcher = new AltRenderDispatcher(
                this.font,
                this.entityModels,
                this::getBlockRenderer,
                this::getItemRenderer,
                this::getEntityRenderDispatcher
        );

        this.resourceManager.registerReloadListener(BBE.GlobalScope.altRenderDispatcher);

        original.call();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void pollManagerQueue(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Level level = mc.level;
        if (level == null || !level.isClientSide()) return;

        ManagerTasks.process();
    }
}
