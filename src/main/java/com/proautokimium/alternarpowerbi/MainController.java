package com.proautokimium.alternarpowerbi;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.proautokimium.alternarpowerbi.infrastructure.services.SwitchPageService;
import com.proautokimium.alternarpowerbi.infrastructure.util.LoggerConfig;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;

public class MainController {

    private static final long INTERVAL_MS = 45_000;
    private static final long INITIAL_DELAY_MS = 5_000;
    private static final int MIN_PAGES = 1;
    private static final int MAX_PAGES = 100;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private Thread workerThread;
    private int totalPages = 0;

    private final SwitchPageService switchPageService = new SwitchPageService();

    @FXML private Button pauseButton;
    @FXML private Button playButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button stopButton;

    @FXML
    void initialize() {
        progressBar.setProgress(0.01);
        updateStatus("Inicializando...");
        totalPages = showPageConfigDialog();
        if (totalPages <= 0) {
            updateStatus("Configuração cancelada ou inválida");
            setButtonStates(false, false, false);
        } else {
            updateStatus(String.format("Configurado para %d páginas. Pronto para iniciar!", totalPages));
            setButtonStates(true, false, false);
        }
    }

    @FXML
    void onPlayButtonClick(ActionEvent event) {
        if (isPaused.get()) resumeAutomation();
        else startAutomation();
    }

    @FXML
    void onPauseButtonClick(ActionEvent event) {
        pauseAutomation();
    }

    @FXML
    void onStopButtonClick(ActionEvent event) {
        stopAutomation();
    }

    private void startAutomation() {
        if (totalPages <= 0) {
            updateStatus("Erro: Número de páginas inválido!");
            return;
        }
        isRunning.set(true);
        isPaused.set(false);
        progressBar.setProgress(0.01);
        setButtonStates(false, true, true);

        Task<Void> automationTask = createAutomationTask();
        workerThread = new Thread(automationTask);
        workerThread.setDaemon(true);
        workerThread.setName("PowerBI-Automation-Thread");
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

    private Task<Void> createAutomationTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, totalPages);
                Platform.runLater(() -> updateStatus("⏳ Iniciando em 5 segundos..."));
                Thread.sleep(INITIAL_DELAY_MS);
                
                
                int currentPage = 1;
                while (isRunning.get()) {
                    waitWhilePaused();
                    if (!isRunning.get()) break;

                    try {
                        processPage(currentPage);

                        if (currentPage < totalPages) {
                            currentPage++;
                        } else {
                            if (isRunning.get()) Thread.sleep(INTERVAL_MS);
                            switchPageService.goToFirstPage(totalPages);
                            currentPage = 1;
                            continue;
                        }

                        if (isRunning.get()) Thread.sleep(INTERVAL_MS);

                    } catch (Exception e) {
                        Platform.runLater(() -> updateStatus("❌ Erro: " + e.getMessage()));
                        break;
                    }
                }

                Platform.runLater(() -> {
                    progressBar.setProgress(0.01);
                    updateStatus("✅ Automação finalizada");
                    setButtonStates(true, false, false);
                });
                return null;
            }

            private void waitWhilePaused() throws InterruptedException {
                while (isPaused.get() && isRunning.get()) Thread.sleep(500);
            }

            private void processPage(int pageNumber) throws Exception {
                double progress = (double) pageNumber / totalPages;
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    updateStatus(String.format("📄 Processando página %d de %d (%.0f%%)", pageNumber, totalPages, progress * 100));
                });

                if (pageNumber == 1) {
                    switchPageService.firstPage();
                } else {
                    switchPageService.nextPage();
                }
            }

        };
    }

    private int showPageConfigDialog() {
        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Configuração Inicial");
        dialog.setHeaderText("Configuração do Alterador Power BI");
        dialog.setContentText("Número total de páginas:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int pages = Integer.parseInt(result.get().trim());
                if (pages < MIN_PAGES || pages > MAX_PAGES) {
                    updateStatus(String.format("Valor deve estar entre %d e %d", MIN_PAGES, MAX_PAGES));
                    return 0;
                }
                return pages;
            } catch (NumberFormatException e) {
                updateStatus("Valor inválido! Digite apenas números.");
                return 0;
            }
        }
        return 0;
    }

    private void setButtonStates(boolean playEnabled, boolean pauseEnabled, boolean stopEnabled) {
        Platform.runLater(() -> {
            playButton.setDisable(!playEnabled);
            pauseButton.setDisable(!pauseEnabled);
            stopButton.setDisable(!stopEnabled);
        });
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
}
