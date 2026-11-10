package com.barotable.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** API 오류를 {"message": "..."} 한 가지 모양으로 돌려준다. 화면은 message 를 그대로 보여 준다 */
@RestControllerAdvice(basePackages = "com.barotable.api")
public class ApiErrors {

    @ExceptionHandler(IllegalStateException.class)        // 규칙 위반, 겹침 등
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)     // 없는 테이블·예약
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", "입력값을 다시 확인해 주세요."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class) // 예약번호 동시 발급 등 드문 충돌
    public ResponseEntity<Map<String, String>> integrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message", "잠시 후 다시 시도해 주세요."));
    }
}
