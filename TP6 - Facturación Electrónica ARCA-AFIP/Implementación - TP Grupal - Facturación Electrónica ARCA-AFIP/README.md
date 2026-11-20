# Sistema de Facturación Electrónica ARCA-AFIP (TP6)

Trabajo Práctico Grupal: **Facturación Electrónica ARCA-AFIP**.

Sistema integral de facturación de venta compuesto por un **backend** Spring Boot que expone una API REST profesional conectada en tiempo real a los servicios **WSAA** y **WSFEv1** de **ARCA / AFIP**, y un **frontend** modular en TypeScript + HTML5 (compilado con Vite) que ofrece una interfaz de usuario accesible y moderna.

---

## Estructura del proyecto

```
.
├── backend/                                  # API REST (Spring Boot 3 + JPA + PostgreSQL + JAX-WS)
│   ├── src/main/java/com/facturaarca/
│   │   ├── arca/                             # Clientes de integración SOAP y WSAA
│   │   │   ├── ArcaWsaaClient.java           # Login CMS firmado en PKCS#7 con BouncyCastle
│   │   │   ├── ArcaWsfeClient.java           # WSFEv1: FECompUltimoAutorizado y FECAESolicitar
│   │   │   └── ResultadoAutorizacionArca.java# DTO técnico del resultado fiscal
│   │   ├── wsfe/                             # Clases SOAP generadas automáticamente del WSDL oficial
│   │   ├── config/                           # Configuración MVC y CORS centralizada (WebConfig)
│   │   ├── controllers/                      # Controladores REST (*RestController) y ManejadorErrores
│   │   ├── dto/                              # DTOs de entrada, salida y paginación
│   │   ├── entities/                         # Entidades JPA del modelo de dominio
│   │   ├── enums/                            # EstadoFactura y reglas de negocio
│   │   ├── repository/                       # Repositorios Spring Data JPA y consultas dinámicas
│   │   ├── services/                         # Lógica de negocio (FacturaService, CatalogoService, ClienteService, ArcaService, FacturaPdfService, ReporteService)
│   │   └── util/                             # Utilidades de formato de comprobante y escape SQL
│   ├── src/main/resources/application.properties
│   ├── src/main/resources/poblar_db.sql      # Script de inicialización y datos demo
│   ├── afip_private.key                      # Clave privada RSA para WSAA (ignorado en git)
│   ├── afip_certificado.crt                  # Certificado X.509 de testing (ignorado en git)
│   └── build.gradle
└── frontend/                                 # Cliente Web (TypeScript + HTML5 + Vite)
    ├── config.ts                             # URL base de la API REST (VITE_API_URL)
    ├── index.html                            # Listado de facturas, filtros y acciones
    ├── listado.ts                            # Controlador de vista de listado
    ├── factura.html                          # Formulario de alta, edición y detalle
    ├── factura.ts                            # Controlador de vista de formulario
    ├── styles.css                            # Sistema de diseño y layout responsivo
    ├── components/                           # Componentes desacoplados
    │   ├── BuscadorArticuloModal.ts          # Modal paginado de artículos
    │   ├── BuscadorClienteModal.ts           # Modal con paginación integrada
    │   ├── NotificadorToast.ts               # Feedback no intrusivo (toasts)
    │   ├── PaginadorControl.ts               # Barra de paginación numérica
    │   └── TablaDetallesFactura.ts           # Tabla reactiva de artículos y totales
    ├── models/                               # Modelos de dominio (Factura, FacturaDetalle) y DTOs (api.dto.ts)
    ├── utils/                                # Utilidades de cálculo, formato y DOM
    ├── services/                             # Clientes HTTP REST desacoplados por dominio
    │   ├── FacturaService.ts                 # Operaciones de facturas y autorización ARCA
    │   ├── ClienteService.ts                 # Búsqueda paginada de clientes
    │   ├── ArticuloService.ts                # Artículos y listas de precios
    │   ├── CatalogoService.ts                # Puntos de venta, IVA y monedas
    │   └── ReporteService.ts                 # Descarga de reportes PDF y Excel
    ├── package.json
    └── vite.config.js                        # Configuración de Vite multipágina
```

