package betterblockentities.client.model.texture;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SpriteMapper;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

/* java/misc */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * rebuild these so we can safely use them asynchronously when meshing
 * to avoid invoking these from each renderer or risking concurrency (not thread-safe)
 */
public class SpriteSelector {
    private static final ConcurrentHashMap<Identifier, SpriteId> BANNER_MATERIALS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Item, SpriteId> DECORATED_POT_MATERIALS = new ConcurrentHashMap<>();

    public static TextureAtlasSprite getBannerPatternSprite(Holder<BannerPattern> holder) {
        Identifier id = holder.value().assetId();
        SpriteMapper mapper = Sheets.BANNER_MAPPER;
        SpriteId material = BANNER_MATERIALS.computeIfAbsent(id, mapper::apply);
        return getBlockSprite(material.texture());
    }

    public static TextureAtlasSprite getDecoratedPotSideSprite(Optional<Item> optional) {
        if (optional.isPresent()) {
            SpriteId material = Sheets.getDecoratedPotSprite(DecoratedPotPatterns.getPatternFromItem((Item)optional.get()));
            if (material != null) {
                return getBlockSprite(material.texture());
            }
        }
        return getBlockSprite(Sheets.DECORATED_POT_SIDE.texture());
    }

    public static TextureAtlasSprite getCopperGolemStatueSprite(CopperGolemStatueBlock cgsBlock) {
        final Identifier texture = CopperGolemOxidationLevels.getOxidationLevel(cgsBlock.getWeatheringState()).texture();

        String path = texture.getPath();
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }

        Identifier strippedTexture = Identifier.withDefaultNamespace(path);
        return SpriteSelector.getBlockSprite(strippedTexture);
    }

    public static TextureAtlasSprite getChestSprite(BlockState state, BlockEntity blockEntity, boolean bl) {
        ChestRenderState.ChestMaterialType materialType = ChestRenderState.ChestMaterialType.REGULAR;

        if (blockEntity.getBlockState().getBlock() instanceof CopperChestBlock copperChestBlock) {
            switch (copperChestBlock.getState()) {
                case UNAFFECTED -> materialType = ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
                case EXPOSED -> materialType = ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
                case WEATHERED -> materialType = ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
                case OXIDIZED -> materialType = ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            };
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            materialType = ChestRenderState.ChestMaterialType.ENDER_CHEST;
        } else if (bl) {
            materialType = ChestRenderState.ChestMaterialType.CHRISTMAS;
        } else if (blockEntity instanceof TrappedChestBlockEntity) {
            materialType = ChestRenderState.ChestMaterialType.TRAPPED;
        }

        final ChestType type = state.hasProperty(ChestBlock.TYPE) ?
                state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;

        return getBlockSprite(Sheets.chooseSprite(materialType, type).texture());
    }

    public static TextureAtlasSprite getShulkerBoxSprite(ShulkerBoxBlock block) {
        DyeColor color = block.getColor();
        SpriteId shulkerMaterial = color == null ?
                Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(color);

        return getBlockSprite(shulkerMaterial.texture());
    }

    public static TextureAtlasSprite getBedSprite(BlockState state) {
        DyeColor color = ((BedBlock) state.getBlock()).getColor();
        SpriteId bedMaterial = Sheets.getBedSprite(color);

        return getBlockSprite(bedMaterial.texture());
    }

    public static TextureAtlasSprite getSignSprite(BlockState state) {
        WoodType woodType = ((SignBlock) state.getBlock()).type();
        SpriteId signMaterial = Sheets.getSignSprite(woodType);

        return getBlockSprite(signMaterial.texture());
    }

    public static TextureAtlasSprite getHangingSignSprite(BlockState state) {
        WoodType woodType = ((SignBlock) state.getBlock()).type();
        SpriteId signMaterial = Sheets.getHangingSignSprite(woodType);

        return getBlockSprite(signMaterial.texture());
    }

    public static TextureAtlasSprite getBlockSprite(Identifier id) {
        var atlas = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS);
        return atlas.getSprite(id);
    }
}
