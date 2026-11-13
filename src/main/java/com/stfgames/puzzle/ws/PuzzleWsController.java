package com.stfgames.puzzle.ws;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WEB SERVICE REST (creacion). Protegido con Spring Security (HTTP Basic, rol WS).
 *   GET  /api/puzzle/state   -> estado actual (tablero + imagen)
 *   PUT  /api/puzzle/image   -> {"image": "data:image/jpeg;base64,..."}
 *   POST /api/puzzle/start   -> {"dimensiones": "3x3"}
 *   POST /api/puzzle/move    -> {"tile": "5"}
 */
@RestController
@RequestMapping("/api/puzzle")
public class PuzzleWsController {

    private final PuzzleService service;

    public PuzzleWsController(PuzzleService service) {
        this.service = service;
    }

    @GetMapping({"", "/state"})
    public PuzzleState get() {
        return service.state();
    }

    @PutMapping("/image")
    public PuzzleState image(@RequestBody Map<String, String> body) {
        return service.saveImage(body.get("image"));
    }

    @PostMapping("/start")
    public PuzzleState start(@RequestBody Map<String, String> body) {
        return service.start(body.get("dimensiones"));
    }

    @PostMapping("/move")
    public PuzzleState move(@RequestBody Map<String, String> body) {
        return service.move(body.get("tile"));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
