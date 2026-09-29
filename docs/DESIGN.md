# Zian-RCT — Diseño e investigación de integración

## Estado

Fase 0 completada para el objetivo Minecraft 1.21.1 / NeoForge 21.1.x / Java 21, con Radical Cobblemon Trainers 0.19.2-beta (`rctmod`) y Radical Cobblemon Trainers API 0.16.1-beta (`rctapi`).

Este documento define únicamente arquitectura e integración. En esta fase no se añade código del mod.

## Restricción legal

Zian-RCT será código propio bajo MIT. No se copiará, pegará, decompilará ni adaptará código de RCT o RCTAPI. La integración se limitará a interfaces, clases, métodos, eventos y formatos de datos públicos.

La distribución de RCT/RCTAPI debe tratarse como externa. La información pública de licencia no es completamente uniforme entre todos los artefactos y páginas históricas: las fuentes actuales del proyecto muestran MCOML, mientras que se han observado metadatos de artefactos con referencias distintas. Zian-RCT no incluirá código ni recursos de RCT/RCTAPI. La advertencia visible de licencia se añadirá al README antes de la entrega pública.

## Evidencia pública revisada

### RCT 0.19.2-beta

La clase pública `com.gitlab.srcmc.rctmod.api.RCTMod` expone:

- `static RCTMod getInstance()`
- `TrainerManager getTrainerManager()`
- `SeriesManager getSeriesManager()`
- `IServerConfig getServerConfig()`
- `boolean makeBattle(TrainerMob mob, Player player)`

`com.gitlab.srcmc.rctmod.api.service.TrainerManager` expone, entre otros:

- `TrainerMobData getData(String trainerId)`
- `TrainerMobData getData(TrainerMob mob)`
- `boolean isValidId(String trainerId)`
- `TrainerPlayerData getData(Player player)`
- `Stream<Map.Entry<String, TrainerMobData>> getAllData(String... series)`
- `TrainerBattleMemory getBattleMemory(ServerLevel level, String trainerId)`
- `void loadTrainers()`

`loadTrainers()` vuelve a cargar los entrenadores desde el `ResourceManager`; no constituye una API de mutación arbitraria de los objetos ya cargados.

`com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData` expone:

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

El tope no es un entero persistido con un setter público en `TrainerPlayerData`: `getLevelCap()` lo calcula mediante la lógica de RCT. Por tanto, no se ha verificado una API pública soportada para imponer un tope arbitrario por jugador sin modificar su progreso o sin añadir una capa propia de override.

`com.gitlab.srcmc.rctmod.api.data.save.TrainerBattleMemory` expone:

- `void addDefeatedBy(String trainerId, Player player)`
- `void setDefeatedBy(String trainerId, Player player, int count)`
- `int getDefeatByCount(String trainerId, Player player)`

Para reconstruir medallas antiguas, `getDefeatByCount(...) > 0` es más apropiado que depender únicamente de `TrainerPlayerData#getDefeatedTrainerIds()`, porque representa memoria de derrotas del entrenador y no solo el conjunto de derrotas de progreso de la serie activa.

`com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData` permite leer datos ya cargados, incluyendo:

- `int getRequiredLevelCap(Player player)`
- `int getRewardLevelCap(Player player)`
- `Optional<Integer> getRelativeLevelCap()`
- `List<Set<String>> getRequiredDefeats(String seriesId)`
- `Set<String> getFollowedBy()`
- `Map<Integer, ?> getWinCommands()`
- `TrainerTeam getTrainerTeam()`

No se encontró un setter público equivalente para `relativeLevelCap`, `requiredDefeats` o `winCommands`. Existen mutadores puntuales de estructuras derivadas, como `addFollowedBy`, pero no se consideran una API soportada para reconfigurar en caliente toda la definición de un entrenador.

### RCTAPI 0.16.1-beta

La API pública contiene un estado de batalla `com.gitlab.srcmc.rctapi.api.battle.BattleState` con:

- `PokemonBattle getBattle()`
- `List<Trainer> getParticipants1()`
- `List<Trainer> getParticipants2()`
- `List<Trainer> getWinners()`
- `List<Trainer> getLosers()`
- `int getWinnerSide()`
- `int getLoserSide()`
- `boolean isEndForced()`

La interfaz pública `com.gitlab.srcmc.rctapi.api.trainer.Trainer` expone `LivingEntity getEntity()`, por lo que un estado de batalla permite relacionar participantes con entidades de Minecraft sin acceder a campos privados.

