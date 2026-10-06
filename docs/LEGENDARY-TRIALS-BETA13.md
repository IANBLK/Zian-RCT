# Prueba de jefe y premio legendario — beta.13

Instala beta.13 en cliente y servidor. Se conserva el progreso anterior; la pestaña nueva requiere el canal npc7.

## Configuración desde el juego

1. En `/zianrct npc`, define un entrenador propio con dificultad **JEFE**, equipo, nombre y diálogos. Guarda, espera la recarga y crea su NPC permanente. Ejemplo: `zian_custom_prueba_jefe`.
2. Desde **Modificar → Loot** configura sus monedas/objetos. El loot y el desbloqueo son independientes: se mantiene la política única/repetible del loot que ya uses.
3. Define otro entrenador propio, por ejemplo `zian_custom_guardian_mewtwo`. Pon un solo Pokémon legendario o singular implementado en **Equipo**, su nivel, y formato **Individual**. Puede usar movimientos automáticos.
4. En **Prueba**, activa la prueba legendaria e introduce el ID exacto del jefe del paso 1. La probabilidad shiny es **1 entre el número indicado**, 512 por defecto. Guarda, espera la recarga y crea el segundo NPC permanente.

La victoria real contra el jefe crea un desbloqueo permanente por UUID de jugador y segundo entrenador. Las victorias anteriores a configurar la prueba no desbloquean retroactivamente el NPC. Las medallas concedidas por comandos tampoco desbloquean la prueba. No se crean NPC automáticamente.

Quien no pasó la prueba recibe una indicación del jefe requerido. Quien sí la pasó puede iniciar el segundo combate. El diálogo de inicio anuncia que demostró su valía y obtuvo la oportunidad de luchar con el legendario. En esta primera versión ese saludo es fijo para las pruebas; los demás diálogos siguen editables.

Perder permite volver a intentarlo después de la espera nativa del NPC. Al ganar se entrega un Pokémon nuevo de la especie y nivel configurados, con IV individuales entre **25 y 30**, y shiny según la tirada congelada. No se copia el objeto equipado del rival. El Pokémon se entrega directamente al **PC**, no mediante una captura con Poké Ball. No cambia el tope de nivel de RCT ni la cadena de medallas.

## Registro y recuperación

El registro del mundo `data/zianrct-legendary-trials.json` contiene dueño, jefe, especie, nivel, probabilidades, UUID de ambos combates, UUID del Pokémon, seis IV, shiny y fase. Los datos del premio se fijan antes de añadirlo al PC: liberar espacio, reconectar o reclamar no vuelve a tirar los IV o shiny.

Si el PC está lleno, el premio queda pendiente. Usa `/zianrct legendary status` y `/zianrct legendary claim` después de liberar espacio. Una prueba con premio pendiente o ya entregado no abre otro combate. Al confirmar la entrega queda cerrada para ese jugador, incluso si mueve, intercambia o libera posteriormente el Pokémon.

La confirmación automática admite el proveedor nativo de archivos de Cobblemon 1.8.1 que administra ese PC. Se descubre el proveedor mediante lectura de metadatos y se usan sus API para añadir, guardar y releer. Los proveedores de base de datos o almacenamiento de terceros sin confirmación compatible conservan el premio pendiente antes de mutar el PC. No se cambia la configuración de almacenamiento.

Una entrega iniciada queda bloqueada hasta releer el mismo UUID, especie, shiny e IV en el PC o equipo guardado. Si no se confirma, pasa a revisión. No se vuelve a generar el Pokémon ni se revierte a pendiente automáticamente. `/zianrct legendary claim` puede cerrar una revisión si encuentra evidencia nativa de ese Pokémon; no vuelve a entregarlo.

Si falta esa evidencia, un administrador revisa el registro y el historial y, si corresponde, compensa manualmente. Solo después usa `/zianrct legendary confirm-delivered <jugador> <ID del segundo entrenador> <evidencia de 8–256 caracteres>`. Este comando cierra una revisión y registra el operador/evidencia en el log; no genera ningún Pokémon. No hay comando para reiniciar una prueba pagada.

No cambies manualmente los registros ni restaures solo parte de los datos del mundo. Una vez existe progreso, el editor bloquea cambios de jefe, especie, nivel o probabilidades de esa prueba; para un reto diferente crea un ID nuevo. El jefe vinculado debe seguir en dificultad JEFE.

## Permisos

- `zianrct.legendary.claim`: combatir, consultar y reclamar pruebas propias; permitido por defecto si no hay denegación de LuckPerms.
- `zianrct.admin.legendary.configure`: vincular/desvincular una prueba desde el creador; además necesita los permisos anteriores del editor (`zianrct.admin.npc.edit` y `zianrct.admin.trainer.create`).
- `zianrct.admin.legendary.resolve`: cerrar entregas en revisión con evidencia. Los nodos administrativos usan OP nivel 2 por defecto y respetan denegaciones explícitas.

El desbloqueo es un registro interno, no se concede un permiso global de LuckPerms. Los datos de un jugador no permiten reclamar la recompensa de otro.

## Validación

Pruebas automáticas: desbloqueo por jugador, victoria sin prueba rechazada, UUID/IV/shiny congelados, imposibilidad de reabrir entregas, persistencia tras reiniciar, corrupción y fallos de escritura. CI verifica dos definiciones propias, incluyendo el legendario y su jefe, así como los diálogos y movimientos nativos. En el mundo aislado de CI, `ZIANRCT_LEGENDARY_SMOKE=true` activa una cuenta sintética sin conexión para ejecutar desbloqueo, victoria, entrega real al PC, relectura de UUID/IV/shiny y bloqueo de otra recompensa. No se activa en servidores normales.

Prueba en juego con dos cuentas sin OP: intentar el segundo NPC antes del jefe, derrotar al jefe y comprobar su loot, volver al segundo y perder, reintentar y ganar, confirmar el premio en el PC, reiniciar e intentar reclamar otra vez. La otra cuenta debe seguir bloqueada. Probar también PC lleno y liberación de espacio. CI no simula una batalla de un cliente real ni la entrega completa al almacenamiento de un jugador conectado.
