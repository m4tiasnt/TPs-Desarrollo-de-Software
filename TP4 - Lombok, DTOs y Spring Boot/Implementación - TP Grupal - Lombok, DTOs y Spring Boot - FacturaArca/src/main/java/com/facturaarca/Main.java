package com.facturaarca;

import com.facturaarca.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Date;

@SpringBootApplication
public class Main {

    private static final boolean CARGAR_DATOS = false;

    public static void main(String[] args) {

        SpringApplication.run(Main.class, args);

        if (CARGAR_DATOS) {
            cargarDatosDePrueba();
        }
    }


    private static void cargarDatosDePrueba() {
        // Iniciar el contenedor de JPA
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            // Iniciar una transacción
            em.getTransaction().begin();

            // Datos de prueba

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
            Cliente cliente = nuevoCliente(em, usuario, "30-12345678-9", "Empresa Demo S.A.", "Av. San Martín", "456");
            Cliente cliGomez = nuevoCliente(em, usuario, "20-12345678-5", "Juan Gomez", "Belgrano", "789");
            Cliente cliDemoCenter = nuevoCliente(em, usuario, "20-87654321-0", "Demo Center SRL", "Sarmiento", "1234");
            Cliente cliFerreteria = nuevoCliente(em, usuario, "27-11111111-2", "Ferreteria El Tornillo", "Las Heras", "880");

            // --- Puntos de venta ---
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

            PuntoVenta pv2 = new PuntoVenta();
            pv2.setNumero(2);
            pv2.setDescripcion("Sucursal Norte");
            pv2.setTipoEmision("Electronica");
            pv2.setDomicilioComercial("Av. San Martin 456");
            pv2.setFechaAlta(new Date());
            pv2.setFechaModificacion(new Date());
            pv2.setUsuarioCarga(usuario);
            pv2.setUsuarioModificacion(usuario);
            em.persist(pv2);

            PuntoVenta pv3 = new PuntoVenta();
            pv3.setNumero(3);
            pv3.setDescripcion("Sucursal Sur");
            pv3.setTipoEmision("Electronica");
            pv3.setDomicilioComercial("Belgrano 789");
            pv3.setFechaAlta(new Date());
            pv3.setFechaModificacion(new Date());
            pv3.setUsuarioCarga(usuario);
            pv3.setUsuarioModificacion(usuario);
            em.persist(pv3);

            PuntoVenta pv5 = new PuntoVenta();
            pv5.setNumero(5);
            pv5.setDescripcion("Deposito");
            pv5.setTipoEmision("Electronica");
            pv5.setDomicilioComercial("Ruta 40 Km 12");
            pv5.setFechaAlta(new Date());
            pv5.setFechaModificacion(new Date());
            pv5.setUsuarioCarga(usuario);
            pv5.setUsuarioModificacion(usuario);
            em.persist(pv5);

            // --- Rubros ---
            Rubro rubro = new Rubro();
            rubro.setDenominacion("Electrónica");
            rubro.setCodigo(1);
            rubro.setFechaAlta(new Date());
            rubro.setFechaModificacion(new Date());
            rubro.setUsuarioCarga(usuario);
            rubro.setUsuarioModificacion(usuario);
            em.persist(rubro);

            Rubro rubroMuebles = new Rubro();
            rubroMuebles.setDenominacion("Muebles");
            rubroMuebles.setCodigo(2);
            rubroMuebles.setFechaAlta(new Date());
            rubroMuebles.setFechaModificacion(new Date());
            rubroMuebles.setUsuarioCarga(usuario);
            rubroMuebles.setUsuarioModificacion(usuario);
            em.persist(rubroMuebles);

            // --- Marcas ---
            Marca marca = new Marca();
            marca.setDenominacion("Generica");
            marca.setCodigo(1);
            marca.setFechaAlta(new Date());
            marca.setFechaModificacion(new Date());
            marca.setUsuarioCarga(usuario);
            marca.setUsuarioModificacion(usuario);
            em.persist(marca);

            Marca marcaSamsung = new Marca();
            marcaSamsung.setDenominacion("Samsung");
            marcaSamsung.setCodigo(2);
            marcaSamsung.setFechaAlta(new Date());
            marcaSamsung.setFechaModificacion(new Date());
            marcaSamsung.setUsuarioCarga(usuario);
            marcaSamsung.setUsuarioModificacion(usuario);
            em.persist(marcaSamsung);

            Marca marcaLG = new Marca();
            marcaLG.setDenominacion("LG");
            marcaLG.setCodigo(3);
            marcaLG.setFechaAlta(new Date());
            marcaLG.setFechaModificacion(new Date());
            marcaLG.setUsuarioCarga(usuario);
            marcaLG.setUsuarioModificacion(usuario);
            em.persist(marcaLG);

            Marca marcaSony = new Marca();
            marcaSony.setDenominacion("Sony");
            marcaSony.setCodigo(4);
            marcaSony.setFechaAlta(new Date());
            marcaSony.setFechaModificacion(new Date());
            marcaSony.setUsuarioCarga(usuario);
            marcaSony.setUsuarioModificacion(usuario);
            em.persist(marcaSony);

            // --- Articulos ---
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

            Articulo tvSamsung = nuevoArticulo(em, usuario, "ART002", "TV Samsung 55", rubro, marcaSamsung);
            Articulo celuSamsung = nuevoArticulo(em, usuario, "ART003", "Celular Samsung A54", rubro, marcaSamsung);
            Articulo heladeraLG = nuevoArticulo(em, usuario, "ART004", "Heladera LG", rubro, marcaLG);
            Articulo monitorLG = nuevoArticulo(em, usuario, "ART005", "Monitor LG 27", rubro, marcaLG);
            Articulo silla = nuevoArticulo(em, usuario, "ART006", "Silla de Madera", rubroMuebles, marca);
            nuevoArticulo(em, usuario, "ART007", "Parlante Sony", rubro, marcaSony);
            nuevoArticulo(em, usuario, "ART008", "Mesa de Luz", rubroMuebles, null);
            nuevoArticulo(em, usuario, "ART009", "Teclado Mecanico", rubro, marca);

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
            ListaPrecioArticulo listaPrecioArticulo = new ListaPrecioArticulo();
            listaPrecioArticulo.setListaPrecio(listaPrecio);
            listaPrecioArticulo.setArticulo(articulo);
            listaPrecioArticulo.setFechaAlta(new Date());
            listaPrecioArticulo.setFechaModificacion(new Date());
            listaPrecioArticulo.setUsuarioCarga(usuario);
            listaPrecioArticulo.setUsuarioModificacion(usuario);
            em.persist(listaPrecioArticulo);

            ListaPrecioArticulo lpaTV = nuevoPrecio(em, usuario, listaPrecio, tvSamsung);
            ListaPrecioArticulo lpaCelu = nuevoPrecio(em, usuario, listaPrecio, celuSamsung);
            ListaPrecioArticulo lpaHeladera = nuevoPrecio(em, usuario, listaPrecio, heladeraLG);
            ListaPrecioArticulo lpaMonitor = nuevoPrecio(em, usuario, listaPrecio, monitorLG);
            ListaPrecioArticulo lpaSilla = nuevoPrecio(em, usuario, listaPrecio, silla);

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

            // Factura 2
            FacturaVenta f2 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, puntoVenta, cliente, 2L, "2026-02-10", "EMITIDA", 75000.0, null, sdf);
            f2.addDetalle(nuevoDetalle(lpaTV, "TV Samsung 55", 1, 75000.0, 75000.0));
            em.persist(f2);

