package com.stfgames.puzzle.web;

import com.stfgames.puzzle.ws.PuzzleState;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * CONSUMO del WS desde Spring Boot: la pagina (fetch, sin recargar) llama a /juego/**, y este
 * controlador consume el WS REST /api/puzzle/** con RestTemplate + credenciales HTTP Basic.
 */
@RestController
@RequestMapping("/juego")
public class JuegoController {

    private final RestTemplate ws;

    public JuegoController(RestTemplate wsRestTemplate) {
        this.ws = wsRestTemplate;
    }

    @GetMapping("/estado")
    public ResponseEntity<?> estado() {
        return call(() -> ws.getForEntity("/state", PuzzleState.class));
    }

    @PutMapping("/imagen")
    public ResponseEntity<?> imagen(@RequestBody Map<String, String> body) {
        return call(() -> ws.exchange("/image", HttpMethod.PUT, new HttpEntity<>(body), PuzzleState.class));
    }

    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciar(@RequestBody Map<String, String> body) {
        return call(() -> ws.postForEntity("/start", body, PuzzleState.class));
    }

    @PostMapping("/mover")
    public ResponseEntity<?> mover(@RequestBody Map<String, String> body) {
        return call(() -> ws.postForEntity("/move", body, PuzzleState.class));
    }

    /** Reenvia al navegador los errores de validacion del WS (400) con su mensaje. */
    private ResponseEntity<?> call(Supplier<ResponseEntity<PuzzleState>> request) {
        try {
            ResponseEntity<PuzzleState> r = request.get();
            return ResponseEntity.ok(r.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .header("Content-Type", "application/json")
                    .body(e.getResponseBodyAsString());
        }
    }
}
