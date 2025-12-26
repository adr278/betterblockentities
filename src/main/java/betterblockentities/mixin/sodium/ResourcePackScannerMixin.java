package betterblockentities.mixin.sodium;

/* minecraft */
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;

/* sodium */
import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/* java/misc */
import java.util.stream.Stream;

/*
    bypass this as we are loading our own core shaders. see DefaultTerrainRenderPasses
    mixin for more context to as why we are doing this
*/

@Pseudo
@Mixin(ResourcePackScanner.class)
public class ResourcePackScannerMixin {
    @Unique private static final String BBE_PACK_ID = "betterblockentities";

    @Redirect(method = "checkIfCoreShaderLoaded",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceManager;listPacks()Ljava/util/stream/Stream;"
            )
    )
    private static Stream<PackResources> skipBBEPack(ResourceManager manager) {
        return manager.listPacks()
                .filter(pack -> !(pack.packId().contains(BBE_PACK_ID)));
    }
}