La familia pública de eventos de RCTAPI contiene eventos de inicio y fin de batalla (`Events.BATTLE_STARTED` y `Events.BATTLE_ENDED`) cuyo valor es un `BattleState`. La API de contexto de eventos utiliza listeners registrados en su `EventContext`.

Antes de implementar Fase 4 se hará una verificación de compilación contra el artefacto exacto 0.16.1-beta para congelar la firma de registro del listener. Si esa firma no coincide con la documentación/fuente pública revisada, no se usará reflexión ni acceso interno: se activará el plan B basado en `winCommands`.

## Respuestas de Fase 0

### 1. ¿Se puede leer o modificar progreso, tope y entrenadores cargados? ¿Hay evento de victoria?

**Progreso: sí, parcialmente y mediante API pública de RCT.**

`RCTMod.getInstance().getTrainerManager().getData(player)` entrega `TrainerPlayerData`. Este objeto permite consultar la serie, las derrotas de progreso y el tope calculado, además de añadir o retirar derrotas de progreso.

**Tope: lectura sí; escritura arbitraria por jugador no verificada.**

`TrainerPlayerData#getLevelCap()` es público, pero no se encontró un `setLevelCap(int)` persistente equivalente en la API pública de progreso. El cap normal seguirá siendo responsabilidad de RCT. Los futuros comandos administrativos de Zian-RCT deberán respetar esta limitación: no se presentará como soportado un override que RCT luego ignore.

**Datos de entrenadores cargados: lectura sí; reconfiguración completa en caliente no.**

`TrainerManager#getData`, `getAllData` e `isValidId` permiten inspeccionarlos. `TrainerManager#loadTrainers()` fuerza una recarga desde recursos. No hay una API pública completa para sustituir de forma segura todos los campos de un `TrainerMobData` vivo.

**Victoria: existe un evento público de fin de batalla en RCTAPI.**

`Events.BATTLE_ENDED` entrega `BattleState`, y `BattleState#getWinners()` / `getLosers()` permiten determinar el resultado. Por diseño, Zian-RCT usará ese evento como camino primario. No interpretará texto de chat, advancements ni archivos internos.

### 2. ¿Es viable generar un data pack desde la configuración?

Sí.

NeoForge expone `AddPackFindersEvent` en el mod event bus para añadir fuentes de packs. Para un pack estático empaquetado dentro del JAR existe el helper `addPackFinders(...)`; para contenido creado desde la configuración y que no vive como recurso fijo del JAR, la arquitectura correcta es registrar una `RepositorySource` mediante `addRepositorySource(...)` o materializar un pack antes de la recarga de recursos y exponerlo al repositorio de packs.

Zian-RCT no escribirá dentro del JAR ni parcheará JSON de RCT. Construirá recursos propios bajo el namespace `zianrct` y, cuando sea necesario sustituir propiedades de la Liga Rassvet, generará definiciones de data pack de mayor prioridad para los IDs configurados.

Cambiar de perfil o ejecutar `/zianrct reload` requerirá reconstruir el pack virtual y provocar una recarga de recursos del servidor. Después de esa recarga se permitirá que RCT vuelva a cargar sus `TrainerMobData` mediante su flujo público de recursos. No se mutarán mapas privados ni se usará reflexión.

Limitación importante: `AddPackFindersEvent` ocurre durante la creación del repositorio de packs. Por ello el perfil activo debe poder resolverse antes de construir la fuente del pack, o la fuente registrada debe leer el snapshot de configuración vigente al abrir sus recursos. Este detalle se cerrará con una prueba de arranque y `/reload` en Fase 3.

### 3. Hook público para inicio de combate RCT

El hook previsto es `Events.BATTLE_STARTED` de RCTAPI, cuyo payload es `BattleState`.

Además, `RCTMod#makeBattle(TrainerMob, Player)` es público y es la entrada usada por RCT para iniciar un combate normal contra un `TrainerMob`, pero Zian-RCT no interceptará ni reemplazará ese método. Escuchar el evento público evita acoplarse a la implementación del arranque.

El evento de inicio se usará únicamente para observación/diagnóstico y, si finalmente hace falta, para validar overrides administrativos propios. No se reimplementarán las validaciones normales de RCT.

### 4. ¿Cómo leer entrenadores derrotados por un jugador?

Hay dos fuentes públicas útiles:

1. `TrainerPlayerData#getDefeatedTrainerIds()` para derrotas que forman el progreso de la serie del jugador.
2. `TrainerManager#getBattleMemory(level, trainerId).getDefeatByCount(trainerId, player)` para saber si el jugador ha derrotado históricamente a un entrenador concreto.

