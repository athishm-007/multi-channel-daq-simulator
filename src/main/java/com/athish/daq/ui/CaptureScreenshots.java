package com.athish.daq.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CaptureScreenshots extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        MainApp app = new MainApp();
        app.start(stage);

        Scene scene = stage.getScene();

        // Run automation sequence on a worker thread with Platform.runLater steps
        new Thread(() -> {
            try {
                Thread.sleep(800);

                // Step 1: Find Start Button and start acquisition
                Button startBtn = findButton(scene.getRoot(), "Start");
                Button stopBtn = findButton(scene.getRoot(), "Stop");
                ComboBox<?> filterBox = findNode(scene.getRoot(), ComboBox.class);

                System.out.println("Starting acquisition for Screenshot 1 (Normal Case)...");
                Platform.runLater(() -> startBtn.fire());

                // Let it run for 2.0 seconds so normal data accumulates
                Thread.sleep(2000);

                // Capture Screenshot 1: Normal Case
                captureSnapshot(scene, "slide10_screenshot1_normal_case.png");
                System.out.println("Saved slide10_screenshot1_normal_case.png");

                // Step 2: Custom inputs to demonstrate it is not hard-coded
                System.out.println("Applying custom inputs for Screenshot 2...");
                Platform.runLater(() -> {
                    // Set Vibration to 40 Hz
                    ChannelControl vibCtrl = findChannelControl(scene.getRoot(), "Vibration");
                    if (vibCtrl != null) {
                        TextField tf = findNode(vibCtrl, TextField.class);
                        Button apply = findButton(vibCtrl, "Apply");
                        tf.setText("40");
                        apply.fire();
                    }

                    // Set Temperature to 10 Hz
                    ChannelControl tempCtrl = findChannelControl(scene.getRoot(), "Temperature");
                    if (tempCtrl != null) {
                        TextField tf = findNode(tempCtrl, TextField.class);
                        Button apply = findButton(tempCtrl, "Apply");
                        tf.setText("10");
                        apply.fire();
                    }

                    // Pause Pressure
                    ChannelControl pressCtrl = findChannelControl(scene.getRoot(), "Pressure");
                    if (pressCtrl != null) {
                        CheckBox cb = findNode(pressCtrl, CheckBox.class);
                        if (cb.isSelected()) cb.fire();
                    }

                    // Filter by Vibration channel
                    if (filterBox != null) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> fb = (ComboBox<String>) filterBox;
                        fb.getSelectionModel().select("Vibration");
                    }
                });

                // Let it run for 1.5 seconds under new configuration
                Thread.sleep(1500);

                // Capture Screenshot 2: Custom Inputs & Filter
                captureSnapshot(scene, "slide10_screenshot2_custom_inputs.png");
                System.out.println("Saved slide10_screenshot2_custom_inputs.png");

                // Step 3: Trigger InvalidRateException (User-defined Exception)
                System.out.println("Triggering InvalidRateException for Slide 11 Screenshot...");
                Platform.runLater(() -> {
                    ChannelControl vibCtrl = findChannelControl(scene.getRoot(), "Vibration");
                    if (vibCtrl != null) {
                        TextField tf = findNode(vibCtrl, TextField.class);
                        Button apply = findButton(vibCtrl, "Apply");
                        tf.setText("500");
                        apply.fire();
                    }
                });

                Thread.sleep(800);

                // Capture Screenshot 4: Exception Handling
                captureSnapshot(scene, "slide11_screenshot_exception_handling.png");
                System.out.println("Saved slide11_screenshot_exception_handling.png");

                // Stop acquisition cleanly
                Platform.runLater(() -> {
                    if (stopBtn != null) stopBtn.fire();
                    Platform.exit();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        }).start();
    }

    private static void captureSnapshot(Scene scene, String filename) {
        Platform.runLater(() -> {
            try {
                WritableImage image = scene.snapshot(null);
                File file = new File(filename);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private static Button findButton(Parent root, String text) {
        for (Node node : getAllNodes(root)) {
            if (node instanceof Button btn && text.equals(btn.getText())) {
                return btn;
            }
        }
        return null;
    }

    private static ChannelControl findChannelControl(Parent root, String channelName) {
        for (Node node : getAllNodes(root)) {
            if (node instanceof ChannelControl cc) {
                CheckBox cb = findNode(cc, CheckBox.class);
                if (cb != null && channelName.equals(cb.getText())) {
                    return cc;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> T findNode(Parent root, Class<T> clazz) {
        for (Node node : getAllNodes(root)) {
            if (clazz.isInstance(node)) {
                return (T) node;
            }
        }
        return null;
    }

    private static List<Node> getAllNodes(Parent root) {
        List<Node> nodes = new ArrayList<>();
        addAllDescendants(root, nodes);
        return nodes;
    }

    private static void addAllDescendants(Parent parent, List<Node> list) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            list.add(node);
            if (node instanceof Parent p) {
                addAllDescendants(p, list);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
