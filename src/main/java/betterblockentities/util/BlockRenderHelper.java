package betterblockentities.util;

/* fabric */

import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;

/* minecraft */


/* java/misc */
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class BlockRenderHelper {
    /* rebuild, takes List<BlockModelPart> instead of the whole model */
    public static void emitQuads(List<BlockModelPart> parts, QuadEmitter emitter, Predicate<@Nullable Direction> cullTest) {
        final int partCount = parts.size();
        for (int i = 0; i < partCount; i++) {
            parts.get(i).emitQuads(emitter, cullTest);
        }
    }

    /* custom emitQuads implementation with QuadTransforms for dynamically swaping the sprite of each quad */
    public static void emitDecoratedPotQuads(BlockStateModel model, BlockState state, QuadEmitter emitter, DecoratedPotBlockEntity blockEntity, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        Sherds sherds = blockEntity.getSherds();

        Sprite[] sideSprites = new Sprite[4];
        sideSprites[0] = sherds.back().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[1] = sherds.right().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[2] = sherds.front().map(BlockRenderHelper::getSherdSprite).orElse(null);
        sideSprites[3] = sherds.left().map(BlockRenderHelper::getSherdSprite).orElse(null);

        QuadTransform[] sideTransforms = new QuadTransform[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = sideSprites[i];
            sideTransforms[i] = (s != null) ? ModelTransform.swapSpriteCached(s) : null;
        }

        int facingIndex = horizontalIndex(state.getValue(DecoratedPotBlock.HORIZONTAL_FACING));

        for (BlockModelPart part : model.collectParts(random)) {
            boolean skipTransform = part.getQuads(null).size() > 10;
            TriState ao = part.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;

            for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
                Direction cullFace = ModelHelper.faceFromIndex(i);
                if (cullTest.test(cullFace)) continue;

                for (BakedQuad quad : part.getQuads(cullFace)) {

                    emitter.cullFace(cullFace);
                    emitter.fromBakedQuad(quad);
                    emitter.ambientOcclusion(ao);
                    emitter.shadeMode(ShadeMode.VANILLA);

                    if (skipTransform) {
                        emitter.emit();
                        continue;
                    }

                    int dirIndex = horizontalIndex(quad.face());
                    if (dirIndex == -1) {
                        emitter.emit(); // skip top + bottom faces
                        continue;
                    }

                    /* compute rotated index for 180° swap */
                    int delta = (dirIndex - facingIndex + 4) % 4;
                    QuadTransform transform = sideTransforms[delta];

                    if (transform != null) {
                        emitter.pushTransform(transform);
                        emitter.emit();
                        emitter.popTransform();
                    } else {
                        emitter.emit();
                    }
                }
            }
        }
    }

    private static Sprite getSherdSprite(Item item) {
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

    /* compute rotation angle in degrees */
    public static float computeSignRotation(BlockState state) {
        if (state.getValue(SignBlock.)) {
            int rot = state.get(SignBlock.ROTATION);
            return rot * 22.5f;
        }
        return 0f;
    }
}
