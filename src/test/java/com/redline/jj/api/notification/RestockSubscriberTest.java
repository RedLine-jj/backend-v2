package com.redline.jj.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestockSubscriberTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    private RestockSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new RestockSubscriber(sseEmitterRepository, new ObjectMapper());
    }

    private DefaultMessage buildMessage(String body) {
        return new DefaultMessage("restock".getBytes(), body.getBytes());
    }

    @Test
    @DisplayName("정상 메시지 수신 - emitter.send 1회 호출")
    void 정상_메시지_send_호출() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        given(sseEmitterRepository.get(1L)).willReturn(Optional.of(emitter));

        String json = "{\"userId\":1,\"modelId\":10,\"modelName\":\"Test\",\"brandName\":\"Brand\"}";
        subscriber.onMessage(buildMessage(json), null);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("해당 userId emitter 없음 - no-op 예외 미발생")
    void emitter_없음_no_op() {
        given(sseEmitterRepository.get(1L)).willReturn(Optional.empty());

        String json = "{\"userId\":1,\"modelId\":10,\"modelName\":\"Test\",\"brandName\":\"Brand\"}";
        assertThatCode(() -> subscriber.onMessage(buildMessage(json), null))
            .doesNotThrowAnyException();

        verify(sseEmitterRepository, never()).remove(any());
    }

    @Test
    @DisplayName("emitter.send IOException - remove 호출")
    void send_IOException_remove_호출() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        given(sseEmitterRepository.get(1L)).willReturn(Optional.of(emitter));
        willThrow(new IOException()).given(emitter).send(any(SseEmitter.SseEventBuilder.class));

        String json = "{\"userId\":1,\"modelId\":10,\"modelName\":\"Test\",\"brandName\":\"Brand\"}";
        subscriber.onMessage(buildMessage(json), null);

        verify(sseEmitterRepository, times(1)).remove(eq(1L), any(SseEmitter.class));
    }

    @Test
    @DisplayName("JSON 역직렬화 실패 - 예외 없이 log만")
    void JSON_역직렬화_실패_예외_없음() {
        assertThatCode(() -> subscriber.onMessage(buildMessage("invalid-json"), null))
            .doesNotThrowAnyException();

        verifyNoInteractions(sseEmitterRepository);
    }

    @Test
    @DisplayName("동시에 다수 메시지 수신 (멀티스레드) - ConcurrentHashMap 동시성 안전")
    void 멀티스레드_메시지_수신_동시성_안전() throws InterruptedException, IOException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        SseEmitter emitter = mock(SseEmitter.class);
        given(sseEmitterRepository.get(1L)).willReturn(Optional.of(emitter));

        String json = "{\"userId\":1,\"modelId\":10,\"modelName\":\"Test\",\"brandName\":\"Brand\"}";
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    subscriber.onMessage(buildMessage(json), null);
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(finished).as("10개 스레드가 5초 내에 완료되어야 합니다").isTrue();
        assertThat(errors).isEmpty();
        verify(emitter, times(threadCount)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
