
let vistaActual = 'campo';

function alternarVista() {
    if (vistaActual === 'campo') {
        vistaActual = 'lista';
        document.getElementById('vista-campo').classList.add('oculto');
        document.getElementById('vista-lista').classList.remove('oculto');
    } else {
        vistaActual = 'campo';
        document.getElementById('vista-lista').classList.add('oculto');
        document.getElementById('vista-campo').classList.remove('oculto');
    }
}

async function cargarTodo() {
    const num = document.getElementById("numJornada").value;
    cargarCampo(num);
    cargarLista(num);
}

async function cargarCampo(num) {
    try {
        const response = await fetch(`/jornada/${num}/resumen-partido`);
        const data = await response.json();

        if(data.error) {
            document.getElementById("marcador").innerText = data.error;
            document.getElementById("area-equipo-a").innerHTML = "";
            document.getElementById("area-equipo-b").innerHTML = "";
            return;
        }

        const puntosA = data.equipoA.reduce((acc, j) => acc + j.puntos, 0);
        const puntosB = data.equipoB.reduce((acc, j) => acc + j.puntos, 0);

        document.getElementById("marcador").innerHTML = `
            <span style="color:${getColorCode(data.colorA)}">${data.colorA} ${puntosA}</span>
            -
            <span style="color:${getColorCode(data.colorB)}">${puntosB} ${data.colorB}</span>
        `;

        renderEquipo("area-equipo-a", data.equipoA, data.colorA);
        renderEquipo("area-equipo-b", data.equipoB, data.colorB);

    } catch (e) { console.error(e); }
}

async function cargarLista(num) {
    try {
        const response = await fetch(`/jornada/${num}/resumen-managers`);
        const data = await response.json();
        const container = document.getElementById('lista-contenido');

        if (!data || data.length === 0) {
            container.innerHTML = '<p style="text-align:center;">Sin datos de mánagers.</p>';
            return;
        }

        container.innerHTML = data.map(m => {
            let htmlJugadores = m.jugadores.map(j => {
                let colorClass = 'text-orange';
                if (j.puntos > 0) colorClass = 'text-green';
                if (j.puntos < 0) colorClass = 'text-red';
                return `
                <div class="player-row">
                    <span>${j.nombre} <small>(${j.posicion.substring(0,3)})</small></span>
                    <span class="${colorClass}">${j.puntos}</span>
                </div>`;
            }).join('');

            return `
            <div class="manager-card">
                <div class="manager-header">
                    <span>${m.manager}</span>
                    <span class="pts-badge">${m.puntosTotal} pts</span>
                </div>
                <div>${htmlJugadores}</div>
            </div>`;
        }).join('');

    } catch (e) { console.error(e); }
}

function renderEquipo(elementId, jugadores, color) {
    const container = document.getElementById(elementId);
    container.innerHTML = "";

    const lineas = { PORTERO: [], DEFENSA: [], MEDIO: [], DELANTERO: [] };
    jugadores.forEach(j => {
        if(lineas[j.posicion]) lineas[j.posicion].push(j);
        else if(lineas.MEDIO) lineas.MEDIO.push(j);
    });

    const orden = ["PORTERO", "DEFENSA", "MEDIO", "DELANTERO"];

    orden.forEach(pos => {
        if (lineas[pos].length > 0) {
            const fila = document.createElement("div");
            fila.className = "linea-jugadores";
            lineas[pos].forEach(jug => {
                let imgUrl = jug.imagen && jug.imagen.startsWith('/') ? jug.imagen : '/icon.png';
                if(jug.imagen && jug.imagen.startsWith('http')) imgUrl = jug.imagen;

                let badgeClass = 'badge-orange';
                if(jug.puntos > 0) badgeClass = 'badge-green';
                if(jug.puntos < 0) badgeClass = 'badge-red';

                let crownHtml = jug.mvp ? '<div class="mvp-crown">👑</div>' : '';

                fila.innerHTML += `
                    <div class="jugador-card camiseta-${color} ${jug.mvp ? 'mvp' : ''}">
                        ${crownHtml}
                        <div class="puntos-badge ${badgeClass}">${jug.puntos}</div>
                        <img src="${imgUrl}" class="jugador-img">
                        <div class="jugador-nombre">${jug.nombre}</div>
                    </div>
                `;
            });
            container.appendChild(fila);
        }
    });
}

function getColorCode(nombreColor) {
    if(!nombreColor) return '#fff';
    switch(nombreColor.toUpperCase()) {
        case 'ROJO': return '#ff5252';
        case 'AZUL': return '#448aff';
        case 'AMARILLO': return '#ffd740';
        case 'BLANCO': return '#ffffff';
        default: return '#fff';
    }
}

window.onload = async () => {
    try {
        const response = await fetch('/jornada/actual');
        const actual = await response.json();
        let objetivo = actual - 1;
        if (objetivo < 1) objetivo = 1;
        document.getElementById("numJornada").value = objetivo;
        cargarTodo();
    } catch (e) {
        console.error("Error al obtener jornada actual:", e);
        cargarTodo();
    }
};