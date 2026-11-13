package com.stfgames.puzzle.ws;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Acceso a la tabla stf_puzzle_sv (una sola fila, 2 columnas). Se usa JdbcTemplate porque la
 * tabla no tiene llave primaria (solo stf_game_board_structure e image).
 */
@Repository
public class PuzzleRepository {

    private final JdbcTemplate jdbc;

    public PuzzleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void ensureRow() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM stf_puzzle_sv", Integer.class);
        if (n == null || n == 0) {
            jdbc.update("INSERT INTO stf_puzzle_sv (stf_game_board_structure, image) VALUES (NULL, NULL)");
        }
    }

    public Map<String, Object> find() {
        ensureRow();
        List<Map<String, Object>> rows =
                jdbc.queryForList("SELECT stf_game_board_structure, image FROM stf_puzzle_sv LIMIT 1");
        return rows.get(0);
    }

    public void updateBoard(String structure) {
        ensureRow();
        jdbc.update("UPDATE stf_puzzle_sv SET stf_game_board_structure = ?", structure);
    }

    public void updateImage(String base64) {
        ensureRow();
        jdbc.update("UPDATE stf_puzzle_sv SET image = ?", base64);
    }
}
