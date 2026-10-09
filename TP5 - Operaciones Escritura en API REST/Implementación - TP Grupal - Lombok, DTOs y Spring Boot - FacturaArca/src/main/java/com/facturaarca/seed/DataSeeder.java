package com.facturaarca.seed;

import com.facturaarca.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Carga los datos de prueba una unica vez: si ya hay usuarios en la base, no hace nada.
 * Corre antes que el CommandLineRunner que genera el reporte (@Order(1)).
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    private int clienteSeq = 0;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long existentes = em.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class).getSingleResult();
        if (existentes > 0) {
            System.out.println("Ya existen datos de prueba, se omite la carga inicial.");
            return;
        }
        cargarDatosDePrueba();
    }

    private void cargarDatosDePrueba() {
        // --- Usuarios ---
        Usuario usuario = new Usuario();
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setUsuario("jperez");
        usuario.setClave("1234");
        em.persist(usuario);

        Usuario usuario2 = new Usuario();
        usuario2.setNombre("Maria");
        usuario2.setApellido("Garcia");
        usuario2.setUsuario("mgarcia");
        usuario2.setClave("1234");
        em.persist(usuario2);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

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

        // --- Clientes ---
        Cliente cliente = nuevoCliente(usuario, "30-12345678-9", "Empresa Demo S.A.", "Av. San Martín", "456");
        Cliente cliGomez = nuevoCliente(usuario, "20-12345678-5", "Juan Gomez", "Belgrano", "789");
        Cliente cliDemoCenter = nuevoCliente(usuario, "20-87654321-0", "Demo Center SRL", "Sarmiento", "1234");
        Cliente cliFerreteria = nuevoCliente(usuario, "27-11111111-2", "Ferreteria El Tornillo", "Las Heras", "880");

        // --- Puntos de venta ---
        PuntoVenta puntoVenta = nuevoPuntoVenta(usuario, 1, "Casa Central", "Av. Siempre Viva 123");
        PuntoVenta pv2 = nuevoPuntoVenta(usuario, 2, "Sucursal Norte", "Av. San Martin 456");
        PuntoVenta pv3 = nuevoPuntoVenta(usuario, 3, "Sucursal Sur", "Belgrano 789");
        PuntoVenta pv5 = nuevoPuntoVenta(usuario, 5, "Deposito", "Ruta 40 Km 12");

        // --- Rubros ---
        Rubro rubro = nuevoRubro(usuario, "Electrónica", 1);
        Rubro rubroMuebles = nuevoRubro(usuario, "Muebles", 2);

        // --- Marcas ---
        Marca marca = nuevaMarca(usuario, "Generica", 1);
        Marca marcaSamsung = nuevaMarca(usuario, "Samsung", 2);
        Marca marcaLG = nuevaMarca(usuario, "LG", 3);
        Marca marcaSony = nuevaMarca(usuario, "Sony", 4);

        // --- Articulos ---
        Articulo articulo = nuevoArticulo(usuario, "ART001", "Mouse Inalambrico", rubro, marca);
        Articulo tvSamsung = nuevoArticulo(usuario, "ART002", "TV Samsung 55", rubro, marcaSamsung);
        Articulo celuSamsung = nuevoArticulo(usuario, "ART003", "Celular Samsung A54", rubro, marcaSamsung);
        Articulo heladeraLG = nuevoArticulo(usuario, "ART004", "Heladera LG", rubro, marcaLG);
        Articulo monitorLG = nuevoArticulo(usuario, "ART005", "Monitor LG 27", rubro, marcaLG);
        Articulo silla = nuevoArticulo(usuario, "ART006", "Silla de Madera", rubroMuebles, marca);
        nuevoArticulo(usuario, "ART007", "Parlante Sony", rubro, marcaSony);
        nuevoArticulo(usuario, "ART008", "Mesa de Luz", rubroMuebles, null);
        nuevoArticulo(usuario, "ART009", "Teclado Mecanico", rubro, marca);

        // --- Lista de Precio ---
        ListaPrecio listaPrecio = new ListaPrecio();
        listaPrecio.setCodigo("LP001");
        listaPrecio.setDenominacion("Lista General");
        listaPrecio.setFechaAlta(new Date());
        listaPrecio.setFechaModificacion(new Date());
        listaPrecio.setUsuarioCarga(usuario);
        listaPrecio.setUsuarioModificacion(usuario);
        em.persist(listaPrecio);

        // --- Precios por articulo ---
        ListaPrecioArticulo listaPrecioArticulo = nuevoPrecio(usuario, listaPrecio, articulo);
        ListaPrecioArticulo lpaTV = nuevoPrecio(usuario, listaPrecio, tvSamsung);
        ListaPrecioArticulo lpaCelu = nuevoPrecio(usuario, listaPrecio, celuSamsung);
        ListaPrecioArticulo lpaHeladera = nuevoPrecio(usuario, listaPrecio, heladeraLG);
        ListaPrecioArticulo lpaMonitor = nuevoPrecio(usuario, listaPrecio, monitorLG);
        ListaPrecioArticulo lpaSilla = nuevoPrecio(usuario, listaPrecio, silla);

        // --- Factura 1 ---
        FacturaVenta facturaVenta = new FacturaVenta();
        facturaVenta.setNumero(1L);
        facturaVenta.setFechaEmision(new Date());
        facturaVenta.setPuntoVenta(puntoVenta);
        facturaVenta.setUsuarioCarga(usuario);
        facturaVenta.setUsuarioModificacion(usuario);
        facturaVenta.setCondicionIva(condicionIva);
        facturaVenta.setTipoMoneda(tipoMoneda);
        facturaVenta.setCliente(cliente);
        facturaVenta.setImporteTotal(3500.0);
        facturaVenta.setImporteCobrado(3500.0);
        facturaVenta.setImporteSaldo(0.0);
        facturaVenta.setEstado("EMITIDA");
        facturaVenta.setFechaAlta(new Date());
        facturaVenta.setFechaModificacion(new Date());
        facturaVenta.setUsuarioCarga(usuario);
        facturaVenta.setUsuarioModificacion(usuario);

        // --- Detalles ---
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

        try {
            // Factura 2
            FacturaVenta f2 = nuevaFactura(usuario, condicionIva, tipoMoneda, puntoVenta, cliente, 2L, "2026-02-10", "EMITIDA", 75000.0, null, sdf);
            f2.addDetalle(nuevoDetalle(lpaTV, "TV Samsung 55", 1, 75000.0, 75000.0));
            em.persist(f2);

            // Factura 3
            FacturaVenta f3 = nuevaFactura(usuario, condicionIva, tipoMoneda, puntoVenta, cliente, 3L, "2026-03-15", "EMITIDA", 25000.0, null, sdf);
            f3.addDetalle(nuevoDetalle(lpaHeladera, "Heladera LG", 1, 25000.0, 25000.0));
            em.persist(f3);

            // Factura 4
            FacturaVenta f4 = nuevaFactura(usuario, condicionIva, tipoMoneda, pv2, cliGomez, 4L, "2026-04-20", "EMITIDA", 5000.0, null, sdf);
            f4.addDetalle(nuevoDetalle(lpaSilla, "Silla de Madera", 2, 2500.0, 5000.0));
            em.persist(f4);

            // Factura 5
            FacturaVenta f5 = nuevaFactura(usuario, condicionIva, tipoMoneda, pv2, cliDemoCenter, 5L, "2026-05-05", "EMITIDA", 60000.0, null, sdf);
            f5.addDetalle(nuevoDetalle(lpaCelu, "Celular Samsung A54", 2, 30000.0, 60000.0));
            em.persist(f5);

            // Factura 6
            FacturaVenta f6 = nuevaFactura(usuario, condicionIva, tipoMoneda, pv5, cliente, 6L, "2026-06-12", "ANULADA", 30000.0, "2026-06-20", sdf);
            f6.addDetalle(nuevoDetalle(lpaMonitor, "Monitor LG 27", 1, 30000.0, 30000.0));
            em.persist(f6);

            // Factura 7
            FacturaVenta f7 = nuevaFactura(usuario, condicionIva, tipoMoneda, puntoVenta, cliFerreteria, 7L, "2026-07-01", "EMITIDA", 12000.0, null, sdf);
            f7.addDetalle(nuevoDetalle(listaPrecioArticulo, "Mouse Inalambrico", 8, 1500.0, 12000.0));
            em.persist(f7);

            // Factura 8
            FacturaVenta f8 = nuevaFactura(usuario, condicionIva, tipoMoneda, pv2, cliGomez, 8L, "2026-08-19", "PENDIENTE", 8000.0, null, sdf);
            f8.addDetalle(nuevoDetalle(lpaSilla, "Silla de Madera", 1, 8000.0, 8000.0));
            em.persist(f8);

            // Factura 9
            FacturaVenta f9 = nuevaFactura(usuario2, condicionIva, tipoMoneda, pv3, cliDemoCenter, 9L, "2026-09-10", "EMITIDA", 45000.0, null, sdf);
            f9.addDetalle(nuevoDetalle(lpaTV, "TV Samsung 55", 1, 45000.0, 45000.0));
            em.persist(f9);

            // Factura 10
            FacturaVenta f10 = nuevaFactura(usuario2, condicionIva, tipoMoneda, puntoVenta, cliFerreteria, 10L, "2025-12-15", "EMITIDA", 1500.0, null, sdf);
            f10.addDetalle(nuevoDetalle(listaPrecioArticulo, "Mouse Inalambrico", 1, 1500.0, 1500.0));
            em.persist(f10);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cargar datos de prueba", e);
        }

        System.out.println("Carga de datos de prueba finalizada.");
    }

    // ---------- Helpers de carga de datos ----------
    private PuntoVenta nuevoPuntoVenta(Usuario u, int numero, String descripcion, String domicilioComercial) {
        PuntoVenta pv = new PuntoVenta();
        pv.setNumero(numero);
        pv.setDescripcion(descripcion);
        pv.setTipoEmision("Electronica");
        pv.setDomicilioComercial(domicilioComercial);
        pv.setFechaAlta(new Date());
        pv.setFechaModificacion(new Date());
        pv.setUsuarioCarga(u);
        pv.setUsuarioModificacion(u);
        em.persist(pv);
        return pv;
    }

    private Rubro nuevoRubro(Usuario u, String denominacion, int codigo) {
        Rubro r = new Rubro();
        r.setDenominacion(denominacion);
        r.setCodigo(codigo);
        r.setFechaAlta(new Date());
        r.setFechaModificacion(new Date());
        r.setUsuarioCarga(u);
        r.setUsuarioModificacion(u);
        em.persist(r);
        return r;
    }

    private Marca nuevaMarca(Usuario u, String denominacion, int codigo) {
        Marca m = new Marca();
        m.setDenominacion(denominacion);
        m.setCodigo(codigo);
        m.setFechaAlta(new Date());
        m.setFechaModificacion(new Date());
        m.setUsuarioCarga(u);
        m.setUsuarioModificacion(u);
        em.persist(m);
        return m;
    }

    private Articulo nuevoArticulo(Usuario u, String codigo, String denominacion, Rubro rubro, Marca marca) {
        Articulo a = new Articulo();
        a.setCodigo(codigo);
        a.setDenominacion(denominacion);
        a.setRubro(rubro);
        a.setMarca(marca);
        a.setFechaAlta(new Date());
        a.setFechaModificacion(new Date());
        a.setUsuarioCarga(u);
        a.setUsuarioModificacion(u);
        em.persist(a);
        return a;
    }

    private ListaPrecioArticulo nuevoPrecio(Usuario u, ListaPrecio lista, Articulo articulo) {
        ListaPrecioArticulo lpa = new ListaPrecioArticulo();
        lpa.setListaPrecio(lista);
        lpa.setArticulo(articulo);
        lpa.setFechaAlta(new Date());
        lpa.setFechaModificacion(new Date());
        lpa.setUsuarioCarga(u);
        lpa.setUsuarioModificacion(u);
        em.persist(lpa);
        return lpa;
    }

    private Cliente nuevoCliente(Usuario u, String cuit, String denominacion, String calle, String numero) {
        Contacto c = new Contacto();
        c.setEmail(denominacion.replaceAll("\\s+", "").toLowerCase() + "@mail.com");
        c.setTelefono("426" + (1000 + clienteSeq * 137));
        c.setCelular("2615" + String.format("%06d", 123456 + clienteSeq * 777));
        clienteSeq++;
        em.persist(c);
        Domicilio d = new Domicilio();
        d.setNombreCalle(calle);
        d.setNumeroCalle(numero);
        em.persist(d);
        Cliente cli = new Cliente();
        cli.setCuitCuil(cuit);
        cli.setDenominacion(denominacion);
        cli.setContacto(c);
        cli.setDomicilio(d);
        cli.setFechaAlta(new Date());
        cli.setFechaModificacion(new Date());
        cli.setUsuarioCarga(u);
        cli.setUsuarioModificacion(u);
        em.persist(cli);
        return cli;
    }

    private FacturaVenta nuevaFactura(Usuario u, CondicionIva ci, TipoMoneda tm,
                                      PuntoVenta pv, Cliente cli, Long numero, String fechaEmision,
                                      String estado, double total, String fechaAnulacion,
                                      java.text.SimpleDateFormat sdf) throws Exception {
        FacturaVenta f = new FacturaVenta();
        f.setNumero(numero);
        f.setFechaEmision(sdf.parse(fechaEmision));
        f.setPuntoVenta(pv);
        f.setCliente(cli);
        f.setCondicionIva(ci);
        f.setTipoMoneda(tm);
        f.setImporteTotal(total);
        f.setImporteCobrado(total);
        f.setImporteSaldo(0.0);
        f.setEstado(estado);
        if (fechaAnulacion != null) {
            f.setFechaAnulacion(sdf.parse(fechaAnulacion));
        }
        f.setFechaAlta(new Date());
        f.setFechaModificacion(new Date());
        f.setUsuarioCarga(u);
        f.setUsuarioModificacion(u);
        return f;
    }

    private FacturaVentaDetalle nuevoDetalle(ListaPrecioArticulo lpa, String descripcion,
                                             double cantidad, double precioUnitario, double subtotal) {
        FacturaVentaDetalle d = new FacturaVentaDetalle();
        d.setListaPrecioArticulo(lpa);
        d.setDescripcion(descripcion);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precioUnitario);
        d.setPorcentajeBonificacion(0.0);
        double neto = Math.round(subtotal / 1.21 * 100.0) / 100.0;
        double iva = Math.round((subtotal - neto) * 100.0) / 100.0;
        d.setImporteNeto(neto);
        d.setImporteIva(iva);
        d.setImporteSubtotal(subtotal);
        return d;
    }
}
