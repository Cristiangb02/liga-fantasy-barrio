document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const jornadaUrl = urlParams.get('jornada');
    if (jornadaUrl) {
        document.getElementById('numJornada').value = jornadaUrl;
    }
    cargarTodo();
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
            // Manejo de errores desde el backend
            if (data.error) {
                marcador.innerText = data.error;
                return;
            }

            // Sumar puntos para el marcador
            const ptsA = data.equipoA.reduce((sum, j) => sum + j.puntos, 0);
            const ptsB = data.equipoB.reduce((sum, j) => sum + j.puntos, 0);

            marcador.innerText = `${data.colorA} ${ptsA} - ${ptsB} ${data.colorB}`;

            // Dibujar a los jugadores en las áreas correspondientes
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

    // Agrupar jugadores por posiciones para dibujarlos en líneas
    const lineas = {
        'POR': [],
        'DEF': [],
        'MED': [],
        'DEL': []
    };

    jugadores.forEach(j => {
        const pos = j.posicion ? j.posicion.toUpperCase() : 'MED';
        if(lineas[pos]) {
            lineas[pos].push(j);
        } else {
            lineas['MED'].push(j); // Fallback por si la posición no existe
        }
    });

    // El orden de dibujo cambia dependiendo de si el equipo está arriba o abajo en la pantalla
    const ordenPosiciones = posicionCampo === 'top'
        ? ['POR', 'DEF', 'MED', 'DEL']
        : ['DEL', 'MED', 'DEF', 'POR'];

    // Dibujar las líneas del equipo
    ordenPosiciones.forEach(pos => {
        if (lineas[pos].length > 0) {
            const divLinea = document.createElement('div');
            divLinea.className = 'linea-jugadores';
            // Estilos en línea para alinear la fila horizontalmente
            divLinea.style.display = 'flex';
            divLinea.style.justifyContent = 'space-evenly';
            divLinea.style.alignItems = 'center';
            divLinea.style.width = '100%';
            divLinea.style.flex = '1';

            lineas[pos].forEach(j => {
                const img = j.imagen ? j.imagen : '/images/avatars/user.png';
                const colorPuntos = j.puntos >= 0 ? '#2e7d32' : '#d32f2f'; // Verde si positivo, rojo si negativo
                const mvpEstilo = j.mvp ? 'border: 3px solid gold; box-shadow: 0 0 15px gold;' : 'border: 2px solid white;';

                const divJugador = document.createElement('div');
                divJugador.className = 'jugador-campo';
                divJugador.style.display = 'flex';
                divJugador.style.flexDirection = 'column';
                divJugador.style.alignItems = 'center';

                // HTML interno del circulito del jugador
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
        // Aquí podrías añadir una llamada para pintar la vista de lista si lo necesitas
        document.getElementById('lista-contenido').innerHTML = "<p style='text-align:center; padding: 20px;'>Vista de lista en desarrollo...</p>";
    }
}