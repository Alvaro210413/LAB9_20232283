// Stf-Puzzle Simplified Version - frontend (sin canvas, sin recargar la pagina).
// Todas las operaciones van a /juego/**, que consume el Web Service REST con Spring Boot.
const DEFAULT_IMG = 'img/default.jpg';
const BOARD_PX = 420;
const DIM_RE = /^\s*(\d+)\s*[xX]\s*(\d+)\s*$/;

const $ = (id) => document.getElementById(id);
const preview = $('preview'), color = $('color'), dims = $('dims'), dimsError = $('dimsError');
const file = $('file'), btnSelect = $('btnSelect'), btnStart = $('btnStart');
const board = $('board'), message = $('message');

let state = { n: null, tiles: null, image: null };
let ratio = 1;   // alto/ancho de la imagen actual (para no deformarla)

// Lee la proporcion real de la imagen y vuelve a dibujar el tablero
function loadRatio() {
    const im = new Image();
    im.onload = () => {
        ratio = im.naturalHeight / im.naturalWidth || 1;
        if (state.tiles) render(state.tiles, state.n);
    };
    im.src = imageUrl();
}
let busy = false;

async function api(method, url, body) {
    const res = await fetch(url, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined,
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || 'Error en el servicio');
    return data;
}

const imageUrl = () => state.image || DEFAULT_IMG;

// ----------------------------------------------------- validacion NxN
function validateDims() {
    const v = dims.value;
    let err = '';
    const m = v.match(DIM_RE);
    if (!v.trim()) err = 'Ingrese las dimensiones del tablero (p. ej. 3x3).';
    else if (!m) err = 'Formato invalido. Use NxN, por ejemplo 3x3.';
    else if (m[1] !== m[2]) err = 'El tablero debe ser cuadrado (NxN).';
    else if (+m[1] < 3 || +m[1] > 10) err = 'N debe estar entre 3 y 10.';
    dimsError.textContent = err;
    dims.classList.toggle('invalid', !!err);
    btnStart.hidden = !!err;        // el boton solo aparece si el formato es correcto
    return !err;
}

// ----------------------------------------------------- tablero (divs)
function render(tiles, n) {
    // El tablero toma la proporcion de la imagen (max. BOARD_PX de ancho o alto)
    let W = BOARD_PX, H = BOARD_PX * ratio;
    if (H > BOARD_PX) { H = BOARD_PX; W = BOARD_PX / ratio; }
    const tw = Math.floor(W / n), th = Math.floor(H / n);
    board.hidden = false;
    board.style.gridTemplateColumns = `repeat(${n}, ${tw}px)`;
    board.style.backgroundColor = color.value;
    board.innerHTML = '';
    tiles.forEach((t, i) => {
        const d = document.createElement('div');
        d.className = 'tile';
        d.style.width = tw + 'px';
        d.style.height = th + 'px';
        if (t === 'X') {
            d.classList.add('empty');
            d.style.backgroundColor = color.value;
        } else {
            const k = parseInt(t, 10) - 1;               // posicion de la pieza en la imagen ordenada
            const r = Math.floor(k / n), c = k % n;
            d.style.backgroundImage = `url("${imageUrl()}")`;
            d.style.backgroundSize = `${n * 100}% ${n * 100}%`;
            d.style.backgroundPosition = `${(c * 100) / (n - 1)}% ${(r * 100) / (n - 1)}%`;
            d.addEventListener('click', () => onTileClick(t, i, d));
        }
        board.appendChild(d);
    });
}

function isAdjacent(i, j, n) {
    return Math.abs(Math.floor(i / n) - Math.floor(j / n)) + Math.abs((i % n) - (j % n)) === 1;
}

async function onTileClick(tile, index, el) {
    if (busy) return;
    const empty = state.tiles.indexOf('X');
    if (!isAdjacent(index, empty, state.n)) {      // validacion en el frontend
        el.classList.remove('shake'); void el.offsetWidth; el.classList.add('shake');
        message.textContent = 'Solo puede mover una casilla contigua al espacio vacio.';
        return;
    }
    busy = true;
    try {
        const s = await api('POST', '/juego/mover', { tile });   // el WS valida y guarda en la BD
        message.textContent = '';
        if (s.finished) {
            state.tiles = s.tiles;
            render(s.tiles, s.n);
            message.textContent = s.message;                    // "Juego terminado !"
            setTimeout(() => {                                   // nuevo juego (ya guardado en la BD)
                state.tiles = s.nextTiles;
                render(state.tiles, state.n);
                message.textContent = '';
                busy = false;
            }, 3000);
            return;
        }
        state.tiles = s.tiles;
        render(s.tiles, s.n);
    } catch (e) {
        message.textContent = e.message;
    }
    busy = false;
}

// ----------------------------------------------------- eventos
btnSelect.addEventListener('click', () => file.click());

file.addEventListener('change', () => {
    const f = file.files[0];
    file.value = '';
    if (!f) return;
    const name = f.name.toLowerCase();
    const ok = (/\.(jpg|jpeg)$/.test(name) && f.type === 'image/jpeg') || (name.endsWith('.png') && f.type === 'image/png');
    if (!ok) { alert('Solo se permite subir imagenes .jpg o .png'); return; }
    const reader = new FileReader();
    reader.onload = async () => {
        try {
            const s = await api('PUT', '/juego/imagen', { image: reader.result });
            state.image = s.image;
            preview.src = s.image;                               // se actualiza sin recargar
            loadRatio();
        } catch (e) { alert(e.message); }
    };
    reader.readAsDataURL(f);
});

dims.addEventListener('input', validateDims);

color.addEventListener('input', () => { if (state.tiles) render(state.tiles, state.n); });

btnStart.addEventListener('click', async () => {
    if (!validateDims()) return;
    try {
        const s = await api('POST', '/juego/iniciar', { dimensiones: dims.value });
        state.n = s.n; state.tiles = s.tiles;
        message.textContent = '';
        render(s.tiles, s.n);
    } catch (e) { dimsError.textContent = e.message; }
});

// ----------------------------------------------------- carga inicial (retoma el juego)
(async function init() {
    validateDims();
    try {
        const s = await api('GET', '/juego/estado');
        state.image = s.image;
        preview.src = imageUrl();
        loadRatio();
        if (s.tiles) {
            state.n = s.n; state.tiles = s.tiles;
            dims.value = `${s.n}x${s.n}`;
            validateDims();
            render(s.tiles, s.n);
        }
    } catch (e) { message.textContent = 'No se pudo conectar con el servicio: ' + e.message; }
})();
