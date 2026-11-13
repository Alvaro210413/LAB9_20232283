package com.stfgames.puzzle.ws;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logica del juego: desordenado, validacion de movimientos y fin del juego. */
@Service
public class PuzzleService {

    public static final String EMPTY = "X";
    public static final int MIN_N = 3, MAX_N = 10;
    private static final Pattern DIM = Pattern.compile("^\\s*(\\d+)\\s*[xX]\\s*(\\d+)\\s*$");

    /**
     * Secuencia UNICA de 8 pasos con la que se desordena el tablero, partiendo de la imagen
     * ordenada (de atras hacia adelante). Cada letra es el movimiento de la casilla vacia:
     * U = arriba, L = izquierda, D = abajo, R = derecha. Funciona para cualquier NxN con N >= 3.
     * En 3x3: 12345678X -> 41372586X.
     */
    public static final char[] SCRAMBLE = {'U', 'L', 'U', 'L', 'D', 'D', 'R', 'R'};

    private final PuzzleRepository repo;

    public PuzzleService(PuzzleRepository repo) {
        this.repo = repo;
    }

    // ------------------------------------------------------------------ estado
    public PuzzleState state() {
        Map<String, Object> row = repo.find();
        String structure = (String) row.get("stf_game_board_structure");
        String image = (String) row.get("image");
        List<String> tiles = structure == null ? null : parse(structure);
        Integer n = tiles == null ? null : (int) Math.round(Math.sqrt(tiles.size()));
        return new PuzzleState(structure, n, tiles, toDataUrl(image),
                false, null, null);
    }

    // ------------------------------------------------------------------ imagen
    public PuzzleState saveImage(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) throw new IllegalArgumentException("Debe enviar una imagen.");
        String b64 = dataUrl;
        if (dataUrl.startsWith("data:")) {
            int comma = dataUrl.indexOf(',');
            String header = dataUrl.substring(0, Math.max(comma, 0)).toLowerCase();
            if (!header.startsWith("data:image/jpeg") && !header.startsWith("data:image/jpg")
                    && !header.startsWith("data:image/png"))
                throw new IllegalArgumentException("Solo se permiten imagenes .jpg o .png");
            b64 = dataUrl.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La imagen no tiene una codificacion valida.");
        }
        if (mimeOf(bytes) == null)
            throw new IllegalArgumentException("Solo se permiten imagenes .jpg o .png");
        repo.updateImage(b64);
        return state();
    }

    /** Detecta el formato por la firma del archivo: JPEG (FF D8 FF) o PNG (89 50 4E 47). */
    private static String mimeOf(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        return null;
    }

    /** Arma el data URL con el tipo correcto a partir del Base64 guardado en la BD. */
    private static String toDataUrl(String b64) {
        if (b64 == null) return null;
        String mime = b64.startsWith("iVBOR") ? "image/png" : "image/jpeg";
        return "data:" + mime + ";base64," + b64;
    }

    // ------------------------------------------------------------------ inicio
    public int parseDimension(String dims) {
        if (dims == null || dims.isBlank())
            throw new IllegalArgumentException("Debe ingresar las dimensiones del tablero (p. ej. 3x3).");
        Matcher m = DIM.matcher(dims);
        if (!m.matches()) throw new IllegalArgumentException("Formato invalido. Use NxN, por ejemplo 3x3.");
        int a = Integer.parseInt(m.group(1)), b = Integer.parseInt(m.group(2));
        if (a != b) throw new IllegalArgumentException("El tablero debe ser cuadrado (NxN).");
        if (a < MIN_N || a > MAX_N)
            throw new IllegalArgumentException("N debe estar entre " + MIN_N + " y " + MAX_N + ".");
        return a;
    }

    public PuzzleState start(String dims) {
        int n = parseDimension(dims);
        repo.updateBoard(serialize(scrambled(n), n));
        return state();
    }

    // ------------------------------------------------------------- movimiento
    public PuzzleState move(String tile) {
        Map<String, Object> row = repo.find();
        String structure = (String) row.get("stf_game_board_structure");
        if (structure == null) throw new IllegalStateException("No hay un juego iniciado.");
        List<String> tiles = parse(structure);
        int n = (int) Math.round(Math.sqrt(tiles.size()));
        int from = tiles.indexOf(tile == null ? "" : tile.trim());
        if (from < 0 || EMPTY.equals(tile)) throw new IllegalArgumentException("Casilla invalida.");
        int empty = tiles.indexOf(EMPTY);
        int dist = Math.abs(from / n - empty / n) + Math.abs(from % n - empty % n);
        if (dist != 1) throw new IllegalArgumentException("Movimiento no valido: la casilla no es contigua a la vacia.");

        Collections.swap(tiles, from, empty);

        if (tiles.equals(solved(n))) {
            // Juego terminado: se inicia un nuevo juego en la BD (el frontend muestra el mensaje)
            String next = serialize(scrambled(n), n);
            repo.updateBoard(next);
            String image = (String) row.get("image");
            return new PuzzleState(serialize(tiles, n), n, tiles,
                    toDataUrl(image), true, "Juego terminado !", parse(next));
        }
        repo.updateBoard(serialize(tiles, n));
        return state();
    }

    // --------------------------------------------------------------- helpers
    public static List<String> solved(int n) {
        List<String> t = new ArrayList<>();
        for (int i = 1; i < n * n; i++) t.add(String.valueOf(i));
        t.add(EMPTY);
        return t;
    }

    public static List<String> scrambled(int n) {
        List<String> t = solved(n);
        int r = n - 1, c = n - 1;
        for (char mv : SCRAMBLE) {
            int nr = r, nc = c;
            switch (mv) {
                case 'U' -> nr--;
                case 'D' -> nr++;
                case 'L' -> nc--;
                case 'R' -> nc++;
            }
            Collections.swap(t, r * n + c, nr * n + nc);
            r = nr;
            c = nc;
        }
        return t;
    }

    /** 3x3: "41372586X" (como en el enunciado). N > 3: separado por comas, p. ej. "1,2,...,15,X". */
    public static String serialize(List<String> tiles, int n) {
        return n <= 3 ? String.join("", tiles) : String.join(",", tiles);
    }

    public static List<String> parse(String s) {
        List<String> out = new ArrayList<>();
        if (s.contains(",")) {
            for (String p : s.split(",")) out.add(p.trim());
        } else {
            for (char ch : s.toCharArray()) out.add(String.valueOf(ch));
        }
        return out;
    }
}
