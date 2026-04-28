package com.redline.jj.batch.matching;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// LLM 응답 스키마 변경 시 알 수 없는 필드로 인한 파싱 실패 방지
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmMatchResult(String brandName, String modelName, double confidence) {}
