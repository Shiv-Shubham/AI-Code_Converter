package com.example.code_converter.controller;

import com.example.code_converter.dto.ConvertRequest;
import com.example.code_converter.dto.ConvertResponse;
import com.example.code_converter.service.AiConversionService;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ConvertController {

    private final AiConversionService aiservice;

    public ConvertController(AiConversionService aiservice) {
        this.aiservice = aiservice;
    }

    // 🔹 Normal (non-streaming) endpoint - keep it for testing
 //   @PostMapping("/convert")
//    public ConvertResponse convert(@RequestBody ConvertRequest request) {
//        String result = aiservice.convertStream(
//                request.getSourceLanguage(),
//                request.getTargetLanguage(),
//                request.getCode()
//        );
//        return new ConvertResponse(result);
//    }

    // 🔹 Streaming endpoint (SSE)
    @GetMapping(value = "/convert/stream", produces = "text/event-stream")
    public SseEmitter convertStream(
           // @RequestParam String sourceLanguage,
            @RequestParam String targetLanguage,
            @RequestParam String code
    ) {

        SseEmitter emitter = new SseEmitter(0L); // no timeout

        new Thread(() -> {
            try {
                aiservice.convertStream(targetLanguage, code, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));  // 🔴 FORCE FLUSH
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
