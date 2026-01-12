package betterblockentities.chunk;

/* local */
import betterblockentities.model.ModelPartWrapper;
import betterblockentities.mixin.sodium.AbstractBlockRenderContextAccessor;

/* minecraft */
import betterblockentities.util.ModelTransform;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.caffeinemc.mods.sodium.client.render.model.*;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;

/* java/misc */
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockRenderHelper {
    private final BlockRenderer ctx;
    private final BlockEntity blockEntity;
    private Material material;
    private ChunkSectionLayer rendertype;
    private float rotation[];
    private int color = -1;

    /* this tag is used to push geometry to our own render pass */
    public static final int BBE_QUAD_TAG = "BBE-QUAD.HASH".hashCode();

    public BlockRenderHelper(BlockRenderer ctx, BlockEntity blockEntity) {
        this.ctx = ctx;
        this.blockEntity = blockEntity;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setRendertype(ChunkSectionLayer layer) {
        this.rendertype = layer;
    }

    public void setRotation(float rotation[]) {
        this.rotation = rotation;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /* rebuild of DefaultModelEmitter->emitModel */
    public static void emitModelPart(List<BlockModelPart> parts, MutableQuadViewImpl quad, BlockState state, Predicate<@Nullable Direction> cullTest, PlatformModelEmitter.Bufferer defaultBuffer) {
        for(int i = 0; i < parts.size(); ++i) {
            BlockModelPart part = (BlockModelPart)parts.get(i);
            defaultBuffer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }

    /* handles most our blocks, applies proper sprite, basic rotations, etc... */
    public void emitGE(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter) {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)ctx;

        TextureAtlasSprite sprite = this.material != null ? ModelTransform.getSprite(this.material.texture()) : null;
        float rotation = getRotationFromBlockState(acc.getState());

        MutableQuadViewImpl editorQuad = acc.getEmitterInvoke();
        acc.prepareAoInfoInvoke(part.useAmbientOcclusion());

        for(int i = 0; i <= 6; ++i) {
            Direction cullFace = ModelHelper.faceFromIndex(i);
            if (!cullTest.test(cullFace)) {
                AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, acc.getState(), this.rendertype, acc.getSlice(), acc.getPos());
                List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(acc.getLevel(), acc.getPos(), part, acc.getState(), cullFace, acc.getRandom(), this.rendertype);
                int count = quads.size();

                for(int j = 0; j < count; ++j) {
                    BakedQuad q = (BakedQuad)quads.get(j);
                    editorQuad.fromBakedQuad(q);
                    editorQuad.setCullFace(cullFace);
                    editorQuad.setRenderType(this.rendertype);
                    editorQuad.setAmbientOcclusion(ao.toTriState());

                    if (sprite != null)
                        ModelTransform.swapSprite(sprite, editorQuad);

                    if (this.rotation != null) {
                        ModelTransform.rotateX(editorQuad, this.rotation[0]);
                        ModelTransform.rotateY(editorQuad, this.rotation[1]);
                    }
                    else
                        ModelTransform.rotateY(editorQuad, rotation);

                    if (this.color != -1) {
                        int diffuseColor = this.color;
                        for (int vertex = 0; vertex < 4; ++vertex) {
                            editorQuad.setColor(vertex, diffuseColor);
                        }
                    }
                    editorQuad.setTag(BBE_QUAD_TAG);
                    emitter.accept(editorQuad);
                }
            }
        }
        editorQuad.clear();
    }

    /* custom implementation, this is a special case as we need some extra logic to map the sherds */
    public void emitDecoratedPotQuads(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter) {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)ctx;
        PotDecorations sherds = ((DecoratedPotBlockEntity)blockEntity).getDecorations();

        boolean skipTransform = part.getQuads(null).size() > 1;

        TextureAtlasSprite[] sideSprites = new TextureAtlasSprite[4];
        sideSprites[0] = sherds.back().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[1] = sherds.right().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[2] = sherds.front().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[3] = sherds.left().map(BlockRenderHelper::getSherdSprite).orElse(null);

        BlockState state = acc.getState();
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float rotation = getRotationFromFacing(facing);

        /* get base sprite */
        Material baseMaterial = Sheets.DECORATED_POT_BASE;
        Material sidesMaterial = Sheets.DECORATED_POT_SIDE;
        TextureAtlasSprite baseSprite = ModelTransform.getSprite(baseMaterial.texture());
        TextureAtlasSprite sidesSprite = ModelTransform.getSprite(sidesMaterial.texture());

        MutableQuadViewImpl editorQuad = acc.getEmitterInvoke();
        acc.prepareAoInfoInvoke(part.useAmbientOcclusion());
        ChunkSectionLayer renderType = PlatformModelAccess.getInstance().getPartRenderType(part, state, acc.getDefaultRenderType());
        ChunkSectionLayer defaultType = acc.getDefaultRenderType();
        acc.setDefaultRenderType(renderType);

        for (int i = 0; i <= 6; ++i) {
            Direction cullFace = ModelHelper.faceFromIndex(i);
            if (cullTest.test(cullFace)) continue;
            AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, state, renderType, acc.getSlice(), acc.getPos());
            List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(acc.getLevel(), acc.getPos(), part, state, cullFace, acc.getRandom(), renderType);
            int count = quads.size();

            for(int j = 0; j < count; ++j) {
                BakedQuad q = (BakedQuad)quads.get(j);
                editorQuad.fromBakedQuad(q);
                editorQuad.setCullFace(cullFace);
                editorQuad.setRenderType(ChunkSectionLayer.SOLID);
                editorQuad.setAmbientOcclusion(ao.toTriState());

                if (!skipTransform) {
                    Vector3f normal = editorQuad.faceNormal();
                    Direction dir = ModelPartWrapper.normalToDirection(normal).getOpposite();

                    int dirIndex = horizontalIndex(dir);
                    if (dirIndex == -1) {
                        emitter.accept(editorQuad);
                        continue;
                    }

                    TextureAtlasSprite sprite = sideSprites[dirIndex];
                    ModelTransform.swapSprite(sprite != null ? sprite : sidesSprite, editorQuad);
                }
                else {
                    ModelTransform.swapSprite(baseSprite, editorQuad);
                }
                ModelTransform.rotateY(editorQuad, rotation);

                editorQuad.setTag(BBE_QUAD_TAG);
                emitter.accept(editorQuad);
            }
        }
        editorQuad.clear();
        acc.setDefaultRenderType(defaultType);
    }

    /* parse item path into a texture path, retrieve stitched sherd sprite from the block atlas */
    private static TextureAtlasSprite getSherdSprite(Item item) {
        if (item == null) return null;
        String itemName = item.toString();
        String pattern = parseSherdName(itemName);
        Identifier spriteId = Identifier.withDefaultNamespace("entity/decorated_pot/" + pattern + "_pottery_pattern");
        return ModelTransform.getSprite(spriteId);
    }
    private static String parseSherdName(String id) {
        int colonIndex = id.indexOf(':');
        String path = colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
        if (path.endsWith("_pottery_sherd")) {
            return path.substring(0, path.length() - "_pottery_sherd".length());
        }
        return path;
    }

    /* map quad direction to sprite index */
    private static int horizontalIndex(Direction dir) {
        return switch (dir) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default -> -1;
        };
    }

    /* compute rotation degrees from block state property HORIZONTAL_FACING/FACING (cardinal directions) */
    private static float getRotationFromFacing(Direction facing) {
        return switch (facing) {
            case NORTH, DOWN -> 180f;
            case EAST  -> 270f;
            case SOUTH, UP -> 0f;
            case WEST  -> 90f;
        };
    }

    /* compute rotation degrees from block state property ROTATION_16 */
    public static float compute16StepRotation(BlockState state) {
        int rot = state.getValue(BlockStateProperties.ROTATION_16);
        return rot * 22.5f;
    }

    /* select what compute function to call based on block state */
    private static float getRotationFromBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return getRotationFromFacing(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        if (state.hasProperty(BlockStateProperties.ROTATION_16))
            return compute16StepRotation(state);
        return 0f;
    }
}
