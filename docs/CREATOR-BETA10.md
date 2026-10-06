# Selector de equipo, skins y dificultad — beta.10

Actualización beta.15: los NPC independientes también tienen respuestas para equipo por encima del tope, falta de Pokémon, combate ocupado, espera y otros rechazos. La respuesta usa los requisitos propios del NPC, sin atribuir el rechazo a una serie de medallas distinta. Si el equipo supera el tope, se muestra el nivel permitido. OP no elimina ese límite. Los diálogos guardados se regeneran sin recrear el NPC.

Guardar una definición nueva o cambiar su contenido recarga los datos del servidor y reconstruye los entrenadores de RCT. En servidores Youer con muchos mods puede provocar una pausa breve; el aviso de watchdog de 10 segundos observado en el log terminó con el registro y verificación de los entrenadores, sin un cierre del servidor. Esta versión corrige los diálogos; no modifica el watchdog ni mueve las operaciones del motor RCT fuera de su hilo de servidor.

Actualización beta.12: las definiciones nuevas empiezan en **Movimientos automáticos**. Basta elegir Pokémon, nivel y dificultad. Fácil prioriza ataques modestos y Normal ataques más fuertes, disponibles por nivel. Difícil/Jefe añaden movimientos legales por MT, tutor, huevo y evolución; priorizan potencia, precisión, ataques del mismo tipo y cobertura, y reservan una plaza de apoyo cuando existe. No es un optimizador competitivo ni exige que cuatro movimientos existan para especies con repertorios pequeños. Si no hay ningún movimiento disponible, el guardado se rechaza y permite pasar a Manual.

El servidor recalcula los ataques al guardar y conserva el resultado. Cambiar nivel o dificultad requiere volver a guardar. En automático, los campos muestran el repertorio anterior y están bloqueados; en Manual se pueden editar y usar los selectores. El campo opcional `autoMoves` está ausente en configuraciones antiguas: se interpreta como false, preservando sus ataques. El canal npc6 exige cliente/servidor beta.12 compatibles.

Actualización beta.11: los diálogos independientes incluyen un valor `translatable` además del texto literal. RCT utiliza directamente ese valor para su burbuja; omitirlo producía una excepción de texto nulo al iniciar/finalizar combates y después un crash secundario de cierre en Showdown. Se usa el texto mostrado como clave de búsqueda, por lo que funciona sin un archivo estático de traducciones para los diálogos editados en el servidor. Una coincidencia con una clave de idioma instalada puede traducirse según el mecanismo nativo de Minecraft.

Se regeneran los recursos de las definiciones guardadas sin borrar configuraciones, NPC, medallas ni historiales. La verificación de carga también construye los componentes de chat y burbuja con la API nativa. Prueba el inicio del combate, ganar, perder y volver a combatir, con las burbujas activadas. La corrección no sustituye el motor de Cobblemon ni convierte combates abortados en victorias.

Fuente de integración: [RCT ChatUtils](https://gitlab.com/srcmc/rct/mod/-/raw/1.21.1/common/src/main/java/com/gitlab/srcmc/rctmod/api/utils/ChatUtils.java), que pasa `Text.getTranslatable()` a `TrainerMob.addMessage()`.

Desde `/zianrct npc`, crea o modifica una definición propia y abre **Equipo**. Cada plaza conserva sus campos manuales y añade **Pokémon…** y **Ataques…**.

El selector de Pokémon utiliza las especies implementadas de Cobblemon y permite buscar por nombre traducido o identificador. Aplicar otro Pokémon sustituye solamente esa plaza y propone hasta cuatro movimientos de nivel disponibles al nivel indicado. Desmarcar la especie y aplicar vacía la plaza. Cancelar o Escape conserva los valores anteriores.

El selector de ataques utiliza `getAllLegalMoves()` de la forma estándar: incluye MT, tutor y otros movimientos legales, sin limitarlos al nivel elegido. Busca por nombre o identificador, marca hasta cuatro, y aplica. El tooltip muestra descripción, potencia, precisión y PP. Los campos manuales siguen permitiendo configuraciones especiales; el servidor mantiene su validación de existencia. Abrir un selector no modifica la definición persistida: hay que pulsar **Guardar definición** después.

El catálogo contiene los seis PNG proporcionados por el administrador, sin alterar sus píxeles: Explorador Ártico, Centinela Nocturno, Guardián Cian, Aventurero del Desierto, Guardabosques y Capitán Ámbar. Los índices antiguos 0–3 apuntan ahora a las primeras cuatro skins nuevas. Para conservar una apariencia antigua habría que restaurar expresamente el catálogo anterior; esta versión aplica la sustitución solicitada.

Las definiciones independientes usan estos ajustes automáticos:

- Fácil: IV 10, EV 0, sin objeto equipado.
- Normal: IV 20, EV 0, Baya Aranja (`oran_berry`).
- Difícil: IV 31, EV 64 por estadística, Baya Zidra (`sitrus_berry`).
- Jefe: IV 31, EV 84 por estadística, Restos (`leftovers`).

Se conservan los niveles elegidos y los ajustes de IA anteriores. Los objetos son equipo de combate, separados del loot. No se añaden objetos con retroceso, bloqueo de movimientos o dependencias de megaevolución. El nuevo balance también se aplica a definiciones independientes existentes al regenerar sus recursos; los entrenadores de Rassvet no cambian.

Instalar **beta.10 en servidor y clientes**: el canal del editor es `npc5` para impedir mezclar catálogos de skins incompatibles. No cambia el formato de recompensas ni los historiales de reclamaciones.

Prueba en juego: abrir selector, filtrar una especie, cancelar y verificar conservación; aplicar otra especie y revisar solo esa plaza; elegir cuatro ataques, intentar un quinto y cancelar; guardar y reiniciar; comprobar skin, niveles y objetos en un nuevo combate individual y doble. Las pruebas automatizadas verifican el JAR, las seis texturas y los IV/EV/objetos; no sustituyen la prueba visual y de batalla del cliente.
