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

    private float smallBall;
    private float largeBall;
    private float star;

    public HeadFireworkConfigScreen() {
        super(Component.literal("HeadFirework 設定"));
        HeadFireworkConfig cfg = HeadFireworkConfig.INSTANCE;
        this.smallBall = cfg.scaleSmallBall;
        this.largeBall = cfg.scaleLargeBall;
        this.star = cfg.scaleStar;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 70;

        this.addRenderableWidget(new ScaleSlider(centerX - 100, y, 200, 20,
                "小玉", smallBall, value -> smallBall = value));
        y += 24;
        this.addRenderableWidget(new ScaleSlider(centerX - 100, y, 200, 20,
                "大玉", largeBall, value -> largeBall = value));
        y += 24;
        this.addRenderableWidget(new ScaleSlider(centerX - 100, y, 200, 20,
                "星型/バースト", star, value -> star = value));
        y += 32;

        this.addRenderableWidget(Button.builder(Component.literal("適用"), button -> applyAndClose())
                .bounds(centerX - 100, y, 95, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose())
                .bounds(centerX + 5, y, 95, 20)
                .build());
        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("RESET"), button -> resetToDefaults())
                .bounds(centerX - 100, y, 200, 20)
                .build());
    }

    private void resetToDefaults() {
        smallBall = HeadFireworkConfig.DEFAULT_SCALE_SMALL_BALL;
        largeBall = HeadFireworkConfig.DEFAULT_SCALE_LARGE_BALL;
        star = HeadFireworkConfig.DEFAULT_SCALE_STAR;
        this.clearWidgets();
        this.init();
    }

    private void applyAndClose() {
        sendConfigCommand("small_ball", smallBall);
        sendConfigCommand("large_ball", largeBall);
        sendConfigCommand("star", star);
        sendConfigCommand("creeper", star);
        sendConfigCommand("burst", star);
        onClose();
    }

    private void sendConfigCommand(String shape, float value) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String command = "headfirework config scale " + shape + " " + value;
            client.player.connection.sendCommand(command);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
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
}