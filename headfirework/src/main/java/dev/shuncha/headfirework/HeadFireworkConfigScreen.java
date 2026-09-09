package dev.shuncha.headfirework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HeadFireworkConfigScreen extends Screen {

    private static final float SCALE_MIN = 1.0f;
    private static final float SCALE_MAX = 60.0f;
    private static final int DISPLAY_DURATION_MIN = 1;
    private static final int DISPLAY_DURATION_MAX = 200;
    private static final int FADE_DURATION_MIN = 0;
    private static final int FADE_DURATION_MAX = 100;
    private static final String[] FACING_ORDER = {"north", "east", "south", "west"};

    private float smallBall;
    private float largeBall;
    private float star;
    private int displayDuration;
    private int fadeDuration;
    private String facing;

    private Button facingButton;

    public HeadFireworkConfigScreen() {
        super(Component.literal("HeadFirework 設定"));
        HeadFireworkConfig cfg = HeadFireworkConfig.INSTANCE;
        this.smallBall = cfg.scaleSmallBall;
        this.largeBall = cfg.scaleLargeBall;
        this.star = cfg.scaleStar;
        this.displayDuration = cfg.displayDurationTicks;
        this.fadeDuration = cfg.fadeDurationTicks;
        this.facing = cfg.facing;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int sliderX = centerX - 110;
        int resetX = centerX + 95;
        int y = this.height / 2 - 120;

        addScaleRow(sliderX, resetX, y, "小玉", smallBall,
                value -> smallBall = value,
                () -> smallBall = HeadFireworkConfig.DEFAULT_SCALE_SMALL_BALL);
        y += 24;

        addScaleRow(sliderX, resetX, y, "大玉", largeBall,
                value -> largeBall = value,
                () -> largeBall = HeadFireworkConfig.DEFAULT_SCALE_LARGE_BALL);
        y += 24;

        addScaleRow(sliderX, resetX, y, "星型/バースト", star,
                value -> star = value,
                () -> star = HeadFireworkConfig.DEFAULT_SCALE_STAR);
        y += 24;

        addDisplayDurationRow(sliderX, resetX, y);
        y += 24;

        addFadeDurationRow(sliderX, resetX, y);
        y += 32;

        facingButton = Button.builder(Component.literal(facingLabel()), button -> cycleFacing())
                .bounds(centerX - 100, y, 200, 20)
                .build();
        this.addRenderableWidget(facingButton);
        y += 32;

        this.addRenderableWidget(Button.builder(Component.literal("適用"), button -> applyAndClose())
                .bounds(centerX - 100, y, 95, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose())
                .bounds(centerX + 5, y, 95, 20)
                .build());
    }

    private void addScaleRow(int sliderX, int resetX, int y, String label, float value,
                              java.util.function.Consumer<Float> onChange, Runnable onReset) {
        this.addRenderableWidget(new ScaleSlider(sliderX, y, 180, 20, label, value, onChange));
        this.addRenderableWidget(Button.builder(Component.literal("R"), button -> {
                    onReset.run();
                    this.clearWidgets();
                    this.init();
                })
                .bounds(resetX, y, 20, 20)
                .build());
    }

    private void addDisplayDurationRow(int sliderX, int resetX, int y) {
        this.addRenderableWidget(new IntSlider(sliderX, y, 180, 20, "表示時間(tick)",
                displayDuration, DISPLAY_DURATION_MIN, DISPLAY_DURATION_MAX,
                value -> displayDuration = value));
        this.addRenderableWidget(Button.builder(Component.literal("R"), button -> {
                    displayDuration = HeadFireworkConfig.DEFAULT_DISPLAY_DURATION_TICKS;
                    this.clearWidgets();
                    this.init();
                })
                .bounds(resetX, y, 20, 20)
                .build());
    }

    private void addFadeDurationRow(int sliderX, int resetX, int y) {
        this.addRenderableWidget(new IntSlider(sliderX, y, 180, 20, "フェードアウト時間(tick)",
                fadeDuration, FADE_DURATION_MIN, FADE_DURATION_MAX,
                value -> fadeDuration = value));
        this.addRenderableWidget(Button.builder(Component.literal("R"), button -> {
                    fadeDuration = HeadFireworkConfig.DEFAULT_FADE_DURATION_TICKS;
                    this.clearWidgets();
                    this.init();
                })
                .bounds(resetX, y, 20, 20)
                .build());
    }

    private String facingLabel() {
        String jp = switch (facing) {
            case "north" -> "北";
            case "east" -> "東";
            case "west" -> "西";
            default -> "南";
        };
        return "顔の向き: " + jp + " (" + facing + ")";
    }

    private void cycleFacing() {
        int currentIndex = 0;
        for (int i = 0; i < FACING_ORDER.length; i++) {
            if (FACING_ORDER[i].equals(facing)) {
                currentIndex = i;
                break;
            }
        }
        facing = FACING_ORDER[(currentIndex + 1) % FACING_ORDER.length];
        if (facingButton != null) {
            facingButton.setMessage(Component.literal(facingLabel()));
        }
    }

    private void applyAndClose() {
        sendConfigCommand("small_ball", smallBall);
        sendConfigCommand("large_ball", largeBall);
        sendConfigCommand("star", star);
        sendConfigCommand("creeper", star);
        sendConfigCommand("burst", star);
        sendIntCommand("display_duration", displayDuration);
        sendIntCommand("fade_duration", fadeDuration);
        sendFacingCommand(facing);
        onClose();
    }

    private void sendConfigCommand(String shape, float value) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String command = "headfirework config scale " + shape + " " + value;
            client.player.connection.sendCommand(command);
        }
    }

    private void sendIntCommand(String key, int value) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String command = "headfirework config " + key + " " + value;
            client.player.connection.sendCommand(command);
        }
    }

    private void sendFacingCommand(String direction) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String command = "headfirework config facing " + direction;
            client.player.connection.sendCommand(command);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 140, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ScaleSlider extends AbstractSliderButton {
        private final String label;
        private final java.util.function.Consumer<Float> onChange;

        ScaleSlider(int x, int y, int width, int height, String label, float initialValue,
                    java.util.function.Consumer<Float> onChange) {
            super(x, y, width, height, Component.literal(label + ": " + String.format("%.1f", initialValue)),
                    (initialValue - SCALE_MIN) / (SCALE_MAX - SCALE_MIN));
            this.label = label;
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {
            float value = SCALE_MIN + (float) this.value * (SCALE_MAX - SCALE_MIN);
            this.setMessage(Component.literal(label + ": " + String.format("%.1f", value)));
        }

        @Override
        protected void applyValue() {
            float value = SCALE_MIN + (float) this.value * (SCALE_MAX - SCALE_MIN);
            onChange.accept(value);
        }
    }

    private static class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final java.util.function.Consumer<Integer> onChange;

        IntSlider(int x, int y, int width, int height, String label, int initialValue, int min, int max,
                   java.util.function.Consumer<Integer> onChange) {
            super(x, y, width, height, Component.literal(label + ": " + initialValue),
                    (double) (initialValue - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {
            int intValue = min + (int) Math.round(this.value * (max - min));
            this.setMessage(Component.literal(label + ": " + intValue));
        }

        @Override
        protected void applyValue() {
            int intValue = min + (int) Math.round(this.value * (max - min));
            onChange.accept(intValue);
        }
    }
}