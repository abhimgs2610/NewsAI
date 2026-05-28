package com.newsai.news_ai_backend.serviceimpl;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BatchPauseService {

	private final AtomicReference<Instant> pausedUntil = new AtomicReference<>(Instant.EPOCH);
	private final AtomicBoolean stopRequested = new AtomicBoolean(false);

	@Value("${sync.stop-file.pauseDurationMs:1800000}")
	private long pauseDurationMs;

	public Instant requestPause(String reason) {
		Instant until = Instant.now().plusMillis(Math.max(1L, pauseDurationMs));
		pausedUntil.set(until);
		stopRequested.set(true);
		return until;
	}

	public boolean isPaused() {
		return Instant.now().isBefore(pausedUntil.get());
	}

	public Instant getPausedUntil() {
		return pausedUntil.get();
	}

	public void throwIfStopRequested() {
		if (stopRequested.get() && isPaused()) {
			throw new BatchPausedException("Batch pause requested until " + pausedUntil.get());
		}
	}

	public void markCurrentBatchStopped() {
		stopRequested.set(false);
	}
}