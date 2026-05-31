package betterblockentities.mixin.render.immediate.blockentity;

/* mojang */
import com.mojang.blaze3d.vertex.VertexConsumer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Double")
public interface VertexMultiConsumerDoubleAccessor {
    @Accessor("first")
    VertexConsumer getFirst();
}
