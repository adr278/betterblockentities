package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.render.immediate.blockentity.ShelfSlotMeshCacheAccess;

/* minecraft */
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/* java/misc */
import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ShelfBlockEntity.class)
public class ShelfBlockEntityMeshCacheMixin implements ShelfSlotMeshCacheAccess {

    @Unique private record SlotEntry(int epoch, long sig0, long sig1, Object mesh) {}

    @Unique private final AtomicReferenceArray<SlotEntry> slots = new AtomicReferenceArray<>(3);

    @Override public Object getSlotMesh(int slot, int epoch, long sig0, long sig1) {
        if (slot < 0 || slot >= 3) return null;

        SlotEntry e = slots.get(slot);
        if (e == null) return null;

        if (e.epoch() != epoch) return null;
        if (e.sig0() != sig0) return null;
        if (e.sig1() != sig1) return null;

        return e.mesh();
    }

    @Override public void putSlotMesh(int slot, int epoch, long sig0, long sig1, Object mesh) {
        if (slot < 0 || slot >= 3) return;

        slots.set(slot, new SlotEntry(epoch, sig0, sig1, mesh));
    }

    @Override public void clearSlotMeshes() {
        for (int i = 0; i < 3; i++) {
            slots.set(i, null);
        }
    }
}
