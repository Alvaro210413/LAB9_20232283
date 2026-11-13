package com.stfgames.puzzle.ws;

import java.util.List;

/** Respuesta del Web Service. */
public record PuzzleState(
        String stf_game_board_structure,
        Integer n,
        List<String> tiles,
        String image,
        boolean finished,
        String message,
        List<String> nextTiles) {
}
