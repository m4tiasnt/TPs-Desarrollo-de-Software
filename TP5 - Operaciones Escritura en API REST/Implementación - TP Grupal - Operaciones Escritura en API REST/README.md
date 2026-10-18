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
    ├── index.html
    ├── app.ts
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

   Opcionalmente, cargar datos de ejemplo con `backend/db/poblar_db.sql`.

2. Configurar las credenciales de conexión en `backend/src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/factura_arca
   spring.datasource.username=postgres
   spring.datasource.password=CAMBIAR
   ```

3. Iniciar la API (desde la carpeta `backend/`):

   ```bash
   # Linux / macOS
   ./gradlew bootRun

   # Windows
   gradlew.bat bootRun
   ```

   La API queda disponible en `http://localhost:8080/api/facturas`.
   El esquema de tablas se crea/actualiza automáticamente (`spring.jpa.hibernate.ddl-auto=update`).

### Endpoints

| Método | Ruta                 | Descripción                          | Respuesta      |
|--------|----------------------|--------------------------------------|----------------|
| GET    | `/api/facturas`      | Lista facturas (con filtros)         | 200 OK         |
| POST   | `/api/facturas`      | Crea una factura en cascada          | 201 Created    |
| PUT    | `/api/facturas/{id}` | Actualiza estado / observaciones     | 200 OK         |
| DELETE | `/api/facturas/{id}` | Elimina una factura                  | 204 No Content |

Filtros de `GET`: `fechaDesde`, `fechaHasta`, `estado`, `montoMinimo`.

## Frontend

Desde la carpeta `frontend/`:

```bash
npm install       # instala dependencias (TypeScript)
npm run build     # compila los .ts a .js
npm run serve     # sirve la app en http://localhost:5173
```

Luego abrir `http://localhost:5173` en el navegador. El backend debe estar corriendo.

### Scripts disponibles

| Script          | Descripción                                        |
|-----------------|----------------------------------------------------|
| `npm run build` | Compila TypeScript (`tsc`).                        |
| `npm run watch` | Recompila automáticamente ante cambios.            |
| `npm run serve` | Sirve la carpeta en `http://localhost:5173`.       |

## Notas

- Los archivos `.js` son el resultado de compilar los `.ts` y **no se versionan**: hay que ejecutar `npm run build` (o `npm run watch`) antes de servir la app.
- `node_modules/` tampoco se versiona; se restaura con `npm install`.
- El backend permite CORS desde cualquier origen (`@CrossOrigin(origins = "*")`), por lo que el frontend puede servirse en cualquier puerto.
- Los reportes PDF se generan en `backend/reportes/`.
