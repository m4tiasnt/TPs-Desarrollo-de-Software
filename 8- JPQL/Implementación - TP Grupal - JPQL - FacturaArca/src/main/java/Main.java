
import com.facturaarca.entities.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

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

        // -- TP3 - CONSULTAS --
        try {
            /* 1. Consulta  de Entidades Completas
            Consigna: Obtener la lista completa de todas las facturas de venta registradas en el sistema. */
            List<FacturaVenta> ej1 = em.createQuery("SELECT factura FROM FacturaVenta factura", FacturaVenta.class).getResultList();

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