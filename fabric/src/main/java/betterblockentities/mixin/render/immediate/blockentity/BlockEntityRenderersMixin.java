package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.mixin.accessors.BlockEntityRenderersAccessor;
import betterblockentities.render.AltRenderers;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.renderers.*;

/* minecraft */
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* java/misc */
import java.util.Map;

@Mixin(BlockEntityRenderers.class)
public class BlockEntityRenderersMixin {
    /**
     * replace vanilla renderers, we can't mixin into the static initializer as we need this function's
     * reload capabilities.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "createEntityRenderers", at = @At("HEAD"))
    private static void bbe$replaceVanillaRenderers(CallbackInfoReturnable<Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>>> cir) {
        if (AltRenderers.hasRendererOverride(BlockEntityType.SIGN)) {
            bbe$removeRegistration(BlockEntityType.SIGN);
        } else {
            BlockEntityRendererProvider r0 =
                    ConfigCache.optimizeSigns ? BBEStandingSignRenderer::new : StandingSignRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.SIGN, r0);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.HANGING_SIGN)) {
            bbe$removeRegistration(BlockEntityType.HANGING_SIGN);
        } else {
            BlockEntityRendererProvider r1 =
                    ConfigCache.optimizeSigns ? BBEHangingSignRenderer::new : HangingSignRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.HANGING_SIGN, r1);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.CHEST)) {
            bbe$removeRegistration(BlockEntityType.CHEST);
        } else {
            BlockEntityRendererProvider r2 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.CHEST, r2);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.ENDER_CHEST)) {
            bbe$removeRegistration(BlockEntityType.ENDER_CHEST);
        } else {
            BlockEntityRendererProvider r3 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.ENDER_CHEST, r3);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.TRAPPED_CHEST)) {
            bbe$removeRegistration(BlockEntityType.TRAPPED_CHEST);
        } else {
            BlockEntityRendererProvider r4 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.TRAPPED_CHEST, r4);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.BANNER)) {
            bbe$removeRegistration(BlockEntityType.BANNER);
        } else {
            BlockEntityRendererProvider r5 =
                    ConfigCache.optimizeBanners ? BBEBannerRenderer::new : BannerRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.BANNER, r5);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.SHULKER_BOX)) {
            bbe$removeRegistration(BlockEntityType.SHULKER_BOX);
        } else {
            BlockEntityRendererProvider r6 =
                    ConfigCache.optimizeShulker ? BBEShulkerBoxRenderer::new : ShulkerBoxRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.SHULKER_BOX, r6);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.BED)) {
            bbe$removeRegistration(BlockEntityType.BED);
        } else {
            BlockEntityRendererProvider r7 =
                    ConfigCache.optimizeBeds ? BBEBedRenderer::new : BedRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.BED, r7);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.BELL)) {
            bbe$removeRegistration(BlockEntityType.BELL);
        } else {
            BlockEntityRendererProvider r8 =
                    ConfigCache.optimizeBells ? BBEBellRenderer::new : BellRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.BELL, r8);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.DECORATED_POT)) {
            bbe$removeRegistration(BlockEntityType.DECORATED_POT);
        } else {
            BlockEntityRendererProvider r9 =
                    ConfigCache.optimizeDecoratedPots ? BBEDecoratedPotRenderer::new : DecoratedPotRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.DECORATED_POT, r9);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityType.COPPER_GOLEM_STATUE)) {
            bbe$removeRegistration(BlockEntityType.COPPER_GOLEM_STATUE);
        } else {
            BlockEntityRendererProvider r10 =
                    ConfigCache.optimizeCopperGolemStatue ? BBECopperGolemStatueBlockRenderer::new : CopperGolemStatueBlockRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityType.COPPER_GOLEM_STATUE, r10);
        }
    }

    /*
        this is super shit but for some reason the renderer seem to be tied to if the block-entity itself gets added to a render section :/
        i.e. we cant just remove it, and we cant pass a null value. performance wise it should be fine as the "dummy" renderer basically does nothing
    */
    @Unique
    private static void bbe$removeRegistration(BlockEntityType<?> blockEntityType) {
        //BlockEntityRenderersAccessor.getProviders().remove(blockEntityType);
        BlockEntityRenderersAccessor.bbe$register(blockEntityType, ctx -> new BBEDummyRenderer());
    }
}