Para reconciliar medallas existentes se usará la segunda. El proceso será:

1. Iterar únicamente los entrenadores configurados en `chain`/`medals`, nunca los entrenadores por defecto de RCT.
2. Consultar `getBattleMemory(...).getDefeatByCount(trainerId, player)`.
3. Si el contador es mayor que cero y Zian-RCT aún no tiene la medalla, crear una concesión de reconciliación idempotente.
4. Sincronizar el estado resultante al cliente.

La fecha exacta histórica de una victoria anterior a la instalación de Zian-RCT no existe en el contador de RCT. En ese caso se guardará la fecha de reconciliación y se marcará internamente el origen como `MIGRATED`/`RECONCILED`, en lugar de inventar una fecha de victoria.

### 5. No verificado / pendiente de prueba

No se considera verificado todavía:

- Una API pública de RCT que permita fijar un nivel máximo arbitrario por jugador sin modificar progreso. No se encontró.
- Que un cambio de cap mediante una estructura cliente/sync de RCT sea persistente o soportado. No se usará esa vía sin documentación pública.
- Que las capturas de Cobblemon estén limitadas por RCT. No se encontró evidencia de que RCT las limite; se probará explícitamente en Fase 7.
- El orden exacto entre `Events.BATTLE_ENDED` y la escritura de `TrainerBattleMemory`. La concesión en tiempo real no depende de ese orden porque el propio `BattleState` expone ganadores/perdedores; la memoria solo se usa para reconciliación.
- La firma final de registro del listener de `EventContext` contra el JAR exacto `rctapi 0.16.1-beta`. Se comprobará en compilación antes de Fase 4. Si no coincide, se usa el plan B de `winCommands`.
- El mecanismo exacto de prioridad y refresco del pack virtual en un servidor dedicado con el conjunto final NeoForge/RCT. Se probará en Fase 3 con arranque, `/reload` y reinicio.
- La semántica de fecha para medallas migradas, más allá de guardar de forma honesta la fecha de reconciliación.

## Arquitectura elegida

### 1. Configuración

`zianrct.json` será la fuente de verdad de Zian-RCT y contendrá caps, serie, cadena, medallas, mensajes, perfil activo y opciones como `giveMedalItem`.

Los perfiles se resolverán a un snapshot inmutable de configuración. El servidor validará el snapshot antes de publicarlo como configuración activa. El cliente recibirá únicamente los datos necesarios para interfaz y presentación.

### 2. Adaptación de RCT

Zian-RCT no modificará objetos internos de RCT en memoria.

Las propiedades de la serie y entrenadores que deban derivarse de `initialCap`, `step`, `maxCap` y `chain` se materializarán como un data pack virtual/de alta prioridad. RCT seguirá siendo responsable de calcular el cap normal, impedir experiencia por encima del tope y rechazar equipos fuera del rango.

Tras un reload de Zian-RCT se regenerará el snapshot del pack y se realizará una recarga de recursos controlada. Si RCT requiere su llamada pública `TrainerManager#loadTrainers()` después de la recarga, se utilizará solamente cuando la prueba de Fase 3 confirme el orden correcto y que no duplica recargas.

### 3. Detección de victoria

Camino primario:

- Obtener la instancia RCTAPI asociada a RCT.
- Registrar un listener para `Events.BATTLE_ENDED`.
- Ignorar batallas forzadamente terminadas cuando no tengan un ganador válido.
- Leer `BattleState#getWinners()` y `getLosers()`.
- Identificar al `ServerPlayer` vencedor mediante `Trainer#getEntity()`.
- Identificar el `TrainerMob` perdedor y su `trainerId` público.
- Consultar el índice de `chain` para saber si ese entrenador concede una medalla.
- Ejecutar `MedalService.grantIfAbsent(...)`.

La operación será idempotente: si el jugador ya posee la medalla, no se modifica fecha, no se entrega un segundo ítem y no se repite toast/sonido.

Plan B, sin reflexión:

Si el evento público no resulta utilizable con los binarios exactos, los JSON generados para los entrenadores clave incluirán un `winCommands` original de Zian-RCT que ejecute:

`/zianrct medal grant @s <medalId>`

Este comando será interno/administrativo, validará que el ID exista y usará exactamente el mismo `MedalService`. El plan B no requiere copiar código de RCT.

### 4. Persistencia de medallas

La autoridad será siempre el servidor.

