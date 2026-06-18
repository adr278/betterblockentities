package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.render.AltRenderers;
import betterblockentities.client.gui.config.ConfigCache;
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
    private static void replaceVanillaRenderers(CallbackInfoReturnable<Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>>> cir) {
        if (AltRenderers.hasRendererOverride(BlockEntityTypes.SIGN)) {
            removeRegistration(BlockEntityTypes.SIGN);
        } else {
            BlockEntityRendererProvider r0 = StandingSignRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.SIGN, r0);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.HANGING_SIGN)) {
            removeRegistration(BlockEntityTypes.HANGING_SIGN);
        } else {
            BlockEntityRendererProvider r1 = HangingSignRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.HANGING_SIGN, r1);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.CHEST)) {
            removeRegistration(BlockEntityTypes.CHEST);
        } else {
            BlockEntityRendererProvider r2 = ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.CHEST, r2);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.ENDER_CHEST)) {
            removeRegistration(BlockEntityTypes.ENDER_CHEST);
        } else {
            BlockEntityRendererProvider r3 = ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.ENDER_CHEST, r3);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.TRAPPED_CHEST)) {
            removeRegistration(BlockEntityTypes.TRAPPED_CHEST);
        } else {
            BlockEntityRendererProvider r4 = ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.TRAPPED_CHEST, r4);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.BANNER)) {
            removeRegistration(BlockEntityTypes.BANNER);
        } else {
            BlockEntityRendererProvider r5 = BannerRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.BANNER, r5);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.SHULKER_BOX)) {
            removeRegistration(BlockEntityTypes.SHULKER_BOX);
        } else {
            BlockEntityRendererProvider r6 = ShulkerBoxRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.SHULKER_BOX, r6);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.BELL)) {
            removeRegistration(BlockEntityTypes.BELL);
        } else {
            BlockEntityRendererProvider r8 = BellRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.BELL, r8);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.DECORATED_POT)) {
            removeRegistration(BlockEntityTypes.DECORATED_POT);
        } else {
            BlockEntityRendererProvider r9 = DecoratedPotRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.DECORATED_POT, r9);
        }

        if (AltRenderers.hasRendererOverride(BlockEntityTypes.COPPER_GOLEM_STATUE)) {
            removeRegistration(BlockEntityTypes.COPPER_GOLEM_STATUE);
        } else {
            BlockEntityRendererProvider r10 = CopperGolemStatueBlockRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(BlockEntityTypes.COPPER_GOLEM_STATUE, r10);
        }
    }

    /*
        this is super shit but for some reason the renderer seem to be tied to if the block-entity itself gets added to a render section :/
        i.e. we cant just remove it, and we cant pass a null value. performance wise it should be fine as the "dummy" renderer basically does nothing
    */
    @Unique
    private static void removeRegistration(BlockEntityType<?> blockEntityType) {
        //BlockEntityRenderersAccessor.getProviders().remove(blockEntityType);
        BlockEntityRenderersAccessor.invokeRegister(blockEntityType, ctx -> new BBEDummyRenderer());
    }
}
