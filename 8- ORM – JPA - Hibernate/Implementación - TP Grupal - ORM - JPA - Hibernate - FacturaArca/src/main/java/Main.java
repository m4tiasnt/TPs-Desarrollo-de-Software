
import com.facturaarca.entities.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Date;

// Crear la clase principal Main con la firma public static void main(String[] args).
public class Main {

    public static void main(String[] args) {

        // Iniciar el contenedor de JPA mediante
        // Persistence.createEntityManagerFactory("FacturacionPU") y obtener el EntityManager.
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            // Iniciar una transacción (em.getTransaction().begin()).
            em.getTransaction().begin();

            // Instanciar los objetos necesarios (ej. Usuario, PuntoVenta, Articulo,
            // ListaPrecioArticulo, FacturaVenta, etc.).

            // --- Usuario ---
            Usuario usuario = new Usuario();
            usuario.setNombre("Juan");
            usuario.setApellido("Perez");
            usuario.setNombreUsuario("jperez");
            usuario.setPassword("1234");
            em.persist(usuario);

            // --- Punto de Venta ---
            PuntoVenta puntoVenta = new PuntoVenta(1, "Casa Central", "Electronica", "Av. Siempre Viva 123");
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

            // Crear una cabecera de FacturaVenta
            // y asignar uno o más ítems FacturaVentaDetalle asociándolos bidireccionalmente.
            FacturaVenta facturaVenta = new FacturaVenta();
            facturaVenta.setNumero(1L);
            facturaVenta.setFechaEmision(new Date());
            facturaVenta.setPuntoVenta(puntoVenta);
            facturaVenta.setImporteTotal(1500.0);
            facturaVenta.setImporteCobrado(1500.0);
            facturaVenta.setImporteSaldo(0.0);
            facturaVenta.setEstado("EMITIDA");
            facturaVenta.setFechaAlta(new Date());
            facturaVenta.setFechaModificacion(new Date());
            facturaVenta.setUsuarioCarga(usuario);
            facturaVenta.setUsuarioModificacion(usuario);

            FacturaVentaDetalle detalle1 = new FacturaVentaDetalle();
            detalle1.setListaPrecioArticulo(listaPrecioArticulo);
            detalle1.setDescripcion("Mouse Inalambrico");
            detalle1.setCantidad(1);
            detalle1.setPrecioUnitario(1500.0);
            detalle1.setPorcentajeBonificacion(0.0);
            detalle1.setImporteNeto(1239.67);
            detalle1.setImporteIva(260.33);
            detalle1.setImporteSubtotal(1500.0);

            // addDetalle() deja la relación bidireccional sincronizada:
            // agrega el detalle a la lista Y hace detalle.setFactura(this).
            facturaVenta.addDetalle(detalle1);

            // Requisito clave: Persistir únicamente el objeto cabecera FacturaVenta
            // utilizando un solo llamado a em.persist(facturaVenta).
            em.persist(facturaVenta);

            // Finalizar la transacción con em.getTransaction().commit()...
            em.getTransaction().commit();

            // Verificar que, gracias a la configuración de cascada (CascadeType.ALL / PERSIST),
            // se hayan insertado automáticamente tanto la factura como todos sus detalles vinculados.
            System.out.println("Factura Nº " + facturaVenta.getNumero()
                    + " persistida correctamente con ID: " + facturaVenta.getId());
            System.out.println("Cantidad de detalles guardados en cascada: " + facturaVenta.getDetalles().size());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            // ...y cerrar el EntityManager y EntityManagerFactory.
            em.close();
            emf.close();
        }
    }
}
