package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.reflection.ReflectionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    private final ReflectionService reflectionService;

    public ReflectionController(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    @GetMapping(value = "/compose", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter compose(@RequestParam Long bookId, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        reflectionService.composeAsync(userId, bookId, emitter);
        return emitter;
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) throw new AuthException("인증이 필요합니다.");
        return userId;
    }
}
