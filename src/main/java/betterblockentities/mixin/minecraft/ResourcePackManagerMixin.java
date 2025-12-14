package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.resource.pack.ResourceBuilder;

/* minecraft */
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/* java/misc */
import java.util.Map;

/* inject our RRP */
@Mixin(PackRepository.class)
public class ResourcePackManagerMixin
{
    @Inject(method = "discoverAvailable", at =
        @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void injectGeneratedPackProfiles(CallbackInfoReturnable<Map<String, Pack>> cir, Map<String, Pack> map) {
        Pack generated = ResourceBuilder.buildPackProfile();

        /* remove the old profile if it exists */
        map.remove(generated.getId());

        if (generated != null && !map.containsKey(generated.getId())) {
            map.put(generated.getId(), generated);
        }
    }
}