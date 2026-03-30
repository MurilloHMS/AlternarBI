package com.proautokimium.alternarpowerbi.infrastructure.services;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

import com.proautokimium.alternarpowerbi.infrastructure.util.LoggerConfig;

public class ConfigService {

    private static final Logger LOGGER = LoggerConfig.getLogger(ConfigService.class.getName());

    private static final String APP_DIR    = System.getProperty("user.home") + File.separator + "AlternarBI";
    private static final String CONFIG_FILE = APP_DIR + File.separator + "config.properties";

    private static final String KEY_TOTAL_PAGES      = "totalPages";
    private static final String KEY_INTERVAL_DEFAULT  = "intervalDefault";
    private static final String KEY_INTERVAL_PREFIX   = "interval.page.";
    private static final String KEY_LOOP              = "loop";

    private static final int    DEFAULT_TOTAL_PAGES  = 10;
    private static final long   DEFAULT_INTERVAL_MS  = 45_000;

    private int  totalPages       = DEFAULT_TOTAL_PAGES;
    private long defaultIntervalMs = DEFAULT_INTERVAL_MS;
    private long[] pageIntervals;
    private boolean loop          = true;

    private static ConfigService instance;

    private ConfigService() {
        pageIntervals = buildDefaultIntervals(DEFAULT_TOTAL_PAGES, DEFAULT_INTERVAL_MS);
        load();
    }

    public static ConfigService getInstance() {
        if (instance == null) instance = new ConfigService();
        return instance;
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            LOGGER.info("Nenhum arquivo de config encontrado — usando padrões");
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            java.util.Properties props = new java.util.Properties();
            props.load(in);

            totalPages        = parseInt(props, KEY_TOTAL_PAGES, DEFAULT_TOTAL_PAGES);
            defaultIntervalMs = parseLong(props, KEY_INTERVAL_DEFAULT, DEFAULT_INTERVAL_MS);
            loop              = Boolean.parseBoolean(props.getProperty(KEY_LOOP, "true"));

            pageIntervals = buildDefaultIntervals(totalPages, defaultIntervalMs);
            for (int i = 1; i <= totalPages; i++) {
                String key = KEY_INTERVAL_PREFIX + i;
                if (props.containsKey(key)) {
                    pageIntervals[i - 1] = parseLong(props, key, defaultIntervalMs);
                }
            }

            LOGGER.info(String.format("Config carregada: %d páginas, intervalo padrão %dms", totalPages, defaultIntervalMs));

        } catch (IOException e) {
            LOGGER.warning("Erro ao carregar config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(Paths.get(APP_DIR));
            java.util.Properties props = new java.util.Properties();
            props.setProperty(KEY_TOTAL_PAGES,     String.valueOf(totalPages));
            props.setProperty(KEY_INTERVAL_DEFAULT, String.valueOf(defaultIntervalMs));
            props.setProperty(KEY_LOOP,             String.valueOf(loop));
            for (int i = 1; i <= totalPages; i++) {
                props.setProperty(KEY_INTERVAL_PREFIX + i, String.valueOf(getIntervalForPage(i)));
            }
            try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "AlternarBI — configurações salvas automaticamente");
            }
            LOGGER.info("Config salva em " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.warning("Erro ao salvar config: " + e.getMessage());
        }
    }

    public int getTotalPages() { return totalPages; }

    public void setTotalPages(int n) {
        this.totalPages = n;
        // Preserva intervalos existentes, preenche novos com o padrão
        long[] novo = buildDefaultIntervals(n, defaultIntervalMs);
        for (int i = 0; i < Math.min(n, pageIntervals.length); i++) {
            novo[i] = pageIntervals[i];
        }
        pageIntervals = novo;
    }

    public long getDefaultIntervalMs() { return defaultIntervalMs; }

    public void setDefaultIntervalMs(long ms) {
        this.defaultIntervalMs = ms;
        // Atualiza só as páginas que ainda estavam no valor padrão anterior
        for (int i = 0; i < pageIntervals.length; i++) {
            if (pageIntervals[i] == this.defaultIntervalMs || pageIntervals[i] == DEFAULT_INTERVAL_MS) {
                pageIntervals[i] = ms;
            }
        }
        this.defaultIntervalMs = ms;
    }

    public long getIntervalForPage(int page) {
        int idx = page - 1;
        if (idx >= 0 && idx < pageIntervals.length) return pageIntervals[idx];
        return defaultIntervalMs;
    }

    public void setIntervalForPage(int page, long ms) {
        int idx = page - 1;
        if (idx >= 0 && idx < pageIntervals.length) pageIntervals[idx] = ms;
    }

    public long[] getPageIntervals() { return pageIntervals; }

    public boolean isLoop() { return loop; }
    public void setLoop(boolean v) { this.loop = v; }

    private static long[] buildDefaultIntervals(int pages, long defMs) {
        long[] arr = new long[pages];
        java.util.Arrays.fill(arr, defMs);
        return arr;
    }

    private static int parseInt(java.util.Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static long parseLong(java.util.Properties p, String key, long def) {
        try { return Long.parseLong(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}