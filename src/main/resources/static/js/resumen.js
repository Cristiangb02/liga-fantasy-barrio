document.addEventListener('DOMContentLoaded', () => {
    // Intentar leer la jornada de la URL si vienes desde otra página (ej: historial)
    const urlParams = new URLSearchParams(window.location.search);
    const jornadaUrl = urlParams.get('jornada');

    if (jornadaUrl) {
        document.getElementById('numJornada').value = jornadaUrl;
        cargarTodo();
    } else {
        // SOLUCIÓN JORNADA: Preguntamos al backend cuál es la jornada actual
        fetch('/jornada/actual')
            .then(res => res.text())
            .then(num => {
                if (num && !isNaN(num)) {
                    document.getElementById('numJornada').value = num;
                }
                cargarTodo();
            })
            .catch(err => {
                console.error("Error obteniendo jornada actual:", err);
                cargarTodo(); // Fallback a la jornada 1 si falla
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
}

function dibujarEquipoEnCampo(jugadores, contenedor, posicionCampo) {
    contenedor.innerHTML = '';

    // SOLUCIÓN AMONTONAMIENTO: Obligamos al área a apilar las líneas en formato columna (1-4-4-2, etc)
    contenedor.style.display = 'flex';
    contenedor.style.flexDirection = 'column';
    contenedor.style.justifyContent = 'space-around'; // Reparte el espacio verticalmente
    contenedor.style.alignItems = 'center';
    contenedor.style.height = '100%';
    contenedor.style.width = '100%';
    contenedor.style.padding = '10px 0';

    // Agrupar jugadores por posiciones
    const lineas = {
        'POR': [],
        'DEF': [],
        'MED': [],
        'DEL': []
    };

    jugadores.forEach(j => {
        // Limpiamos la cadena por si viene con espacios
        const pos = (j.posicion || 'MED').trim().toUpperCase();
        if(lineas[pos]) {
            lineas[pos].push(j);
        } else {
            lineas['MED'].push(j); // Fallback de seguridad
        }
    });

    // El equipo de arriba (top) dibuja Portero arriba, el de abajo (bottom) dibuja Portero abajo
    const ordenPosiciones = posicionCampo === 'top'
        ? ['POR', 'DEF', 'MED', 'DEL']
        : ['DEL', 'MED', 'DEF', 'POR'];

    // Dibujar las líneas
    ordenPosiciones.forEach(pos => {
        if (lineas[pos].length > 0) {
            const divLinea = document.createElement('div');
            divLinea.className = `linea-${pos.toLowerCase()}`;
            // Distribución horizontal de los jugadores en su línea correspondiente
            divLinea.style.display = 'flex';
            divLinea.style.flexDirection = 'row';
            divLinea.style.justifyContent = 'center';
            divLinea.style.gap = '15px'; // Espacio entre jugadores de la misma línea
            divLinea.style.width = '100%';

            lineas[pos].forEach(j => {
                const img = j.imagen ? j.imagen : '/images/avatars/user.png';

                // SOLUCIÓN COLORES: Verde (>0), Naranja (0), Rojo (<0)
                let colorPuntos;
                if (j.puntos > 0) colorPuntos = '#2e7d32'; // Verde oscuro
                else if (j.puntos === 0) colorPuntos = '#f57c00'; // Naranja
                else colorPuntos = '#d32f2f'; // Rojo

                const mvpEstilo = j.mvp ? 'border: 3px solid gold; box-shadow: 0 0 15px gold;' : 'border: 2px solid white;';

                const divJugador = document.createElement('div');
                divJugador.className = 'jugador-campo';
                divJugador.style.display = 'flex';
                divJugador.style.flexDirection = 'column';
                divJugador.style.alignItems = 'center';
                divJugador.style.zIndex = '2'; // Asegura que estén por encima del césped

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
        document.getElementById('lista-contenido').innerHTML = "<p style='text-align:center; padding: 20px;'>Vista de lista en desarrollo...</p>";
    }
}