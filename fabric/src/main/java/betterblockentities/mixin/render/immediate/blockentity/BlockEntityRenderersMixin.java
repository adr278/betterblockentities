package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.mixin.accessors.BlockEntityRenderersAccessor;
import betterblockentities.render.AltRenderers;
import betterblockentities.client.render.immediate.blockentity.renderers.*;

/* minecraft */
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

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
        if (AltRenderers.hasRendererOverride(BlockEntityTypes.SIGN)) {
            bbe$removeRegistration(BlockEntityTypes.SIGN);
        } else {
            BlockEntityRendererProvider r0 = StandingSignRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.SIGN, r0);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.HANGING_SIGN)) {
            bbe$removeRegistration(BlockEntityTypes.HANGING_SIGN);
        } else {
            BlockEntityRendererProvider r1 = HangingSignRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.HANGING_SIGN, r1);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.CHEST)) {
            bbe$removeRegistration(BlockEntityTypes.CHEST);
        } else {
            BlockEntityRendererProvider r2 = ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.CHEST, r2);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.ENDER_CHEST)) {
            bbe$removeRegistration(BlockEntityTypes.ENDER_CHEST);
        } else {
            BlockEntityRendererProvider r3 = ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.ENDER_CHEST, r3);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.TRAPPED_CHEST)) {
            bbe$removeRegistration(BlockEntityTypes.TRAPPED_CHEST);
        } else {
            BlockEntityRendererProvider r4 = ChestRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.TRAPPED_CHEST, r4);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.BANNER)) {
            bbe$removeRegistration(BlockEntityTypes.BANNER);
        } else {
            BlockEntityRendererProvider r5 = BannerRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.BANNER, r5);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.SHULKER_BOX)) {
            bbe$removeRegistration(BlockEntityTypes.SHULKER_BOX);
        } else {
            BlockEntityRendererProvider r6 = ShulkerBoxRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.SHULKER_BOX, r6);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.BELL)) {
            bbe$removeRegistration(BlockEntityTypes.BELL);
        } else {
            BlockEntityRendererProvider r8 = BellRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.BELL, r8);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.DECORATED_POT)) {
            bbe$removeRegistration(BlockEntityTypes.DECORATED_POT);
        } else {
            BlockEntityRendererProvider r9 = DecoratedPotRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.DECORATED_POT, r9);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.COPPER_GOLEM_STATUE)) {
            bbe$removeRegistration(BlockEntityTypes.COPPER_GOLEM_STATUE);
        } else {
            BlockEntityRendererProvider r10 = CopperGolemStatueBlockRenderer::new;
            BlockEntityRenderersAccessor.bbe$register(BlockEntityTypes.COPPER_GOLEM_STATUE, r10);
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
