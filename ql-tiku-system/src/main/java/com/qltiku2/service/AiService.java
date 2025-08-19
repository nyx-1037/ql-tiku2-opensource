package com.qltiku2.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface AiService {
    Flux<ResponseEntity<String>> analyzeQuestion(Map<String, Object> params);
    Flux<ResponseEntity<String>> sendMessage(Map<String, Object> params);
}