            // Factura 3
            FacturaVenta f3 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, puntoVenta, cliente, 3L, "2026-03-15", "EMITIDA", 25000.0, null, sdf);
            f3.addDetalle(nuevoDetalle(lpaHeladera, "Heladera LG", 1, 25000.0, 25000.0));
            em.persist(f3);

            // Factura 4
            FacturaVenta f4 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, pv2, cliGomez, 4L, "2026-04-20", "EMITIDA", 5000.0, null, sdf);
            f4.addDetalle(nuevoDetalle(lpaSilla, "Silla de Madera", 2, 2500.0, 5000.0));
            em.persist(f4);

            // Factura 5
            FacturaVenta f5 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, pv2, cliDemoCenter, 5L, "2026-05-05", "EMITIDA", 60000.0, null, sdf);
            f5.addDetalle(nuevoDetalle(lpaCelu, "Celular Samsung A54", 2, 30000.0, 60000.0));
            em.persist(f5);

            // Factura 6
            FacturaVenta f6 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, pv5, cliente, 6L, "2026-06-12", "ANULADA", 30000.0, "2026-06-20", sdf);
            f6.addDetalle(nuevoDetalle(lpaMonitor, "Monitor LG 27", 1, 30000.0, 30000.0));
            em.persist(f6);

            // Factura 7
            FacturaVenta f7 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, puntoVenta, cliFerreteria, 7L, "2026-07-01", "EMITIDA", 12000.0, null, sdf);
            f7.addDetalle(nuevoDetalle(listaPrecioArticulo, "Mouse Inalambrico", 8, 1500.0, 12000.0));
            em.persist(f7);

            // Factura 8
            FacturaVenta f8 = nuevaFactura(em, usuario, condicionIva, tipoMoneda, pv2, cliGomez, 8L, "2026-08-19", "PENDIENTE", 8000.0, null, sdf);
            f8.addDetalle(nuevoDetalle(lpaSilla, "Silla de Madera", 1, 8000.0, 8000.0));
            em.persist(f8);

            // Factura 9
            FacturaVenta f9 = nuevaFactura(em, usuario2, condicionIva, tipoMoneda, pv3, cliDemoCenter, 9L, "2026-09-10", "EMITIDA", 45000.0, null, sdf);
            f9.addDetalle(nuevoDetalle(lpaTV, "TV Samsung 55", 1, 45000.0, 45000.0));
            em.persist(f9);

            // Factura 10
            FacturaVenta f10 = nuevaFactura(em, usuario2, condicionIva, tipoMoneda, puntoVenta, cliFerreteria, 10L, "2025-12-15", "EMITIDA", 1500.0, null, sdf);
            f10.addDetalle(nuevoDetalle(listaPrecioArticulo, "Mouse Inalambrico", 1, 1500.0, 1500.0));
            em.persist(f10);

            // Finalizar la transacción
            em.getTransaction().commit();

            System.out.println("Carga de datos de prueba finalizada.");

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

    // ---------- Helpers de carga de datos ----------
    private static Articulo nuevoArticulo(EntityManager em, Usuario u, String codigo, String denominacion, Rubro rubro, Marca marca) {
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

    private static ListaPrecioArticulo nuevoPrecio(EntityManager em, Usuario u, ListaPrecio lista, Articulo articulo) {
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

    private static int clienteSeq = 0;

    private static Cliente nuevoCliente(EntityManager em, Usuario u, String cuit, String denominacion, String calle, String numero) {
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

    private static FacturaVenta nuevaFactura(EntityManager em, Usuario u, CondicionIva ci, TipoMoneda tm,
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

    private static FacturaVentaDetalle nuevoDetalle(ListaPrecioArticulo lpa, String descripcion,
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