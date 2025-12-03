package betterblockentities;

/* fabric */
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/* minecraft */
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class ModelLoader implements ModelLoadingPlugin
{
    public static final ExtraModelKey<BlockStateModel> BELL_BODY_KEY =
            ExtraModelKey.create(() -> "BellBody");

    public static final Identifier BELL_BODY_ID =
            Identifier.fromNamespaceAndPath("betterblockentities", "block/bell_body");

    @Override
    public void initialize(Context ctx) {
        ctx.addModel(BELL_BODY_KEY, SimpleUnbakedExtraModel.blockStateModel(BELL_BODY_ID));
    }
}


