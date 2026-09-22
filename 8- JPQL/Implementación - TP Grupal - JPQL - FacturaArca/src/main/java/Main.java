
import com.facturaarca.entities.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Main {
    private static final boolean CARGAR_DATOS = false; // Cambiar a true para cargar datos de prueba

    public static void main(String[] args) {
        if (CARGAR_DATOS) {
            cargarDatosDePrueba();
        } else {
            System.out.println("La carga de datos de prueba está deshabilitada. Cambie CARGAR_DATOS a true para habilitarla.");
        }
        // Iniciar el contenedor de JPA
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            // -- TP3 - CONSULTAS --

            // -- Nivel 1: Consultas Básicas y Proyecciones --

            // 1. Consulta de Entidades Completas
            // Consigna: Obtener la lista completa de todas las facturas de venta registradas en el sistema. */
            System.out.println("\n=== RESULTADOS EJERCICIO 1 ===");
            List<FacturaVenta> resultados1 = em.createQuery("SELECT factura FROM FacturaVenta factura", FacturaVenta.class).getResultList();

            for (FacturaVenta f : resultados1) {
                System.out.println("- Factura Nro: " + f.getNumero() + " | Fecha Emisión: " + f.getFechaEmision() + " Cliente: " + f.getCliente().getDenominacion() + " | Total: $" + f.getImporteTotal());
            }

            // --------------------------------------------------

            // 2. Proyección de Atributos Específicos
            // Consigna: Seleccionar únicamente el número de factura, la fecha de emisión y el importe total de todas las facturas de venta.
            System.out.println("\n=== RESULTADOS EJERCICIO 2 ===");
            List<Object[]> resultados2 = em.createQuery("SELECT f.numero, f.fechaEmision, f.importeTotal FROM FacturaVenta f", Object[].class).getResultList();

            for (Object[] fila : resultados2) {
                System.out.println("- Factura Nro: " + fila[0] + " | Fecha: " + fila[1] + " | Total: $" + fila[2]);
            }

            // 3. Filtrado por Igualdad (WHERE)
            //Consigna: Obtener todos los artículos que pertenecen a un rubro con una denominación específica (ej. "Electrónica").
            System.out.println("\n=== RESULTADOS EJERCICIO 3 ===");
            String rubroBuscado = "Electrónica";

            List<Articulo> resultados3 = em.createQuery(
                            "SELECT a FROM Articulo a WHERE a.rubro.denominacion = :denominacion", Articulo.class)
                    .setParameter("denominacion", rubroBuscado)
                    .getResultList();

            for (Articulo a : resultados3) {
                System.out.println("- Artículo: " + a.getDenominacion() + " | Código: " + a.getCodigo());
            }

            
            // 4. Filtrado por Rango de Fechas (BETWEEN)
            //Consigna: Listar todas las facturas de venta emitidas dentro de un rango de fechas determinado.
            System.out.println("\n=== RESULTADOS EJERCICIO 4 ===");
            Date fechaInicio = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-01-01");
            Date fechaFin = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-12-31");
            String jpql4 = "SELECT f FROM FacturaVenta f WHERE f.fechaEmision BETWEEN :fechaInicio AND :fechaFin";
            List<FacturaVenta> resultados4 = em.createQuery(jpql4, FacturaVenta.class)
            .setParameter("fechaInicio", fechaInicio)
            .setParameter("fechaFin", fechaFin)
            .getResultList();

            for (FacturaVenta f : resultados4) {
            System.out.println("- Factura Nro: " + f.getNumero() + " | Fecha Emisión: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal());
            }

            // --------------------------------------------------

            // -- Nivel 2: Condicionales Combinados, Operadores de Texto y Agregaciones Básicas --

            // 5. Condicionales Complejos y Verificación de Nulos (AND, OR, IS NULL)
            // Consigna: Obtener las facturas cuyo estado sea "EMITIDA", con un importe total superior a $10,000 y que no hayan sido anuladas (fechaAnulacion sea nula).
            System.out.println("\n=== RESULTADOS EJERCICIO 5 ===");
            List<FacturaVenta> resultados5 = em.createQuery("SELECT factura FROM FacturaVenta factura WHERE factura.estado='EMITIDA' AND factura.importeTotal>10000 AND factura.fechaAnulacion IS NULL", FacturaVenta.class).getResultList();

            for (FacturaVenta f : resultados5) {
                System.out.println("- Factura Nro: " + f.getNumero() + " | Fecha: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal());
            }

            // 6. Búsqueda por Patrón de Texto (LIKE y LOWER)
            // Consigna: Buscar todos los clientes cuya denominación contenga un texto parcial (sin importar mayúsculas/minúsculas) o cuyo CUIT/CUIL comience con "20-". */
            System.out.println("\n=== RESULTADOS EJERCICIO 6 ===");
            List<Cliente> resultados6 = em.createQuery("SELECT c FROM Cliente c WHERE LOWER(c.denominacion) LIKE LOWER('%demo%') OR c.cuitCuil LIKE '20-%'", Cliente.class).getResultList();
            
            for (Cliente c : resultados6) {
                System.out.println("- Cliente: " + c.getDenominacion() + " | CUIT/CUIL: " + c.getCuitCuil());
            }

            // 7. Valores Distintos y Ordenamiento (DISTINCT y ORDER BY)
            //Consigna: Obtener sin duplicados todos los estados posibles registrados en las facturas de venta, ordenados alfabéticamente de forma ascendente.
            System.out.println("\n=== RESULTADOS EJERCICIO 7 ===");
            List<String> resultados7 = em.createQuery(
                            "SELECT DISTINCT f.estado FROM FacturaVenta f ORDER BY f.estado ASC", String.class)
                    .getResultList();

            for (String estado : resultados7) {
                System.out.println("- Estado: " + estado);
            }

            //8. Funciones de Agregación Simples (COUNT, SUM, AVG)

            System.out.println("\n=== RESULTADOS EJERCICIO 8 ===");
            String jpql8 = "SELECT COUNT(f), SUM(f.importeTotal), AVG(f.importeTotal) FROM FacturaVenta f";
            Object[] resultado8 = em.createQuery(jpql8, Object[].class).getSingleResult();
            System.out.println("Cantidad total de facturas: " + resultado8[0]);
            System.out.println("Suma acumulada de importes: $" + resultado8[1]);
            System.out.println("Importe promedio: $" + resultado8[2]);


            // 9.Operador de inclusión (IN)
            // Consigna: Obtener todos los puntos de venta cuyo número coincida con una lista de enteros proporcionada por parámetro (ej. 1, 2, 5).
            System.out.println("\n=== RESULTADOS EJERCICIO 9 ===");
            {String jpql = "SELECT p FROM PuntoVenta p WHERE p.numero IN :numeros";
            TypedQuery<PuntoVenta> query = em.createQuery(jpql, PuntoVenta.class);
            query.setParameter("numeros", Arrays.asList(1, 2, 5));
            List<PuntoVenta> resultado = query.getResultList();
                for (PuntoVenta pv : resultado) {
                System.out.println(pv.getNumero() + " - " + pv.getDescripcion());
            }}

            // -- Nivel 3: Navegación de Entidades, JOINs y Subconsultas Simples --

            // 10. Navegación Implícita por Relaciones (Path Expressions)
            // Consigna: Consultar todas las facturas de venta creadas por un usuario en particular navegando por su nombre de usuario de carga (usuarioCarga.usuario).
            System.out.println("\n=== RESULTADOS EJERCICIO 10 ===");
            String nombreUsuario = "jperez"; // Nombre de usuario a buscar
            List<FacturaVenta> resultados10 = em.createQuery("SELECT factura FROM FacturaVenta factura WHERE factura.usuarioCarga.usuario = :nombreUsuario", FacturaVenta.class)
                .setParameter("nombreUsuario", nombreUsuario)
                .getResultList();

            for (FacturaVenta f : resultados10) {
                System.out.println("- Factura Nro: " + f.getNumero() + " | Fecha: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal() + " | Usuario Carga: " + f.getUsuarioCarga().getUsuario());
            }


            // 11. Cláusula INNER JOIN Explícita
            // Consigna: Obtener todos los detalles de factura (FacturaVentaDetalle) que correspondan a facturas emitidas por un punto de venta determinado.
            System.out.println("\n=== RESULTADOS EJERCICIO 11 ===");
            PuntoVenta puntoVenta11 = em.find(PuntoVenta.class, 1L); // 1L = id del punto de venta buscado

            List<FacturaVentaDetalle> resultados11 = em.createQuery(
                    "SELECT d FROM FacturaVentaDetalle d JOIN d.factura f WHERE f.puntoVenta = :pv", FacturaVentaDetalle.class)
                .setParameter("pv", puntoVenta11)
                .getResultList();

            for (FacturaVentaDetalle d : resultados11) {
                System.out.println("- Detalle ID: " + d.getId()
                                            + " | Cantidad: "
                                            + d.getCantidad()
                                            + " | Factura Nro: "
                                            + d.getFactura().getNumero());
            }

            // 12. Cláusula LEFT JOIN (Inclusión de Nulos)
            //Consigna: Listar la denominación de todos los artículos junto con la denominación de su marca asociada, incluyendo también aquellos artículos que no posean una marca asignada.
            System.out.println("\n=== RESULTADOS EJERCICIO 12 ===");

            List<Object[]> resultados12 = em.createQuery(
                            "SELECT a.denominacion, m.denominacion FROM Articulo a LEFT JOIN a.marca m", Object[].class)
                    .getResultList();

            for (Object[] fila : resultados12) {
                String articulo = (String) fila[0];
                String marca = fila[1] != null ? (String) fila[1] : "Sin Marca";
                System.out.println("- Artículo: " + articulo + " | Marca: " + marca);
            }


            // 13. Cláusula LEFT JOIN (Inclusión de Nulos)
            //Consigna: Obtener todas las facturas de venta que contengan al menos un detalle de artículo perteneciente a una marca específica. 
            System.out.println("\n=== RESULTADOS EJERCICIO 13 ===");
            {String jpql = "SELECT DISTINCT f FROM FacturaVenta f " +
              "JOIN f.detalles d " +
              "JOIN d.listaPrecioArticulo lpa " +
              "JOIN lpa.articulo a " +
              "JOIN a.marca m " +
              "WHERE m.denominacion = :marca";
            TypedQuery<FacturaVenta> query = em.createQuery(jpql, FacturaVenta.class);
            query.setParameter("marca", "Samsung");
            List<FacturaVenta> resultado = query.getResultList();}

            //14.Subconsulta en Cláusula WHERE 
            //Consigna: Listar las facturas de venta cuyo importeTotal sea estrictamente mayor al promedio de importeTotal de todas las facturas registradas.
            System.out.println("\n=== RESULTADOS EJERCICIO 14 ===");
            {String jpql = "SELECT f FROM FacturaVenta f " +
              "WHERE f.importeTotal > (SELECT AVG(f2.importeTotal) FROM FacturaVenta f2)";
            TypedQuery<FacturaVenta> query = em.createQuery(jpql, FacturaVenta.class);
            List<FacturaVenta> resultado = query.getResultList(); }


            // -- Nivel 4: Agrupamiento (GROUP BY) y Filtros de Grupo (HAVING) --


            // 15. Agrupamiento Básico (GROUP BY)
            //Consigna: Obtener la descripción del punto de venta, la cantidad de facturas emitidas por cada uno y la suma total facturada.
            System.out.println("\n=== RESULTADOS EJERCICIO 15 ===");
            String jpql15 = "SELECT f.puntoVenta.descripcion, COUNT(f), SUM(f.importeTotal) " +
                "FROM FacturaVenta f GROUP BY f.puntoVenta.descripcion";
            List<Object[]> resultados15 = em.createQuery(jpql15, Object[].class).getResultList();

            for (Object[] fila : resultados15) {
            System.out.println("Punto de Venta: " + fila[0] + " | Cant. Facturas: " + fila[1] + " | Total Facturado: $" + fila[2]);
            }

            // 16. Agrupamiento con Condicional de Grupo (HAVING)
            //Consigna: Obtener los nombres de los usuarios de carga que hayan registrado más de 5 facturas de venta en el sistema. 

            System.out.println("\n=== RESULTADOS EJERCICIO 16 ===");
            String jpql16 = "SELECT f.usuarioCarga.usuario " +
                "FROM FacturaVenta f GROUP BY f.usuarioCarga.usuario HAVING COUNT(f) > 5";
            List<String> resultados16 = em.createQuery(jpql16, String.class).getResultList();

            for (String usuario : resultados16) {
            System.out.println("Usuario: " + usuario);
            }   


            //17. Agrupamiento y Agregación sobre Entidades Relacionadas
            // Obtener la denominación de cada marca, la cantidad total de unidades vendidas (SUM(cantidad)) y el subtotal acumulado, agrupado por marca.
            System.out.println("\n=== RESULTADOS EJERCICIO 17 ===");
            {
            String jpql = "SELECT m.denominacion, SUM(d.cantidad), SUM(d.importeSubtotal) " +
              "FROM FacturaVentaDetalle d " +
              "JOIN d.listaPrecioArticulo lpa " +
              "JOIN lpa.articulo a " +
              "JOIN a.marca m " +
              "GROUP BY m.denominacion";
            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            List<Object[]> resultado = query.getResultList();
            for (Object[] fila : resultado) {
            System.out.println("Marca: " + fila[0] + " | Unidades: " + fila[1] + " | Subtotal: " + fila[2]);
            }}
        


            // -- Nivel 5: Subconsultas Correlacionadas, EXISTS, NOT EXISTS y Expresiones Condicionales --

            // 18. Subconsulta Correlacionada con EXISTS
            // Consigna: Obtener la lista de todas las marcas que tienen al menos un artículo que haya sido facturado en alguna factura de venta.
            System.out.println("\n=== RESULTADOS EJERCICIO 18 ===");
            List<Marca> resultados18 = em.createQuery("SELECT marca FROM Marca marca WHERE EXISTS (SELECT facturaVentaDetalle FROM FacturaVentaDetalle facturaVentaDetalle WHERE facturaVentaDetalle.listaPrecioArticulo.articulo.marca = marca)", Marca.class).getResultList();

            for (Marca m : resultados18) {
                System.out.println("- Marca con ventas: " + m.getDenominacion() + " | Código: " + m.getCodigo());
            }


            System.out.println("\n=== RESULTADOS EJERCICIO 19 ===");

            List<Articulo> resultados19 = em.createQuery("SELECT a FROM Articulo a WHERE NOT EXISTS (SELECT d FROM FacturaVentaDetalle d WHERE d.listaPrecioArticulo.articulo = a)", Articulo.class).getResultList();

            for (Articulo a : resultados19) {
            System.out.println("- Artículo sin ventas: " + a.getDenominacion() + " | Código: " + a.getCodigo());
            }
            // 20. Proyección Condicional (CASE WHEN)
            //Consigna: Listar el número de factura, su importe total y una columna calculada
            //llamada "Categoría" que clasifique la factura como:
            //o "ALTO VALOR" si el importeTotal es mayor a $50,000.
            //o "MEDIO VALOR" si el importeTotal está entre $10,000 y $50,000.
            //o "BAJO VALOR" si el importeTotal es menor a $10,000. Ordenar los
            //resultados de mayor a menor importe.
            System.out.println("\n=== RESULTADOS EJERCICIO 20 ===");

            List<Object[]> resultados20 = em.createQuery(
                            "SELECT f.numero, f.importeTotal, " +
                                    "CASE " +
                                    "  WHEN f.importeTotal > 50000 THEN 'ALTO VALOR' " +
                                    "  WHEN f.importeTotal >= 10000 AND f.importeTotal <= 50000 THEN 'MEDIO VALOR' " +
                                    "  ELSE 'BAJO VALOR' " +
                                    "END " +
                                    "FROM FacturaVenta f " +
                                    "ORDER BY f.importeTotal DESC", Object[].class)
                    .getResultList();

            for (Object[] fila : resultados20) {
                Long numero = (Long) fila[0];
                Double importeTotal = (Double) fila[1];
                String categoria = (String) fila[2];
                System.out.println("- Factura Nro: " + numero + " | Total: $" + importeTotal + " | Categoría: " + categoria);
            }



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }

    }






    private static void cargarDatosDePrueba() {
        // Iniciar el contenedor de JPA
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            // Iniciar una transacción
            em.getTransaction().begin();

            // Instanciar los objetos necesarios

            // --- Usuario ---
            Usuario usuario = new Usuario();
            usuario.setNombre("Juan");
            usuario.setApellido("Perez");
            usuario.setUsuario("jperez");
            usuario.setClave("1234");
            em.persist(usuario);

            // --- Condición de IVA ---
            CondicionIva condicionIva = new CondicionIva();
            condicionIva.setDenominacion("Responsable Inscripto");
            condicionIva.setCodigoAfip(1);
            condicionIva.setFechaAlta(new Date());
            condicionIva.setFechaModificacion(new Date());
            condicionIva.setUsuarioCarga(usuario);
            condicionIva.setUsuarioModificacion(usuario);
            em.persist(condicionIva);

            // --- Tipo de Moneda ---
            TipoMoneda tipoMoneda = new TipoMoneda();
            tipoMoneda.setCodigoAfip(1);
            tipoMoneda.setDenominacion("Peso Argentino");
            tipoMoneda.setSimbolo("$");
            tipoMoneda.setFechaAlta(new Date());
            tipoMoneda.setFechaModificacion(new Date());
            tipoMoneda.setUsuarioCarga(usuario);
            tipoMoneda.setUsuarioModificacion(usuario);
            em.persist(tipoMoneda);

            // --- Contacto para Cliente ---
            Contacto contacto = new Contacto();
            contacto.setCelular("2615863203");
            contacto.setEmail("empresademo@gmail.com");
            contacto.setTelefono("49181200");
            em.persist(contacto);

            // --- Domicilio para Cliente  ---
            Domicilio domicilio = new Domicilio();
            domicilio.setNombreCalle("Famatina");
            domicilio.setNumeroCalle("900");
            em.persist(domicilio);

            // --- Cliente ---
            Cliente cliente = new Cliente();
            cliente.setDenominacion("Empresa Demo S.A.");
            cliente.setCuitCuil("30-12345678-9");
            cliente.setContacto(contacto);
            cliente.setDomicilio(domicilio);
            cliente.setFechaAlta(new Date());
            cliente.setFechaModificacion(new Date());
            cliente.setUsuarioCarga(usuario);
            cliente.setUsuarioModificacion(usuario);
            em.persist(cliente);

            // --- Punto de Venta ---
            PuntoVenta puntoVenta = new PuntoVenta();
            puntoVenta.setNumero(1);
            puntoVenta.setDescripcion("Casa Central");
            puntoVenta.setTipoEmision("Electronica");
            puntoVenta.setDomicilioComercial("Av. Siempre Viva 123");
            puntoVenta.setFechaAlta(new Date());
            puntoVenta.setFechaModificacion(new Date());
            puntoVenta.setUsuarioCarga(usuario);
            puntoVenta.setUsuarioModificacion(usuario);
            em.persist(puntoVenta);

            // --- Rubro ---
            Rubro rubro = new Rubro();
            rubro.setDenominacion("Electronica");
            rubro.setCodigo(1);
            rubro.setFechaAlta(new Date());
            rubro.setFechaModificacion(new Date());
            rubro.setUsuarioCarga(usuario);
            rubro.setUsuarioModificacion(usuario);
            em.persist(rubro);

            // --- Marca ---
            Marca marca = new Marca();
            marca.setDenominacion("Generica");
            marca.setCodigo(1);
            marca.setFechaAlta(new Date());
            marca.setFechaModificacion(new Date());
            marca.setUsuarioCarga(usuario);
            marca.setUsuarioModificacion(usuario);
            em.persist(marca);

            // --- Articulo ---
            Articulo articulo = new Articulo();
            articulo.setCodigo("ART001");
            articulo.setDenominacion("Mouse Inalambrico");
            articulo.setRubro(rubro);
            articulo.setMarca(marca);
            articulo.setFechaAlta(new Date());
            articulo.setFechaModificacion(new Date());
            articulo.setUsuarioCarga(usuario);
            articulo.setUsuarioModificacion(usuario);
            em.persist(articulo);

            // --- Lista de Precio ---
            ListaPrecio listaPrecio = new ListaPrecio();
            listaPrecio.setCodigo("LP001");
            listaPrecio.setDenominacion("Lista General");
            listaPrecio.setFechaAlta(new Date());
            listaPrecio.setFechaModificacion(new Date());
            listaPrecio.setUsuarioCarga(usuario);
            listaPrecio.setUsuarioModificacion(usuario);
            em.persist(listaPrecio);

            // --- Lista de Precio del Articulo (define el precio de venta) ---
            ListaPrecioArticulo listaPrecioArticulo = new ListaPrecioArticulo();
            listaPrecioArticulo.setListaPrecio(listaPrecio);
            listaPrecioArticulo.setArticulo(articulo);
            listaPrecioArticulo.setFechaAlta(new Date());
            listaPrecioArticulo.setFechaModificacion(new Date());
            listaPrecioArticulo.setUsuarioCarga(usuario);
            listaPrecioArticulo.setUsuarioModificacion(usuario);
            em.persist(listaPrecioArticulo);

            // --- Cabecera de FacturaVenta ---
            FacturaVenta facturaVenta = new FacturaVenta();
            facturaVenta.setNumero(1L);
            facturaVenta.setFechaEmision(new Date());
            facturaVenta.setPuntoVenta(puntoVenta);
            facturaVenta.setUsuarioCarga(usuario);
            facturaVenta.setUsuarioModificacion(usuario);
            facturaVenta.setCondicionIva(condicionIva);
            facturaVenta.setTipoMoneda(tipoMoneda);
            facturaVenta.setCliente(cliente);
            facturaVenta.setImporteTotal(1500.0);
            facturaVenta.setImporteCobrado(1500.0);
            facturaVenta.setImporteSaldo(0.0);
            facturaVenta.setEstado("EMITIDA");
            facturaVenta.setFechaAlta(new Date());
            facturaVenta.setFechaModificacion(new Date());
            facturaVenta.setUsuarioCarga(usuario);
            facturaVenta.setUsuarioModificacion(usuario);

            // --- Detalle 1 de FacturaVenta ---
            FacturaVentaDetalle detalle1 = new FacturaVentaDetalle();
            detalle1.setListaPrecioArticulo(listaPrecioArticulo);
            detalle1.setDescripcion("Mouse Inalambrico");
            detalle1.setCantidad(1);
            detalle1.setPrecioUnitario(1500.0);
            detalle1.setPorcentajeBonificacion(0.0);
            detalle1.setImporteNeto(1239.67);
            detalle1.setImporteIva(260.33);
            detalle1.setImporteSubtotal(1500.0);

            FacturaVentaDetalle detalle2 = new FacturaVentaDetalle();
            detalle2.setListaPrecioArticulo(listaPrecioArticulo);
            detalle2.setDescripcion("Teclado Gamer");
            detalle2.setCantidad(1);
            detalle2.setPrecioUnitario(2000.0);
            detalle2.setPorcentajeBonificacion(0.0);
            detalle2.setImporteNeto(1639.34);
            detalle2.setImporteIva(360.66);
            detalle2.setImporteSubtotal(2000.0);

            // addDetalle() deja la relación bidireccional sincronizada
            facturaVenta.addDetalle(detalle1);
            facturaVenta.addDetalle(detalle2);

            // Persistir el objeto cabecera FacturaVenta
            em.persist(facturaVenta);

            // Finalizar la transacción
            em.getTransaction().commit();

            // Verificar que, gracias a la configuración de cascada, se hayan insertado
            // automáticamente tanto la factura como todos sus detalles vinculados
            System.out.println("Factura Nº " + facturaVenta.getNumero()
                    + " persistida correctamente con ID: " + facturaVenta.getId());
            System.out.println("Cantidad de detalles guardados en cascada: " + facturaVenta.getDetalles().size());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            // Cerrar el EntityManager y EntityManagerFactory
            em.close();
            emf.close();
        }
    }
}