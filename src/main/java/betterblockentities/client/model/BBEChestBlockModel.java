package betterblockentities.client.model;

/* local */
import betterblockentities.client.gui.ConfigManager;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.chest.ChestModel;

public class BBEChestBlockModel extends ChestModel {
    public BBEChestBlockModel(ModelPart root) {
        super(root);
        if (ConfigManager.CONFIG.updateType == 1) {
            root.getChild("bottom").skipDraw = true;
            root.getChild("bottom").visible = false;
        }
    }
}
