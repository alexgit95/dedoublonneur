package fr.dedoublonneur.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ProcessingProgressRegistry {

    private final Map<Long, Progress> progressByJob = new ConcurrentHashMap<>();

    public void start(Long jobId, int totalItems) {
        progressByJob.put(jobId, new Progress(totalItems));
    }

    public void itemProcessed(Long jobId, long bytesCopied) {
        progressByJob.computeIfPresent(jobId, (id, progress) -> progress.process(bytesCopied));
    }

    public void complete(Long jobId) {
        progressByJob.computeIfPresent(jobId, (id, progress) -> progress.complete());
    }

    public void cancel(Long jobId) {
        progressByJob.computeIfPresent(jobId, (id, progress) -> progress.cancel());
    }

    public ProcessingProgressResponse get(Long jobId) {
        Progress progress = progressByJob.get(jobId);
        if (progress == null) {
            return new ProcessingProgressResponse("IDLE", 0, 0, 0, 0);
        }
        return progress.toResponse();
    }

    private static final class Progress {
        private final int totalItems;
        private int processedItems;
        private long bytesCopied;
        private String status = "PROCESSING";

        private Progress(int totalItems) {
            this.totalItems = totalItems;
        }

        private synchronized Progress process(long bytes) {
            processedItems++;
            bytesCopied += bytes;
            return this;
        }

        private synchronized Progress complete() {
            processedItems = totalItems;
            status = "DONE";
            return this;
        }

        private synchronized Progress cancel() {
            status = "CANCELLED";
            return this;
        }

        private synchronized ProcessingProgressResponse toResponse() {
            int percent = totalItems == 0 ? 100 : Math.round(processedItems * 100.0f / totalItems);
            return new ProcessingProgressResponse(status, totalItems, processedItems, percent, bytesCopied);
        }
    }
}