---

## Máquina de Estados de Facturas

El ciclo de vida de cada factura se rige por el enum `EstadoFactura`:

| Estado | Editar | Informar ARCA | Anular | Eliminar |
| :--- | :---: | :---: | :---: | :---: |
| **BORRADOR** | Sí | Sí | Sí | Sí (baja física) |
| **EN_PROCESO** | No | No | No | No |
| **AUTORIZADA** | No | No | Sí | No |
| **RECHAZADA** | Sí | Sí | Sí | No |
| **ANULADA** | No | No | No | No |

- El sistema emite comprobantes **Factura C (COD. 011)** en **Pesos Argentinos ($)**.
- El número correlativo y la fecha de emisión son asignados cuando ARCA autoriza el comprobante; hasta ese momento permanecen nulos y la UI muestra el identificador provisorio `Borrador #ID`.
- Al informar a ARCA, el estado pasa transitoriamente a `EN_PROCESO`, se consulta el último comprobante autorizado, se solicita el CAE mediante WSFEv1 y se persiste como `AUTORIZADA` o `RECHAZADA` según la respuesta del fisco.
- Tanto en el envío individual como en el lote masivo (`/api/facturas/informar-arca-lote`), la orquestación corre íntegramente en el backend con transacciones independientes (`TransactionTemplate`): si una factura es rechazada o el usuario cierra el navegador, las demás continúan procesándose y las autorizadas conservan su CAE legal.
- Las facturas rechazadas permiten corregir cliente, detalles o listas de precios para su posterior reintento.
- Una factura autorizada no puede editarse ni eliminarse, pero admite anulación lógica en el sistema (`ANULADA`).

---

## Requisitos previos

- **Java 17 a 21** (recomendado JDK 21).
- **Node.js 18 o superior** con `npm`.
- **PostgreSQL 14 o superior** en ejecución.
- **Certificado Digital y Clave Privada de Homologación de ARCA / AFIP**.

---

## Configuración de Certificados y Credenciales ARCA / AFIP

Para que el backend pueda comunicarse con los servidores de homologación de ARCA / AFIP (WSAA para autenticación y WSFEv1 para autorización fiscal con CAE), se requieren **3 elementos fundamentales**:

### 1. Archivos de Certificado y Clave Privada
Por razones de seguridad y buenas prácticas, las claves privadas y los certificados digitales están excluidos del repositorio (`.gitignore`).
Deberán copiarse localmente dentro de la carpeta `backend/` con los siguientes nombres:
- `backend/afip_private.key`: Clave privada RSA generada con OpenSSL (sin passphrase o cifrado).
- `backend/afip_certificado.crt`: Certificado digital X.509 emitido por ARCA para el ambiente de homologación (Testing).

> **Aviso:** Si `ARCA_CUIT` está configurado pero faltan los archivos `.key` / `.crt`, el servidor arranca normalmente para la gestión local de borradores y catálogos, e informará de forma prolija que faltan las credenciales al intentar autorizar una factura mediante ARCA.

### 2. Variable de Entorno Obligatoria (`ARCA_CUIT`)
El backend requiere obligatoriamente que el CUIT emisor asociado al certificado esté definido en el entorno para arrancar. No posee valor por defecto para evitar arranques accidentales con identificadores fijos o no autorizados.

**Opción A: Ejecución por consola en Windows**
- **PowerShell:**
  ```powershell
  $env:ARCA_CUIT="TU_CUIT_SIN_GUIONES"
  cd backend
  .\gradlew.bat bootRun
  ```
- **CMD:**
  ```cmd
  set ARCA_CUIT=TU_CUIT_SIN_GUIONES
  cd backend
  gradlew.bat bootRun
  ```

