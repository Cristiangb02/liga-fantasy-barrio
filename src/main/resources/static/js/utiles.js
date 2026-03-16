//Variables y funciones globales

//De sesión y utilidades
const usuarioId = localStorage.getItem('usuarioId');
const esAdmin = (localStorage.getItem('esAdmin') === 'true');

//Formateador de moneda compartido
const formatoDinero = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0
});

//Cerrar sesión
function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
}