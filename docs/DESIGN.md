# Zian-RCT — Diseño e investigación de integración

## Estado

Fase 0 completada para Minecraft 1.21.1 / NeoForge 21.1.x / Java 21, con Radical Cobblemon Trainers 0.19.2-beta (`rctmod`) y Radical Cobblemon Trainers API 0.16.1-beta (`rctapi`).

Este documento define la arquitectura de integración. Zian-RCT no copia, adapta ni decompila código de RCT/RCTAPI; usa exclusivamente API y formatos públicos.

## Restricción legal

Zian-RCT usa licencia MIT para su código y recursos propios. RCT y RCTAPI son dependencias externas y no se redistribuyen dentro del JAR de Zian-RCT.

La información pública de licencia de RCT/RCTAPI no ha sido históricamente uniforme entre metadatos de artefactos y páginas de distribución. Las fuentes actuales revisadas muestran MCOML. El README de Zian-RCT debe advertir esta situación y dejar claro que el proyecto no incluye código ni recursos de RCT/RCTAPI.

## Evidencia pública revisada

### RCT 0.19.2-beta

`com.gitlab.srcmc.rctmod.api.RCTMod` expone, entre otros:

- `static RCTMod getInstance()`
- `TrainerManager getTrainerManager()`
- `SeriesManager getSeriesManager()`
- `IServerConfig getServerConfig()`
- `boolean makeBattle(TrainerMob mob, Player player)`

`com.gitlab.srcmc.rctmod.api.service.TrainerManager` expone:

- `TrainerMobData getData(String trainerId)`
- `TrainerMobData getData(TrainerMob mob)`
- `boolean isValidId(String trainerId)`
- `TrainerPlayerData getData(Player player)`
- `Stream<Map.Entry<String, TrainerMobData>> getAllData(String... series)`
- `TrainerBattleMemory getBattleMemory(ServerLevel level, String trainerId)`
- `void loadTrainers()`

`TrainerPlayerData` expone:

- `int getLevelCap()`
- `Set<String> getDefeatedTrainerIds()`
- `boolean addProgressDefeat(String trainerId)`
- `boolean removeProgressDefeat(String trainerId)`
- `boolean removeProgressDefeats()`
- `String getCurrentSeries()`
- `void setCurrentSeries(String seriesId)`
- `void setCurrentSeries(String seriesId, boolean keepProgress)`
- `Map<String, Integer> getCompletedSeries()`
- `void sync()`

No existe un `setLevelCap(int)` público equivalente. El cap normal es calculado por RCT a partir del progreso y la configuración de la serie/entrenadores.

`TrainerBattleMemory` expone:

- `void addDefeatedBy(String trainerId, Player player)`
- `void setDefeatedBy(String trainerId, Player player, int count)`
- `int getDefeatByCount(String trainerId, Player player)`

`TrainerMobData` permite leer, entre otros:

- `int getRequiredLevelCap(Player player)`
- `int getRewardLevelCap(Player player)`
- `Optional<Integer> getRelativeLevelCap()`
- `List<Set<String>> getRequiredDefeats(String seriesId)`
- `Set<String> getFollowedBy()`
- `Map<Integer, ?> getWinCommands()`
- `TrainerTeam getTrainerTeam()`

No se encontró una API pública completa para reescribir en caliente `relativeLevelCap`, `requiredDefeats` o `winCommands` de un entrenador ya cargado.

### RCTAPI 0.16.1-beta

`com.gitlab.srcmc.rctapi.api.battle.BattleState` expone:

- `PokemonBattle getBattle()`
- `List<Trainer> getParticipants1()`
- `List<Trainer> getParticipants2()`
- `List<Trainer> getWinners()`
- `List<Trainer> getLosers()`
- `int getWinnerSide()`
- `int getLoserSide()`
- `boolean isEndForced()`

`com.gitlab.srcmc.rctapi.api.trainer.Trainer` expone `LivingEntity getEntity()`.

`Events.BATTLE_STARTED` y `Events.BATTLE_ENDED` son eventos públicos con `BattleState` como valor. `EventContext` permite registrar listeners públicos.

## Respuestas de Fase 0

### 1. Progreso, cap, entrenadores y victoria

**Progreso:** se puede leer y modificar mediante `TrainerPlayerData`, incluyendo añadir y retirar derrotas de progreso.

**Cap:** se puede leer mediante `getLevelCap()`, pero no se encontró un setter directo. Por ello Zian-RCT no mantendrá un entero paralelo fingiendo que RCT lo respeta.

**Entrenadores cargados:** se pueden inspeccionar mediante `TrainerManager`, pero la reconfiguración completa en caliente no está expuesta como API pública soportada.

**Victoria:** RCTAPI expone `Events.BATTLE_ENDED`; el camino primario de medallas usará ese evento y `BattleState#getWinners()/getLosers()`.

