# Zian-RCT

Tope de nivel, progresión administrativa y sistema de medallas sobre Radical Cobblemon Trainers para Minecraft 1.21.1 / NeoForge.

## Estado

Proyecto en desarrollo por fases. Las fases actuales ya incluyen configuración validada, integración de progresión RCT, persistencia de medallas y sincronización/GUI de cliente.

## Dependencias objetivo

- Minecraft 1.21.1
- NeoForge 21.1.x
- Cobblemon 1.8.1
- Radical Cobblemon Trainers 0.19.2-beta (`rctmod`)
- Radical Cobblemon Trainers API 0.16.1-beta (`rctapi`)
- Architectury 13.0.11
- Kotlin for Forge 5.12.0

## Medallas y texturas personalizadas

Las definiciones de medallas pueden referenciar una textura mediante `ResourceLocation`, pero el servidor **no envía archivos PNG dentro del payload de red**. La textura debe existir ya en los recursos del cliente.

Para servidores públicos, las medallas con arte propio deben distribuirse mediante un resource pack. La opción recomendada es configurar el resource pack del servidor en `server.properties` (`resource-pack`, `resource-pack-sha1` y, si se desea exigirlo, `require-resource-pack=true`). Si una textura configurada no existe en el cliente, Zian-RCT usa su representación visual de respaldo.

## Licencia e integración con RCT

El código y los recursos originales de Zian-RCT se publican bajo la licencia MIT.

RCT y RCTAPI son dependencias externas. Zian-RCT no incluye, copia, decompila ni adapta código o recursos de esos proyectos; la integración usa únicamente sus APIs y formatos públicos.

La información de licencia publicada para RCT/RCTAPI no ha sido completamente uniforme entre todos los metadatos históricos: se han observado referencias GNU-LGPL-3 en artefactos/metadatos anteriores, mientras que las páginas y fuentes actuales usan MCOML/Custom License. Antes de redistribuir RCT o RCTAPI, consulta siempre la licencia de sus proyectos oficiales. Esta licencia externa no cambia la licencia MIT del código propio de Zian-RCT.

Consulta `docs/DESIGN.md` para las decisiones técnicas y limitaciones verificadas.
