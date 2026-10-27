package com.athish.daq.ui;

import com.athish.daq.core.AcquisitionManager;
import com.athish.daq.core.Channel;
import com.athish.daq.core.LoggerException;
import com.athish.daq.core.SharedLogger;
import com.athish.daq.model.Sample;
import com.athish.daq.persist.StateStore;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** JavaFX front end: controls on top, live time-ordered table in the middle, counters below. */
public class MainApp extends Application {
    private static final int MAX_ROWS = 2000;
    private static final int RESTORE_ROWS = 200;

    private final ObservableList<Sample> rows = FXCollections.observableArrayList();
    private final List<ChannelControl> controls = new ArrayList<>();
    private final TableView<Sample> table = new TableView<>();
    private final Label statusLabel = new Label("Ready");
    private final Label statsLabel = new Label();
    private final Button startBtn = new Button("Start");
    private final Button stopBtn = new Button("Stop");
    private final Button clearBtn = new Button("Clear table");
    private final CheckBox autoScroll = new CheckBox("Auto-scroll");
    private final ComboBox<String> filterBox = new ComboBox<>();

    private AcquisitionManager manager;
    private StateStore state;
    private long sessionStartLifetime;

    @Override
    public void start(Stage stage) {
        Path dir = Paths.get(System.getProperty("user.dir"));
        state = new StateStore(dir.resolve("daq_state.properties"));
        manager = new AcquisitionManager(dir.resolve("daq_log.csv"),
                batch -> Platform.runLater(() -> appendRows(batch)));   // logger thread -> FX thread
        state.load(manager.getChannels());

        // Restore the end of the previous log so the saved state is visible straight away.
        List<Sample> restored = SharedLogger.readTail(manager.getLogFile(), RESTORE_ROWS);
        rows.addAll(restored);
        if (!restored.isEmpty()) {
            setStatus("Restored last " + restored.size() + " rows from the previous log", false);
        }

        Label title = new Label("Multi-Channel Data Acquisition Simulator");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #d4af37;");
        Label sub = new Label("ATHISH M (711525BEC014)  |  one thread per channel -> synchronized logger -> time-ordered CSV");
        sub.setStyle("-fx-text-fill: #9a9a9a;");

        VBox channelBox = new VBox(2);
        for (Channel c : manager.getChannels()) {
            ChannelControl cc = new ChannelControl(c, m -> setStatus(m, false), m -> setStatus(m, true));
            controls.add(cc);
            channelBox.getChildren().add(cc);
        }

        filterBox.getItems().add("All channels");
        for (Channel c : manager.getChannels()) filterBox.getItems().add(c.getName());
        filterBox.getSelectionModel().selectFirst();
        FilteredList<Sample> filtered = new FilteredList<>(rows, s -> true);
        filterBox.setOnAction(e -> {
            String sel = filterBox.getValue();
            filtered.setPredicate(s -> filterBox.getSelectionModel().getSelectedIndex() == 0
                    || s.getChannel().equals(sel));
        });

        buildTable(filtered);

        startBtn.setOnAction(e -> onStart());
        stopBtn.setOnAction(e -> onStop());
        clearBtn.setOnAction(e -> { rows.clear(); setStatus("Table cleared (the file is untouched)", false); });
        autoScroll.setSelected(true);
        setRunning(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, startBtn, stopBtn, clearBtn, new Label("Show:"), filterBox, autoScroll);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(8, title, sub, channelBox, toolbar);
        top.setPadding(new Insets(12));

        statusLabel.setMaxWidth(Double.MAX_VALUE);
        VBox bottom = new VBox(4, statsLabel, statusLabel);
        bottom.setPadding(new Insets(8, 12, 10, 12));

        BorderPane root = new BorderPane(table);
        root.setTop(top);
        root.setBottom(bottom);
        root.setStyle("-fx-base: #23252b; -fx-background: #1b1c20;");

        Timeline ticker = new Timeline(new KeyFrame(Duration.millis(250), e -> refreshStats()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();

        stage.setTitle("DAQ Simulator - Athish M");
        stage.setScene(new Scene(root, 960, 700));
        stage.setOnCloseRequest(e -> { if (manager.isRunning()) onStop(); else saveState(); });
        stage.show();
        refreshStats();
    }

    private void buildTable(FilteredList<Sample> data) {
        table.setItems(data);
        table.setPlaceholder(new Label("Press Start to begin acquiring"));
        table.getColumns().add(col("#", 70, s -> String.valueOf(s.getIndex())));
        table.getColumns().add(col("Wall time", 190, Sample::getWallTime));
        table.getColumns().add(col("Elapsed (ms)", 110, s -> String.format(Locale.ROOT, "%.1f", s.getElapsedMs())));

        TableColumn<Sample, String> ch = col("Channel", 120, Sample::getChannel);
        ch.setCellFactory(c -> new TableCell<>() {          // colour each channel like its control row
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Channel channel = (empty || item == null) ? null : manager.getChannel(item);
                setStyle(channel == null ? "" : "-fx-text-fill: " + channel.getColor() + ";");
            }
        });
        table.getColumns().add(ch);
        table.getColumns().add(col("Value", 100, s -> String.format(Locale.ROOT, "%.3f", s.getValue())));
        table.getColumns().add(col("Unit", 70, Sample::getUnit));
    }

    private TableColumn<Sample, String> col(String title, double width, Function<Sample, String> text) {
        TableColumn<Sample, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);        // the point of the table is the time order of the log
        return c;
    }

    private void appendRows(List<Sample> batch) {
        rows.addAll(batch);
        int excess = rows.size() - MAX_ROWS;
        if (excess > 0) rows.remove(0, excess);
        if (autoScroll.isSelected() && !table.getItems().isEmpty()) {
            table.scrollTo(table.getItems().size() - 1);
        }
    }

    private void onStart() {
        try {
            sessionStartLifetime = state.getLong("lifetimeSamples", 0);
            manager.start();
            setRunning(true);
            setStatus("Acquisition running, logging to " + manager.getLogFile().getFileName(), false);
        } catch (LoggerException ex) {
            setStatus("Cannot start: " + ex.getMessage(), true);
        }
    }

    private void onStop() {
        long written = manager.stop();
        state.putLong("sessions", state.getLong("sessions", 0) + 1);
        state.putLong("lifetimeSamples", sessionStartLifetime + written);
        saveState();
        setRunning(false);
        refreshStats();
        setStatus("Stopped. " + written + " rows written this session; settings saved", false);
    }

    private void saveState() {
        try {
            state.save(manager.getChannels());
        } catch (IOException ex) {
            setStatus("Could not save state: " + ex.getMessage(), true);
        }
    }

    private void setRunning(boolean running) {
        startBtn.setDisable(running);
        stopBtn.setDisable(!running);
    }

    private void refreshStats() {
        for (ChannelControl c : controls) c.refresh();
        statsLabel.setText(String.format("Session rows written: %d   |   Waiting in queue: %d   |   Sessions so far: %d   |   Lifetime rows: %d",
                manager.getWritten(), manager.getPending(),
                state.getLong("sessions", 0), state.getLong("lifetimeSamples", 0)));
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setStyle(error ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #9ccc65;");
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
