# Selector de equipo, skins y dificultad — beta.10

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
