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

## Medallas y texturas personalizadas

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
