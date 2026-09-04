package com.recoup.backend.dto;

public record BatchRunResponse(String batchId, int totalEvents, String gatewayMode,
                                boolean modelTrained, int modelTrainingSamples) {
}
