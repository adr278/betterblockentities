package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityExt {
    @Unique private boolean justReceivedUpdate = false;
    @Unique private boolean shouldRemoveChunkVariant = false;

    @Override public boolean getJustReceivedUpdate() { return justReceivedUpdate;}
    @Override public void setJustReceivedUpdate(boolean value) { this.justReceivedUpdate = value; }

    @Override public void setRemoveChunkVariant(boolean value) { this.shouldRemoveChunkVariant = value; }
    @Override public boolean getRemoveChunkVariant() {return this.shouldRemoveChunkVariant; }
}