**Opción B: Desde el IDE (IntelliJ IDEA / VS Code)**
- **IntelliJ IDEA:**
  1. Ir al menú superior: `Run` > `Edit Configurations...`
  2. Seleccionar la aplicación Spring Boot (`FacturaArcaApplication`).
  3. En la sección **Environment variables**, hacer clic en el botón de edición y agregar:
     - Nombre: `ARCA_CUIT`
     - Valor: `TU_CUIT_SIN_GUIONES` (ej. `20111111112`)
  4. Clic en `Apply` y ejecutar con el botón verde Play.
- **VS Code:**
  Dentro de `.vscode/launch.json`, configurar el parámetro `"env"`:
  ```json
  "env": {
    "ARCA_CUIT": "TU_CUIT_SIN_GUIONES"
  }
  ```

### 3. Caché de Tickets de Acceso (WSAA)
Al emitir la primera factura (o durante el arranque preventivo), el cliente se conecta al WSAA de AFIP, obtiene un Ticket de Acceso (Token y Sign) y lo almacena localmente en memoria y en `backend/ta_wsfe_cache.xml`. Este archivo tiene una validez de hasta 12 horas y está excluido de Git para preservar la confidencialidad de la sesión activa.

> 💡 **Autenticación en tiempo real:** Al arrancar el servidor backend, el servicio precarga y valida automáticamente el ticket de acceso WSAA para asegurar comunicación fluida con WSFEv1.

---

## Puesta en Marcha

### 1. Base de datos (PostgreSQL)

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

```bash
# Windows
gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

La API REST estará disponible en `http://localhost:8080/api/facturas`.

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
| `npm run build` | Chequea tipos (`tsc`) y compila los assets para producción. |
| `npm run preview` | Previsualiza el build de producción localmente. |

---

## Endpoints de la API REST

### Facturación

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/facturas` | Listado paginado con filtros (`fechaDesde`, `fechaHasta`, `estado`, `montoMinimo`, `clienteId`, `busqueda`, `informadaArca`, `pagina`, `tamanio`). |
| `GET` | `/api/facturas/{id}` | Detalle completo de una factura con sus artículos y totales. |
| `GET` | `/api/facturas/{id}/pdf` | Descarga el comprobante individual en PDF (Factura C con QR fiscal). |
| `POST` | `/api/facturas` | Crea una factura en estado `BORRADOR`. |
| `PUT` | `/api/facturas/{id}` | Edita cabecera y detalles de una factura editable (`BORRADOR` o `RECHAZADA`). |
| `DELETE` | `/api/facturas/{id}` | Baja física (solo facturas en estado `BORRADOR`). |
| `POST` | `/api/facturas/{id}/anular` | Anula una factura (permitido en `BORRADOR`, `RECHAZADA` y `AUTORIZADA`). |
| `POST` | `/api/facturas/{id}/informar-arca` | Envío de una factura a ARCA (solicita CAE ante WSFEv1 y persiste resultado). |
| `POST` | `/api/facturas/informar-arca-lote` | Envío masivo de comprobantes a ARCA (procesamiento atómico por factura en el servidor). |

### Catálogos y Búsqueda

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/clientes/buscar` | Búsqueda paginada de clientes por razón social o CUIT. |
| `GET` | `/api/articulos` | Búsqueda de artículos filtrada por lista de precios (mínimo 3 caracteres). |
| `GET` | `/api/articulos/buscar` | Búsqueda paginada de artículos por código, denominación o marca y lista de precios. |
| `GET` | `/api/articulos/{id}/listas-precio` | Listas de precios disponibles para un artículo. |
| `GET` | `/api/listas-precio` | Catálogo de listas de precios activas. |
| `GET` | `/api/puntos-venta` | Catálogo de puntos de venta configurados. |
| `GET` | `/api/condiciones-iva` | Catálogo de condiciones de IVA (AFIP). |
| `GET` | `/api/tipos-moneda` | Catálogo de monedas (Peso Argentino). |

### Reportes Consolidados

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/reportes/facturas/pdf` | Descarga directa del reporte consolidado en PDF (apaisado). |
| `GET` | `/api/reportes/facturas/excel` | Descarga directa del reporte consolidado en Microsoft Excel (.xlsx). |
