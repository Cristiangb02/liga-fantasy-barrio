
let presupuesto = parseInt(localStorage.getItem('presupuesto') || 0);
let listaUsuariosOnline = [];
let modalCallback = null;
let miPuestoActual = "--º";

function mostrarModal(titulo, mensaje, tipo, callback) {
    document.getElementById('modal-content-wrapper').classList.remove('oculto');
    document.getElementById('modal-jugador-detalle').classList.add('oculto');
    document.getElementById('modal-titulo').innerText = titulo;
    document.getElementById('modal-mensaje').innerText = mensaje;
    document.getElementById('modal-overlay').classList.remove('oculto');

    const btnOk = document.getElementById('btn-modal-ok');
    const inputContainer = document.getElementById('modal-input-container');
    const input = document.getElementById('modal-input');
    const infoExtra = document.getElementById('modal-info-extra');

    input.value = '';
    inputContainer.classList.add('oculto');
    infoExtra.classList.add('oculto');
    btnOk.className = 'btn-modal btn-modal-ok';
    btnOk.classList.remove('oculto');
    btnOk.innerText = "Confirmar";

    if (tipo === 'input' || tipo === 'blindar' || tipo === 'oferta') {
        inputContainer.classList.remove('oculto');

        if(tipo === 'oferta') {
            input.placeholder = "Cantidad a ofrecer";
            input.type = "text";
            input.onkeyup = function() {
                let val = this.value.replace(/\./g, '').replace(/\D/g, '');
                if(val === "") { this.value = ""; return; }
                let num = parseInt(val);
                this.value = num.toLocaleString('es-ES');
            };
        }
        else if(tipo === 'blindar') {
            input.placeholder = "Cantidad a invertir (sin puntos)";
            input.type = "number";
            input.onkeyup = function() {
                let val = parseInt(this.value);
                if(!isNaN(val) && val > 0) {
                    infoExtra.classList.remove('oculto');
                    infoExtra.innerHTML = `<strong>Coste:</strong> ${formatoDinero.format(val)}<br><strong>Subida Cláusula:</strong> ${formatoDinero.format(val*2)}`;
                } else { infoExtra.classList.add('oculto'); }
            }
        } else {
            input.placeholder = "Escribe...";
            input.type = "text";
            input.onkeyup = null;
        }

    } else if (tipo === 'danger') {
        inputContainer.classList.remove('oculto');
        input.placeholder = "Escribe RESET para confirmar";
        btnOk.className = 'btn-modal btn-modal-danger';
    } else if (tipo === 'info') {
        btnOk.classList.add('oculto');

    } else if (tipo === 'vender') {
        btnOk.className = 'btn-modal btn-modal-danger';
        btnOk.innerText = "Vender";
    }

    modalCallback = () => {
        if (tipo === 'confirm' || tipo === 'vender') callback();
        else if (tipo === 'input' || tipo === 'blindar' || tipo === 'danger') callback(input.value);
        else if (tipo === 'oferta') {
            let valLimpio = input.value.replace(/\./g, '');
            callback(valLimpio);
        }
        cerrarModal();
    };
}

function verDetalleJugador(id, nombre, img, posicion) {
    document.getElementById('modal-overlay').classList.remove('oculto');
    document.getElementById('modal-content-wrapper').classList.add('oculto');
    const detalleDiv = document.getElementById('modal-jugador-detalle');
    detalleDiv.classList.remove('oculto');
    document.getElementById('btn-modal-ok').classList.add('oculto');
    document.getElementById('btn-modal-cancel').innerText = "Cerrar";
    let posClass = 'pos-' + posicion.toLowerCase();

    detalleDiv.innerHTML = `
        <img src="${img}" class="modal-jugador-img">
        <h3 style="margin:0; color:#1a237e;">${nombre}</h3>
        <div class="card-pos-badge ${posClass}" style="margin-top:5px;">${posicion}</div>
        <div id="media-puntos" style="font-weight:bold; margin-top:10px; color:#546e7a; font-size:1.1em;">MEDIA: Calculando...</div>

        <div id="stats-loading" style="margin-top: 15px;">Cargando historial...</div>
        <div id="stats-content" class="detalle-stats"></div>
    `;

    fetch(`/jugador/${id}/historial-puntos`).then(r=>r.json()).then(stats => {
        document.getElementById('stats-loading').style.display = 'none';
        const content = document.getElementById('stats-content');
        const divMedia = document.getElementById('media-puntos');

        if(stats.length === 0) {
            content.innerHTML = '<p>Sin puntos registrados.</p>';
            divMedia.innerText = 'MEDIA: 0.00';
        } else {
            let suma = 0;
            stats.forEach(s => suma += s.puntos);
            let media = (suma / stats.length).toFixed(2);

            divMedia.innerText = `MEDIA: ${media}`;
            content.innerHTML = stats.map(s => {
                let colorClass = s.puntos > 0 ? 'bg-green' : (s.puntos < 0 ? 'bg-red' : 'bg-orange');
                return `
                    <div class="stat-row">
                        <span>Jornada ${s.jornada}</span>
                        <span class="badge-puntos ${colorClass}">${s.puntos}</span>
                    </div>
                `;
            }).join('');
        }
    }).catch(error => {
        console.error("Error cargando el detalle del jugador: ", error);
        document.getElementById('media-puntos').innerText = 'Error al calcular';
    });
}

function cerrarModal() {
    document.getElementById('modal-overlay').classList.add('oculto');
    modalCallback = null;
    document.getElementById('btn-modal-cancel').innerText = "Cancelar";
    document.getElementById('btn-modal-ok').classList.remove('oculto');
}
document.getElementById('btn-modal-ok').onclick = () => { if(modalCallback) modalCallback(); };
document.getElementById('btn-modal-cancel').onclick = cerrarModal;

if (!usuarioId) window.location.href = 'login.html';

window.onload = function() {
    document.getElementById('titulo-web').innerText = localStorage.getItem('usuarioNombre');
    actualizarPresupuestoUI();
    if (esAdmin) {
        document.getElementById('tab-admin').classList.remove('oculto');
        cargarUsuariosAdmin();
        pintarSelectAdmin();
        actualizarBotonBloqueo();
        pintarSelectsAdminJugadores();
    }
    cargarTodo();
    cargarOfertas();
    cargarNoticias();
    iniciarContadorMercado();
    iniciarRelojesBlindaje();
    pingOnline();
};

