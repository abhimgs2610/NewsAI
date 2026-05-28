package com.newsai.news_ai_backend.serviceimpl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sync.scheduler.enabled", havingValue = "true")
public class NewsSyncScheduler {

	private static final Logger logger = LoggerFactory.getLogger(NewsSyncScheduler.class);

	private final NewsSyncService newsSyncService;
	private final BatchPauseService batchPauseService;
	private final AtomicBoolean running = new AtomicBoolean(false);

	@Value("${sync.scheduler.hours:24}")
	private int hours;

	public NewsSyncScheduler(NewsSyncService newsSyncService, BatchPauseService batchPauseService) {
		this.newsSyncService = newsSyncService;
		this.batchPauseService = batchPauseService;
	}

	@Scheduled(
			initialDelayString = "${sync.scheduler.initialDelayMs:60000}",
			fixedDelayString = "${sync.scheduler.fixedDelayMs:3600000}")
	public void syncLatestNews() {
		if (batchPauseService.isPaused()) {
			logger.info("Scheduled news sync skipped because batch is paused until {}.", batchPauseService.getPausedUntil());
			return;
		}

		if (!running.compareAndSet(false, true)) {
			logger.info("Scheduled news sync skipped because a previous sync is still running.");
			return;
		}

		try {
			logger.info("Scheduled news sync started for last {} hours.", hours);
			newsSyncService.syncLatestIndiaNews(hours);
			logger.info("Scheduled news sync completed.");
		} catch (BatchPausedException e) {
			batchPauseService.markCurrentBatchStopped();
			logger.warn("Scheduled news sync stopped because batch pause was requested. pausedUntil={}",
					batchPauseService.getPausedUntil());
		} catch (Exception e) {
			logger.warn("Scheduled news sync failed.", e);
		} finally {
			running.set(false);
		}
	}
}