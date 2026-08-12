package su.nightexpress.excellentcrates.opening.cinematic.scene.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.opening.cinematic.scene.AbstractFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;

/**
 * Shows a title and subtitle to the viewer.
 *
 * <p>The three timing values are the vanilla title timings and are independent of the frame's own
 * duration: a title can keep fading out while later frames run.
 */
public class SendTitleFrame extends AbstractFrame {

    private static final long DEFAULT_DURATION = 0L;

    public static final String KEY_TITLE     = "Title";
    public static final String KEY_SUBTITLE  = "Subtitle";
    public static final String KEY_FADE_IN   = "Times.FadeIn";
    public static final String KEY_STAY      = "Times.Stay";
    public static final String KEY_FADE_OUT  = "Times.FadeOut";

    private static final int DEFAULT_FADE_IN  = 10;
    private static final int DEFAULT_STAY     = 40;
    private static final int DEFAULT_FADE_OUT = 10;

    private String title;
    private String subtitle;
    private int    fadeIn;
    private int    stay;
    private int    fadeOut;

    public SendTitleFrame(long duration, @NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        super(duration);
        this.title = title;
        this.subtitle = subtitle;
        this.setFadeIn(fadeIn);
        this.setStay(stay);
        this.setFadeOut(fadeOut);
    }

    @NotNull
    public static CinematicFrame createDefault() {
        return new SendTitleFrame(DEFAULT_DURATION, "", "", DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    @NotNull
    public static CinematicFrame read(@NotNull ConfigurationSection config, @NotNull String path) {
        long duration = readDuration(config, path, DEFAULT_DURATION);
        String title = config.getString(path + "." + KEY_TITLE, "");
        String subtitle = config.getString(path + "." + KEY_SUBTITLE, "");
        int fadeIn = config.getInt(path + "." + KEY_FADE_IN, DEFAULT_FADE_IN);
        int stay = config.getInt(path + "." + KEY_STAY, DEFAULT_STAY);
        int fadeOut = config.getInt(path + "." + KEY_FADE_OUT, DEFAULT_FADE_OUT);

        return new SendTitleFrame(duration, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    @NotNull
    public FrameType getType() {
        return FrameType.SEND_TITLE;
    }

    @Override
    @NotNull
    public String getSummary() {
        String text = this.title.isBlank() ? this.subtitle : this.title;
        return text.isBlank() ? "Title (empty)" : "Title " + text;
    }

    @Override
    public void write(@NotNull ConfigurationSection config, @NotNull String path) {
        this.writeBase(config, path);
        config.set(path + "." + KEY_TITLE, this.title);
        config.set(path + "." + KEY_SUBTITLE, this.subtitle);
        config.set(path + "." + KEY_FADE_IN, this.fadeIn);
        config.set(path + "." + KEY_STAY, this.stay);
        config.set(path + "." + KEY_FADE_OUT, this.fadeOut);
    }

    @NotNull
    public String getTitle() {
        return this.title;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    @NotNull
    public String getSubtitle() {
        return this.subtitle;
    }

    public void setSubtitle(@NotNull String subtitle) {
        this.subtitle = subtitle;
    }

    public int getFadeIn() {
        return this.fadeIn;
    }

    public void setFadeIn(int fadeIn) {
        this.fadeIn = Math.max(0, fadeIn);
    }

    public int getStay() {
        return this.stay;
    }

    public void setStay(int stay) {
        this.stay = Math.max(0, stay);
    }

    public int getFadeOut() {
        return this.fadeOut;
    }

    public void setFadeOut(int fadeOut) {
        this.fadeOut = Math.max(0, fadeOut);
    }
}