### 2. Data pack generado

Es viable generar o exponer un data pack propio desde Zian-RCT usando el sistema de packs de NeoForge (`AddPackFindersEvent` / `RepositorySource`).

La configuración de Zian-RCT será la fuente de verdad y generará recursos propios de mayor prioridad para la serie y los entrenadores de Liga Rassvet. No se parchearán archivos de RCT ni se tocarán sus mapas privados.

Un cambio de perfil o `/zianrct reload` requerirá reconstruir el snapshot del pack y recargar recursos. En Fase 3 se probará el orden exacto con `TrainerManager#loadTrainers()`.

### 3. Hook de inicio de combate

El hook público previsto es `Events.BATTLE_STARTED` de RCTAPI. `RCTMod#makeBattle(...)` es público, pero Zian-RCT no lo interceptará ni lo reemplazará.

### 4. Reconciliación con victorias existentes

Hay dos fuentes públicas:

1. `TrainerPlayerData#getDefeatedTrainerIds()` para progreso de la serie activa.
2. `TrainerManager#getBattleMemory(level, trainerId).getDefeatByCount(trainerId, player)` para memoria histórica de derrotas del entrenador.

Para medallas existentes se usará preferentemente la segunda.

#### Semántica de dimensión de `getBattleMemory`

Aunque la firma recibe `ServerLevel`, en RCT 0.19.2-beta la implementación de `TrainerManager#getBattleMemory(ServerLevel, String)` obtiene el almacenamiento con:

`level.getServer().overworld().getDataStorage()`

Por tanto, **la memoria no es por dimensión**. El parámetro `ServerLevel` sirve como contexto para llegar al servidor, pero el `SavedData` se lee y escribe siempre en el almacenamiento del Overworld.

Consecuencia: la reconciliación **no debe recorrer todas las dimensiones**. Basta con usar un `ServerLevel` válido del servidor, preferentemente `server.overworld()`, y consultar una vez cada `trainerId` configurado.

Proceso de reconciliación:

1. Iterar exclusivamente entrenadores de `chain`/`medals`.
2. Consultar `getBattleMemory(server.overworld(), trainerId)`.
3. Si `getDefeatByCount(trainerId, player) > 0` y falta la medalla, concederla con origen `RECONCILED`.
4. No duplicar ítems, toast ni fecha si la medalla ya existe.
5. Sincronizar el snapshot final al cliente.

RCT no conserva en este contador la fecha histórica exacta de la primera victoria. Para una medalla migrada se almacenará la fecha de reconciliación, nunca una fecha inventada.

### 5. Instancia de RCTAPI usada por RCT

Zian-RCT no asumirá silenciosamente un id de instancia.

La API pública `RCTApi#getInstances()` devuelve todas las instancias registradas como pares `id -> RCTApi`, además de la instancia por defecto con id vacío.

En RCT 0.19.2-beta, `ModCommon` declara:

- `MOD_ID = "rctmod"`
- `RCT = RCTApi.initInstance(MOD_ID)`

Por tanto, actualmente la instancia esperada es `rctmod`. Sin embargo, para evitar acoplar la lógica a una suposición oculta, Zian-RCT hará lo siguiente al arrancar el servidor:

1. Enumerar `RCTApi.getInstances()`.
2. Registrar en log los ids disponibles a nivel DEBUG/INFO de diagnóstico.
3. Buscar explícitamente el id `rctmod`.
4. Verificar que la instancia encontrada no sea la instancia por defecto vacía.
5. Registrar listeners de `BATTLE_STARTED`/`BATTLE_ENDED` solo sobre esa instancia.
6. Si `rctmod` no existe, fallar de forma clara y no registrar listeners en una instancia incorrecta.

No se llamará a `RCTApi.initInstance("rctmod")` desde Zian-RCT para “forzar” la instancia, porque esa instancia pertenece a RCT y debe ser creada por RCT.

### 6. No verificado / pendiente de prueba

- Capturas de Cobblemon respecto al cap: se probarán expresamente en Fase 7.
- Orden exacto entre `BATTLE_ENDED` y escritura de `TrainerBattleMemory`: no afecta al otorgado en tiempo real; la memoria solo se usa para reconciliación.
- Firma final del listener de `EventContext` contra el JAR exacto 0.16.1-beta: se congelará por compilación antes de Fase 4.
- Prioridad y refresco del pack virtual tras `/reload`: Fase 3.
- Comportamiento exacto de los comandos administrativos al manipular progreso: Fase 3.

## Arquitectura elegida

### 1. Configuración

`zianrct.json` será la fuente de verdad de caps, serie, cadena, medallas, mensajes, perfil activo y opciones como `giveMedalItem`.

Los perfiles producirán un snapshot inmutable y validado. El cliente solo recibirá datos de presentación necesarios para la UI.

