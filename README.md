# Zian-RCT

Tope de nivel, progresión administrativa y sistema de medallas sobre Radical Cobblemon Trainers para Minecraft 1.21.1 / NeoForge.

## Estado

Proyecto en desarrollo por fases. Las fases actuales ya incluyen configuración validada, integración de progresión RCT, persistencia de medallas, sincronización/GUI de cliente y comandos administrativos.

## Dependencias objetivo

- Minecraft 1.21.1
- NeoForge 21.1.x
- Cobblemon 1.8.1
- Radical Cobblemon Trainers 0.19.2-beta (`rctmod`)
- Radical Cobblemon Trainers API 0.16.1-beta (`rctapi`)
- Architectury 13.0.11
- Kotlin for Forge 5.12.0

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

## Permisos (Beta 2)

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

## Recompensas únicas por entrenador (Beta 4)

**Beta 13:** crea un jefe con loot y un segundo NPC que custodia un legendario. En la pestaña **Prueba**, vincula el ID del jefe. Solo quienes lo derroten acceden al segundo combate; al ganar reciben el Pokémon en el PC, con IV de 25–30 y posibilidad de shiny. El registro impide repetir el premio. Consulta [configuración, permisos y pruebas](docs/LEGENDARY-TRIALS-BETA13.md).

**Beta 12:** al crear un entrenador, el modo **Movimientos automáticos** permite elegir solo Pokémon, nivel y dificultad. Los ataques se preparan al guardar; IV y objetos siguen el preset de dificultad. El modo manual conserva los selectores y los equipos anteriores. Incluye la corrección del crash de diálogos de beta 11. Instala beta 12 en servidor y clientes.

**Beta 11:** corrige el crash de los entrenadores independientes al iniciar o terminar un combate con burbujas de diálogo activadas. Los diálogos guardados se regeneran automáticamente; no hace falta recrear los NPC. Se conserva el contenido de beta 10. Instala la misma beta en servidor y clientes.

**Beta 10:** el editor de equipo incluye **Pokémon…** y **Ataques…**, con búsqueda, páginas y selección de hasta cuatro movimientos de la especie. Se incluyen las seis skins entregadas por el administrador. Difícil y Jefe tienen IV perfectos y objetos equipados automáticos. Instala la misma beta en servidor y clientes. Consulta [los cambios y pruebas](docs/CREATOR-BETA10.md).

**Beta 9:** consulta la espera con Mayús + clic derecho en el entrenador. Los pagos repetibles muestran su nombre y cuándo volver. Crea definiciones independientes desde **Crear NPC → Definir entrenador desde cero**, con Pokémon, niveles, movimientos, dificultad, skins instaladas y diálogos. No se añaden a la cadena de medallas Rassvet. Consulta [el creador y sus límites](docs/CREATOR-BETA9.md).

**Beta 8:** el editor permite premios únicos o repetibles con espera por jugador, y cambiar el formato entre Individual y Doble. Consulta [repetición, migración y pruebas](docs/REPEAT-AND-DOUBLES-BETA8.md). Se conservan premios ya pagados y registros anteriores; el creador de equipos nuevos, skins y diálogos será una etapa posterior.

**Beta 6:** `/zianrct npc` abre una lista de NPC cargados en tu dimensión, con coordenadas y UUID para distinguir duplicados. Desde ella puedes crear, modificar o eliminar un NPC con confirmación, y mover el seleccionado al bloque y dirección del administrador. El editor conserva permanencia, movimiento y loot. Consulta [la guía del gestor](docs/NPC-MANAGER-BETA6.md). Instala beta.6 tanto en clientes como servidor.

Configura objetos con `/zianrct reward add-item <trainerId>` teniendo el stack en la mano, y monedas con `/zianrct reward set-money <trainerId> avecoins:coppercoin 5`. Cada premio es único por jugador y entrenador, conserva los componentes del objeto y sobrevive a reinicios. Los premios pendientes se consultan con `/zianrct reward pending` y se reclaman con `/zianrct reward claim <operationId>`.

La configuración inicial está vacía. Los pagos inciertos requieren revisión y no se repiten automáticamente. Los nuevos permisos administrativos, configuración y pruebas se explican en [la guía de recompensas](docs/TRAINER-REWARDS-BETA4.md). Los premios repetibles con espera y la creación de equipos nuevos son etapas posteriores.

**Beta 7:** `/zianrct reward claim ` autocompleta con Tab los IDs pendientes propios que pueden reclamarse. `/zianrct reward pending` también ofrece un botón **[Reclamar]** en el chat. Los premios pagados o bloqueados por revisión no se ofrecen para autocompletar.

## Medallero

**Beta 3:** las diez medallas predeterminadas muestran el render aprobado mediante una imagen original integrada, sin volver a generar los diseños. El medallero es compacto, tiene botón Cerrar y paginación según el espacio disponible. Conserva los identificadores, el progreso y el protocolo 2. Consulta [el arte y la validación](docs/MEDALS-BETA3.md).

Zian-RCT incluye arte original para las diez medallas del perfil Rassvet en `assets/zianrct/textures/gui/medals/`, junto con fondos de tarjeta para estados obtenida/bloqueada, overlay de hover y candado. La configuración por defecto ya apunta a `zianrct:textures/gui/medals/<id>.png`, por lo que no requiere un resource pack adicional para usar el arte incluido.

Cada definición de medalla admite el campo opcional `trainerName` para mostrar un nombre legible en el medallero. Si se omite, Zian-RCT lo deduce del id del entrenador, eliminando los prefijos `rassvet_leader_` o `rassvet_master_` cuando correspondan.

Las definiciones de medallas siguen pudiendo referenciar otras texturas mediante `ResourceLocation`. El servidor **no envía archivos PNG dentro del payload de red**: cualquier textura personalizada externa debe existir ya en los recursos del cliente, por ejemplo mediante un resource pack. Si una textura configurada no existe o no puede decodificarse como imagen, el medallero usa su representación visual de respaldo en lugar de mostrar la textura de error de Minecraft.

Los cambios del payload del medallero requieren que cliente y servidor ejecuten la misma versión de Zian-RCT. El protocolo actual del snapshot de medallas es la versión 2.

## Licencia e integración con RCT

El código y los recursos originales de Zian-RCT se publican bajo la licencia MIT.

RCT y RCTAPI son dependencias externas. Zian-RCT no incluye, copia, decompila ni adapta código o recursos de esos proyectos; la integración usa únicamente sus APIs y formatos públicos.

La información de licencia publicada para RCT/RCTAPI no ha sido completamente uniforme entre todos los metadatos históricos: se han observado referencias GNU-LGPL-3 en artefactos/metadatos anteriores, mientras que las páginas y fuentes actuales usan MCOML/Custom License. Antes de redistribuir RCT o RCTAPI, consulta siempre la licencia de sus proyectos oficiales. Esta licencia externa no cambia la licencia MIT del código propio de Zian-RCT.

Consulta `docs/DESIGN.md` para las decisiones técnicas y limitaciones verificadas.
