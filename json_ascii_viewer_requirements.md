# Requisitos — JSON → ASCII Tree Viewer

## Objetivo
Crear una app pequeña y minimalista para visualizar cualquier archivo JSON como un árbol ASCII legible. La app debe permitir arrastrar un archivo JSON sobre la ventana, validarlo, renderizarlo como árbol, navegar archivos grandes con scroll y mantener un histórico local de archivos importados.

## Inspiración visual
Basarse en el último screenshot aprobado: una ventana flotante oscura, limpia y compacta, con estética de herramienta developer.

La interfaz debe sentirse como un popup o mini app:

- Fondo negro / gris muy oscuro.
- Ventana centrada con bordes redondeados.
- Borde exterior sutil en violeta.
- Header compacto con icono, título `JSON → TREE` y botón de cerrar.
- Sin zona dedicada de drag and drop.
- Toda la ventana debe aceptar drag and drop.
- Tipografía monoespaciada para el árbol.
- Colores de sintaxis suaves:
  - claves en blanco/gris claro,
  - strings en naranja/violeta,
  - números en verde,
  - booleanos en verde,
  - arrays con contador azul/cian,
  - estado válido en verde.

## Pantalla principal — Viewer

### Estructura general
La pantalla principal debe contener:

1. Header superior.
2. Barra de archivo cargado.
3. Visor ASCII del JSON.
4. Footer con acciones y estado.

No debe existir un bloque grande de carga ni una caja punteada para drag and drop. El archivo se carga arrastrándolo sobre cualquier punto de la ventana.

### Header
Debe mostrar:

- Icono de JSON o llaves `{}`.
- Título: `JSON → TREE`.
- Botón de cerrar en la esquina superior derecha.

Opcional:

- Botón para abrir histórico.
- Botón para minimizar o volver.

### Barra de archivo
Debajo del header debe aparecer una fila compacta con:

- Icono de archivo.
- Nombre del archivo original, por ejemplo `data.json`.
- Tamaño del archivo, por ejemplo `2.4 KB`.
- Estado de validación:
  - `Válido` en verde si el JSON es correcto.
  - `Inválido` en rojo si hay error.
- Botón para eliminar/quitar el archivo actual.

La barra debe ser baja, no ocupar espacio innecesario.

### Visor ASCII
El área principal debe mostrar el JSON transformado a un árbol ASCII.

Ejemplo visual esperado:

```txt
root
  └─ user
     ├─ id: 12345
     ├─ name: "Terax"
     ├─ email: "terax@dev"
     └─ created_at: "2024-05-15T14:32:00Z"
  └─ bookmarks [3]
     ├─ 0
     │  ├─ title: "TERAX.md - Project Overview"
     │  └─ url: "https://github.com/terax/terax"
     ├─ 1
     │  ├─ title: "Vercel - Ship faster websites"
     │  └─ url: "https://vercel.com"
     └─ 2
        ├─ title: "Linear - Issue tracking"
        └─ url: "https://linear.app"
  └─ tags [2]
     ├─ 0: "dev"
     └─ 1: "productivity"
  └─ settings
     ├─ dark_mode: true
     └─ version: "1.0.0"
```

Características del visor:

- Scroll vertical para archivos grandes.
- Scroll horizontal si las líneas son muy largas.
- Mantener indentación clara.
- Mostrar arrays con contador: `items [12]`.
- Mostrar objetos como ramas.
- Mostrar valores primitivos inline.
- Soportar strings, números, booleanos, null, arrays y objetos.
- Resaltar valores importantes mediante color.
- Usar un contenedor con borde sutil y fondo ligeramente más oscuro.

### Footer
El footer debe mostrar:

- Acción `Copiar` para copiar el árbol ASCII completo.
- Número de líneas generadas, por ejemplo `200 líneas`.
- Estado `JSON válido` con punto verde.

Opcional:

- Botón `Guardar como .txt`.
- Botón `Ver histórico`.

## Drag and drop

La ventana completa debe aceptar archivos por drag and drop.

Comportamiento esperado:

- Al arrastrar un archivo sobre la ventana, mostrar un overlay ligero indicando que se puede soltar.
- No reservar espacio fijo para drag and drop.
- Al soltar un archivo:
  - verificar que sea `.json` o que el contenido sea JSON válido,
  - leerlo localmente,
  - validarlo,
  - convertirlo a árbol ASCII,
  - mostrarlo inmediatamente en el viewer.

Si el JSON no es válido:

- Mostrar estado `Inválido`.
- Mostrar mensaje de error claro.
- Indicar línea/columna si es posible.
- No añadirlo al histórico como archivo válido, salvo que se quiera guardar intentos fallidos de forma explícita.

## Validación JSON

La app debe validar el JSON antes de renderizar.

Estados:

- Sin archivo: pantalla vacía compacta o mensaje mínimo.
- Archivo válido: renderiza árbol y muestra estado verde.
- Archivo inválido: muestra error y estado rojo.

El error debe ser útil:

```txt
JSON inválido
Unexpected token } at line 18, column 4
```

## Histórico

Debe existir una segunda pantalla para ver el histórico local.

### Acceso
Desde la pantalla principal debe poder abrirse el histórico mediante:

- botón/icono en header,
- o acción en footer.

### Pantalla de histórico
Debe listar todos los archivos JSON importados previamente.

Cada item debe mostrar:

- nombre uniforme generado por la app,
- nombre original del archivo,
- fecha de importación,
- tamaño,
- número de líneas del árbol generado,
- estado de validación,
- acción para borrar.

