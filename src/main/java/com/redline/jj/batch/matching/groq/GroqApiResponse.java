package com.redline.jj.batch.matching.groq;

import java.util.List;

public record GroqApiResponse(List<GroqChoice> choices) {
}
