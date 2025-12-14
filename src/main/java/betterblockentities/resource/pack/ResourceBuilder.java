package betterblockentities.resource.pack;

/* local */
import betterblockentities.resource.model.ModelGenerator;

/* minecraft */
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

/* java/misc */
import java.util.*;

public class ResourceBuilder {
    /* generate the "in memory" resource pack to later be passed to our pack profile */
    public static byte[] buildZip() {
        PackMetadataBuilder meta = new PackMetadataBuilder();
        ModelGenerator models = new ModelGenerator();
        ResourcePackAssembler assembler = new ResourcePackAssembler();

        Map<String, byte[]> entries = new HashMap<>();
        entries.putAll(meta.createMetadataAndIcon());
        entries.putAll(models.generateAllModels());

        return assembler.assemble(entries);
    }

    /* builds the resource pack and its profile */
    public static Pack buildPackProfile() {
        byte[] packData = buildZip();
        PackResources pack = new BBEPack("betterblockentities-generated", packData);

        Pack.ResourcesSupplier factory = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return pack;
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return pack;
            }
        };

        PackSelectionConfig pos = new PackSelectionConfig(true, Pack.Position.TOP, true);
        return Pack.readMetaAndCreate(pack.location(), factory, PackType.CLIENT_RESOURCES, pos);
    }
}
