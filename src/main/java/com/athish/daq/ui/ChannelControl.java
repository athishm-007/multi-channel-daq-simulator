package com.athish.daq.ui;

import com.athish.daq.core.Channel;
import com.athish.daq.core.InvalidRateException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/** One control row per channel: enable box, live rate field, sample counter. */
public final class ChannelControl extends HBox {
    private final Channel channel;
    private final TextField rateField = new TextField();
    private final Label countLabel = new Label("0");

    public ChannelControl(Channel channel, Consumer<String> info, Consumer<String> error) {
        super(10);
        this.channel = channel;
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(2, 0, 2, 0));

        CheckBox enabled = new CheckBox(channel.getName());
        enabled.setSelected(channel.isEnabled());
        enabled.setTextFill(Color.web(channel.getColor()));
        enabled.setPrefWidth(120);
        enabled.setOnAction(e -> {
            channel.setEnabled(enabled.isSelected());
            info.accept(channel.getName() + (enabled.isSelected() ? " enabled" : " paused"));
        });

        rateField.setText(String.valueOf(channel.getRateHz()));
        rateField.setPrefColumnCount(4);
        Button apply = new Button("Apply");
        Runnable applyRate = () -> {
            try {
                int hz = Integer.parseInt(rateField.getText().trim());
                channel.setRateHz(hz);
                info.accept(channel.getName() + " now samples at " + hz + " Hz");
            } catch (NumberFormatException ex) {
                rateField.setText(String.valueOf(channel.getRateHz()));
                error.accept("Rate must be a whole number");
            } catch (InvalidRateException ex) {          // user-defined exception caught here
                rateField.setText(String.valueOf(channel.getRateHz()));
                error.accept(ex.getMessage());
            }
        };
        apply.setOnAction(e -> applyRate.run());
        rateField.setOnAction(e -> applyRate.run());

        getChildren().addAll(enabled, new Label("Rate (Hz)"), rateField, apply,
                new Label("Samples:"), countLabel, new Label("[" + channel.getUnit() + "]"));
    }

    public void refresh() {
        countLabel.setText(String.valueOf(channel.getProduced()));
    }
}
