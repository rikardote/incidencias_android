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
- Captura queda preparada como siguiente fase.

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

Implementar captura en 3 pasos:

1. Seleccionar empleado.
2. Seleccionar código.
3. Formulario dinámico y `POST /api/v1/incidencias`.
