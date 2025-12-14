package betterblockentities.resource.pack;

/* gson */
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/* minecraft */
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

/* mojang */
import com.mojang.serialization.JsonOps;

/* java/misc */
import org.jetbrains.annotations.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BBEPack implements PackResources {
    private final String name;
    private final byte[] packData;
    private final Map<String, byte[]> entries = new HashMap<>();
    private final Map<PackType, Set<String>> namespaces = new EnumMap<>(PackType.class);

    public BBEPack(String name, byte[] packData) {
        this.name = name;
        this.packData = packData;
        buildCache();
    }

    private void buildCache() {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(packData))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] data = zip.readAllBytes();
                    entries.put(entry.getName(), data);

                    // Track namespaces for each type
                    for (PackType type : PackType.values()) {
                        String dir = type.getDirectory() + "/";
                        if (entry.getName().startsWith(dir)) {
                            String rest = entry.getName().substring(dir.length());
                            String namespace = rest.split("/", 2)[0];
                            namespaces.computeIfAbsent(type, t -> new HashSet<>()).add(namespace);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... segments) {
        String path = String.join("/", segments);
        byte[] data = entries.get(path);
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        String path = type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath();
        byte[] data = entries.get(path);
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput consumer) {
        String base = type.getDirectory() + "/" + namespace + "/" + prefix;
        entries.forEach((path, data) -> {
            if (path.startsWith(base)) {
                String relative = path.substring(type.getDirectory().length() + 1 + namespace.length() + 1);
                consumer.accept(Identifier.fromNamespaceAndPath(namespace, relative), () -> new ByteArrayInputStream(data));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return namespaces.getOrDefault(type, Collections.emptySet());
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
        IoSupplier<InputStream> input = getRootResource("pack.mcmeta");
        if (input == null) return null;
        try (InputStream stream = input.get()) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            if (!json.has(metadataSerializer.name())) return null;
            JsonElement section = json.get(metadataSerializer.name());
            return metadataSerializer.codec().parse(JsonOps.INSTANCE, section).result().orElse(null);
        }
    }

    @Override
    public PackLocationInfo location() {
        return new PackLocationInfo(name, Component.literal("BBE-generated"), PackSource.BUILT_IN, Optional.empty());
    }

    @Override
    public void close() {}
}
