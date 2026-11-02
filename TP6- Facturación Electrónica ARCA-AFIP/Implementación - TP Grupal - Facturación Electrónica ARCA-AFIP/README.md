# Sistema de Facturación Electrónica ARCA-AFIP (TP6)

Trabajo Práctico Grupal: **Facturación Electrónica ARCA-AFIP**.

Sistema integral de facturación de venta compuesto por un **backend** Spring Boot que expone una API REST profesional y un **frontend** modular en TypeScript + HTML5 (compilado con Vite) que la consume.

---

## Estructura del proyecto

```
.
├── backend/                                  # API REST (Spring Boot + JPA + PostgreSQL)
│   ├── src/main/java/com/facturaarca/
│   │   ├── controllers/                      # Controladores REST (*RestController)
│   │   ├── dto/                              # DTOs de entrada, salida y paginación
│   │   ├── entities/                         # Entidades JPA del modelo de dominio
│   │   ├── enums/                            # EstadoFactura y reglas de negocio
│   │   ├── repository/                       # Repositorios Spring Data JPA y consultas personalizadas
│   │   └── services/                         # Lógica de facturación y reportes
│   ├── src/main/resources/application.properties
│   ├── src/main/resources/application-demo.properties
│   ├── db/poblar_db.sql
│   └── build.gradle
└── frontend/                                 # Cliente Web (TypeScript + HTML5 + Vite)
    ├── index.html                            # Listado de facturas, filtros y acciones
    ├── listado.ts                            # Controlador de vista de listado
    ├── nueva-factura.html                    # Formulario de alta, edición y detalle
    ├── nueva-factura.ts                      # Controlador de vista de formulario
    ├── styles.css                            # Sistema de diseño y layout responsivo
    ├── components/                           # Componentes desacoplados
    │   ├── BuscadorArticuloCombobox.ts       # Autocompletado accesible con debounce
    │   ├── BuscadorClienteModal.ts           # Modal con paginación integrada
    │   ├── NotificadorToast.ts               # Feedback no intrusivo
    │   ├── PaginadorControl.ts               # Barra de paginación numérica
    │   └── TablaDetallesFactura.ts           # Tabla reactiva de artículos y totales
    ├── utils/                                # Utilidades de cálculo, formato y DOM
    ├── models/api.dto.ts                     # DTOs sincronizados con el backend
    ├── services/FacturaService.ts            # Cliente HTTP REST centralizado
    ├── package.json
    └── vite.config.js                        # Configuración de Vite multipágina
```

---

## Máquina de Estados de Facturas

El ciclo de vida de cada factura se rige por el enum `EstadoFactura`:

| Estado | Editar | Informar ARCA | Anular | Eliminar |
| :--- | :---: | :---: | :---: | :---: |
| **BORRADOR** | Sí | Sí | Sí | Sí (baja lógica) |
| **EN_PROCESO** | No | No | No | No |
| **AUTORIZADA** | No | No | No | No |
| **RECHAZADA** | Sí | Sí | Sí | No |
| **ANULADA** | No | No | No | No |

- El número correlativo y la fecha de emisión son asignados cuando ARCA autoriza el comprobante; hasta ese momento permanecen nulos y la UI muestra el identificador provisorio `Borrador #ID`.
- Al informar a ARCA, el estado pasa a `EN_PROCESO`.
- Las facturas rechazadas permiten corregir cliente, detalles o listas de precios para su posterior reintento.

---

## Requisitos previos

- **Java 17 a 21** (recomendado JDK 21).
- **Node.js 18 o superior** con `npm`.
- **PostgreSQL 14 o superior** en ejecución.

---

## Puesta en Marcha

### 1. Base de datos

1. Crear la base de datos `factura_arca` en PostgreSQL:
   ```sql
   CREATE DATABASE factura_arca;
   ```

2. Configurar credenciales en `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/factura_arca
   spring.datasource.username=postgres
   spring.datasource.password=TU_PASSWORD
   ```

### 2. Backend (Spring Boot)

Desde la carpeta `backend/`:

- **Opción recomendada para demo/corrección** (perfil `demo`, carga datos de prueba iniciales):
  ```bash
  # Windows
  gradlew.bat bootRun --args=--spring.profiles.active=demo

  # Linux / macOS
  ./gradlew bootRun --args=--spring.profiles.active=demo
  ```

- **Uso estándar**:
  ```bash
  gradlew.bat bootRun    # Windows
  ./gradlew bootRun        # Linux / macOS
  ```

La API estará disponible en `http://localhost:8080/api/facturas`.

### 3. Frontend (Vite + TypeScript)

Desde la carpeta `frontend/`:

```bash
npm install       # Instala dependencias
npm run dev       # Inicia el servidor de desarrollo en http://localhost:5173
```

Abrir `http://localhost:5173` en el navegador.

#### Scripts disponibles en frontend:

| Script | Descripción |
| :--- | :--- |
| `npm run dev` | Inicia el servidor Vite en desarrollo con recarga rápida. |
| `npm run build` | Chequea tipos (`tsc`) y empaqueta la aplicación con Vite para producción. |
| `npm run preview` | Previsualiza el build de producción localmente. |

---

## Endpoints de la API REST

### Facturación

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/facturas` | Listado paginado con filtros (`fechaDesde`, `fechaHasta`, `estado`, `montoMinimo`, `clienteId`, `busqueda`, `informadaArca`, `pagina`, `tamanio`). |
| `GET` | `/api/facturas/{id}` | Detalle completo de una factura con sus artículos y totales. |
| `POST` | `/api/facturas` | Crea una factura en estado `BORRADOR`. |
| `PUT` | `/api/facturas/{id}` | Edita cabecera y detalles de una factura editable. |
| `DELETE` | `/api/facturas/{id}` | Baja lógica (solo facturas en estado `BORRADOR`). |
| `POST` | `/api/facturas/{id}/anular` | Anula una factura (permitido en `BORRADOR` y `RECHAZADA`). |
| `POST` | `/api/facturas/informar-arca` | Envío masivo de facturas a ARCA (pasa a `EN_PROCESO`). |
| `POST` | `/api/facturas/{id}/informar-arca` | Envío individual de una factura a ARCA. |

### Catálogos y Búsqueda

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/clientes/buscar` | Búsqueda paginada de clientes por razón social o CUIT. |
| `GET` | `/api/articulos` | Búsqueda de artículos filtrada por lista de precios (mínimo 3 caracteres). |
| `GET` | `/api/articulos/{id}/listas-precio` | Listas de precios disponibles para un artículo. |
| `GET` | `/api/puntos-venta` | Catálogo de puntos de venta configurados. |
| `GET` | `/api/condiciones-iva` | Catálogo de condiciones de IVA (AFIP). |
| `GET` | `/api/tipos-moneda` | Catálogo de monedas (Pesos, Dólar, etc.). |

### Reportes Consolidados

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/reportes/facturas/pdf` | Descarga directa del reporte consolidado en PDF (apaisado). |
| `GET` | `/api/reportes/facturas/excel` | Descarga directa del reporte consolidado en Microsoft Excel (.xlsx). |
