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
│   │   ├── controllers/                      # Controladores REST (*RestController)
│   │   ├── dto/                              # DTOs de entrada, salida y paginación
│   │   ├── entities/                         # Entidades JPA del modelo de dominio
│   │   ├── enums/                            # EstadoFactura y reglas de negocio
│   │   ├── repository/                       # Repositorios Spring Data JPA
│   │   └── services/                         # Lógica de facturación (FacturaService), ARCA (ArcaService) y reportes
│   ├── src/main/resources/application.properties
│   ├── db/poblar_db.sql                      # Script de inicialización y datos demo
│   ├── afip_private.key                      # Clave privada RSA para WSAA (ignorado en git)
│   ├── afip_certificado.crt                  # Certificado X.509 de testing (ignorado en git)
│   └── build.gradle
└── frontend/                                 # Cliente Web (TypeScript + HTML5 + Vite)
    ├── index.html                            # Listado de facturas, filtros y acciones
    ├── listado.ts                            # Controlador de vista de listado
    ├── factura.html                          # Formulario de alta, edición y detalle
    ├── factura.ts                            # Controlador de vista de formulario
    ├── styles.css                            # Sistema de diseño y layout responsivo
    ├── components/                           # Componentes desacoplados
    │   ├── BuscadorArticuloCombobox.ts       # Autocompletado accesible con debounce
    │   ├── BuscadorClienteModal.ts           # Modal con paginación integrada
    │   ├── NotificadorToast.ts               # Feedback no intrusivo (toasts)
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
- Al informar a ARCA, el estado pasa transitoriamente a `EN_PROCESO`, se consulta el último comprobante autorizado, se solicita el CAE mediante WSFEv1 y se persiste como `AUTORIZADA` o `RECHAZADA` según la respuesta del fisco.
- Las facturas rechazadas permiten corregir cliente, detalles o listas de precios para su posterior reintento.

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

> **Aviso:** Si el servidor se inicia sin estos archivos, la aplicación funcionará normalmente para la gestión local de borradores y catálogos, pero al momento de intentar autorizar una factura mediante ARCA se informará de forma prolija que faltan las credenciales.

### 2. Variable de Entorno Obligatoria (`ARCA_CUIT`)
El backend requiere que el CUIT emisor asociado al certificado esté definido en el entorno. No posee valor por defecto para evitar arranques accidentales con identificadores no autorizados.

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
     - Valor: `TU_CUIT_SIN_GUIONES` (ej. `20468665647`)
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
| `POST` | `/api/facturas` | Crea una factura en estado `BORRADOR`. |
| `PUT` | `/api/facturas/{id}` | Edita cabecera y detalles de una factura editable (`BORRADOR` o `RECHAZADA`). |
| `DELETE` | `/api/facturas/{id}` | Baja lógica (solo facturas en estado `BORRADOR`). |
| `POST` | `/api/facturas/{id}/anular` | Anula una factura (permitido en `BORRADOR` y `RECHAZADA`). |
| `POST` | `/api/facturas/{id}/informar-arca` | Envío de una factura a ARCA (solicita CAE ante WSFEv1 y persiste resultado). |

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
