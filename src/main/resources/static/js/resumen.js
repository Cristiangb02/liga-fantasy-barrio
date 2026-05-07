document.addEventListener('DOMContentLoaded', () => {
    // Intentar leer la jornada de la URL si vienes desde otra página (ej: historial)
    const urlParams = new URLSearchParams(window.location.search);
    const jornadaUrl = urlParams.get('jornada');

    if (jornadaUrl) {
        document.getElementById('numJornada').value = jornadaUrl;
        cargarTodo();
    } else {
        // SOLUCIÓN JORNADA: Preguntamos al backend cuál es la actual y le restamos 1
        fetch('/jornada/actual')
            .then(res => res.text())
            .then(num => {
                if (num && !isNaN(num)) {
                    let jornadaTerminada = parseInt(num) - 1;
                    if (jornadaTerminada < 1) jornadaTerminada = 1; // Para que no ponga jornada 0
                    document.getElementById('numJornada').value = jornadaTerminada;
                }
                cargarTodo();
            })
            .catch(err => {
                console.error("Error obteniendo jornada actual:", err);
                cargarTodo(); // Fallback a lo que haya en el input si falla
            });
    }
});

function cargarTodo() {
    const numeroJornada = document.getElementById('numJornada').value;
    if (!numeroJornada) return;

    const marcador = document.getElementById('marcador');
    const areaA = document.getElementById('area-equipo-a');
    const areaB = document.getElementById('area-equipo-b');

    marcador.innerText = "Cargando partido...";
    areaA.innerHTML = "";
    areaB.innerHTML = "";

    // 1. Cargar datos del campo de fútbol
    fetch(`/jornada/${numeroJornada}/resumen-partido`)
        .then(res => res.json())
        .then(data => {
            if (data.error) {
                marcador.innerText = data.error;
                return;
            }

            const ptsA = data.equipoA.reduce((sum, j) => sum + j.puntos, 0);
            const ptsB = data.equipoB.reduce((sum, j) => sum + j.puntos, 0);

            marcador.innerText = `${data.colorA} ${ptsA} - ${ptsB} ${data.colorB}`;

            dibujarEquipoEnCampo(data.equipoA, areaA, 'top');
            dibujarEquipoEnCampo(data.equipoB, areaB, 'bottom');
        })
        .catch(err => {
            console.error("Error al cargar la jornada:", err);
            marcador.innerText = "Error de conexión con el servidor";
        });

    // 2. Cargar datos de la lista de mánagers
    fetch(`/jornada/${numeroJornada}/resumen-managers`)
        .then(res => res.json())
        .then(managers => {
            const listaDiv = document.getElementById('lista-contenido');
            if (!managers || managers.length === 0) {
                listaDiv.innerHTML = '<p style="text-align:center; padding:20px;">No hay datos de mánagers.</p>';
                return;
            }

            listaDiv.innerHTML = managers.map(m => `
                <div class="manager-card">
                    <div class="manager-header">
                        <span>${m.manager}</span>
                        <span class="pts-badge">${m.puntosTotal} pts</span>
                    </div>
                    <div>
                        ${m.jugadores.map(j => {
                            let colorPuntos = j.puntos > 0 ? '#2e7d32' : (j.puntos < 0 ? '#d32f2f' : '#f57c00');
                            return `
                            <div class="player-row">
                                <span>${j.nombre} <small style="color:#666;">(${j.posicion})</small></span>
                                <strong style="color:${colorPuntos};">${j.puntos}</strong>
                            </div>`;
                        }).join('')}
                    </div>
                </div>
            `).join('');
        })
        .catch(err => console.error("Error cargando vista lista:", err));
}

