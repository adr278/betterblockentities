package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.util.ModelResourceUtil;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.tasks.ResourceTasks;
import betterblockentities.render.AltRenderers;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* java/misc */
import org.joml.Matrix4f;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BBEBlockRenderer {
    public static final int NO_QUAD_SPLITTING_TAG = "BBE-TS-QUAD-NO-SPLIT".hashCode();

    private static final Map<BlendMode, RenderMaterial> MATERIALS = buildMaterials();
    private static final Set<Material> MISSING_MATERIAL_SPRITES = ConcurrentHashMap.newKeySet();

    private final BBEEmitter emitter = new BBEEmitter();
    private final PoseStack poseStack = new PoseStack();

    public void emitTerrainBlockEntityGeometry(
            final MutableQuadViewImpl quadEmitter,
            final LevelSlice slice,
            final BlockPos pos,
            final BlockState state
    ) {
        if (!ConfigCache.masterOptimize || !state.hasBlockEntity()) {
            return;
        }

        ensureGeometryReady();

        final BlockEntity blockEntity = tryGetBlockEntity(slice, pos);
        if (!(blockEntity instanceof BlockEntityExt ext)) {
            return;
        }

        if (!ext.supportedBlockEntity()) {
            return;
        }
        if (!BBEConfig.OptEnabledTable.ENABLED[ext.optKind() & 0xFF]) {
            return;
        }
        if (AltRenderers.hasRendererOverride(blockEntity.getType())) {
            return;
        }

        this.emitter.bind(quadEmitter);
        final int light = LevelRenderer.getLightColor(slice, pos);
        final BlockState blockState = blockEntity.getBlockState();
        final var block = blockState.getBlock();

        if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
            if (ConfigCache.optimizeChests) {
                emitChest(blockEntity, ext, blockState, light);
            }
        } else if (block instanceof ShulkerBoxBlock shulkerBoxBlock) {
            if (ConfigCache.optimizeShulker) {
                emitShulker(ext, shulkerBoxBlock, blockState, light);
            }
        } else if (block instanceof BedBlock bedBlock) {
            if (ConfigCache.optimizeBeds) {
                emitBed(bedBlock, blockState, light);
            }
        } else if (block instanceof BellBlock) {
            if (ConfigCache.optimizeBells) {
                emitBell(ext, light);
            }
        } else if (block instanceof DecoratedPotBlock) {
            if (ConfigCache.optimizeDecoratedPots && blockEntity instanceof DecoratedPotBlockEntity decoratedPot) {
                emitDecoratedPot(ext, decoratedPot, light);
            }
        } else if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            if (ConfigCache.optimizeBanners && blockEntity instanceof BannerBlockEntity banner) {
                emitBanner(banner, blockState, light);
            }
        } else if (block instanceof StandingSignBlock || block instanceof WallSignBlock) {
            if (ConfigCache.optimizeSigns) {
                SignBlock signBlock = (SignBlock) block;
                emitSign(blockState, signBlock.type(), light);
            }
        } else if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
            if (ConfigCache.optimizeSigns) {
                SignBlock signBlock = (SignBlock) block;
                emitHangingSign(blockState, signBlock.type(), light);
            }
        }

        this.emitter.clearState();
    }

    private void emitChest(
            final BlockEntity blockEntity,
            final BlockEntityExt ext,
            final BlockState state,
            final int light
    ) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getChestLayer(state));
        if (template == null) {
            return;
        }

        final ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        final Material material = Sheets.chooseMaterial(blockEntity, type, ConfigCache.christmasChests);
        final TextureAtlasSprite sprite = spriteForMaterial(material);
        if (sprite == null) {
            return;
        }

        this.poseStack.setIdentity();
        final float angle = state.getValue(ChestBlock.FACING).toYRot();
        this.poseStack.translate(0.5F, 0.5F, 0.5F);
        this.poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        this.poseStack.translate(-0.5F, -0.5F, -0.5F);

        final boolean drawLid = shouldRender(ext);
        final boolean addBase = drawLid || ConfigCache.updateType != EnumTypes.UpdateSchedulerType.SMART.ordinal();

        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));

        final List<GeometryRegistry.QuadTemplate> bottom = template.quads("bottom");
        if (addBase) {
            if (!bottom.isEmpty()) {
                this.emitter.emit(bottom, light);
            } else {
                this.emitter.emit(template.quads(), light);
                return;
            }
        }

        if (drawLid) {
            final List<GeometryRegistry.QuadTemplate> lid = template.quads("lid");
            final List<GeometryRegistry.QuadTemplate> lock = template.quads("lock");

            if (!lid.isEmpty()) {
                this.emitter.emit(lid, light);
            }
            if (!lock.isEmpty()) {
                this.emitter.emit(lock, light);
            }
        }
    }

    private void emitShulker(
            final BlockEntityExt ext,
            final ShulkerBoxBlock block,
            final BlockState state,
            final int light
    ) {
        if (!shouldRender(ext)) {
            return;
        }

        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getShulkerBoxLayer());
        if (template == null) {
            return;
        }

        final DyeColor color = block.getColor();
        final Material material = color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.SHULKER_TEXTURE_LOCATION.get(color.getId());
        final TextureAtlasSprite sprite = spriteForMaterial(material);
        if (sprite == null) {
            return;
        }

        this.poseStack.setIdentity();
        final Direction facing = state.getValue(ShulkerBoxBlock.FACING);
        this.poseStack.translate(0.5F, 0.5F, 0.5F);
        this.poseStack.scale(0.9995F, 0.9995F, 0.9995F);
        this.poseStack.mulPose(facing.getRotation());
        this.poseStack.scale(1.0F, -1.0F, -1.0F);
        this.poseStack.translate(0.0F, -1.0F, 0.0F);

        this.emitter.setMaterial(toMaterial(BlendMode.CUTOUT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));
        this.emitter.emit(template.quads(), light);
    }

    private void emitBed(final BedBlock bedBlock, final BlockState state, final int light) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getBedLayer(state));
        if (template == null) {
            return;
        }

        final Material material = Sheets.BED_TEXTURES[bedBlock.getColor().getId()];
        final TextureAtlasSprite sprite = spriteForMaterial(material);
        if (sprite == null) {
            return;
        }

        final Direction facing = state.getValue(BedBlock.FACING);

        this.poseStack.setIdentity();
        this.poseStack.translate(0.0F, 0.5625F, 0.0F);
        this.poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        this.poseStack.translate(0.5F, 0.5F, 0.5F);
        this.poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + facing.toYRot()));
        this.poseStack.translate(-0.5F, -0.5F, -0.5F);

        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));
        this.emitter.emit(template.quads(), light);
    }

    private void emitBell(final BlockEntityExt ext, final int light) {
        if (!shouldRender(ext)) {
            return;
        }

        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getBellLayer());
        if (template == null) {
            return;
        }

        final TextureAtlasSprite sprite = spriteForMaterial(BellRenderer.BELL_RESOURCE_LOCATION);
        if (sprite == null) {
            return;
        }

        this.poseStack.setIdentity();
        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));
        this.emitter.emit(template.quads(), light);
    }

    private void emitDecoratedPot(
            final BlockEntityExt ext,
            final DecoratedPotBlockEntity pot,
            final int light
    ) {
        if (!shouldRender(ext)) {
            return;
        }

        final GeometryRegistry.ModelTemplate baseTemplate = GeometryRegistry.getModel(ModelResourceUtil.getDecoratedPotBaseLayer());
        final GeometryRegistry.ModelTemplate sideTemplate = GeometryRegistry.getModel(ModelResourceUtil.getDecoratedPotSideLayer());
        if (baseTemplate == null || sideTemplate == null) {
            return;
        }

        this.poseStack.setIdentity();
        final Direction facing = pot.getDirection();
        this.poseStack.translate(0.5D, 0.0D, 0.5D);
        this.poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        this.poseStack.translate(-0.5D, 0.0D, -0.5D);

        final Matrix4f transform = new Matrix4f(this.poseStack.last().pose());
        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setTransform(transform);

        final TextureAtlasSprite baseSprite = spriteForMaterial(Sheets.DECORATED_POT_BASE);
        if (baseSprite != null) {
            this.emitter.setSprite(baseSprite);
            this.emitter.emit(baseTemplate.quads(), light);
        }

        final PotDecorations decorations = pot.getDecorations();
        emitPotSide(sideTemplate, "front", getPotSideMaterial(decorations.front()), light);
        emitPotSide(sideTemplate, "back", getPotSideMaterial(decorations.back()), light);
        emitPotSide(sideTemplate, "left", getPotSideMaterial(decorations.left()), light);
        emitPotSide(sideTemplate, "right", getPotSideMaterial(decorations.right()), light);
    }

    private void emitPotSide(
            final GeometryRegistry.ModelTemplate sideTemplate,
            final String part,
            final Material material,
            final int light
    ) {
        final TextureAtlasSprite sprite = spriteForMaterial(material);
        if (sprite == null) {
            return;
        }

        this.emitter.setSprite(sprite);
        final List<GeometryRegistry.QuadTemplate> quads = sideTemplate.quads(part);
        if (quads.isEmpty()) {
            return;
        }
        this.emitter.emit(quads, light);
    }

    private void emitBanner(final BannerBlockEntity banner, final BlockState state, final int light) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getBannerLayer());
        if (template == null) {
            return;
        }

        this.poseStack.setIdentity();
        final boolean wall = state.getBlock() instanceof WallBannerBlock;
        if (!wall) {
            this.poseStack.translate(0.5F, 0.5F, 0.5F);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-RotationSegment.convertToDegrees(state.getValue(BannerBlock.ROTATION))));
        } else {
            this.poseStack.translate(0.5F, -0.16666667F, 0.5F);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(WallBannerBlock.FACING).toYRot()));
            this.poseStack.translate(0.0F, -0.3125F, -0.4375F);
        }
        this.poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));

        final TextureAtlasSprite poleSprite = spriteForMaterial(ModelBakery.BANNER_BASE);
        if (poleSprite != null) {
            this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
            this.emitter.setSprite(poleSprite);
            this.emitter.setColor(0xFFFFFFFF);
            if (!wall) {
                this.emitter.emit(template.quads("pole"), light);
            }
            this.emitter.emit(template.quads("bar"), light);
        }

        final List<GeometryRegistry.QuadTemplate> flagQuads = template.quads("flag");
        if (flagQuads.isEmpty()) {
            return;
        }
        final BlendMode flagBlend = ConfigCache.bannerGraphics == EnumTypes.BannerGraphicsType.FAST.ordinal()
                ? BlendMode.CUTOUT
                : BlendMode.TRANSLUCENT;
        final TextureAtlasSprite solidFlagSprite = spriteForMaterial(ModelBakery.BANNER_BASE);
        final TextureAtlasSprite basePatternSprite = spriteForMaterial(Sheets.BANNER_BASE);
        if (solidFlagSprite == null || basePatternSprite == null) {
            return;
        }

        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setDisableSplit(false);
        this.emitter.setSprite(solidFlagSprite);
        this.emitter.setColor(0xFFFFFFFF);
        this.emitter.emit(flagQuads, light);

        this.emitter.setMaterial(toMaterial(flagBlend));
        this.emitter.setDisableSplit(true);
        this.emitter.setSprite(basePatternSprite);
        this.emitter.setColor(banner.getBaseColor().getTextureDiffuseColor() | 0xFF000000);
        this.emitter.emit(flagQuads, light);

        final List<BannerPatternLayers.Layer> layers = banner.getPatterns().layers();
        for (int i = 0; i < 16 && i < layers.size(); i++) {
            final BannerPatternLayers.Layer layer = layers.get(i);
            final Material patternMaterial = Sheets.getBannerMaterial(layer.pattern());
            final TextureAtlasSprite sprite = spriteForMaterial(patternMaterial);
            if (sprite == null) {
                continue;
            }
            this.emitter.setSprite(sprite);
            this.emitter.setColor(layer.color().getTextureDiffuseColor() | 0xFF000000);
            this.emitter.emit(flagQuads, light);
        }

        this.emitter.setDisableSplit(false);
        this.emitter.setColor(0xFFFFFFFF);
    }

    private void emitSign(final BlockState state, final WoodType woodType, final int light) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getSignLayer(state));
        if (template == null) {
            return;
        }

        this.poseStack.setIdentity();
        if (state.getBlock() instanceof StandingSignBlock) {
            final float angle = RotationSegment.convertToDegrees(state.getValue(BlockStateProperties.ROTATION_16));
            this.poseStack.translate(0.5F, 0.5F, 0.5F);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        } else {
            final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            this.poseStack.translate(0.5F, 0.5F, 0.5F);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            this.poseStack.translate(0.0F, -0.3125F, -0.4375F);
        }
        this.poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        final TextureAtlasSprite sprite = spriteForMaterial(Sheets.getSignMaterial(woodType));
        if (sprite == null) {
            return;
        }

        this.emitter.setMaterial(toMaterial(BlendMode.DEFAULT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));

        final List<GeometryRegistry.QuadTemplate> signQuads = template.quads("sign");
        if (signQuads.isEmpty()) {
            this.emitter.emit(template.quads(), light);
            return;
        }

        this.emitter.emit(signQuads, light);
        if (state.getBlock() instanceof StandingSignBlock) {
            this.emitter.emit(template.quads("stick"), light);
        }
    }

    private void emitHangingSign(final BlockState state, final WoodType woodType, final int light) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(ModelResourceUtil.getHangingSignLayer(state));
        if (template == null) {
            return;
        }

        this.poseStack.setIdentity();
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            final float angle = RotationSegment.convertToDegrees(state.getValue(BlockStateProperties.ROTATION_16));
            this.poseStack.translate(0.5, 0.9375, 0.5);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
            this.poseStack.translate(0.0F, -0.3125F, 0.0F);
        } else {
            final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            this.poseStack.translate(0.5, 0.9375, 0.5);
            this.poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            this.poseStack.translate(0.0F, -0.3125F, 0.0F);
        }
        this.poseStack.scale(1.0F, -1.0F, -1.0F);

        final TextureAtlasSprite sprite = spriteForMaterial(Sheets.getHangingSignMaterial(woodType));
        if (sprite == null) {
            return;
        }

        this.emitter.setMaterial(toMaterial(BlendMode.CUTOUT));
        this.emitter.setSprite(sprite);
        this.emitter.setTransform(new Matrix4f(this.poseStack.last().pose()));
        final List<GeometryRegistry.QuadTemplate> boardQuads = template.quads("board");
        if (!boardQuads.isEmpty()) {
            this.emitter.emit(boardQuads, light);
        }

        final boolean wall = !(state.getBlock() instanceof CeilingHangingSignBlock);
        final boolean attached = !wall && state.getValue(CeilingHangingSignBlock.ATTACHED);

        if (wall) {
            this.emitter.emit(template.quads("plank"), light);
            this.emitter.emit(template.quads("normalChains"), light);
            return;
        }

        if (attached) {
            this.emitter.emit(template.quads("vChains"), light);
        } else {
            this.emitter.emit(template.quads("normalChains"), light);
        }
    }

    private static Material getPotSideMaterial(final Optional<Item> decorationItem) {
        if (decorationItem.isPresent()) {
            final Material material = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem(decorationItem.get()));
            if (material != null) {
                return material;
            }
        }
        return Sheets.DECORATED_POT_SIDE;
    }

    private static void ensureGeometryReady() {
        if (GeometryRegistry.getCache().isEmpty()) {
            ResourceTasks.populateGeometryRegistry();
        }
    }

    private static BlockEntity tryGetBlockEntity(final LevelSlice slice, final BlockPos pos) {
        try {
            return slice.getBlockEntity(pos);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean shouldRender(final BlockEntityExt ext) {
        return ext.renderingMode() == RenderingMode.TERRAIN;
    }

    private static TextureAtlasSprite spriteForMaterial(final Material material) {
        try {
            final TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(material.texture());
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) && MISSING_MATERIAL_SPRITES.add(material)) {
                betterblockentities.client.BBE.getLogger().warn(
                        "Missing material sprite for texture {} from atlas {}",
                        material.texture(),
                        material.atlasLocation()
                );
            }
            return sprite;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static RenderMaterial toMaterial(final BlendMode blendMode) {
        final RenderMaterial material = MATERIALS.get(blendMode);
        if (material != null) {
            return material;
        }
        return MATERIALS.get(BlendMode.DEFAULT);
    }

    private static Map<BlendMode, RenderMaterial> buildMaterials() {
        final Map<BlendMode, RenderMaterial> materials = new EnumMap<>(BlendMode.class);
        final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            return materials;
        }

        final MaterialFinder finder = renderer.materialFinder();
        for (BlendMode mode : BlendMode.values()) {
            materials.put(mode, finder.clear().blendMode(mode).find());
        }
        return materials;
    }
}
