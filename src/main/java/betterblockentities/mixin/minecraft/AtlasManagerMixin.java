package betterblockentities.mixin.minecraft;

import betterblockentities.resource.BBEAtlasRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    /*
        injects our custom atlas into the KNOWN_ATLASES list at class load.
        KNOWN_ATLASES is static final, so we modify the constant when the list is created.
        This is for a future implementation

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<AtlasManager.AtlasConfig> addCustomAtlasIterator(List<AtlasManager.AtlasConfig> original) {
        List<AtlasManager.AtlasConfig> modified = new ArrayList<>(original);

        //output png atlas spritesheet
        Identifier textureId = Identifier.withDefaultNamespace("textures/atlas/bbe_atlas.png");

        //inject atlas entry
        modified.add(new AtlasManager.AtlasConfig(textureId, BBEAtlasRegistry.BBE_ATLAS, true));

        return modified.iterator();
    }
     */
}
