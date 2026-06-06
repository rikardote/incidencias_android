# Incidencias Android

Cliente Android nativo para consumir la API Laravel `/api/v1` del sistema de incidencias.

## Estado inicial

- Proyecto Android Kotlin + Jetpack Compose creado.
- Login contra `POST /api/v1/login`.
- Restauración de sesión con `GET /api/v1/me`.
- Token guardado localmente de forma inicial.
- Menú principal con permisos `can_capture`.
- Búsqueda de empleados.
- Incidencias recientes.
- Biométrico reciente.
- Captura en 3 pasos: empleado, código y formulario dinámico.
- Formulario dinámico para rangos, incapacidades/médico, periodos, TXT, comisión y otorgado.

## API local

En emulador Android usar:

```txt
http://10.0.2.2:8190/
```

En dispositivo físico usar la IP LAN de la máquina donde corre Docker/Laravel.

## Build

```bash
./gradlew assembleDebug
```

## Siguiente fase

Implementar detalle completo de empleado con pestañas:

1. Incidencias por empleado.
2. Asistencia biométrica.
3. Vacaciones.
