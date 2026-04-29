package com.redline.jj.api.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestockSubscriber implements MessageListener {

    private final SseEmitterRepository sseEmitterRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String rawMessage = new String(message.getBody());
        Map<String, Object> map;
        try {
            map = objectMapper.readValue(rawMessage, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("재입고 알림 메시지 역직렬화 실패: {}", rawMessage, e);
            return;
        }

        Long userId = ((Number) map.get("userId")).longValue();
        sseEmitterRepository.get(userId).ifPresent(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(rawMessage));
            } catch (IOException e) {
                sseEmitterRepository.remove(userId);
            }
        });
    }
}
