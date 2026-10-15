# Persistencia beta 0.2.0-beta.2

El cambio utiliza únicamente código propio de ZianRCT. RCT API continúa siendo
una dependencia separada; no se ha integrado implementación de upstream.

- Índices de recibos por jugador y combate; se reutiliza el JSON ya serializado.
- En Linux se sincroniza el directorio después del reemplazo atómico de recibos
  y pruebas legendarias.
- Migración offline opcional a recompensas y pruebas por jugador, con caché de 32
  propietarios (configurable 1–64). Los recibos antiguos, cooldowns, ciclos y
  estados inciertos se mantienen. La consulta de un ID de jugador carga únicamente
  su archivo; no se buscan IDs arbitrarios por todos los usuarios.
- Las entregas activas fijan su archivo en la caché durante los efectos externos.
- La verificación periódica de pruebas legendarias consulta solo a jugadores
  conectados y participantes con verificación en curso, no todo el historial
  del servidor cada segundo.

## Procedimiento

Detener el servidor, impedir reinicios durante el mantenimiento y respaldar el
servidor completo, incluyendo PC/inventarios, economía, configuraciones y JAR.
Usar Java 21, el JAR de ZianRCT y Gson del servidor:

```text
java -cp <zianrct.jar>:<gson.jar> com.ianblk.zianrct.reward.RewardStorageMaintenance migrate-offline <mundo>/data/zianrct-rewards.json
java -cp <zianrct.jar>:<gson.jar> com.ianblk.zianrct.legendary.TrialStorageMaintenance migrate-offline <mundo>/data/zianrct-legendary-trials.json
```

Usar `;` como separador del classpath en Windows y entrecomillar rutas con espacios.
Las herramientas también inicializan un almacén vacío si el archivo no existía.
No hay migración automática al instalar la actualización.

El JSON original se conserva en una copia `.archive-<uuid>`. Se verifica cada
archivo antes de publicar el manifiesto. Los premios legendarios conservan UUID,
IV, shiny, evidencia de victoria y fase de entrega: una prueba entregada no se
reabre y una APPLYING/REVIEW no vuelve a sortearse.

Manifiestos nuevos: recompensas esquema 3; pruebas esquema 2. No son legibles por
versiones anteriores. Restaurar después de pagos requiere una copia consistente
de todos los datos, no solo el recibo. Tras una interrupción conservar staging,
copias y `.maintenance` para revisión antes de arrancar o retroceder.

El progreso y las medallas continúan con su formato propio. No se cambian caps,
entrenadores ni la política de victorias. Los límites de recibos pasan a ser por
jugador tras migrar; no se borra historia y aún se deben monitorizar los archivos
individuales y la capacidad total del disco.
