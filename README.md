# Zian-RCT

Entrenadores propios, NPC, progresión, medallas y recompensas sobre RCTAPI para Minecraft 1.21.1 / NeoForge.

## Estado

**0.2.1 Release**, implementación independiente de RCTMod. Antes de actualizar un mundo existente, consulta [la migración y las condiciones de licencia](docs/RCTAPI-STANDALONE.md). Usa el mismo JAR en cliente y servidor. La beta 0.1.0-beta.15 permanece en su rama anterior.

Incluye configuración validada, NPC/editor propios, equipos automáticos, combates individuales y dobles, progresión y medallas persistentes, loot único/repetible y pruebas legendarias con entrega registrada.

## Dependencias objetivo

- Minecraft 1.21.1
- NeoForge 21.1.x
- Cobblemon 1.8.2
- RCTMod debe estar ausente en esta rama.
- Radical Cobblemon Trainers API 0.16.2-beta (`rctapi`)
- Architectury 13.0.11
- Kotlin for Forge 5.12.0

AVECOINS 2.3, 2.4 y 2.5 son opcionales para recompensas económicas. AVECOINS 2.3, 2.4 y 2.5 están inspeccionados. Una versión posterior solo se admite si las tres clases económicas verificadas conservan exactamente sus huellas revisadas y pasan las comprobaciones de métodos y capacidad. Un cambio real de contrato requiere revisión; no se promete compatibilidad automática con cualquier actualización. Consulta [compatibilidad económica](docs/AVECOINS25_COMPATIBILITY.md).

