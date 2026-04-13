package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.resource.ClientReloaderRegister;

/* mojang */
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.opengl.GlStateManager;

/* minecraft */
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/* fabric */
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

/* java */
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/* annotations */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/* lwjgl */
import org.lwjgl.opengl.GL11;

public final class MapAtlasManager implements PreparableReloadListener {
    public static final Identifier ATLAS_TEXTURE = Identifier.fromNamespaceAndPath(
            "betterblockentities",
            "textures/atlas/maps.png"
    );
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(
            "betterblockentities",
            "map_atlas_manager"
    );

    private static @Nullable MapAtlasManager INSTANCE;

    private final MapAtlasTexture atlas;
    private final int maxSupportedTextureSize;
    private volatile MapAtlasBudgetPlanner.BudgetResult budget;

    private MapAtlasManager(TextureManager textureManager) {
        this.atlas = new MapAtlasTexture(ATLAS_TEXTURE);
        textureManager.register(ATLAS_TEXTURE, this.atlas);
        this.maxSupportedTextureSize = probeMaxSupportedTextureSize();
        this.budget = MapAtlasBudgetPlanner.computeBudget(this.maxSupportedTextureSize, 0);
        this.atlas.resize(this.budget.atlasWidth(), this.budget.atlasHeight());
    }

    public static void initialize(TextureManager textureManager) {
        if (INSTANCE != null) return;

        MapAtlasManager manager = new MapAtlasManager(textureManager);
        ClientReloaderRegister.register(
                RELOAD_LISTENER_ID,
                manager,
                ResourceReloaderKeys.Client.TEXTURES
        );
        INSTANCE = manager;
    }

    public static MapAtlasManager.@Nullable MapAtlasTexture atlasNullable() {
        return INSTANCE != null ? INSTANCE.atlas : null;
    }

    public static MapAtlasBudgetPlanner.@Nullable BudgetResult budgetNullable() {
        return INSTANCE != null ? INSTANCE.budget : null;
    }

    public static @Nullable MapAtlasRef refForSlot(int slotId) {
        return INSTANCE != null ? INSTANCE.refForSlotInternal(slotId) : null;
    }

    @Override public @NonNull CompletableFuture<Void> reload(
            PreparableReloadListener.@NonNull SharedState currentReload,
            @NonNull Executor taskExecutor,
            PreparableReloadListener.@NonNull PreparationBarrier preparationBarrier,
            @NonNull Executor reloadExecutor
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) {
            MapAtlasBudgetPlanner.BudgetResult disabledBudget = MapAtlasBudgetPlanner.computeBudget(
                    maxSupportedTextureSize,
                    0
            );
            disabledBudget = new MapAtlasBudgetPlanner.BudgetResult(
                    0,
                    0,
                    disabledBudget.maxTextureSize(),
                    0,
                    0
            );

            return preparationBarrier.wait(disabledBudget)
                    .thenAcceptAsync(budget -> {
                        this.budget = budget;
                        this.atlas.resize(0, 0);
                    }, reloadExecutor);
        }

        return CompletableFuture
                .supplyAsync(() -> MapAtlasBudgetPlanner.computeBudget(
                        maxSupportedTextureSize,
                        0
                ), taskExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(budget -> {
                    this.budget = budget;
                    this.atlas.resize(budget.atlasWidth(), budget.atlasHeight());

                    BBE.getLogger().info(
                            "Map atlas budget: capacity={}, chosen={}, max texture size={}, atlas={}x{}",
                            budget.exactFit(),
                            budget.safeBudget(),
                            budget.maxTextureSize(),
                            budget.atlasWidth(),
                            budget.atlasHeight()
                    );
                }, reloadExecutor);
    }

    private static int probeMaxSupportedTextureSize() {
        int maxReported = GlStateManager._getInteger(GL11.GL_MAX_TEXTURE_SIZE);

        for (int textureSize = Math.max(32768, maxReported); textureSize >= 1024; textureSize >>= 1) {
            GlStateManager._texImage2D(
                    GL11.GL_PROXY_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA,
                    textureSize,
                    textureSize,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    null
            );

            if (GlStateManager._getTexLevelParameter(
                    GL11.GL_PROXY_TEXTURE_2D,
                    0,
                    GL11.GL_TEXTURE_WIDTH
            ) != 0) {
                return textureSize;
            }
        }

        int fallback = Math.max(maxReported, 1024);
        BBE.getLogger().info(
                "Failed to determine map atlas max size by proxy probe, falling back to GL_MAX_TEXTURE_SIZE={}",
                fallback
        );
        return fallback;
    }

    private @Nullable MapAtlasRef refForSlotInternal(int slotId) {
        MapAtlasBudgetPlanner.BudgetResult budget = this.budget;
        if (slotId < 0 || slotId >= budget.safeBudget()) return null;

        int slotStride = MapAtlasBudgetPlanner.SLOT_STRIDE;
        int gutter = MapAtlasBudgetPlanner.PAGE_GUTTER;
        int slotsPerAxis = budget.atlasWidth() / slotStride;
        if (slotsPerAxis <= 0) return null;

        int slotX = (slotId % slotsPerAxis) * slotStride;
        int slotY = (slotId / slotsPerAxis) * slotStride;
        int innerX = slotX + gutter;
        int innerY = slotY + gutter;

        float invWidth = 1.0F / budget.atlasWidth();
        float invHeight = 1.0F / budget.atlasHeight();

        return new MapAtlasRef(
                slotX,
                slotY,
                innerX * invWidth,
                innerY * invHeight,
                (innerX + MapAtlasBudgetPlanner.PAGE_SIZE) * invWidth,
                (innerY + MapAtlasBudgetPlanner.PAGE_SIZE) * invHeight
        );
    }

    public static final class MapAtlasTexture extends AbstractTexture {
        private final Identifier textureId;
        private int width;
        private int height;

        private MapAtlasTexture(Identifier textureId) { this.textureId = textureId; }

        public void resize(int width, int height) {
            if (this.width == width && this.height == height && this.texture != null) {
                return;
            }

            this.width = width;
            this.height = height;
            this.close();

            if (width <= 0 || height <= 0) return;

            this.texture = RenderSystem.getDevice().createTexture(
                    this.textureId::toString,
                    5,
                    TextureFormat.RGBA8,
                    width,
                    height,
                    1,
                    1
            );
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, false);
        }

        public boolean isAllocated() { return this.texture != null; }
    }
}
