package com.redline.jj.batch.matching.groq;

import java.util.List;

public record GroqApiRequest(String model, List<GroqMessage> messages) {
}
