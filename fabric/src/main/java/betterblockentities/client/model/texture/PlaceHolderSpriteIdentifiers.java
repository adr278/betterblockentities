package betterblockentities.client.model.texture;

import net.minecraft.resources.Identifier;

public final class PlaceHolderSpriteIdentifiers {
    public static final Identifier CHEST = Identifier.withDefaultNamespace("entity/chest/normal");
    public static final Identifier BELL_BODY = Identifier.withDefaultNamespace("entity/bell/bell_body");
    public static final Identifier DECORATED_POT_BASE = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_base");
    public static final Identifier DECORATED_POT_SIDES = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_side");
    public static final Identifier SHULKER = Identifier.withDefaultNamespace("entity/shulker/shulker");
    public static final Identifier BANNER = Identifier.withDefaultNamespace("entity/banner_base");
    public static final Identifier COPPER_GOLEM_STATUE = Identifier.withDefaultNamespace("entity/copper_golem/copper_golem");

    public static final Identifier[] ALL = {
            CHEST,
            BELL_BODY,
            DECORATED_POT_BASE,
            DECORATED_POT_SIDES,
            SHULKER,
            BANNER,
            COPPER_GOLEM_STATUE
    };
}
