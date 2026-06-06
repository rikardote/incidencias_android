# Incidencias Android

Cliente Android nativo para consumir la API Laravel `/api/v1` del sistema de incidencias.

## Estado inicial

- Proyecto Android Kotlin + Jetpack Compose creado.
- Login contra `POST /api/v1/login`.
- Restauración de sesión con `GET /api/v1/me`.
- Token guardado localmente de forma inicial.
- Menú principal con permisos `can_capture`.
- Búsqueda de empleados.
- Detalle de empleado con pestañas compactas de incidencias, asistencia y vacaciones.
- Filtros de fecha opcionales/colapsables para incidencias y asistencia en detalle de empleado.
- Incidencias recientes.
- Eliminación de incidencias por token desde recientes para usuarios con captura.
- Reporte resumen por quincena y departamento.
- Reportes compactos para recientes y resumen QNA, reduciendo scroll y mostrando totales.
- Biométrico con pestañas de recientes y asistencia por empleado.
- Visualización compacta de checadas y asistencia para reducir scroll, sin mostrar ubicación innecesaria.
- Tema visual institucional basado en backend: guinda, oro y verde.
- DatePicker nativo en captura y filtros de fecha.
- Primer rediseño UI/UX móvil con hero headers, action cards, tarjetas elevadas y estados visuales.
- Captura rediseñada como wizard móvil con stepper visual, tarjetas de selección y resumen de captura.
- Selección de códigos sin teclear: catálogo cargado automáticamente con límite aumentado a 100, filtros locales y categorías rápidas.
- Fechas de captura precargadas con el día actual y periodos vacacionales cargados automáticamente cuando aplican.
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

Mejoras recomendadas:

1. Separar pantallas en ViewModels/repositorios.
2. Mejorar almacenamiento seguro del token con Keystore/EncryptedSharedPreferences.
3. Separar pantallas grandes en ViewModels/repositorios.
