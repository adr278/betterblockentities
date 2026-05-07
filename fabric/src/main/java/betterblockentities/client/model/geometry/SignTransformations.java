package betterblockentities.client.model.geometry;

import com.mojang.math.Transformation;

public record SignTransformations(Transformation body) {
    public static final SignTransformations IDENTITY = new SignTransformations(
            Transformation.identity()
    );
}