function iniciarContadorMercado() {
    function actualizar() {
        const now = new Date();
        const minutosDia = now.getHours() * 60 + now.getMinutes();
        const cerradoNoche = 1290;
        const cerradoMadrugada = 600;
        const msgCerrado = document.getElementById('msg-mercado-cerrado');

        if (minutosDia >= cerradoNoche || minutosDia < cerradoMadrugada) {
            msgCerrado.classList.remove('oculto');
        } else {
            msgCerrado.classList.add('oculto');
        }

        const fechaMadridStr = now.toLocaleString("en-US", {timeZone: "Europe/Madrid"});
        const nowMadrid = new Date(fechaMadridStr);
        const midnightMadrid = new Date(nowMadrid);
        midnightMadrid.setHours(24, 0, 0, 0);
        const diferencia = midnightMadrid - nowMadrid;

        if (diferencia > 0) {
            const h = Math.floor((diferencia % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const m = Math.floor((diferencia % (1000 * 60 * 60)) / (1000 * 60));
            const s = Math.floor((diferencia % (1000 * 60)) / 1000);
            document.getElementById('market-countdown').innerText =
                `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}`;
        }
    }
    actualizar(); setInterval(actualizar, 1000);
}

function iniciarRelojesBlindaje() {
    setInterval(() => {
        document.querySelectorAll('.timer-blindaje').forEach(el => {
            let segs = parseInt(el.getAttribute('data-segundos'));
            if (!isNaN(segs) && segs > 0) {
                segs--;
                el.setAttribute('data-segundos', segs);
                let d = Math.floor(segs / (3600 * 24));
                let h = Math.floor((segs % (3600 * 24)) / 3600);
                let m = Math.floor((segs % 3600) / 60);
                let s = segs % 60;
                el.innerText = `${d}d ${h}h ${m}m ${s}s`;
            } else {
                el.innerText = "Expirado";
                el.style.color = "#d32f2f";
            }
        });
    }, 1000);
}

function cargarTodo() {
    fetch('/jornada/actual').then(r=>r.json()).then(n => {
        document.getElementById('badge-jornada').innerText = "J-" + n;
        document.getElementById('num-jornada-alineacion').innerText = n;
    });

    fetch('/jugadores').then(r=>r.json()).then(jugadores => {
        window.todosLosJugadores = jugadores;
        pintarPlantilla(jugadores);
        pintarRanking(jugadores);

        fetch(`/alineacion/${usuarioId}`).then(r => r.json()).then(alineados => {
            const idsAlineados = new Set(alineados.map(a => a.id));
            pintarAlineacion(jugadores, idsAlineados);
        });
    });

    fetch('/mercado-diario?t=' + new Date().getTime())
        .then(r=>r.json())
        .then(mercado => {
            pintarMercado(mercado);
        });

    fetch('/usuarios').then(r=>r.json()).then(usuarios => {
        pintarRivales(usuarios);
        const yo = usuarios.find(u => u.id == usuarioId);

        if (!yo) {
            alert("⛔ Tu usuario ha sido eliminado o no existe.");
            logout();
            return;
        }

        if (yo) {
            presupuesto = yo.presupuesto;
            localStorage.setItem('presupuesto', presupuesto);
            actualizarPresupuestoUI();

            let miAvatar = yo.urlImagen && yo.urlImagen !== "null" ? yo.urlImagen : '/images/avatars/user.png';
            document.getElementById('dashboard-avatar').src = miAvatar;
            sessionStorage.setItem("urlImagen", miAvatar);
        }
    });

    fetch('/clasificacion').then(r=>r.json()).then(data => {
            const miNombre = localStorage.getItem('usuarioNombre');
            const miIndex = data.findIndex(userMap => userMap.nombre === miNombre);
            if (miIndex !== -1) {
                miPuestoActual = (miIndex + 1) + "º";
            } else {
                miPuestoActual = "--º";
            }
                    let html = data.map((fila, i) => {
                        let img = fila.urlImagen && fila.urlImagen !== "null" ? fila.urlImagen : '/images/avatars/user.png';
                        let puesto = i + 1; // Calculamos el puesto

                        return `
                        <div class="fila-clasif" style="cursor:pointer; transition: 0.2s;" onclick="verFichaManager('${fila.nombre}', '${img}', ${puesto})" onmouseover="this.style.background='#f5f5f5'" onmouseout="this.style.background='transparent'">
                            <div class="clasif-user" style="display:flex; align-items:center;">
                                <span class="pos-num" style="width:30px;">${puesto}º</span>
                                <img src="${img}" style="width:35px; height:35px; object-fit:cover; border-radius:50%; border:2px solid #ccc; margin:0 10px;">
                                <strong>${fila.nombre}</strong>
                            </div>
                            <div style="text-align:right;">
                                <div style="font-weight:bold; color:#1a237e;">${fila.puntos} pts</div>
                                <div style="font-size:0.85em; color:#666;">💰 ${formatoDinero.format(fila.valorPlantilla)}</div>
                            </div>
                        </div>`;
                    }).join('');
                    document.getElementById('lista-clasificacion').innerHTML = html;
        });

    cargarHistorial();
    cargarNoticias();
}

function pintarRanking(jugadores) {
    let lista = [...jugadores];
    lista.sort((a, b) => {
        if (b.puntosAcumulados !== a.puntosAcumulados) {
            return b.puntosAcumulados - a.puntosAcumulados;
        }
        return a.nombre.localeCompare(b.nombre);
    });

    let html = lista.map((j, i) => {
        let extraClass = '';
        let icon = `#${i+1}`;
        if (i === 0) { extraClass = 'top-1'; icon = '🥇'; }
        else if (i === 1) { extraClass = 'top-2'; icon = '🥈'; }
        else if (i === 2) { extraClass = 'top-3'; icon = '🥉'; }

        let img = j.urlImagen && j.urlImagen.startsWith('/') ? j.urlImagen : (j.urlImagen || 'https://via.placeholder.com/150');

        return `
        <div class="fila-ranking ${extraClass}">
            <div class="ranking-details">
                <span class="ranking-medal">${icon}</span>
                <div class="avatar-small">
                    <img src="${img}" style="width:100%; height:100%; object-fit:cover;">
                </div>
                <div style="display:flex; flex-direction:column;">
                    <strong style="color:#1a237e;">${j.nombre}</strong>
                    <small style="opacity:0.7">${j.posicion}</small>
                </div>
            </div>
            <div class="ranking-points">${j.puntosAcumulados} pts</div>
        </div>`;
    }).join('');

    document.getElementById('lista-ranking').innerHTML = html;
}

function cargarHistorial() {
    fetch('/historial/' + usuarioId).then(r=>r.json()).then(historial => {
        const div = document.getElementById('lista-historial');
        if (historial.length === 0) {
            div.innerHTML = '<p>No hay jornadas registradas.</p>';
            return;
        }
        div.innerHTML = historial.map(h => {
            let jugadoresHtml = h.jugadores.map(j => {
                let colorClass = j.puntos > 0 ? 'text-green' : (j.puntos < 0 ? 'text-red' : 'text-orange');
                return `
                <div class="player-mini">
                    <div style="display:flex; justify-content:space-between;">
                        <strong>${j.nombre}</strong>
                        <span class="mini-puntos ${colorClass}">${j.puntos}</span>
                    </div>
                    <span class="mini-pos">${j.posicion}</span>
                </div>`;
            }).join('');

            return `
            <div class="historial-card">
                <div class="historial-header">
                    <span>Jornada ${h.jornadaNumero}</span>
                    <span class="historial-badge">${h.puntosTotal} pts</span>
                </div>
                <div class="historial-body">
                    ${jugadoresHtml || '<small>Sin jugadores alineados</small>'}
                </div>
            </div>`;
        }).join('');
    });
}

function cargarNoticias() {
    fetch('/noticias').then(r=>r.json()).then(noticias => {
        let html = noticias.map(n => `<div class="noticia-item ${n.mensaje.includes("CLAUSULAZO") || n.mensaje.includes("ADMIN") ? 'noticia-robo':''}"><span class="noticia-fecha">${n.fechaBonita}</span><span class="noticia-texto">${n.mensaje}</span></div>`).join('');
        const container = document.getElementById('lista-noticias');
        if (container.innerHTML !== html) container.innerHTML = html || '<p>No hay noticias.</p>';
    });

    fetch('/premios-pendientes/' + usuarioId).then(r=>r.json()).then(premios => {
        const zonaPremios = document.getElementById('zona-premios');
        if (premios.length > 0) {
            let htmlPremios = premios.map(p => {
                let htmlMvp = '';
                let claseExtra = '';

                if (p.tieneMvp) {
                    claseExtra = 'border: 2px solid #ffd700; background: #fffde7;';
                    htmlMvp = `<div style="background:#ffd700; color:#333; padding:5px; border-radius:4px; margin-top:5px; font-size:0.9em; text-align:center;">
                                  <strong>🏆 ¡BONUS MVP!</strong><br>
                                  Tenías a ${p.nombreMvp}<br>
                                  <span style="font-weight:900;">+${p.bonusFmt}</span>
                               </div>`;
                }

                return `
                <div class="bloque-premios" style="${claseExtra}">
                    <div>
                        <strong>🏁 JORNADA ${p.jornada}</strong><br>
                        Has ganado: ${p.puntos} pts
                        ${htmlMvp}
                    </div>
                    <button class="btn-reclamar" onclick="reclamar(${p.idEquipo})">Reclamar ${p.dineroFmt}</button>
                </div>
            `;
            }).join('');
            zonaPremios.innerHTML = htmlPremios;
        } else { zonaPremios.innerHTML = ''; }
    });
}

function reclamar(idEquipo) { post(`/reclamar-premio/${idEquipo}`, {}); }

function cargarUsuariosAdmin() {
    fetch('/admin/usuarios-gestion').then(r => r.json()).then(usuarios => {
        document.getElementById('admin-lista-usuarios').innerHTML = usuarios.map(u => `
            <div class="user-row">
                <div style="overflow:hidden; text-overflow:ellipsis;">
                    <strong>${u.nombre}</strong> ${u.esAdmin ? '(ADMIN)' : ''}<br>
                    <small style="color:#666;">Pass: ${u.password}</small>
                </div>
                <div style="flex-shrink:0;">
                    <button class="btn-edit-user" onclick="editarUsuario(${u.id}, '${u.nombre}')">✏️</button>
                    ${(!u.esAdmin || u.nombre === 'Cristian') ? `<button class="btn-delete-user" onclick="expulsarUsuario(${u.id}, '${u.nombre}')">Expulsar</button>` : ''}                    </div>
            </div>
        `).join('');

        const selectPuntos = document.getElementById('admin-usuario-puntos');
        if (selectPuntos) {
            selectPuntos.innerHTML = usuarios.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }

        const selectSaldo = document.getElementById('admin-usuario-saldo');
        if (selectSaldo) {
            selectSaldo.innerHTML = `<option value="0">🌍 TODOS LOS MÁNAGERS</option>` +
                                    usuarios.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }

        const selectAvatar = document.getElementById('admin-usuario-avatar');
        if (selectAvatar) {
            selectAvatar.innerHTML = usuarios.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }
    });

    fetch('/admin/pendientes').then(r => r.json()).then(pendientes => {
        const div = document.getElementById('admin-lista-pendientes');
        if(pendientes.length === 0) {
            div.innerHTML = '<small>No hay solicitudes.</small>';
        } else {
            div.innerHTML = pendientes.map(u => `
                <div class="solicitud-row">
                    <strong>${u.nombre}</strong>
                    <div>
                        <button class="btn-aprobar" onclick="gestionarUsuario(${u.id}, 'aprobar')">✅</button>
                        <button class="btn-rechazar" onclick="gestionarUsuario(${u.id}, 'rechazar')">❌</button>
                    </div>
                </div>
            `).join('');
        }
    });
}

function gestionarUsuario(id, accion) {
    let method = accion === 'aprobar' ? 'POST' : 'DELETE';
    fetch(`/admin/${accion}/${id}`, { method: method })
    .then(r => r.text())
    .then(msg => {
        mostrarModal("Gestión", msg, 'confirm', () => cargarUsuariosAdmin());
    });
}

function crearCarta(j, tipo, alineado = false) {
    let img = j.urlImagen && j.urlImagen.startsWith('/') ? j.urlImagen : (j.urlImagen || 'https://via.placeholder.com/150');
    let precio = formatoDinero.format(j.valor);
    let clausula = formatoDinero.format(j.clausula);
    let contenido = '';
    let extraClass = alineado ? 'alineado' : '';
    let checked = alineado ? 'checked' : '';
    let overlay = '';

    if (j.blindado) {
        if(tipo === 'robar') {
            overlay = `<div class="blindado-overlay">
                            <div class="candado-icon">🔒</div>
                            <div class="timer-blindaje" data-segundos="${j.segundosBlindaje}">Calculando...</div>
                       </div>`;
        }
    }

    let colorTriangulo = 'triangle-pos';
    if (j.puntosAcumulados < 0) colorTriangulo = 'triangle-neg';
    else if (j.puntosAcumulados === 0) colorTriangulo = 'triangle-neu';

    if (tipo === 'alinear') {
        contenido = `<div class="alineacion-overlay"><input type="checkbox" class="check-alinear" value="${j.id}" ${checked} onclick="actualizarContador(this)"></div>`;
    }
    else if (tipo === 'fichar') {
        contenido = `<button class="btn-card btn-fichar" onclick="fichar(${j.id}, ${j.valor})">Fichar (${precio})</button>`;
    }
    else if (tipo === 'robar') {
        let btnRobar = `<button class="btn-card btn-robar" onclick="robar(${j.id}, ${j.clausula})">Fichar: ${clausula}</button>`;
        let btnOferta = `<button class="btn-card btn-ofertar" onclick="hacerOferta(${j.id}, '${j.nombre}')">🤝 Negociar</button>`;

        if (j.blindado) btnRobar = '';

        contenido = `
            <div class="card-coste" style="margin-bottom:5px;">Valor: ${precio}</div>
            ${btnRobar}
            ${btnOferta}
        `;
    }
    else if (tipo === 'gestion') {
        let htmlBlindaje = '';
        if (j.blindado) {
            htmlBlindaje = `<div class="timer-blindaje timer-mini" data-segundos="${j.segundosBlindaje}">⏳ Calculando...</div>`;
        }

        contenido = `
            <div class="card-coste">Valor de mercado: ${precio}</div>
            <div class="card-clausula">Cláusula 🔒: ${clausula}</div>
            ${htmlBlindaje}
            <div style="display:flex; gap:5px; margin-top:auto; width:100%;">
                <button class="btn-card btn-vender" style="flex:1;" onclick="vender(${j.id}, ${j.valor}, ${j.clausula})">Vender jugador</button>
                <button class="btn-card btn-blindar" style="flex:1;" onclick="blindar(${j.id})">Subir cláusula 🔒</button>
            </div>`;
    }

    let accionDetalle = `onclick="verDetalleJugador(${j.id}, '${j.nombre}', '${img}', '${j.posicion}')"`;
    let posClass = 'pos-' + j.posicion.toLowerCase();

    let iconoEstado = '✅';
    let colorEstado = '#2e7d32';
    let textoEstado = j.estado || 'DISPONIBLE';

    if (textoEstado === 'DUDOSO') {
        iconoEstado = '⚠️';
        colorEstado = '#f57c00';
    } else if (textoEstado === 'LESIONADO') {
        iconoEstado = '🚑';
        colorEstado = '#d32f2f';
    } else if (textoEstado === 'NO DISPONIBLE' || textoEstado === 'NO-DISPONIBLE') {
        iconoEstado = '❌';
        colorEstado = '#c62828';
    }

    let badgeEstado = `<div style="font-size: 0.75em; font-weight: 900; color: ${colorEstado}; margin-top: 4px;">${iconoEstado} ${textoEstado}</div>`;

    return `<div class="card ${extraClass}" id="card-${j.id}">
        <div class="puntos-triangle ${colorTriangulo}"></div>
        <div class="puntos-val">${j.puntosAcumulados}</div>
        ${tipo === 'alinear' ? contenido : ''}
        <div class="card-img-container" ${accionDetalle}><img src="${img}" class="card-img">${overlay}</div>
        <div class="card-body">
            <div ${accionDetalle}>
                <div class="card-name">${j.nombre}</div>
                <div class="card-pos-badge ${posClass}">${j.posicion}</div>
                ${badgeEstado}
            </div>
            ${tipo !== 'alinear' ? contenido : ''}
        </div>
    </div>`;
}

function pintarPlantilla(jugadores) { document.getElementById('grid-mi-plantilla').innerHTML = jugadores.filter(j => j.propietario && j.propietario.id == usuarioId).map(j => crearCarta(j, 'gestion')).join('') || '<p>Sin jugadores.</p>'; }
function pintarAlineacion(jugadores, idsAlineados) {
    const misJugadores = jugadores.filter(j => j.propietario && j.propietario.id == usuarioId);
    misJugadores.sort((a, b) => {
        const aAlineado = idsAlineados.has(a.id) ? 1 : 0;
        const bAlineado = idsAlineados.has(b.id) ? 1 : 0;
        return bAlineado - aAlineado;
    });
    document.getElementById('grid-alineacion').innerHTML = misJugadores.map(j => crearCarta(j, 'alinear', idsAlineados.has(j.id))).join('') || '<p>Sin jugadores.</p>';
    actualizarContador();
}
function pintarMercado(jugadores) { document.getElementById('grid-mercado').innerHTML = jugadores.map(j => crearCarta(j, 'fichar')).join('') || '<p>Mercado cerrado o vacío.</p>'; }
function pintarRivales(usuarios) { document.getElementById('lista-rivales').innerHTML = usuarios.filter(u => u.id != usuarioId).map(u => `<div style="background:white; padding:15px; border-radius:8px; cursor:pointer; font-weight:bold; display:flex; justify-content:space-between;" onclick="espiar(${u.id}, '${u.nombre}')"><span>⚽ ${u.nombre}</span> <span>🔍</span></div>`).join(''); }

function espiar(id, nombre) {
    document.querySelectorAll('.seccion').forEach(s => s.classList.add('oculto'));
    const seccionEspia = document.getElementById('sec-detalle-rival');
    seccionEspia.classList.remove('oculto');

    document.getElementById('titulo-espia').innerText = "Equipo de " + nombre;
    document.getElementById('grid-rival').innerHTML = window.todosLosJugadores.filter(j => j.propietario && j.propietario.id == id).map(j => crearCarta(j, 'robar')).join('') || '<p>Sin jugadores.</p>';
}

function actualizarContador(checkbox) {
    if(checkbox) {
        const card = document.getElementById('card-' + checkbox.value);
        if(checkbox.checked) card.classList.add('alineado'); else card.classList.remove('alineado');
    }
    document.getElementById('contador-alineados').innerText = document.querySelectorAll('.check-alinear:checked').length;
}

function fichar(id, p) { mostrarModal("Fichar Jugador", `¿Fichar por ${formatoDinero.format(p)}?`, 'confirm', () => post(`/mercado/comprar/${id}/${usuarioId}`, {}, p)); }
function robar(id, p) { mostrarModal("Fichar por Cláusula", `¿Pagar cláusula de ${formatoDinero.format(p)}?`, 'confirm', () => post(`/mercado/robar/${id}/${usuarioId}`, {}, p)); }

function vender(id, valor, clausula) {
    let ingreso = valor;

    mostrarModal(
        "Vender al Mercado",
        `⚠️ ATENCIÓN: Al vender al mercado SOLO recuperas el valor de mercado actual del jugador. Se perderá todo el dinero que hayas invertido en subir su cláusula.\n\n💰 Recibirás: ${formatoDinero.format(ingreso)}\n\n¿Confirmar venta?`,
        'vender',
        () => post(`/mercado/vender/${id}/${usuarioId}`, {}, -ingreso)
    );
}
function blindar(id) {
    mostrarModal("Subir Cláusula 🔒", "Introduce cantidad a invertir.", 'blindar', (cant) => {
        let valor = parseInt(cant);
        if (!isNaN(valor) && valor > 0) {
            post(`/jugador/subir-clausula/${id}/${valor}`, {}, valor);
        } else {
            mostrarModal("⛔ Error", "Debes introducir un número válido.", 'confirm', ()=>{});
        }
    });
}

function hacerOferta(idJugador, nombreJugador) {
    mostrarModal("Oferta por " + nombreJugador, "Introduce cantidad:", 'oferta', (cant) => {
        if(cant > 0) {
            const data = { idJugador: idJugador, idComprador: usuarioId, cantidad: cant };
            post('/ofertas/crear', data);
        }
    });
}

function cargarOfertas() {
    fetch(`/ofertas/mis-ofertas/${usuarioId}`).then(r=>r.json()).then(data => {
        const divRecibidas = document.getElementById('lista-ofertas-recibidas');
        const divEnviadas = document.getElementById('lista-ofertas-enviadas');
        const tabButton = document.getElementById('tab-ofertas');
        const notifBadge = document.getElementById('notif-ofertas');

        if(data.recibidas.length > 0) { tabButton.classList.add('notif-active'); }
        else { tabButton.classList.remove('notif-active'); }

        if(data.recibidas.length === 0) {
            divRecibidas.innerHTML = '<p style="font-size:0.9em; color:#888;">No tienes ofertas pendientes.</p>';
        } else {
            divRecibidas.innerHTML = data.recibidas.map(o => `
                <div class="oferta-item">
                    <div class="oferta-info">
                        <strong>${o.comprador}</strong> quiere a <strong>${o.jugador}</strong><br>
                        Oferta: <span style="color:#2e7d32; font-weight:bold;">${o.cantidadFmt}</span>
                    </div>
                    <div>
                        <button class="btn-oferta-ok" onclick="responderOferta(${o.id}, 'aceptar')">✔</button>
                        <button class="btn-oferta-ko" onclick="responderOferta(${o.id}, 'rechazar')">✖</button>
                    </div>
                </div>
            `).join('');
        }

        if(data.enviadas.length === 0) {
            divEnviadas.innerHTML = '<p style="font-size:0.9em; color:#888;">No has hecho ofertas.</p>';
        } else {
            divEnviadas.innerHTML = data.enviadas.map(o => `
                <div class="oferta-item enviada">
                    <div class="oferta-info">
                        Quieres a <strong>${o.jugador}</strong> (de ${o.vendedor})<br>
                        Tu oferta: <span style="color:#0277bd; font-weight:bold;">${o.cantidadFmt}</span>
                    </div>
                    <div style="font-size:0.8em; color:#666; font-style:italic;">Pendiente...</div>
                </div>
            `).join('');
        }
    });
}

function responderOferta(idOferta, accion) { post(`/ofertas/responder/${idOferta}/${accion}`, {}); }
function expulsarUsuario(id, nombre) { mostrarModal("Expulsar", `¿Echar a ${nombre}?`, 'confirm', () => { fetch(`/admin/eliminar-usuario/${id}`, { method: 'DELETE' }).then(r=>r.text()).then(msg => { mostrarModal("Info", msg, 'confirm', ()=>{ cargarUsuariosAdmin(); cargarTodo(); }); }); }); }

function resetearLiga() {
    mostrarModal("⚠️ PELIGRO: RESET", "Escribe RESET para borrar todo:", 'danger', (t) => {
        if(t==="RESET") {
            fetch('/admin/reset-liga', { method: 'POST' })
            .then(r => r.text())
            .then(msg => {
                mostrarModal("Info", msg, 'confirm', () => { location.reload(); });
            });
        } else {
            mostrarModal("Error","Código mal.",'confirm',()=>{});
        }
    });
}

function editarUsuario(id, nombreActual) {
    mostrarModal("Cambiar Nombre", "Introduce el nuevo nombre para " + nombreActual + ":", 'input', (nuevoNombre) => {
        if (nuevoNombre && nuevoNombre.trim() !== "") {
            post(`/admin/editar-usuario/${id}`, { nombre: nuevoNombre.trim() });
            setTimeout(cargarUsuariosAdmin, 1000);
        }
    });
}

function guardarAlineacion() {
    const ids = Array.from(document.querySelectorAll('.check-alinear:checked')).map(cb => parseInt(cb.value));
    if(ids.length > 7) {
        mostrarModal("Alineación", "Máximo 7 jugadores.", 'confirm', ()=>{});
        return;
    }
    post(`/alinear/${usuarioId}`, ids);
}

function post(url, data, coste = 0) {
    fetch(url, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(data) })
    .then(r => r.text()).then(msg => {
        let titulo = msg.includes("❌") || msg.includes("⛔") ? "Info" : "✅ Éxito";
        mostrarModal(titulo, msg, 'confirm', () => {
            if(!msg.includes("❌") && !msg.includes("⛔")) {
                if(coste !== 0) {
                    presupuesto -= coste;
                    localStorage.setItem('presupuesto', presupuesto);
                    actualizarPresupuestoUI();
                }
                if (presupuesto < 0) {
                    mostrarModal("⚠️ AVISO DE SALDO", "¡Estás en números rojos!\n\nAsegúrate de tener saldo positivo antes de que empiece la siguiente jornada para poder puntuar.", "confirm", ()=>{});
                }
                cargarTodo();
                cargarOfertas();
                if(esAdmin) cargarUsuariosAdmin();
            }
        });
    });
}

function pintarSelectAdmin() {
    fetch('/admin/jugadores-pendientes').then(r=>r.json()).then(jugadores => {
        const select = document.getElementById('admin-jugador');
        if (jugadores.length === 0) { select.innerHTML = '<option>Todos puntuados</option>'; }
        else { select.innerHTML = jugadores.map(j => `<option value="${j.id}">${j.nombre} (${j.posicion})</option>`).join(''); }
    });
    cargarJugadoresPuntuados();
}

function cargarJugadoresPuntuados() {
    fetch('/admin/jugadores-puntuados').then(r=>r.json()).then(jugadores => {
        const select = document.getElementById('admin-jugador-reset');
        if (jugadores.length === 0) { select.innerHTML = '<option>Nadie ha puntuado aún</option>'; }
        else { select.innerHTML = jugadores.map(j => `<option value="${j.id}">${j.nombre} (${j.posicion})</option>`).join(''); }
    });
}

function resetearPuntos() {
    const idJugador = document.getElementById('admin-jugador-reset').value;
    if (!idJugador || isNaN(idJugador)) {
        mostrarModal("Error", "Selecciona un jugador válido.", 'confirm', ()=>{});
        return;
    }
    mostrarModal("⚠️ Confirmar Reset", "¿Seguro que quieres borrar los puntos de este jugador? Se revertirá su valor y cláusula.", 'confirm', () => {
        post(`/admin/reset-puntos/${idJugador}`, {});
        setTimeout(pintarSelectAdmin, 500);
    });
}

function registrarActa() {
    const resultado = document.getElementById('select-resultado').value;
    const datos = {
        idJugador: parseInt(document.getElementById('admin-jugador').value),
        jugado: document.getElementById('check-jugado').checked,
        victoria: resultado === 'victoria',
        derrota: resultado === 'derrota',
        goles: parseInt(document.getElementById('input-goles').value)||0,
        golesEncajados: parseInt(document.getElementById('input-encajados').value)||0,
        autogoles: parseInt(document.getElementById('input-autogoles').value)||0,
        colorEquipo: document.getElementById('select-equipo-color').value
    };
    post('/admin/registrar', datos);
    setTimeout(pintarSelectAdmin, 500);
}

function cerrarJornada() { mostrarModal("Cerrar Jornada", "¿Repartir puntos y dinero?", 'confirm', () => post('/admin/cerrar-jornada', {})); }

function cambiarPestaña(tab) {
    document.querySelectorAll('.seccion').forEach(s => s.classList.add('oculto'));
    document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
    document.getElementById('sec-' + tab).classList.remove('oculto');
    document.getElementById('tab-' + tab).classList.add('active');
    if(tab === 'ofertas') cargarOfertas();
    if(tab === 'admin') {
        cargarUsuariosAdmin();
        actualizarBotonBloqueo();
        pintarSelectAdmin();
        pintarSelectEliminar();
        pintarSelectsAdminJugadores();
    }
}

function actualizarPresupuestoUI() {
    const el = document.getElementById('presupuesto-display');
    el.innerText = formatoDinero.format(presupuesto);
    if(presupuesto < 0) {
        el.classList.add('presupuesto-negativo');
        document.getElementById('aviso-saldo').classList.remove('oculto');
    } else {
        el.classList.remove('presupuesto-negativo');
        document.getElementById('aviso-saldo').classList.add('oculto');
    }
}

function pingOnline() {
    if(!usuarioId) return;
    fetch(`/usuarios/ping/${usuarioId}`, { method: 'POST' })
        .then(r => r.json())
        .then(nombres => {
            listaUsuariosOnline = nombres;
            document.getElementById('num-online').innerText = nombres.length;
        });
}

function verQuienEstaOnline() {
    if(listaUsuariosOnline.length === 0) return;
    mostrarModal("Usuarios en línea 🟢", "", 'info', ()=>{});
    let listaHTML = listaUsuariosOnline.map(n => `<li style="margin-bottom:5px;">👤 ${n}</li>`).join('');
    document.getElementById('modal-mensaje').innerHTML = `<ul style="list-style:none; padding:0; font-size:1.1em; text-align:left;">${listaHTML}</ul>`;
}

function resetearMercado() {
    mostrarModal("Resetear Mercado", "¿Seguro que quieres cambiar los jugadores del mercado actual?", 'confirm', () => {
        post('/admin/reset-mercado', {});
    });
}

function eliminarJugador() {
    mostrarModal("Eliminar jugador", "¿Seguro que quieres borrar al jugador?", 'confirm', () => {
        post('/admin/eliminar-jugador', {});
    });
}

function toggleBloqueo() {
    post('/admin/toggle-bloqueo', {}, 0);
    setTimeout(actualizarBotonBloqueo, 500);
}

function actualizarBotonBloqueo() {
    if (!esAdmin) return;
    const btn = document.getElementById('btn-bloqueo');
    if (!btn) return;

    fetch('/admin/estado-bloqueo')
        .then(r => r.json())
        .then(bloqueado => {
            if (bloqueado) {
                btn.innerText = "🔒 EL BLOQUEO ESTÁ ACTIVO";
                btn.style.background = "#d32f2f";
                btn.style.color = "white";
                btn.style.boxShadow = "0 0 15px rgba(211, 47, 47, 0.6)";
            } else {
                btn.innerText = "🔓 EL BLOQUEO ESTÁ DESACTIVADO";
                btn.style.background = "#2e7d32";
                btn.style.color = "white";
                btn.style.boxShadow = "none";
            }
        });
}

function pintarSelectEliminar() {
    fetch('/jugadores').then(r => r.json()).then(jugadores => {
        jugadores.sort((a, b) => a.nombre.localeCompare(b.nombre));
        let opcionesHTML = '';

        if (jugadores.length === 0) {
            opcionesHTML = '<option>No hay jugadores</option>';
        } else {
            opcionesHTML = jugadores.map(j =>
                `<option value="${j.id}">${j.nombre} (${j.posicion}) - ${j.propietario && j.propietario.nombre ? j.propietario.nombre : 'Libre'}</option>`
            ).join('');
        }

        document.getElementById('select-eliminar-jugador').innerHTML = opcionesHTML;
        const selectQuitar = document.getElementById('edit-jugador-quitar');
        if (selectQuitar) selectQuitar.innerHTML = opcionesHTML;

        const selectPoner = document.getElementById('edit-jugador-poner');
        if (selectPoner) selectPoner.innerHTML = opcionesHTML;
    });
}

function eliminarJugadorSeleccionado() {
    const select = document.getElementById('select-eliminar-jugador');
    const idJugador = select.value;
    const nombreJugador = select.options[select.selectedIndex].text;

    if (!idJugador) return;

    mostrarModal("Eliminar Jugador", `¿Estás seguro de que quieres eliminar a:\n\n👉 ${nombreJugador}?\n\nEsta acción es irreversible.`, 'confirm', () => {
        post(`/admin/eliminar-jugador/${idJugador}`, {});
        setTimeout(() => {
            pintarSelectEliminar();
            pintarSelectAdmin();
        }, 1000);
    });
}

function ejecutarQuitar() {
    const jor = document.getElementById('edit-num-jornada').value;
    const idJugador = document.getElementById('edit-jugador-quitar').value;
    if(!jor || !idJugador) return alert("Rellena el número de jornada.");

    mostrarModal("Corregir Jornada", "¿Seguro que quieres borrar la actuación de este jugador en la jornada " + jor + "?", 'confirm', () => {
        post(`/admin/reset-puntos-jornada/${idJugador}/${jor}`, {});
    });
}

function ejecutarPoner() {
    const jor = document.getElementById('edit-num-jornada').value;
    const idJugador = document.getElementById('edit-jugador-poner').value;
    const pts = document.getElementById('edit-puntos-poner').value;
    const color = document.getElementById('edit-color-poner').value;

    if(!jor || !idJugador || !pts) return alert("Rellena la jornada y los puntos.");

    mostrarModal("Añadir a Jornada", `¿Seguro que quieres sumar ${pts} puntos a este jugador en la jornada ${jor} con el equipo ${color}?`, 'confirm', () => {
        post(`/admin/add-puntos-jornada/${idJugador}/${jor}/${pts}/${color}`, {});
    });
}

function modificarPuntosManager() {
    const idUsuario = document.getElementById('admin-usuario-puntos').value;
    const puntos = parseInt(document.getElementById('input-puntos-extra').value);

    if (!idUsuario || isNaN(puntos)) {
        mostrarModal("Error", "Selecciona un mánager e introduce una cantidad válida.", "confirm", ()=>{});
        return;
    }

    const accion = puntos >= 0 ? "sumar" : "restar";
    mostrarModal("Compensar Puntos", `¿Seguro que quieres ${accion} ${Math.abs(puntos)} puntos a este mánager en la clasificación general?`, "confirm", () => {
        post(`/admin/modificar-puntos-extra/${idUsuario}/${puntos}`, {});
        document.getElementById('input-puntos-extra').value = '';
    });
}

function pintarSelectsAdminJugadores() {
    fetch('/jugadores').then(r => r.json()).then(jugadores => {
        const selectEstado = document.getElementById('admin-jugador-estado');
        const selectFoto = document.getElementById('admin-jugador-foto'); // El nuevo select

        const posiciones = ['PORTERO', 'DEFENSA', 'MEDIO', 'DELANTERO'];
        let htmlEstado = '';
        let htmlFoto = '';

        posiciones.forEach(pos => {
            let filtrados = jugadores.filter(j => j.posicion === pos).sort((a, b) => a.nombre.localeCompare(b.nombre));

            if (filtrados.length > 0) {
                htmlEstado += `<optgroup label="${pos}">`;
                htmlFoto += `<optgroup label="${pos}">`;

                filtrados.forEach(j => {
                    let estadoActual = j.estado || 'DISPONIBLE';
                    let icon = estadoActual === 'DISPONIBLE' ? '✅' : (estadoActual === 'DUDOSO' ? '⚠️' : (estadoActual === 'LESIONADO' ? '🚑' : '❌'));

                    htmlEstado += `<option value="${j.id}">${j.nombre} (${icon} ${estadoActual})</option>`;
                    htmlFoto += `<option value="${j.id}">${j.nombre}</option>`; // Sin iconos para la foto
                });

                htmlEstado += `</optgroup>`;
                htmlFoto += `</optgroup>`;
            }
        });

        if (selectEstado) selectEstado.innerHTML = htmlEstado;
        if (selectFoto) selectFoto.innerHTML = htmlFoto;
    });
}

function cambiarEstadoJugador() {
    const idJug = document.getElementById('admin-jugador-estado').value;
    const nuevoEstado = document.getElementById('admin-nuevo-estado').value;
    if(!idJug) return;

    mostrarModal("Cambiar Estado", "¿Aplicar este nuevo estado médico?", "confirm", () => {
        post(`/admin/cambiar-estado/${idJug}/${nuevoEstado}`, {});
        setTimeout(() => {
            pintarSelectsAdminJugadores();
            cargarTodo();
        }, 500);
    });
}

function actualizarFotoJugador() {
    const idJug = document.getElementById('admin-jugador-foto').value;
    const nuevaUrl = document.getElementById('admin-nueva-foto-url').value;

    if (!idJug || !nuevaUrl.trim()) {
        mostrarModal("Error", "Debes seleccionar un jugador y escribir la ruta de la nueva imagen.", "confirm", ()=>{});
        return;
    }

    mostrarModal("Actualizar Foto", "¿Seguro que quieres aplicar esta nueva imagen?", "confirm", () => {
        post(`/admin/actualizar-imagen/${idJug}`, { urlImagen: nuevaUrl });
        document.getElementById('admin-nueva-foto-url').value = '';

        setTimeout(() => {
            cargarTodo();
        }, 500);
    });
}

function modificarSaldoManager(tipo) {
    const idUsuario = document.getElementById('admin-usuario-saldo').value;
    let cantidad = parseInt(document.getElementById('input-saldo-cantidad').value);

    if (isNaN(cantidad) || cantidad <= 0) {
        mostrarModal("Error", "Introduce una cantidad válida mayor que 0.", "confirm", ()=>{});
        return;
    }
    if (tipo === 'retirar') {
        cantidad = -cantidad;
    }

    const accionTxt = tipo === 'ingresar' ? 'INGRESAR' : 'RETIRAR MULTA DE';
    const targetTxt = idUsuario === '0' ? 'a TODOS los mánagers' : 'al mánager seleccionado';

    mostrarModal("Gestión de Saldo", `¿Seguro que quieres ${accionTxt} ${formatoDinero.format(Math.abs(cantidad))} ${targetTxt}?`, "confirm", () => {
        post(`/admin/modificar-saldo/${idUsuario}/${cantidad}`, {});

        document.getElementById('input-saldo-cantidad').value = '';
        setTimeout(() => {
            cargarTodo();
        }, 500);
    });
}

function actualizarAvatarManager() {
    const idUsuario = document.getElementById('admin-usuario-avatar').value;
    const nuevaUrl = document.getElementById('admin-nuevo-avatar-url').value;

    if (!idUsuario || !nuevaUrl.trim()) {
        mostrarModal("Error", "Debes seleccionar un mánager y escribir la ruta de la nueva imagen.", "confirm", ()=>{});
        return;
    }

    mostrarModal("Actualizar Avatar", "¿Seguro que quieres aplicar esta foto de perfil al mánager?", "confirm", () => {
        post(`/admin/actualizar-avatar/${idUsuario}`, { urlImagen: nuevaUrl });
        document.getElementById('admin-nuevo-avatar-url').value = '';
        setTimeout(() => {

            if (idUsuario === sessionStorage.getItem("usuarioId")) {
                sessionStorage.setItem("urlImagen", nuevaUrl);
                const avatarDiv = document.getElementById('dashboard-avatar');
                if (avatarDiv) avatarDiv.src = nuevaUrl;
            }

            cargarTodo();
        }, 500);
    });
}

function verFichaManager(nombre, urlImagen, puesto) {
    let imagen = (!urlImagen || urlImagen === "null" || urlImagen === "undefined") ? '/images/avatars/user.png' : urlImagen;

    document.getElementById('modal-perfil-imagen').src = imagen;
    document.getElementById('modal-perfil-nombre').innerText = nombre;
    document.getElementById('modal-perfil-puesto').innerText = puesto + "º";
    document.getElementById('modal-perfil-wrapper').classList.remove('oculto');
}

function verMiFichaPerfil() {
    let imagen = sessionStorage.getItem("urlImagen");
    let nombre = localStorage.getItem("usuarioNombre");
    let puestoNum = miPuestoActual.replace("º", "");
    verFichaManager(nombre ? nombre : "Mánager", imagen, puestoNum);
}

function cerrarModalPerfil() {
    document.getElementById('modal-perfil-wrapper').classList.add('oculto');
}

// --- SISTEMA DE MANTENIMIENTO ---
function checkMantenimiento() {
    let resultado = true;

    // El "?t=" evita el caché del navegador obligando a descargar el estado real
    fetch('/estado-mantenimiento?t=' + new Date().getTime())
    .then(r => r.json())
    .then(isMantenimiento => {
        const pantalla = document.getElementById('pantalla-mantenimiento');
        const btnAdmin = document.getElementById('btn-mantenimiento');

        if (esAdmin) {
            if (btnAdmin) {
                if (isMantenimiento) {
                    btnAdmin.innerText = "🚧 DESACTIVAR MANTENIMIENTO";
                    btnAdmin.style.background = "#d32f2f";
                    btnAdmin.style.color = "white";
                } else {
                    btnAdmin.innerText = "✅ ACTIVAR MANTENIMIENTO";
                    btnAdmin.style.background = "#2e7d32";
                    btnAdmin.style.color = "white";
                }
            } else {
                // No hay botón en esta pantalla
            }
        } else {
            // No es admin, no actualizamos botón
        }

        if (isMantenimiento) {
            if (!esAdmin) {
                pantalla.classList.remove('oculto');
                document.body.style.overflow = 'hidden';
            } else {
                pantalla.classList.add('oculto');
                document.body.style.overflow = 'auto';
            }
        } else {
            pantalla.classList.add('oculto');
            document.body.style.overflow = 'auto';
        }
    })
    .catch(err => {
        console.error("Error comprobando mantenimiento:", err);
    });

    return resultado;
}

function toggleMantenimiento() {
    let resultado = true;

    post('/admin/toggle-mantenimiento', {}, 0);
    // Le damos 500ms al backend para procesar antes de volver a preguntar
    setTimeout(checkMantenimiento, 500);

    return resultado;
}