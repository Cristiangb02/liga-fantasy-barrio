// 🔴 BUG 3 CORREGIDO: MERCADO ESTÁTICO (NO SE RELLENA AL FICHAR)
    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        // 1. Obtenemos datos de referencia
        Jornada jornadaActual = getJornadaActiva();
        List<Jugador> todos = jugadorRepository.findAll();
        
        // 2. Semilla: Día del año + ID Jornada. 
        // Esto garantiza que el orden sea EL MISMO durante todo el día.
        long seed = LocalDate.now(ZoneId.of("Europe/Madrid")).toEpochDay() + jornadaActual.getId();
        Collections.shuffle(todos, new Random(seed));
        
        // 3. Lógica de "HUECOS RESERVADOS":
        // Recorremos la lista barajada y seleccionamos los 14 primeros jugadores que cumplan:
        // A) Están Libres
        // B) O... han sido fichados EN ESTA MISMA JORNADA (es decir, eran libres esta mañana)
        
        List<Jugador> mercadoVisible = new ArrayList<>();
        int slotsOcupados = 0;

        for (Jugador j : todos) {
            boolean esLibre = j.getPropietario() == null;
            // Si tiene dueño, miramos si lo fichó "hoy" (en esta jornada)
            // Nota: j.getJornadaFichaje() debe ser un long o int que guardaste al comprar
            boolean fichadoHoy = !esLibre && (j.getJornadaFichaje() == jornadaActual.getId());

            // Si es un candidato válido para el mercado de hoy (Libre o recien vendido)
            if (esLibre || fichadoHoy) {
                slotsOcupados++;
                
                // SOLO lo añadimos a la lista visible si SIGUE LIBRE.
                // Si fue "fichadoHoy", contamos el slot (para llegar a 14) pero NO lo mostramos.
                if (esLibre) {
                    mercadoVisible.add(j);
                }
            }

            // En cuanto hayamos revisado 14 huecos de mercado, paramos.
            // Si compraste a uno, slotsOcupados será 14, pero mercadoVisible será 13.
            if (slotsOcupados == 14) {
                break;
            }
        }

        return mercadoVisible;
    }