Descarga el JAR en [Releases](https://github.com/IANBLK/Zian-RCT/releases/latest) y usa la misma versión en cliente y servidor.

## Comandos

El comando público `/medals` sincroniza el snapshot autoritativo del servidor y abre el medallero en el cliente.

Los comandos administrativos `/zianrct` requieren OP nivel 2 por defecto.
Con LuckPerms pueden delegarse mediante los nodos siguientes; una denegación
explícita también se respeta para operadores. LuckPerms es opcional, como mod
de NeoForge o plugin de Bukkit en Youer, y no se incluye en el JAR.

- `/zianrct medal give <jugador> <medalla>`
- `/zianrct medal revoke <jugador> <medalla>`
- `/zianrct medal list <jugador>`
- `/zianrct cap get <jugador>`
- `/zianrct cap set <jugador> <cap>`
- `/zianrct cap add <jugador>`
- `/zianrct cap remove <jugador>`
- `/zianrct progress <jugador>`
- `/zianrct reload`

## Permisos

- `zianrct.medals`: abrir `/medals` (todos por defecto).
- `zianrct.battle`: interactuar y aceptar desafíos de la cadena configurada (todos por defecto).
- `zianrct.admin.medal.give`, `zianrct.admin.medal.revoke`, `zianrct.admin.medal.list`.
- `zianrct.admin.cap.get`, `zianrct.admin.cap.set`, `zianrct.admin.cap.add`, `zianrct.admin.cap.remove`.
- `zianrct.admin.progress`, `zianrct.admin.reload`.

Los nodos administrativos usan OP nivel 2 cuando están sin definir. Si el proveedor
instalado no está disponible, el acceso se deniega. La consola conserva sus permisos.
En Youer se habilita el acceso al envoltorio de comandos de Bukkit para que las
comprobaciones anteriores puedan ejecutarse; los permisos ya registrados no se sobrescriben.

Ejemplos: `/lp group moderador permission set zianrct.admin.medal.give true`
y `/lp user IANBLK permission set zianrct.admin.reload false`.
Reconecta después de cambiar permisos para actualizar el autocompletado.

`cap set` solo acepta topes alcanzables por la cadena configurada. Cuando varios entrenadores producen el mismo tope, se usa el primer prefijo que alcanza ese valor. En el perfil Rassvet por defecto, el tope 100 se alcanza al derrotar a Glacius, por lo que `cap set ... 100` marca la cadena hasta Glacius y no fuerza la derrota de Aurelia. Aurelia sigue siendo una victoria final/medalla separada con el tope ya en 100.

`/zianrct reload` vuelve a leer `config/zianrct.json`, rechaza la recarga si la configuración es inválida, regenera el pack virtual de progresión y vuelve a enviar el snapshot de medallas a todos los jugadores conectados.

## Entrenadores y recompensas

- Crea entrenadores propios desde **Crear NPC → Definir entrenador desde cero**, con equipos, niveles, dificultad, skins y diálogos.
- El editor permite buscar Pokémon y ataques, seleccionar hasta cuatro movimientos y usar movimientos automáticos al guardar. El modo manual conserva los equipos existentes.
- Configura combates individuales o dobles y recompensas únicas o repetibles con espera por jugador. Mayús + clic derecho muestra la espera del entrenador.
- Usa el gestor de NPC para crear, editar, eliminar con confirmación o mover un NPC a la posición del administrador.
- Las pruebas legendarias vinculan una victoria de jefe a un desafío posterior. Los premios se reservan y registran antes de entregarse al PC para impedir repeticiones.
- Los premios económicos usan AVECOINS mediante un contrato comprobado. Las entregas inciertas permanecen en revisión; no se reintentan a ciegas.

`/zianrct reward pending` muestra operaciones pendientes y `/zianrct reward claim <operationId>` permite reclamarlas cuando su estado lo permite. Tab sugiere los IDs propios reclamables; los premios ya pagados o bloqueados no se ofrecen.

Guías: [creador de equipos](docs/CREATOR-BETA10.md), [repetición y combates dobles](docs/REPEAT-AND-DOUBLES-BETA8.md), [gestor de NPC](docs/NPC-MANAGER-BETA6.md), [recompensas y recuperación](docs/TRAINER-REWARDS-BETA4.md) y [pruebas legendarias](docs/LEGENDARY-TRIALS-BETA13.md). Los nombres beta de estos documentos indican cuándo se introdujeron las funciones; instala la versión actual **0.2.1** en cliente y servidor.

## Combinaciones externas

La validación con Legendary Monuments 8.2 detectó callbacks de victoria que asumen entidades NPC nativas. El servidor de pruebas usa una protección local para esa combinación; no está incluida en Zian RCT. Consulta la compatibilidad de esa versión externa antes de combinarla. Esto no cambia la persistencia de medallas ni las reglas de recompensas de Zian RCT.

## Medallero

Las diez medallas predeterminadas muestran el render aprobado mediante una imagen original integrada, sin volver a generar los diseños. El medallero es compacto, tiene botón Cerrar y paginación según el espacio disponible. Conserva los identificadores, el progreso y el protocolo 2. Consulta [el arte y la validación](docs/MEDALS-BETA3.md).

Zian-RCT incluye arte original para las diez medallas del perfil Rassvet en `assets/zianrct/textures/gui/medals/`, junto con fondos de tarjeta para estados obtenida/bloqueada, overlay de hover y candado. La configuración por defecto ya apunta a `zianrct:textures/gui/medals/<id>.png`, por lo que no requiere un resource pack adicional para usar el arte incluido.

Cada definición de medalla admite el campo opcional `trainerName` para mostrar un nombre legible en el medallero. Si se omite, Zian-RCT lo deduce del id del entrenador, eliminando los prefijos `rassvet_leader_` o `rassvet_master_` cuando correspondan.

Las definiciones de medallas siguen pudiendo referenciar otras texturas mediante `ResourceLocation`. El servidor **no envía archivos PNG dentro del payload de red**: cualquier textura personalizada externa debe existir ya en los recursos del cliente, por ejemplo mediante un resource pack. Si una textura configurada no existe o no puede decodificarse como imagen, el medallero usa su representación visual de respaldo en lugar de mostrar la textura de error de Minecraft.

Los cambios del payload del medallero requieren que cliente y servidor ejecuten la misma versión de Zian-RCT. El protocolo actual del snapshot de medallas es la versión 2.

## Licencia e integración con RCT

El código y los recursos originales de Zian-RCT se publican bajo la licencia MIT.

RCTAPI es una dependencia externa obligatoria. RCTMod debe permanecer ausente en esta versión independiente. Zian-RCT no incluye, copia, decompila ni adapta código o recursos de esos proyectos; la integración usa únicamente sus APIs y formatos públicos.

La información de licencia publicada para RCT/RCTAPI no ha sido completamente uniforme entre todos los metadatos históricos: se han observado referencias GNU-LGPL-3 en artefactos/metadatos anteriores, mientras que las páginas y fuentes actuales usan MCOML/Custom License. Antes de redistribuir RCT o RCTAPI, consulta siempre la licencia de sus proyectos oficiales. Esta licencia externa no cambia la licencia MIT del código propio de Zian-RCT.

Consulta `docs/DESIGN.md` para las decisiones técnicas y limitaciones verificadas.
