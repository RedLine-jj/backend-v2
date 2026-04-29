package com.redline.jj.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class SseEmitterRepositoryTest {

    private SseEmitterRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SseEmitterRepository();
    }

    @Test
    @DisplayName("add 후 get - 동일 emitter 반환")
    void add_후_get_동일_emitter() {
        SseEmitter emitter = new SseEmitter();
        repository.add(1L, emitter);

        assertThat(repository.get(1L)).contains(emitter);
    }

    @Test
    @DisplayName("동일 userId 재add - 기존 emitter complete 후 새 emitter 반환")
    void 동일_userId_재add_기존_complete() {
        SseEmitter oldEmitter = mock(SseEmitter.class);
        SseEmitter newEmitter = new SseEmitter();

        repository.add(1L, oldEmitter);
        repository.add(1L, newEmitter);

        verify(oldEmitter, times(1)).complete();
        assertThat(repository.get(1L)).contains(newEmitter);
    }

    @Test
    @DisplayName("remove 후 get - Optional.empty 반환")
    void remove_후_get_empty() {
        SseEmitter emitter = new SseEmitter();
        repository.add(1L, emitter);
        repository.remove(1L);

        assertThat(repository.get(1L)).isEmpty();
    }

    @Test
    @DisplayName("없는 userId remove - 예외 미발생")
    void 없는_userId_remove_예외_없음() {
        assertThatCode(() -> repository.remove(999L))
            .doesNotThrowAnyException();
    }
}
