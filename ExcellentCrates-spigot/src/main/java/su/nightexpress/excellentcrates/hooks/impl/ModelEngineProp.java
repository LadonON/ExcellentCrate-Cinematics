package su.nightexpress.excellentcrates.hooks.impl;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * A handle to a ModelEngine model spawned by {@link ModelEngineHook#spawnProp}.
 *
 * <p>Deliberately holds no ModelEngine type of its own — only the invisible base entity the model
 * is attached to and a callback that tears the model down — so code holding this handle (the
 * cinematic hand-off) never has to reference ModelEngine types itself.
 */
public class ModelEngineProp {

    private final Entity   baseEntity;
    private final Runnable destroyCallback;

    ModelEngineProp(@NotNull Entity baseEntity, @NotNull Runnable destroyCallback) {
        this.baseEntity = baseEntity;
        this.destroyCallback = destroyCallback;
    }

    /**
     * Destroys the ModelEngine model, then removes the base entity it rides on.
     */
    public void remove() {
        this.destroyCallback.run();

        if (this.baseEntity.isValid()) {
            this.baseEntity.remove();
        }
    }
}
