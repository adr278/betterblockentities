package betterblockentities.util;

/* fabric */
import betterblockentities.mixin.sodium.AbstractBlockRenderContextAccessor;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.caffeinemc.mods.sodium.client.render.model.*;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;

/* minecraft */


/* java/misc */
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockRenderHelper {
    private final BlockRenderer ctx;
    private final BlockEntity blockEntity;

    public BlockRenderHelper(BlockRenderer ctx, BlockEntity blockEntity) {
        this.ctx = ctx;
        this.blockEntity = blockEntity;
    }

    /* rebuild of DefaultModelEmitter->emitModel */
    public static void emitModelPart(List<BlockModelPart> parts, MutableQuadViewImpl quad, BlockState state, Predicate<@Nullable Direction> cullTest, PlatformModelEmitter.Bufferer defaultBuffer) {
        if (quad instanceof AbstractBlockRenderContext.BlockEmitter emitter) {
            ChunkSectionLayer type = ItemBlockRenderTypes.getChunkRenderType(state);

            for(int i = 0; i < parts.size(); ++i) {
                if (PlatformModelAccess.getInstance().getPartRenderType((BlockModelPart)parts.get(i), state, type) != type) {
                    emitter.markInvalidToDowngrade();
                    break;
                }
            }
        }
        for(int i = 0; i < parts.size(); ++i) {
            BlockModelPart part = (BlockModelPart)parts.get(i);
            defaultBuffer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }

    /*
        TODO:
         Implement a generic emit quads function that applies a transform
         that shrinks the quads uvs towards the center by a few texels
         use on models with "seems" at edges
    */

    /* added rotation transform */
    public void emitSignQuads(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter) {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)ctx;

        MutableQuadViewImpl editorQuad = acc.getEmitterInvoke();
        acc.prepareAoInfoInvoke(part.useAmbientOcclusion());
        ChunkSectionLayer renderType = PlatformModelAccess.getInstance().getPartRenderType(part, acc.getState(), acc.getDefaultRenderType());
        ChunkSectionLayer defaultType = acc.getDefaultRenderType();
        acc.setDefaultRenderType(renderType);

        for(int i = 0; i <= 6; ++i) {
            Direction cullFace = ModelHelper.faceFromIndex(i);
            if (!cullTest.test(cullFace)) {
                AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, acc.getState(), renderType, acc.getSlice(), acc.getPos());
                List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(acc.getLevel(), acc.getPos(), part, acc.getState(), cullFace, acc.getRandom(), renderType);
                int count = quads.size();

                for(int j = 0; j < count; ++j) {
                    BakedQuad q = (BakedQuad)quads.get(j);
                    editorQuad.fromBakedQuad(q);
                    editorQuad.setCullFace(cullFace);
                    editorQuad.setRenderType(ChunkSectionLayer.CUTOUT);
                    editorQuad.setAmbientOcclusion(ao.toTriState());

                    ModelTransform.rotateY(editorQuad, compute16StepRotation(acc.getState()));
                    emitter.accept(editorQuad);
                }
            }
        }
        editorQuad.clear();
        acc.setDefaultRenderType(defaultType);
    }

    /* custom impl. maps the sherds from the block entity data and swaps the "side" quads sprites to the appropriate one */
    public void emitDecoratedPotQuads(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter) {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)ctx;
        PotDecorations sherds = ((DecoratedPotBlockEntity)blockEntity).getDecorations();

        boolean skipTransform = part.getQuads(null).size() > 10;

        TextureAtlasSprite[] sideSprites = new TextureAtlasSprite[4];
        sideSprites[0] = sherds.back().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[1] = sherds.right().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[2] = sherds.front().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[3] = sherds.left().map(BlockRenderHelper::getSherdSprite).orElse(null);

        BlockState state = acc.getState();
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        int facingIndex = horizontalIndex(facing);

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
                editorQuad.setRenderType(ChunkSectionLayer.CUTOUT);
                editorQuad.setAmbientOcclusion(ao.toTriState());

                int dirIndex = horizontalIndex(q.direction());
                if (skipTransform || dirIndex == -1) {
                    emitter.accept(editorQuad);
                    continue;
                }

                int delta = (dirIndex - facingIndex + 4) % 4;
                TextureAtlasSprite sprite = sideSprites[delta];

                if (sprite != null) {
                    ModelTransform.swapSprite(sprite, editorQuad);
                }
                emitter.accept(editorQuad);
            }
        }
        editorQuad.clear();
        acc.setDefaultRenderType(defaultType);
    }

    public void emitBannerQuads(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter) {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)ctx;
        BannerBlockEntity bannerBlockEntity = (BannerBlockEntity)this.blockEntity;
        BlockState state = acc.getState();

        MutableQuadViewImpl editorQuad = acc.getEmitterInvoke();
        acc.prepareAoInfoInvoke(part.useAmbientOcclusion());
        ChunkSectionLayer renderType = PlatformModelAccess.getInstance().getPartRenderType(part, state, acc.getDefaultRenderType());
        ChunkSectionLayer defaultType = acc.getDefaultRenderType();
        acc.setDefaultRenderType(renderType);

        int quadThreshold = 6;
        int quadSize = part.getQuads(null).size();
        boolean isBaseModel = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? quadSize < quadThreshold : quadSize > quadThreshold;

        /* base (bar and/or pole) */
        if (isBaseModel) {
            for (int i = 0; i <= 6; ++i) {
                Direction cullFace = ModelHelper.faceFromIndex(i);
                if (cullTest.test(cullFace)) continue;
                AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, state, renderType, acc.getSlice(), acc.getPos());
                List<BakedQuad> quads = part.getQuads(null);
                int count = quads.size();

                for(int j = 0; j < count; ++j) {
                    BakedQuad q = (BakedQuad)quads.get(j);
                    editorQuad.fromBakedQuad(q);
                    editorQuad.setCullFace(cullFace);
                    editorQuad.setRenderType(ChunkSectionLayer.SOLID);
                    editorQuad.setAmbientOcclusion(ao.toTriState());
                    editorQuad.setShadeMode(SodiumShadeMode.VANILLA);

                    /*
                        this model does not have the rotation blockstate property so this just flips the model
                        we should change the block model file instead. todo I guess
                    */
                    float rotation = (compute16StepRotation(state) + 180f) % 360f;
                    ModelTransform.rotateY(editorQuad, rotation);

                    emitter.accept(editorQuad);
                }
            }
        }

        /* canvas */
        else {
            /* base canvas aka "layer 0" */
            for (int i = 0; i <= 6; ++i) {
                Direction cullFace = ModelHelper.faceFromIndex(i);
                if (cullTest.test(cullFace)) continue;
                AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, state, renderType, acc.getSlice(), acc.getPos());
                List<BakedQuad> quads = part.getQuads(null);
                int count = quads.size();

                int quadIdx = 1;
                for(int j = 0; j < count; ++j) {
                    BakedQuad q = (BakedQuad)quads.get(j);
                    editorQuad.fromBakedQuad(q);
                    editorQuad.setCullFace(cullFace);
                    editorQuad.setRenderType(ChunkSectionLayer.TRANSLUCENT);
                    editorQuad.setAmbientOcclusion(ao.toTriState());
                    editorQuad.setShadeMode(SodiumShadeMode.VANILLA);

                    float rotation = (compute16StepRotation(state) + 180f) % 360f;
                    ModelTransform.rotateY(editorQuad, rotation);

                    DyeColor baseLayerColor = bannerBlockEntity.getBaseColor();
                    int color = baseLayerColor.getTextureDiffuseColor();

                    for (int vertex = 0; vertex < 4; ++vertex) {
                        editorQuad.setColor(vertex, color);
                    }

                    emitter.accept(editorQuad);
                    quadIdx++;
                }
            }
            editorQuad.clear();

            /* canvas layers, each layer is pushed out and expanded by an offset to prevent z fighting */
            BannerPatternLayers component = bannerBlockEntity.getPatterns();
            List<BannerPatternLayers.Layer> layers = component.layers();
            if (!layers.isEmpty()) {
                int layerIndex = 1;
                for (BannerPatternLayers.Layer layer : layers) {
                    Holder<BannerPattern> entry = layer.pattern();
                    BannerPattern pattern = entry.value();
                    Identifier texture = pattern.assetId();

                    String stripped = texture.toString().replaceFirst("^minecraft:", "");
                    String resultPath = "entity/banner/" + stripped;
                    Identifier resultId = Identifier.withDefaultNamespace(resultPath);

                    TextureAtlasSprite sprite = ModelTransform.getSprite(resultId);

                    DyeColor layerColor = layer.color();
                    int colorLayer = layerColor.getTextureDiffuseColor();

                    for (int i = 0; i <= 6; ++i) {
                        Direction cullFace = ModelHelper.faceFromIndex(i);
                        if (cullTest.test(cullFace)) continue;
                        AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, state, renderType, acc.getSlice(), acc.getPos());
                        List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(acc.getLevel(), acc.getPos(), part, state, cullFace, acc.getRandom(), renderType);

                        for (int j = 0; j < quads.size(); ++j) {
                            BakedQuad q = quads.get(j);
                            editorQuad.fromBakedQuad(q);

                            editorQuad.setCullFace(cullFace);
                            editorQuad.setRenderType(ChunkSectionLayer.TRANSLUCENT);
                            editorQuad.setAmbientOcclusion(ao.toTriState());
                            editorQuad.setShadeMode(SodiumShadeMode.VANILLA);

                            /* apply rotation, invert because model is rotated wrong in model file, fix this too */
                            float rotation = (compute16StepRotation(state) + 180f) % 360f;
                            ModelTransform.rotateY(editorQuad, rotation);

                            /* swap this quads sprite to mapped sherd texture */
                            ModelTransform.swapSprite(sprite, editorQuad);

                            /* apply layer color to all vertices */
                            for (int vertex = 0; vertex < 4; ++vertex) {
                                editorQuad.setColor(vertex, colorLayer);
                            }

                            /* push out along normal and expand by same amount */
                            float offset = layerIndex * 0.0001f;
                            ModelTransform.pushAndExpand(offset, editorQuad);

                            /* "emit" */
                            emitter.accept(editorQuad);
                        }
                    }
                    editorQuad.clear();
                    layerIndex++;
                }
            }
            editorQuad.clear();
            acc.setDefaultRenderType(defaultType);
        }
    }

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

    private static int horizontalIndex(Direction dir) {
        return switch (dir) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default -> -1;
        };
    }

    public float compute16StepRotation(BlockState state) {
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int rot = state.getValue(BlockStateProperties.ROTATION_16);
            return rot * 22.5f;
        }
        return 0f;
    }
}
