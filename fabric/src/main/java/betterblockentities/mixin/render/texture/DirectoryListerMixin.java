package betterblockentities.mixin.render.texture;

/* local */
import betterblockentities.client.render.texture.RawSpriteSourceBridge;

/* minecraft */
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.server.packs.resources.ResourceManager;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DirectoryLister.class)
public abstract class DirectoryListerMixin {
    private static final String RANDOM_ENTITY_SOURCE = "optifine/random/entity";

    @Shadow public abstract String sourcePath();
    @Shadow public abstract String idPrefix();

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void includeRandomEntityTextures(
            ResourceManager resourceManager,
            SpriteSource.Output output,
            CallbackInfo ci
    ) {
        String sourcePath = sourcePath();
        if (!sourcePath.startsWith(RANDOM_ENTITY_SOURCE)) return;

        RawSpriteSourceBridge.addDirectory(resourceManager, output, sourcePath, idPrefix());
        ci.cancel();
    }
}
