package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.BetterBlockEntities;
import betterblockentities.ModelLoader;
import betterblockentities.gui.ConfigManager;
import betterblockentities.util.*;

/* sodium */
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

/* fabric */


/* minecraft */


/* mixin */
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Pseudo
@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Shadow @Final private Vector3f posOffset;
    @Shadow @Nullable private ColorProvider<BlockState> colorProvider;
    @Shadow @Final private ColorProviderRegistry colorProviderRegistry;

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        try {
            /* should always be valid once this function executes, same goes for (model, state, pos, origin) */
            Block block = state.getBlock();

            if (BlockEntityManager.isSupportedBlock(block) && !ConfigManager.CONFIG.master_optimize) {
                if (block instanceof BellBlock) return;
                if (block instanceof BedBlock) return;

                ci.cancel();
                return;
            }

            /* setup context */
            AbstractBlockRenderContextAccessor acc = setupContext(state, pos, origin);
            if (acc == null) return;
            final AbstractBlockRenderContext.BlockEmitter emitter = acc.getEmitterInvoke();

            /* SIGNS */
            if (block instanceof SignBlock || block instanceof CeilingHangingSignBlock) {
                ci.cancel();

                if (!ConfigManager.CONFIG.optimize_signs) return;

                emitter.(ModelTransform.rotateY(BlockRenderHelper.computeSignRotation(state)));
                ((FabricBlockStateModel) model).emitQuads(emitter, acc.getLevel(), pos, state, acc.getRandom(), acc::isFaceCulledInvoke);
                emitter.popTransform();
            }
            else if (block instanceof WallHangingSignBlock || block instanceof WallSignBlock) {
                if (!ConfigManager.CONFIG.optimize_signs)
                    ci.cancel();
            }

            /* SHULKERS, and CHESTS */
            else if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock) {
                boolean isShulker = block instanceof ShulkerBoxBlock;

                if ((isShulker && !ConfigManager.CONFIG.optimize_shulkers) || (!isShulker && !ConfigManager.CONFIG.optimize_chests))
                    return;

                ci.cancel();

                List<BlockModelPart> parts = model.collectParts(acc.getRandom());

                /* splice BlockModelParts from MultipartBlockStateModel */
                int quadThreshold = isShulker ? 10 : 6;
                Map<Boolean, List<BlockModelPart>> partitioned = parts.stream()
                        .collect(Collectors.partitioningBy(p -> p.getQuads(null).size() > quadThreshold));

                List<BlockModelPart> lidParts   = partitioned.get(true);
                List<BlockModelPart> trunkParts = partitioned.get(false);

                List<BlockModelPart> merged =  new ArrayList<>();

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                if (ConfigManager.CONFIG.updateType == 1)
                    merged.addAll(trunkParts);
                else {
                    if (shouldRender)
                        merged.addAll(trunkParts);
                }

                /* merge BlockModelParts after splicing */
                if (shouldRender) merged.addAll(lidParts);

                BlockRenderHelper.emitQuads(merged, emitter, acc::isFaceCulledInvoke);
            }

            /* BELLS */
            else if (block instanceof BellBlock) {
                if (!ConfigManager.CONFIG.optimize_bells) return;
                ci.cancel();

                RandomSource rand = acc.getRandom();
                List<BlockModelPart> bell_part = model.collectParts(rand);
                List<BlockModelPart> bell_body_part = new ArrayList<>();

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                if (shouldRender) {
                    try {
                        ModelManager manager = Minecraft.getInstance().getModelManager();
                        BlockStateModel bell_body = manager(ModelLoader.BELL_BODY_KEY);
                        bell_body_part.addAll(bell_body.collectParts(rand));
                    }
                    catch (Exception e) {
                        BetterBlockEntities.getLogger().error("Error: Retrieving bell body BlockModelPart at {}", pos, e);
                    }
                }

                List<BlockModelPart> merged = new ArrayList<>(bell_part);
                if (!bell_body_part.isEmpty())
                    merged.addAll(bell_body_part);
                BlockRenderHelper.emitQuads(merged, emitter, acc::isFaceCulledInvoke);
            }

            /* DECORATED POTS */
            else if (block instanceof DecoratedPotBlock) {
                if (!ConfigManager.CONFIG.optimize_decoratedpots) {
                    ci.cancel();
                    return;
                }

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                ci.cancel();
                if (!shouldRender) return;

                BlockRenderHelper.emitDecoratedPotQuads(model, state, emitter, (DecoratedPotBlockEntity)ext, acc.getRandom(), acc::isFaceCulledInvoke);
            }
            restoreContext();
        }
        catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: General fault in BlockRenderer at {}", pos, e);
        }
    }

    @Unique
    private boolean shouldRender(BlockEntityExt ext) {
        return ext == null || !ext.getRemoveChunkVariant();
    }

    /* safely retrieve block entity and an instance to our accessor  */
    @Unique
    private BlockEntityExt getBlockEntityInstance(BlockPos pos) {
        try {
            ClientLevel world = Minecraft.getInstance().level;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return (blockEntity instanceof BlockEntityExt bex) ? bex : null;
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: Getting Block Entity and accessor at {}", pos, e);
            return null;
        }
    }

    @Unique
    AbstractBlockRenderContextAccessor setupContext(BlockState state, BlockPos pos, BlockPos origin) {
        try {
            AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)(Object)this;
            acc.setState(state);
            acc.setPos(pos);
            acc.prepareAoInfoInvoke(true);

            this.posOffset.set(origin.getX(), origin.getY(), origin.getZ());
            if (state.hasOffsetFunction()) {
                Vec3 modelOffset = state.getOffset(pos);
                this.posOffset.add((float)modelOffset.x, (float)modelOffset.y, (float)modelOffset.z);
            }

            this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());
            acc.prepareCullingInvoke(true);
            acc.setDefaultRenderType(ItemBlockRenderTypes.getChunkRenderType(state));
            acc.setAllowDowngrade(true);
            acc.getRandom().setSeed(state.getSeed(pos));
            return acc;
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: Setting up BlockRenderer context failed! at {}", pos, e);
            return null;
        }
    }

    @Unique
    void restoreContext() {
        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)(Object)this;
        acc.setDefaultRenderType(null);
    }
}