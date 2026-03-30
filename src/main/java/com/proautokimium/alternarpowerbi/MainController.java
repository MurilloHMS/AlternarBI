package com.proautokimium.alternarpowerbi;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.proautokimium.alternarpowerbi.infrastructure.services.ConfigService;
import com.proautokimium.alternarpowerbi.infrastructure.services.SwitchPageService;
import com.proautokimium.alternarpowerbi.infrastructure.util.LoggerConfig;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class MainController {

    private static final Logger LOGGER = LoggerConfig.getLogger(MainController.class.getName());

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused  = new AtomicBoolean(false);
    private Thread workerThread;

    private final SwitchPageService switchPageService = new SwitchPageService();
    private final ConfigService     config            = ConfigService.getInstance();

    @FXML private Button      playButton;
    @FXML private Button      pauseButton;
    @FXML private Button      stopButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label       statusLabel;
    @FXML private Label       subtitleLabel;

    @FXML private Spinner<Integer> spinnerPages;
    @FXML private Slider           sliderDefault;
    @FXML private Label            lblDefaultSecs;
    @FXML private CheckBox         checkLoop;
    @FXML private javafx.scene.layout.VBox pageIntervalsContainer;
    @FXML private TitledPane settingsPane;

    @FXML
    void initialize() {
        progressBar.setProgress(0.01);
        setButtonStates(true, false, false);

        spinnerPages.getValueFactory().setValue(config.getTotalPages());
        sliderDefault.setValue(config.getDefaultIntervalMs() / 1000.0);
        checkLoop.setSelected(config.isLoop());
        updateSubtitle();
        updateDefaultLabel();

        spinnerPages.valueProperty().addListener((obs, o, n) -> {
            config.setTotalPages(n);
            rebuildPageIntervalRows();
            updateSubtitle();
        });

        sliderDefault.valueProperty().addListener((obs, o, n) -> {
            long ms = n.longValue() * 1000L;
            config.setDefaultIntervalMs(ms);
            updateDefaultLabel();
            rebuildPageIntervalRows();
        });

        settingsPane.expandedProperty().addListener((obs, oldVal, expanded) -> {

            Stage stage = (Stage) settingsPane.getScene().getWindow();

            if (expanded) {
                stage.setHeight(stage.getHeight() + 350);
            } else {
                stage.setHeight(stage.getHeight() - 350);
            }

        });

        rebuildPageIntervalRows();
        updateStatus("Pronto para iniciar");
    }

    @FXML void onPlayButtonClick(ActionEvent e)  {
        if (isPaused.get()) resumeAutomation(); else startAutomation();
    }
    @FXML void onPauseButtonClick(ActionEvent e) { pauseAutomation(); }
    @FXML void onStopButtonClick(ActionEvent e)  { stopAutomation(); }

    @FXML
    void onLoopChanged(ActionEvent e) {
        config.setLoop(checkLoop.isSelected());
    }

    @FXML
    void onApplyDefaultInterval(ActionEvent e) {
        long ms = (long)(sliderDefault.getValue()) * 1000L;
        for (int i = 1; i <= config.getTotalPages(); i++) {
            config.setIntervalForPage(i, ms);
        }
        rebuildPageIntervalRows();
    }

    @FXML
    void onSaveConfig(ActionEvent e) {
        config.save();
        updateStatus("💾 Configurações salvas!");
        LOGGER.info("Configurações salvas pelo usuário");
    }

    private void startAutomation() {
        isRunning.set(true);
        isPaused.set(false);
        progressBar.setProgress(0.01);
        setButtonStates(false, true, true);

        workerThread = new Thread(buildAutomationTask(), "PowerBI-Automation-Thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void pauseAutomation() {
        isPaused.set(true);
        setButtonStates(true, false, true);
        updateStatus("⏸ Pausado");
    }

    private void resumeAutomation() {
        isPaused.set(false);
        setButtonStates(false, true, true);
        updateStatus("▶ Retomando...");
    }

    private void stopAutomation() {
        isRunning.set(false);
        isPaused.set(false);
        if (workerThread != null && workerThread.isAlive()) workerThread.interrupt();
        Platform.runLater(() -> {
            progressBar.setProgress(0.01);
            setButtonStates(true, false, false);
            updateStatus("⏹ Parado pelo usuário");
        });
    }

    private Task<Void> buildAutomationTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                final int total = config.getTotalPages();

                for (int i = 5; i > 0; i--) {
                    if (!isRunning.get()) return null;
                    final int sec = i;
                    Platform.runLater(() ->
                            updateStatus("⏳ Clique no Power BI! Iniciando em " + sec + "s...")
                    );
                    Thread.sleep(1_000);
                }

                Platform.runLater(() -> updateStatus("🔄 Indo para a página 1..."));
                //switchPageService.goToFirstPage(total);

                int currentPage = 1;

                while (isRunning.get()) {
                    waitWhilePaused();
                    if (!isRunning.get()) break;

                    final int pg   = currentPage;
                    final double p = (double) pg / total;
                    Platform.runLater(() -> {
                        progressBar.setProgress(p);
                        statusLabel.setText(String.format("📄 Página %d de %d (%.0f%%)", pg, total, p * 100));
                    });

                    LOGGER.info("Exibindo página " + currentPage + "/" + total);

                    long intervalMs = config.getIntervalForPage(currentPage);
                    sleepWithCountdown(intervalMs, currentPage, total);

                    if (!isRunning.get()) break;
                    waitWhilePaused();
                    if (!isRunning.get()) break;

                    if (currentPage < total) {
                        switchPageService.nextPage();
                        currentPage++;
                    } else if (config.isLoop()) {
                        Platform.runLater(() -> updateStatus("🔄 Reiniciando..."));
                        switchPageService.goToFirstPage(total);
                        currentPage = 1;
                    } else {
                        break;
                    }
                }

                Platform.runLater(() -> {
                    progressBar.setProgress(0.01);
                    updateStatus(isRunning.get() ? "✅ Apresentação concluída!" : "⏹ Parado");
                    setButtonStates(true, false, false);
                });
                isRunning.set(false);
                return null;
            }

            private void waitWhilePaused() throws InterruptedException {
                while (isPaused.get() && isRunning.get()) Thread.sleep(300);
            }

            private void sleepWithCountdown(long totalMs, int page, int total) throws InterruptedException {
                long elapsed = 0;
                while (elapsed < totalMs && isRunning.get() && !isPaused.get()) {
                    long slice = Math.min(500, totalMs - elapsed);
                    Thread.sleep(slice);
                    elapsed += slice;
                    long secsLeft = (totalMs - elapsed) / 1000;
                    final long sl = secsLeft;
                    Platform.runLater(() ->
                            statusLabel.setText(String.format(
                                    "📄 Página %d de %d — próxima em %ds", page, total, sl))
                    );
                }
            }
        };
    }

    private void rebuildPageIntervalRows() {
        pageIntervalsContainer.getChildren().clear();
        int total = config.getTotalPages();

        for (int i = 1; i <= total; i++) {
            final int page = i;
            long currentMs = config.getIntervalForPage(page);

            Label lblPage = new Label(String.format("Pg %2d", page));
            lblPage.getStyleClass().add("page-row-label");
            lblPage.setPrefWidth(42);

            Slider slider = new Slider(5, 300, currentMs / 1000.0);
            slider.setBlockIncrement(5);
            HBox.setHgrow(slider, Priority.ALWAYS);

            Label lblVal = new Label(formatSecs((long)(currentMs / 1000)));
            lblVal.getStyleClass().add("page-row-value");
            lblVal.setPrefWidth(42);

            slider.valueProperty().addListener((obs, o, n) -> {
                long ms = n.longValue() * 1000L;
                config.setIntervalForPage(page, ms);
                lblVal.setText(formatSecs(n.longValue()));
            });

            HBox row = new HBox(8, lblPage, slider, lblVal);
            row.setAlignment(Pos.CENTER_LEFT);
            pageIntervalsContainer.getChildren().add(row);
        }
    }

    private void updateDefaultLabel() {
        long secs = (long) sliderDefault.getValue();
        lblDefaultSecs.setText(formatSecs(secs));
    }

    private void updateSubtitle() {
        subtitleLabel.setText(String.format(
                "Configurado: %d páginas · %ds padrão por página",
                config.getTotalPages(),
                config.getDefaultIntervalMs() / 1000
        ));
    }

    private String formatSecs(long secs) {
        return secs < 60 ? secs + "s" : String.format("%.1fm", secs / 60.0);
    }

    private void setButtonStates(boolean play, boolean pause, boolean stop) {
        Platform.runLater(() -> {
            playButton.setDisable(!play);
            pauseButton.setDisable(!pause);
            stopButton.setDisable(!stop);
        });
    }

    private void updateStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }
}