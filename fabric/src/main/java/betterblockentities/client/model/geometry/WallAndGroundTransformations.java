package betterblockentities.client.model.geometry;

import net.minecraft.core.Direction;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

public class WallAndGroundTransformations<T> {
    private final Map<Direction, T> wallTransforms;
    private final T[] freeTransformations;

    public WallAndGroundTransformations(
            final Function<Direction, T> wallTransformationFactory, final IntFunction<T> freeTransformationFactory, final int segments
    ) {
        this.wallTransforms = Util.makeEnumMap(Direction.class, wallTransformationFactory);
        this.freeTransformations = (T[])(new Object[segments]);

        for (int segment = 0; segment < segments; segment++) {
            this.freeTransformations[segment] = (T)freeTransformationFactory.apply(segment);
        }
    }

    public T wallTransformation(final Direction facing) {
        return (T)this.wallTransforms.get(facing);
    }

    public T freeTransformations(final int segment) {
        return this.freeTransformations[segment];
    }
}
