# Sistema de Facturación — API REST (TP5)

Trabajo Práctico Grupal: **Operaciones de Escritura en API REST (POST, PUT, DELETE)**.

Sistema de facturación de venta compuesto por un **backend** Spring Boot que expone una API REST y un **frontend** en TypeScript + HTML5 que la consume.

## Estructura del proyecto

```
.
├── backend/    # API REST (Spring Boot + JPA + PostgreSQL)
│   ├── src/main/java/com/facturaarca/
│   ├── src/main/resources/application.properties
│   ├── db/poblar_db.sql
│   └── build.gradle
└── frontend/   # Cliente (TypeScript + HTML5)
    ├── index.html          # Búsqueda y listado de facturas
    ├── listado.ts
    ├── nueva-factura.html  # Alta de factura
    ├── nueva-factura.ts
    ├── styles.css
    ├── models/factura.dto.ts
    ├── services/FacturaService.ts
    ├── package.json
    └── tsconfig.json
```

## Requisitos previos

- **Java 17 a 21** (recomendado JDK 21). Spring Boot 3.2.5, con el wrapper de Gradle 8.7, **no arranca en JDK 26 o superior**.
  Si `java -version` muestra una versión más nueva, apuntá `JAVA_HOME` a un JDK 21 (o definí `org.gradle.java.home` en `backend/gradle.properties`).
- **Node.js 18 o superior** con `npm`.
- **PostgreSQL 14 o superior** en ejecución.

## Backend

1. Crear la base de datos `factura_arca` en PostgreSQL:

   ```sql
   CREATE DATABASE factura_arca;
   ```

2. Configurar las credenciales de conexión en `backend/src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/factura_arca
   spring.datasource.username=postgres
   spring.datasource.password=CAMBIAR
   ```

3. Iniciar la API una primera vez para que Hibernate cree el esquema (desde la carpeta `backend/`):

   ```bash
   # Linux / macOS
   ./gradlew bootRun

   # Windows
   gradlew.bat bootRun
   ```

   El esquema `model` y sus tablas se crean/actualizan automáticamente
   (`spring.jpa.hibernate.ddl-auto=update`).

4. **Con la API detenida**, cargar datos de ejemplo (opcional). El script hace
   `TRUNCATE`/`INSERT` sobre tablas que ya deben existir, por eso se ejecuta
   recién después del primer arranque:

   ```bash
   # Desde la raíz del repositorio (pgAdmin o la consola psql)
   psql -U postgres -d factura_arca -f backend/db/poblar_db.sql
   ```

5. Volver a iniciar la API:

   ```bash
   ./gradlew bootRun        # o gradlew.bat bootRun
   ```

   La API queda disponible en `http://localhost:8080/api/facturas`.

### Endpoints

| Método | Ruta                 | Descripción                                       | Respuesta      |
|--------|----------------------|---------------------------------------------------|----------------|
| GET    | `/api/facturas`      | Lista facturas (con filtros)                      | 200 OK         |
| POST   | `/api/facturas`      | Crea una factura en cascada                       | 201 Created    |
| PUT    | `/api/facturas/{id}` | Actualiza cabecera/detalles; con `"estado": "ANULADA"` anula la factura | 200 OK |
| DELETE | `/api/facturas/{id}` | Da de baja **lógicamente** la factura (setea `fecha_baja`; deja de listarse pero queda registrada) | 204 No Content |

Filtros de `GET`: `fechaDesde`, `fechaHasta`, `estado`, `montoMinimo`
(`fechaHasta` incluye todo el día indicado). El listado y el reporte excluyen
las facturas dadas de baja.

Los renglones se envían con `articuloId` y `listaPrecioArticuloId`: el backend
valida que esa lista de precios pertenezca al artículo y toma de ahí el precio
de venta. El cliente nunca envía precios ni importes. Las listas disponibles por
artículo se consultan en `GET /api/articulos/{id}/listas-precio` (lo usa el
selector del formulario de alta).

Los errores de la API se devuelven siempre con el mismo formato:

```json
{ "mensaje": "Factura no encontrada con id: 99" }
```


## Frontend

Desde la carpeta `frontend/`:

```bash
npm install       # instala dependencias (TypeScript)
npm run build     # compila los .ts a .js
npm run serve     # sirve la app en http://localhost:5173
```

La app tiene dos páginas: `index.html` (búsqueda y listado de facturas) y `nueva-factura.html` (alta de factura), que se enlazan entre sí.

Luego abrir `http://localhost:5173` en el navegador. El backend debe estar corriendo.

### Scripts disponibles

| Script          | Descripción                                        |
|-----------------|----------------------------------------------------|
| `npm run build` | Compila TypeScript (`tsc`).                        |
| `npm run watch` | Recompila automáticamente ante cambios.            |
| `npm run serve` | Sirve la carpeta en `http://localhost:5173`.       |

## Notas

- La interfaz está dividida en dos páginas (`listado` y `alta`) en lugar de un
  único `index.html`: misma funcionalidad, pero con menos estado global y
  responsabilidades más claras por archivo.
- Los archivos `.js` son el resultado de compilar los `.ts` y **no se versionan**: hay que ejecutar `npm run build` (o `npm run watch`) antes de servir la app.
- `node_modules/` tampoco se versiona; se restaura con `npm install`.
- El backend permite CORS desde cualquier origen (`@CrossOrigin(origins = "*")`), por lo que el frontend puede servirse en cualquier puerto.
- Al iniciar, la aplicación genera un reporte PDF y uno Excel de todas las
  facturas en `backend/reportes/` (ver `ReporteService`).
- La auditoría (`usuarioCarga` / `usuarioModificacion`) toma el primer usuario de
  la tabla `usuarios`, ya que el TP no incluye autenticación.
