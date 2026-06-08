package betterblockentities.render;

/* minecraft */
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface AltRendererProvider<T extends BlockEntity> {
    AltRenderer<T> create(AltRendererProvider.Context context);

    @Environment(EnvType.CLIENT)
    public static class Context {
        private final AltRenderDispatcher blockEntityRenderDispatcher;
        private final BlockRenderDispatcher blockRenderDispatcher;
        private final ItemRenderer itemRenderer;
        private final EntityRenderDispatcher entityRenderer;
        private final EntityModelSet modelSet;
        private final Font font;

        public Context(
                AltRenderDispatcher blockEntityRenderDispatcher,
                BlockRenderDispatcher blockRenderDispatcher,
                ItemRenderer itemRenderer,
                EntityRenderDispatcher entityRenderDispatcher,
                EntityModelSet entityModelSet,
                Font font
        ) {
            this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
            this.blockRenderDispatcher = blockRenderDispatcher;
            this.itemRenderer = itemRenderer;
            this.entityRenderer = entityRenderDispatcher;
            this.modelSet = entityModelSet;
            this.font = font;
        }

        public AltRenderDispatcher getBlockEntityRenderDispatcher() {
            return this.blockEntityRenderDispatcher;
        }

        public BlockRenderDispatcher getBlockRenderDispatcher() {
            return this.blockRenderDispatcher;
        }

        public EntityRenderDispatcher getEntityRenderer() {
            return this.entityRenderer;
        }

        public ItemRenderer getItemRenderer() {
            return this.itemRenderer;
        }

        public EntityModelSet getModelSet() {
            return this.modelSet;
        }

        public ModelPart bakeLayer(ModelLayerLocation modelLayerLocation) {
            return this.modelSet.bakeLayer(modelLayerLocation);
        }

        public Font getFont() {
            return this.font;
        }
    }
}
