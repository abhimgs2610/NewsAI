package com.newsai.news_ai_backend.serviceimpl;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(name = "sync.stop-file.enabled", havingValue = "true", matchIfMissing = true)
public class BatchStopFileListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchStopFileListener.class);

	private final BatchPauseService batchPauseService;
	private WatchService watchService;
	private Thread listenerThread;

	@Value("${sync.stop-file.directory:C:\\users2\\newsAIBatch}")
	private String stopFileDirectory;

	@Value("${sync.stop-file.name:STOP.txt}")
	private String stopFileName;

	public BatchStopFileListener(BatchPauseService batchPauseService) {
		this.batchPauseService = batchPauseService;
	}

	@PostConstruct
	public void start() throws IOException {
		Path directory = Path.of(stopFileDirectory);
		Files.createDirectories(directory);
		watchService = directory.getFileSystem().newWatchService();
		directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

		listenerThread = new Thread(() -> listen(directory), "news-ai-batch-stop-file-listener");
		listenerThread.setDaemon(true);
		listenerThread.start();

		logger.info("Batch STOP file listener started. directory={}, file={}", directory, stopFileName);
		processStopFileIfPresent(directory);
	}

	@PreDestroy
	public void stop() throws IOException {
		if (watchService != null) {
			watchService.close();
		}
	}

	private void listen(Path directory) {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				WatchKey key = watchService.take();
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					Path changedFile = (Path) event.context();
					if (stopFileName.equalsIgnoreCase(changedFile.toString())) {
						processStopFile(directory.resolve(changedFile));
					}
				}
				if (!key.reset()) {
					logger.warn("Batch STOP file listener stopped because watch key is no longer valid.");
					return;
				}
			} catch (ClosedWatchServiceException e) {
				return;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (Exception e) {
				logger.warn("Batch STOP file listener failed while processing file event.", e);
			}
		}
	}

	private void processStopFileIfPresent(Path directory) {
		Path stopFile = directory.resolve(stopFileName);
		if (Files.exists(stopFile)) {
			processStopFile(stopFile);
		}
	}

	private void processStopFile(Path stopFile) {
		Instant pausedUntil = batchPauseService.requestPause("STOP file detected: " + stopFile);
		logger.warn("Batch pause requested by STOP file. stopFile={}, pausedUntil={}", stopFile, pausedUntil);
		try {
			Files.deleteIfExists(stopFile);
		} catch (IOException e) {
			logger.warn("Could not delete STOP file after processing. stopFile={}", stopFile, e);
		}
	}
}