Ejemplo:

```txt
2025-01-18_14-32-08_data.json
Original: data.json
2.4 KB · 200 líneas · JSON válido
```

Al hacer click en cualquier archivo del histórico:

- se abre de nuevo en la pantalla principal,
- se muestra el árbol ASCII ya generado o se vuelve a generar desde el JSON guardado,
- se conserva la experiencia visual del viewer.

### Borrado de histórico
El usuario debe poder borrar cualquier archivo del histórico.

Requisitos:

- Borrar un item individual.
- Confirmar antes de borrar, o permitir deshacer.
- Si el archivo abierto actualmente se borra desde el histórico, el viewer debe quedar vacío o mostrar otro archivo seleccionado.

Opcional:

- Botón `Borrar todo el histórico`.

## Persistencia local

No usar base de datos.

La persistencia debe ser local, basada en archivos.

Al importar un JSON válido, la app debe guardar una copia local del archivo en una carpeta gestionada por la app.

### Estructura sugerida

```txt
/app-data/
  history/
    2025-01-18_14-32-08_data.json
    2025-01-18_15-10-22_config.json
  metadata.json
```

### Nombres uniformes
Los archivos guardados deben nombrarse con fecha uniforme:

```txt
YYYY-MM-DD_HH-mm-ss_nombre-original.json
```

Ejemplo:

```txt
2025-01-18_14-32-08_data.json
```

Reglas:

- Mantener nombre original sanitizado.
- Evitar espacios conflictivos.
- Evitar caracteres especiales problemáticos.
- Si hay colisión, añadir sufijo incremental.

Ejemplo:

```txt
2025-01-18_14-32-08_data.json
2025-01-18_14-32-08_data_2.json
```

### Metadata
Aunque no haya base de datos, puede existir un archivo `metadata.json` para acelerar el histórico.

Ejemplo:

```json
[
  {
    "id": "2025-01-18_14-32-08_data.json",
    "originalName": "data.json",
    "storedName": "2025-01-18_14-32-08_data.json",
    "importedAt": "2025-01-18T14:32:08Z",
    "sizeBytes": 2457,
    "lineCount": 200,
    "valid": true
  }
]
```

Si `metadata.json` no existe o está corrupto, la app debe poder reconstruir el histórico leyendo la carpeta `history/`.

## Conversión JSON → ASCII Tree

La app debe incluir una función que convierta cualquier JSON en texto ASCII.

Reglas:

- La raíz se muestra como `root`.
- Los objetos se muestran como ramas con el nombre de la clave.
- Los arrays se muestran como ramas con contador `[n]`.
- Los elementos de arrays se muestran por índice.
- Los valores primitivos se muestran en la misma línea.
- `null` debe mostrarse como `null`.
- No truncar por defecto, pero soportar scroll.

Ejemplo:

```txt
root
├─ user
│  ├─ id: 12345
│  ├─ name: "Terax"
│  └─ active: true
└─ tags [2]
   ├─ 0: "dev"
   └─ 1: "productivity"
```

## Requisitos de UX

- La app debe abrir rápido.
- No debe sentirse como una app pesada.
- Debe funcionar con teclado y ratón.
- El árbol debe ser fácil de copiar.
- Los archivos grandes no deben bloquear la UI.
- El usuario debe entender inmediatamente si el JSON es válido.
- El historial debe ser simple, sin carpetas complejas.
- El diseño debe evitar ruido visual.

## Estados de pantalla

### Estado vacío
Cuando no hay archivo cargado:

- Mostrar ventana limpia.
- Mensaje discreto: `Suelta un JSON en cualquier parte`.
- No mostrar una zona grande de carga.

### Estado cargado
Cuando hay archivo válido:

- Mostrar barra de archivo.
- Mostrar árbol ASCII.
- Mostrar footer con copiar, líneas y validación.

### Estado inválido
Cuando el JSON no se puede parsear:

- Mostrar barra de archivo.
- Mostrar estado rojo `Inválido`.
- Mostrar panel de error.
- No renderizar árbol.

### Estado histórico
Pantalla con listado de archivos previos y acciones.

## Acciones principales

- Arrastrar JSON sobre la ventana.
- Seleccionar archivo manualmente opcionalmente.
- Validar JSON.
- Generar árbol ASCII.
- Copiar árbol ASCII.
- Ver histórico.
- Abrir archivo del histórico.
- Borrar archivo del histórico.
- Cerrar ventana.

## Criterios de aceptación

- Al soltar un JSON válido sobre cualquier parte de la ventana, se muestra el árbol ASCII.
- No existe una zona de drag and drop ocupando espacio fijo en la interfaz final.
- El viewer permite scroll vertical y horizontal.
- El estado de validación se ve claramente.
- El botón copiar copia el árbol completo.
- Cada JSON válido importado se guarda localmente como archivo.
- Los nombres guardados siguen el formato `YYYY-MM-DD_HH-mm-ss_nombre-original.json`.
- La pantalla de histórico lista los archivos guardados.
- Al hacer click en un item del histórico se vuelve a abrir el JSON en el viewer.
- Se puede borrar cualquier item del histórico.
- La app no usa base de datos.

## Notas visuales finales

El diseño debe mantener la dirección del último screenshot:

- Modal grande pero compacto.
- Mucho espacio dedicado al árbol, no a la carga.
- Header fino.
- Barra de archivo mínima.
- Viewer protagonista.
- Footer discreto.
- Colores oscuros con acento violeta.
- Validación verde clara.
- Estilo developer, elegante y funcional.