Se utilizará almacenamiento persistente propio por UUID de jugador, preferiblemente data attachment de NeoForge si su ciclo de copia/guardado resulta adecuado para jugadores en 1.21.1; en caso contrario se utilizará `SavedData` propio. La elección concreta se cerrará en Fase 4 después de probar persistencia y reconexión.

Cada concesión almacenará como mínimo:

- `medalId`
- instante de concesión
- origen (`BATTLE`, `COMMAND`, `RECONCILED`)

No se guardará la textura en el jugador; la definición visual proviene de la configuración activa.

### 5. Reconciliación

En login, reload y bajo un comando administrativo de reparación se ejecutará reconciliación:

- por cada medalla configurada, localizar su entrenador;
- consultar el contador público de `TrainerBattleMemory`;
- si `count > 0`, conceder si falta;
- nunca revocar automáticamente una medalla existente porque RCT haya sido reconfigurado.

Esto permite migrar el progreso existente de Liga Rassvet sin depender del medallero/advancements anteriores ni de sus rutas defectuosas.

### 6. Sincronización cliente

Flujo previsto:

1. Al login, el servidor envía las definiciones de medallas necesarias para UI y el conjunto de medallas obtenidas por ese jugador.
2. Al conceder/revocar, envía un delta o snapshot actualizado.
3. El cliente mantiene solo una copia de presentación; nunca puede concederse medallas a sí mismo.
4. La pantalla `/medals` y la tecla usan ese snapshot.
5. El toast se dispara únicamente tras un payload de concesión del servidor.

Los payloads usarán la API de networking de NeoForge 1.21.1. El protocolo incluirá una versión simple para poder rechazar clientes incompatibles de forma clara.

### 7. Cliente requerido

La intención de diseño es **mod requerido en ambos lados**. La interfaz, tecla, texturas/fallback y payloads forman parte del cliente; permitir clientes vanilla o sin Zian-RCT produciría una experiencia incompleta y complica el protocolo sin aportar valor al servidor objetivo.

`neoforge.mods.toml` declarará las dependencias necesarias y la negociación de red se configurará para que un cliente sin Zian-RCT no pueda entrar al servidor cuando Zian-RCT está activo. El mensaje de incompatibilidad se documentará en README.

## Diseño del cap administrativo: limitación y decisión provisional

La progresión normal no se reimplementará.

Los comandos administrativos `get`, `set`, `add` y `remove` requieren una decisión técnica adicional porque RCT expone el cap calculado pero no un setter persistente público de cap. En Fase 3 se hará una prueba de integración para determinar una de estas dos estrategias, en este orden:

1. **Preferida:** expresar el cambio mediante mecanismos públicos de progresión/configuración de RCT sin falsificar medallas ni derrotas.
2. **Solo si lo anterior es imposible:** introducir un `adminCapOverride` propio y aplicar únicamente las restricciones adicionales indispensables alrededor de RCT. Nunca se escribirá directamente en campos privados ni se mantendrá un fork de RCT.

No se implementará un comando `/zianrct set` que muestre un valor que RCT no vaya a respetar.

## Data pack de Liga Rassvet

El nuevo sistema de medallas no leerá los advancements antiguos como fuente de verdad y no llamará a la función `rassvet:medallero`.

La cadena configurada en Zian-RCT será la única relación entre entrenador clave, cap desbloqueado y medalla. El data pack generado podrá corregir mensajes de desbloqueo y `winCommands` si se necesita el plan B, sin depender de los aproximadamente 1500 entrenadores incluidos por defecto con RCT.

## Criterios para pasar a Fase 1

Fase 0 se considera cerrada cuando:

- este documento está en `main` en un commit propio;
- no se ha añadido código ejecutable;
- las limitaciones no verificadas están registradas explícitamente.

Fase 1 deberá crear el esqueleto Gradle/NeoForge, declarar dependencias y demostrar compilación/arranque antes de su commit. Ninguna decisión marcada como no verificada en este documento se tratará como hecho durante la implementación.

## Referencias públicas consultadas

- RCT Mod 0.19.2-beta, API pública: `com.gitlab.srcmc.rctmod.api.*` y clases públicas relacionadas en el repositorio oficial `srcmc/rct/mod`.
- RCTAPI 0.16.1-beta, tag oficial `v0.16.1-beta` y `com.gitlab.srcmc.rctapi.api.*` en `srcmc/rct/api`.
- NeoForge 1.21.1: documentación oficial de eventos y sistema de packs, incluyendo `AddPackFindersEvent`.

No se ha incorporado código fuente de ninguno de esos proyectos a Zian-RCT.