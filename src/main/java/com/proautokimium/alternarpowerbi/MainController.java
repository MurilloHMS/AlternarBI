package com.proautokimium.alternarpowerbi;

import java.awt.AWTException;
import java.util.Optional;

import com.proautokimium.alternarpowerbi.infrastructure.services.SwitchPageService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;

public class MainController {

    private boolean isRunning = false;
    private boolean isPaused = false;
    private Thread workerThread;

    private final SwitchPageService service = new SwitchPageService();
    private int PAGE_TOTAL = 0;
    private final long INTERVAL_MS = 45000;
    private final long INITIAL_DELAY_MS = 5000;
    @FXML
    private Button pauseButton;

    @FXML
    private Button playButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private Button stopButton;

    @FXML
    void onPauseButtonClick(ActionEvent event) {
        isPaused = true;

        playButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(false);

        updateStatus("Pausado");
    }

    @FXML
    void onPlayButtonClick(ActionEvent event) {
        if (isPaused) {
            isPaused = false;
            playButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        } else {
            startAutomation();
        }
    }

    @FXML
    void onStopButtonClick(ActionEvent event) {
        stopAutomation();
    }

    @FXML
    void initialize() {
        progressBar.setProgress(0);
        statusLabel.setText("Pronto para iniciar");
        
        PAGE_TOTAL = initialQuestion();
        if(PAGE_TOTAL <= 0 ) {
        	updateStatus("Configuração cancelada ou inválida. Informe um número válido para continuar.");
        	playButton.setDisable(true);
        }else {
        	updateStatus("Total de paginas definido: " + PAGE_TOTAL);
        }
    }

    private void startAutomation() {
    	
    	if(PAGE_TOTAL <=0) {
    		updateStatus("Defina o total de paginas antes de iniciar!");
    		return;
    	}
        isRunning = true;
        isPaused = false;

        playButton.setDisable(true);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
             
                Platform.runLater(() -> updateStatus("Aguardando 5 segundos para iniciar..."));
                Thread.sleep(INITIAL_DELAY_MS);

                int pageActual = 1;

                while (isRunning) {
                    
                    while (isPaused && isRunning) {
                        Thread.sleep(500);
                    }

                    if (!isRunning) break;

                    try {
                        final int currentPage = pageActual;

                        Platform.runLater(() -> {
                            double progress = (double) currentPage / PAGE_TOTAL;
                            progressBar.setProgress(progress);
                            updateStatus(String.format("Processando página %d de %d", currentPage, PAGE_TOTAL));
                        });

                        service.nextPage(pageActual, PAGE_TOTAL);

                        pageActual++;
                        if (pageActual > PAGE_TOTAL) {
                            pageActual = 1;
                        }

                        Thread.sleep(INTERVAL_MS);
                        
                    } catch (AWTException | InterruptedException e) {
                        Platform.runLater(() -> updateStatus("Erro: " + e.getMessage()));
                        break;
                    }
                }

                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    updateStatus("Automação finalizada");
                    resetButtons();
                });

                return null;
            }
        };

        workerThread = new Thread(task);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void stopAutomation() {
        isRunning = false;
        isPaused = false;

        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        resetButtons();
        progressBar.setProgress(0);
        updateStatus("Parado pelo usuário");
    }

    private void resetButtons() {
        playButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private int initialQuestion() {
    	TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Configurações Iniciais");
        dialog.setHeaderText("Por favor, informe a quantidade total de páginas do relatório");
        dialog.setContentText("Páginas Totais:");
        
        Optional<String> result = dialog.showAndWait();
        
        if(result.isPresent()) {
        	try {
				return Integer.parseInt(result.get());
			} catch (NumberFormatException e) {
				return 0;
			}
        }
        return 0;
    }
}