### 2. Adaptación de RCT

Zian-RCT no mutará internals privados de RCT.

Las propiedades derivadas de `initialCap`, `step`, `maxCap` y `chain` se materializarán mediante data pack generado/de alta prioridad. RCT seguirá siendo responsable de su lógica normal de experiencia, validación de equipos y cálculo del cap.

### 3. Detección de victoria

Camino primario:

1. Resolver la instancia `rctmod` mediante `RCTApi.getInstances()`.
2. Registrar listener para `Events.BATTLE_ENDED`.
3. Leer `BattleState#getWinners()` y `getLosers()`.
4. Identificar `ServerPlayer` vencedor mediante `Trainer#getEntity()`.
5. Identificar el `TrainerMob` derrotado y su `trainerId` público.
6. Consultar `chain`.
7. Ejecutar `MedalService.grantIfAbsent(...)`.

La concesión será idempotente.

Plan B sin reflexión: si el evento público no resulta utilizable con los binarios exactos, los JSON generados usarán `winCommands` para ejecutar:

`/zianrct medal grant @s <medalId>`

El comando usará el mismo `MedalService`.

### 4. Persistencia de medallas

La autoridad es el servidor. Cada concesión almacenará al menos:

- `medalId`
- instante de concesión
- origen (`BATTLE`, `COMMAND`, `RECONCILED`)

La elección final entre data attachments y `SavedData` propio se cerrará en Fase 4 tras probar guardado y reconexión.

### 5. Sincronización cliente

- Login: snapshot de definiciones necesarias para UI + medallas obtenidas.
- Cambio: delta o snapshot actualizado.
- El cliente nunca concede medallas.
- `/medals` y la tecla abren la UI usando el snapshot local.
- El toast solo se dispara tras confirmación del servidor.

El protocolo tendrá versión propia y Zian-RCT será requerido en cliente y servidor.

## Cap administrativo: vía preferida para Fase 3

Los comandos `/zianrct get|set|add|remove` no reimplementarán el sistema de nivel de RCT.

La **vía preferida** a evaluar en Fase 3 es representar el cap administrativo modificando únicamente el progreso de RCT mediante:

- `TrainerPlayerData#addProgressDefeat(trainerId)`
- `TrainerPlayerData#removeProgressDefeat(trainerId)`
- `TrainerPlayerData#sync()`

La cadena de Zian-RCT permite mapear un cap objetivo al prefijo de entrenadores que RCT debe considerar derrotados. Ejemplo conceptual: para fijar cap 40, Zian-RCT ajustaría solo las derrotas de progreso necesarias para que la cadena quede exactamente en el punto que produce 40.

Reglas obligatorias:

1. **No tocar medallas.** Un cambio administrativo de cap/progreso no concede ni revoca medallas automáticamente.
2. No tocar `TrainerBattleMemory`, porque representa historial real de victorias y se usa para reconciliar medallas.
3. Antes de modificar, calcular qué `trainerId` deben añadirse o retirarse según `chain`.
4. Aplicar únicamente `addProgressDefeat`/`removeProgressDefeat` sobre esos entrenadores.
5. Ejecutar `sync()` y releer `getLevelCap()`.
6. Si el cap resultante no coincide con el solicitado, revertir o informar de la limitación en lugar de mostrar éxito falso.
7. `add` y `remove` operarán en pasos válidos de la cadena; `set` normalizará/rechazará valores que no correspondan a un cap alcanzable.

Esta vía será aceptada definitivamente **solo después de la prueba de Fase 3**, porque hay que verificar cómo interactúan `requiredDefeats`, series completadas, entrenadores opcionales y cambios de progreso hacia atrás.

Solo si esta estrategia pública no permite un comportamiento correcto se evaluará un `adminCapOverride` propio, sin reflexión ni escritura en campos privados.

## Liga Rassvet y medallero anterior

El nuevo sistema de medallas no usará los advancements antiguos ni llamará a `rassvet:medallero`.

La cadena de Zian-RCT será la única relación entre entrenador clave, cap desbloqueado y medalla. El data pack generado podrá corregir los mensajes de desbloqueo y añadir `winCommands` únicamente si hace falta el plan B.

## Criterios de avance

Fase 1 debe crear el esqueleto Gradle/NeoForge, declarar las dependencias exactas, cargar una configuración inicial y demostrar compilación y arranque de servidor antes de integrar lógica de Fase 2 o posterior.

## Referencias públicas consultadas

- RCT Mod 0.19.2-beta, API pública del repositorio oficial `srcmc/rct/mod`.
- RCTAPI 0.16.1-beta, API pública del repositorio oficial `srcmc/rct/api`.
- NeoForge 1.21.1, documentación oficial de ModDevGradle, eventos y packs.

No se ha incorporado código fuente de esos proyectos a Zian-RCT.