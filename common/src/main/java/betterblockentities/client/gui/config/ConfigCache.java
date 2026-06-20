package betterblockentities.client.gui.config;

import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

public class ConfigCache {
        public static boolean chestAnims, shulkerAnims, bellAnims, potAnims,
                              signText, masterOptimize, christmasChests, optimizeChests,
                              optimizeSigns, optimizeDecoratedPots, optimizeBanners,
                              optimizeBells, optimizeShulker, optimizeCopperGolemStatue,
                              signTextCulling, optimizeShelves, optimizeCampfire, optimizeLectern,
                              chestMovingLighting, shulkerMovingLighting, bellMovingLighting;
        public static int signTextRenderDistance, updateType, bannerGraphics, bannerPose, shadeMode;

        public static boolean isImmediateLightingEnabled(byte optKind) {
                if (shadeMode != EnumTypes.ShadeMode.SODIUM.ordinal()) {
                        return false;
                }

                return switch (optKind) {
                        case InstancedBlockEntityManager.OptKind.CHEST,
                             InstancedBlockEntityManager.OptKind.SHULKER,
                             InstancedBlockEntityManager.OptKind.BELL,
                             InstancedBlockEntityManager.OptKind.POT -> true;
                        default -> false;
                };
        }

        public static boolean isMovingLightingEnabled(byte optKind) {
                if (shadeMode != EnumTypes.ShadeMode.SODIUM.ordinal()) {
                        return false;
                }

                return switch (optKind) {
                        case InstancedBlockEntityManager.OptKind.CHEST -> chestMovingLighting;
                        case InstancedBlockEntityManager.OptKind.SHULKER -> shulkerMovingLighting;
                        case InstancedBlockEntityManager.OptKind.BELL -> bellMovingLighting;
                        default -> false;
                };
        }
}