function dibujarEquipoEnCampo(jugadores, contenedor, posicionCampo) {
    contenedor.innerHTML = '';

    // FORZAR EL TAMAÑO: Cada área ocupa el 50% de la altura para no pisar al otro equipo
    contenedor.style.display = 'flex';
    contenedor.style.flexDirection = 'column';
    contenedor.style.justifyContent = 'space-evenly';
    contenedor.style.height = '50%';
    contenedor.style.width = '100%';
    contenedor.style.position = 'absolute';

    if (posicionCampo === 'top') {
        contenedor.style.top = '0'; // Pegado a la portería de arriba
    } else {
        contenedor.style.bottom = '0'; // Pegado a la portería de abajo
    }

    // Agrupar jugadores por posiciones
    const lineas = {
        'POR': [],
        'DEF': [],
        'MED': [],
        'DEL': []
    };

    jugadores.forEach(j => {
        // SOLUCIÓN TEXTO: Buscamos la subcadena sin importar cómo esté escrito en la base de datos
        const posStr = (j.posicion || 'MED').trim().toUpperCase();
        let posKey = 'MED'; // Por defecto

        if (posStr.includes('POR') || posStr === 'PT') posKey = 'POR';
        else if (posStr.includes('DEF') || posStr === 'DF') posKey = 'DEF';
        else if (posStr.includes('MED') || posStr.includes('CEN') || posStr === 'MC') posKey = 'MED';
        else if (posStr.includes('DEL') || posStr === 'DL') posKey = 'DEL';

        lineas[posKey].push(j);
    });

    // El equipo de arriba dibuja Portero arriba (primero), el de abajo dibuja Delantero primero (para que quede en el centro)
    const ordenPosiciones = posicionCampo === 'top'
        ? ['POR', 'DEF', 'MED', 'DEL']
        : ['DEL', 'MED', 'DEF', 'POR'];

    // Dibujar las líneas
    ordenPosiciones.forEach(pos => {
        if (lineas[pos].length > 0) {
            const divLinea = document.createElement('div');
            divLinea.className = `linea-${pos.toLowerCase()}`;
            divLinea.style.display = 'flex';
            divLinea.style.flexDirection = 'row';
            divLinea.style.justifyContent = 'center';
            divLinea.style.gap = '20px'; // Un poco más de espacio para que respiren
            divLinea.style.width = '100%';

            lineas[pos].forEach(j => {
                const img = j.imagen ? j.imagen : '/images/avatars/user.png';

                // Colores correctos
                let colorPuntos;
                if (j.puntos > 0) colorPuntos = '#2e7d32'; // Verde
                else if (j.puntos === 0) colorPuntos = '#f57c00'; // Naranja
                else colorPuntos = '#d32f2f'; // Rojo

                const mvpEstilo = j.mvp ? 'border: 3px solid gold; box-shadow: 0 0 15px gold;' : 'border: 2px solid white;';

                const divJugador = document.createElement('div');
                divJugador.className = 'jugador-campo';
                divJugador.style.display = 'flex';
                divJugador.style.flexDirection = 'column';
                divJugador.style.alignItems = 'center';
                divJugador.style.zIndex = '2';

                divJugador.innerHTML = `
                    <div style="position:relative; display:inline-block; margin-bottom: 5px;">
                        <img src="${img}" style="width:45px; height:45px; border-radius:50%; object-fit:cover; background:white; ${mvpEstilo}" onerror="this.src='/images/avatars/user.png'">
                        <div style="position:absolute; bottom:-5px; right:-8px; background:${colorPuntos}; color:white; border-radius:50%; width:22px; height:22px; display:flex; justify-content:center; align-items:center; font-size:12px; font-weight:bold; border: 2px solid white;">
                            ${j.puntos}
                        </div>
                    </div>
                    <div style="color:white; font-size:11px; background:rgba(0,0,0,0.65); padding:2px 6px; border-radius:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:70px; font-weight:600;">
                        ${j.nombre}
                    </div>
                `;
                divLinea.appendChild(divJugador);
            });
            contenedor.appendChild(divLinea);
        }
    });
}

function alternarVista() {
    const vistaCampo = document.getElementById('vista-campo');
    const vistaLista = document.getElementById('vista-lista');

    if (vistaCampo.classList.contains('oculto')) {
        vistaCampo.classList.remove('oculto');
        vistaLista.classList.add('oculto');
    } else {
        vistaCampo.classList.add('oculto');
        vistaLista.classList.remove('oculto');
    }
}
}