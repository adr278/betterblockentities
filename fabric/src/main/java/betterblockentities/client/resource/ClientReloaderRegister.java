package betterblockentities.client.resource;

/* fabric */
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

/* minecraft */
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class ClientReloaderRegister {
    private ClientReloaderRegister() {
    }

    public static void register(
            Identifier reloaderId,
            PreparableReloadListener reloader,
            Identifier... dependencies
    ) {
        ResourceLoader resourceLoader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        resourceLoader.registerReloadListener(reloaderId, reloader);

        for (Identifier dependency : dependencies) {
            resourceLoader.addListenerOrdering(dependency, reloaderId);
        }
    }
}
