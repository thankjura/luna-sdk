package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PluginWatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PluginWatcher.class);
    private static final long DEBOUNCE_TIME_MS = 1000;

    private final WatchService watchService;
    private final Thread watcherThread;
    private final Path targetDirectory;
    private final String pluginName;
    private final Consumer<Path> onFileChange;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<Path, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    public PluginWatcher(Path targetDirectory, String pluginName, Consumer<Path> onFileChange) throws IOException {
        this.targetDirectory = targetDirectory;
        this.pluginName = pluginName;
        this.onFileChange = onFileChange;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.targetDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        this.watcherThread = new Thread(this::watchLoop, "PluginWatcher");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FileDebounceScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    private boolean isFileStable(Path path) throws IOException, InterruptedException {
        long currentSize = Files.size(path);
        long currentModifiedTime = Files.getLastModifiedTime(path).toMillis();

        Thread.sleep(300);

        long nextSize = Files.size(path);
        long nextModifiedTime = Files.getLastModifiedTime(path).toMillis();

        return currentSize == nextSize && currentModifiedTime == nextModifiedTime;
    }

    private boolean isPluginFile(Path jarPath) {
        if (!jarPath.getFileName().toString().toLowerCase().endsWith(".jar")) {
            return false;
        }

        return jarPath.getFileName().startsWith(pluginName);
    }

    private void onFileModified(Path file) {
        ScheduledFuture<?> oldTask = pendingTasks.remove(file);
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        ScheduledFuture<?> newTask = scheduler.schedule(() -> {
            try {
                if (isFileStable(file)) {
                    onFileChange.accept(file);
                }
            } catch (Exception e) {
                log.warn("Error processing file changes", e);
            } finally {
                pendingTasks.remove(file);
            }
        }, DEBOUNCE_TIME_MS, TimeUnit.MILLISECONDS);

        pendingTasks.put(file, newTask);
    }

    private void watchLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path fileName = (Path) event.context();
                    Path jarPath = targetDirectory.resolve(fileName);
                    if (!isPluginFile(jarPath)) {
                        continue;
                    }

                    onFileModified(jarPath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    log.warn("Directory [{}] is invalid", targetDirectory.toAbsolutePath());
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Interrupt watch loop");
        }
    }

    @Override
    public void close() throws IOException {
        if (watcherThread != null) {
            watcherThread.interrupt();
        }

        if (watchService != null) {
            watchService.close();
        }

        if (scheduler != null) {
            scheduler.close();
        }

        log.debug("File watcher stopped watch [{}]", targetDirectory.toAbsolutePath());
    }
}
