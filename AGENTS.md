# json-tree

Proyecto desktop JavaFX + Spring Boot para visualizar archivos JSON como árbol ASCII.

## Arquitectura
- `src/main/java/com/davidpe/jsontree/bootstrap`: arranque JavaFX + Spring y wiring principal.
- `src/main/java/com/davidpe/jsontree/application`: casos de uso, puertos y orquestación.
- `src/main/java/com/davidpe/jsontree/domain`: modelo puro del negocio.
- `src/main/java/com/davidpe/jsontree/infrastructure`: adaptadores técnicos y persistencia local.
- `src/main/java/com/davidpe/jsontree/ui`: controladores JavaFX y sistema de pantallas.
- `src/main/resources/com/davidpe/jsontree/ui`: FXML y estilos de interfaz.

## Reglas
- Mantener separación estricta entre capas.
- No introducir lógica de negocio en controladores JavaFX.
- La persistencia del histórico debe seguir siendo basada en archivos, no base de datos de aplicación.
- Cualquier nueva pantalla debe integrarse mediante `UiScreen`, `UiScreenFactory` y `UiFlowManager`.
