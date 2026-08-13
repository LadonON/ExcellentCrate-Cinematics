package su.nightexpress.excellentcrates.opening;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.Placeholders;
import su.nightexpress.excellentcrates.api.crate.Reward;
import su.nightexpress.excellentcrates.api.opening.Opening;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.crate.cost.Cost;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.excellentcrates.crate.impl.CrateSource;
import su.nightexpress.excellentcrates.data.crate.GlobalCrateData;
import su.nightexpress.excellentcrates.data.crate.UserCrateData;
import su.nightexpress.excellentcrates.user.CrateUser;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.Replacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractOpening implements Opening {

    protected final CratesPlugin plugin;
    protected final Player       player;
    protected final CrateSource  source;
    protected final Crate        crate;
    protected final Cost         cost;
    protected final List<Reward> rewards;

    protected long    tickCount;
    protected boolean running;
    protected boolean refundable;

    /** Guards {@link #completeOpening()} so an opening can never pay out twice. */
    private boolean opened;

    public AbstractOpening(@NotNull CratesPlugin plugin, @NotNull Player player, @NotNull CrateSource source, @Nullable Cost cost) {
        this.plugin = plugin;
        this.player = player;
        this.source = source;
        this.crate = source.getCrate();
        this.cost = cost;
        this.rewards = new ArrayList<>();
        this.setRefundable(true);
    }

    @Override
    public void start() {
        if (this.running) return;

        this.running = true;
        this.onStart();
    }

    @Override
    public void stop() {
        if (!this.running) return;

        this.running = false;
        this.onStop();
    }

    @Override
    public void tick() {
        if (!this.running) return;

        if (this.isCompleted()) {
            this.stop();
            return;
        }

        if (this.isTickTime()) {
            this.onTick();
        }

        this.tickCount = Math.max(0L, this.tickCount + 1L);
    }

    @Override
    public final boolean isRunning() {
        return this.running;
    }

    @Override
    public long getTickCount() {
        return this.tickCount;
    }

    @Override
    public boolean isTickTime() {
        return this.tickCount == 0 || this.tickCount % this.getInterval() == 0L;
    }

    protected abstract void onStart();

    protected abstract void onTick();

    protected abstract void onComplete();

    protected void onStop() {
        if (this.isRefundable()) {
            if (this.cost != null) {
                this.cost.refundAll(this.player);
            }
            if (this.source.getItem() != null) {
                Players.addItem(this.player, this.crate.getItemStack());
            }
        }

        this.plugin.getOpeningManager().removeOpening(this.getPlayer());

        if (this.isCompleted()) {
            this.completeOpening();
        }
    }

    /**
     * Performs the crate opening proper: rewards, stats, cooldown, milestones, the result message
     * and post-open commands.
     *
     * <p>Normally reached from {@link #onStop()} when the animation finishes. It is exposed to
     * subclasses so an animation can decide for itself <i>when</i> the crate opens — the cinematic
     * opening calls it on its {@code TRIGGER_CRATE_OPEN} frame rather than at the end.
     *
     * <p>Idempotent: the first call does the work and every later one is ignored, so an animation
     * that both triggers early and then stops normally still opens the crate exactly once.
     *
     * @return {@code true} if this call is the one that opened the crate.
     */
    protected final boolean completeOpening() {
        if (this.opened) return false;
        this.opened = true;

        this.onComplete();

        CrateUser user = plugin.getUserManager().getOrFetch(player);
        UserCrateData userData = user.getCrateData(this.crate);
        GlobalCrateData globalData = plugin.getDataManager().getCrateDataOrCreate(this.crate);

        userData.addOpenings(1);
        globalData.setLatestOpener(this.player);
        globalData.setDirty(true);

        this.rewards.forEach(reward -> reward.give(this.player));

        if (crate.isOpeningCooldownEnabled()) {
            userData.addOpeningStreak(1);

            if (!userData.isOnCooldown() && !crate.hasCooldownBypassPermission(player)) {
                userData.setCooldown(crate.getOpeningCooldownTime());
            }
        }

        if (crate.hasMilestones()) {
            userData.addMilestones(1);
            plugin.getCrateManager().triggerMilestones(player, crate, userData.getMilestone());
            if (userData.getMilestone() >= crate.getMaxMilestone() && crate.isMilestonesRepeatable()) {
                userData.setMilestone(0);
            }
        }

        Lang.CRATE_OPEN_RESULT_INFO.message().send(this.player, replacer -> replacer
            .replace(this.crate.replacePlaceholders())
            .replace(Placeholders.GENERIC_REWARDS, this.rewards.stream()
                .map(reward -> reward.replacePlaceholders().apply(Lang.CRATE_OPEN_RESULT_REWARD.text()))
                .collect(Collectors.joining(", "))
            )
        );

        List<String> postOpenCommands = Replacer.create().replace(this.crate.replacePlaceholders()).apply(this.crate.getPostOpenCommands());
        Players.dispatchCommands(this.player, postOpenCommands);

        this.plugin.getUserManager().save(user);
        return true;
    }

    /**
     * @return {@code true} once the crate has actually been opened, i.e. once rewards have been
     * handed out. Distinct from {@link #isCompleted()}, which asks whether the <i>animation</i> has
     * run its course.
     */
    protected final boolean isOpened() {
        return this.opened;
    }

    @Override
    @NotNull
    public List<Reward> getRewards() {
        return this.rewards;
    }

    @Override
    public void addReward(@NotNull Reward reward) {
        this.rewards.add(reward);
    }

    @Override
    public void addRewards(@NotNull Collection<Reward> rewards) {
        this.rewards.addAll(rewards);
    }

    @Override
    public boolean isRefundable() {
        return this.refundable;
    }

    @Override
    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }

    @Override
    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    @Override
    @NotNull
    public CrateSource getSource() {
        return this.source;
    }

    @Override
    @NotNull
    public Crate getCrate() {
        return this.crate;
    }

    @Override
    @Nullable
    public Cost getCost() {
        return this.cost;
    }
}
