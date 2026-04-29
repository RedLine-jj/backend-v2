package com.redline.jj.api.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterRepository {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void add(Long userId, SseEmitter emitter) {
        emitters.compute(userId, (key, existing) -> {
            if (existing != null) {
                existing.complete();
            }
            return emitter;
        });
    }

    public void remove(Long userId) {
        emitters.remove(userId);
    }

    public Optional<SseEmitter> get(Long userId) {
        return Optional.ofNullable(emitters.get(userId));
    }
}
