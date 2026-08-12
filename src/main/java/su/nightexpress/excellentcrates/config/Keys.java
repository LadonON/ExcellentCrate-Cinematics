package su.nightexpress.excellentcrates.config;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;

public class Keys {

    public static NamespacedKey crateId;
    public static NamespacedKey keyId;
    public static NamespacedKey linkToolCrateId;
    /** Optional Nexo model id, set when the link tool should place a custom block before linking. */
    public static NamespacedKey linkToolNexoModel;

    // Capture tool: one shared tool item stamps which cinematic scene, which kind of location, and
    // (for camera keyframes) which frame the next right-click should write to.
    public static NamespacedKey captureToolSceneId;
    public static NamespacedKey captureToolTarget;
    public static NamespacedKey captureToolFrameIndex;

    public static void load(@NotNull CratesPlugin plugin) {
        crateId = new NamespacedKey(plugin, "crate.id");
        keyId = new NamespacedKey(plugin, "crate_key.id");
        linkToolCrateId = new NamespacedKey(plugin, "linktool.crate_id");
        linkToolNexoModel = new NamespacedKey(plugin, "linktool.nexo_model");

        captureToolSceneId = new NamespacedKey(plugin, "capturetool.scene_id");
        captureToolTarget = new NamespacedKey(plugin, "capturetool.target");
        captureToolFrameIndex = new NamespacedKey(plugin, "capturetool.frame_index");
    }

    public static void clear() {
        crateId = null;
        keyId = null;
        linkToolCrateId = null;
        linkToolNexoModel = null;

        captureToolSceneId = null;
        captureToolTarget = null;
        captureToolFrameIndex = null;
    }
}
