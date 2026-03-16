
//Comprobar si ya hay sesión iniciada
if (localStorage.getItem('usuarioId')) {
    window.location.replace('fantasy.html');
}

function login() {
    const nombre = document.getElementById('nombre').value;
    const pass = document.getElementById('pass').value;

    fetch('/auth/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ nombre: nombre, password: pass })
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            mostrarError(data.error);
        } else {
            localStorage.setItem('usuarioId', data.id);
            localStorage.setItem('usuarioNombre', data.nombre);
            localStorage.setItem('esAdmin', data.esAdmin);
            localStorage.setItem('presupuesto', data.presupuesto);
            window.location.href = 'fantasy.html';
        }
    });
}

function registro() {
    const nombre = document.getElementById('nombre').value;
    const pass = document.getElementById('pass').value;

    if(!nombre || !pass) { mostrarError("Rellena todos los campos."); return; }

    fetch('/auth/registro', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ nombre: nombre, password: pass })
    })
    .then(r => r.text())
    .then(msg => {
        if(msg.includes("❌")) mostrarError(msg);
        else {
            document.getElementById('error-msg').style.display = 'none';
            const s = document.getElementById('success-msg');
            s.innerText = msg;
            s.style.display = 'block';
        }
    });
}

function mostrarError(msg) {
    document.getElementById('success-msg').style.display = 'none';
    const e = document.getElementById('error-msg');
    e.innerText = msg;
    e.style.display = 'block';
}