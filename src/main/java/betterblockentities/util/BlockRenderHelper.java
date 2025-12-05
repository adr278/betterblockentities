package betterblockentities.util;

/* fabric */

import betterblockentities.mixin.sodium.AbstractBlockRenderContextAccessor;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
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
                    editorQuad.setRenderType(renderType);
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
                editorQuad.setRenderType(renderType);
                editorQuad.setAmbientOcclusion(ao.toTriState());

                int dirIndex = horizontalIndex(q.direction());
                if (skipTransform || dirIndex == -1) {
                    emitter.accept(editorQuad);
                    continue;
                }

                int delta = (dirIndex - facingIndex + 4) % 4;
                TextureAtlasSprite sprite = sideSprites[delta];

                if (sprite != null) {
                    ModelTransform.swapSpriteCached(sprite, editorQuad);
                }
                emitter.accept(editorQuad);
            }
        }
        editorQuad.clear();
        acc.setDefaultRenderType(defaultType);
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
