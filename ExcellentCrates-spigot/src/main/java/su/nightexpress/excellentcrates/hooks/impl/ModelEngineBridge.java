package su.nightexpress.excellentcrates.hooks.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The only class in the plugin that touches ModelEngine types.
 *
 * <p>Package-private and reached exclusively through {@link ModelEngineHook}, which guards every
 * entry point with an installation check. Keeping the ModelEngine imports isolated here means the
 * JVM never has to resolve them on a server where ModelEngine is absent, mirroring
 * {@link NexoBridge}.
 */
final class ModelEngineBridge {

    private ModelEngineBridge() {}

    /**
     * @return every registered blueprint id, sorted for a stable editor listing.
     */
    @NotNull
    static List<String> modelIds() {
        List<String> ids = new ArrayList<>(ModelEngineAPI.getAPI().getModelRegistry().getOrderedId());
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    static boolean hasBlueprint(@NotNull String modelId) {
        return ModelEngineAPI.getBlueprint(modelId) != null;
    }

    /**
     * Spawns {@code modelId} riding an invisible, collision-free {@link ArmorStand} at
     * {@code location}, facing {@code yaw}, and plays {@code animationId} on it if one was given.
     *
     * <p>Whether the model holds on its last frame once the animation finishes is entirely up to
     * how that animation's loop mode is authored in the blueprint - this only starts it once.
     *
     * @return the spawned prop, or {@code null} if the blueprint id does not resolve to a model.
     */
    @Nullable
    static ModelEngineProp spawn(@NotNull String modelId, @NotNull String animationId, @NotNull Location location, double yaw) {
        if (!hasBlueprint(modelId)) return null;

        ArmorStand base = location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCollidable(false);
        });

        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(base);

        // createModeledEntity() alone never hands the entity to ModelEngine's own updater - without
        // this it is never ticked, so the model neither renders correctly nor advances any animation
        // playing on it.
        modeledEntity.registerSelf();

        modeledEntity.setYBodyRotImmediately((float) yaw);
        modeledEntity.setYHeadRotImmediately((float) yaw);

        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        modeledEntity.addModel(activeModel, true);

        if (!animationId.isBlank()) {
            activeModel.getAnimationHandler().playAnimation(animationId, 1D, 0D, 0D, true);
        }

        return new ModelEngineProp(base, modeledEntity::destroy);
    }
}
