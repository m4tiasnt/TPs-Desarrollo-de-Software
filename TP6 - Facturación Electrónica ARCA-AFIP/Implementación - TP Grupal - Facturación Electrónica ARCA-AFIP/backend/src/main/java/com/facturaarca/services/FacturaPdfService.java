package com.facturaarca.services;

import com.facturaarca.entities.Articulo;
import com.facturaarca.entities.Cliente;
import com.facturaarca.entities.Domicilio;
import com.facturaarca.entities.FacturaVenta;
import com.facturaarca.entities.FacturaVentaDetalle;
import com.facturaarca.entities.PuntoVenta;
import com.facturaarca.entities.TipoMoneda;
import com.facturaarca.enums.EstadoFactura;
import com.facturaarca.repository.FacturaVentaRepository;
import com.facturaarca.util.FormatoComprobante;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

/**
 * Servicio encargado de generar la representación gráfica en PDF (A4 vertical)
 * de una Factura C electrónica siguiendo la normativa de ARCA
 */
@Service
public class FacturaPdfService {

    public static final String URL_QR_AFIP = "https://www.afip.gob.ar/fe/qr/?p=";
    private static final int CODIGO_AFIP_FACTURA_C = 11;

    private static final DeviceRgb COLOR_PRIMARIO = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb COLOR_GRIS_TEXTO = new DeviceRgb(71, 85, 105);
    private static final DeviceRgb COLOR_FONDO_SUAVE = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb COLOR_BORDE = new DeviceRgb(203, 213, 225);
    private static final DeviceRgb COLOR_BORDE_FUERTE = new DeviceRgb(51, 65, 85);
    private static final DeviceRgb COLOR_ALERTA_BORRADOR_BG = new DeviceRgb(254, 243, 199);
    private static final DeviceRgb COLOR_ALERTA_BORRADOR_FG = new DeviceRgb(146, 64, 14);
    private static final DeviceRgb COLOR_ALERTA_ANULADA_BG = new DeviceRgb(254, 226, 226);
    private static final DeviceRgb COLOR_ALERTA_ANULADA_FG = new DeviceRgb(153, 27, 27);

    private final FacturaVentaRepository facturaVentaRepository;

    @Value("${arca.cuit}")
    private Long arcaCuit;

    @Value("${arca.emisor.nombre-fantasia:Facturitas}")
    private String emisorNombreFantasia;

    @Value("${arca.emisor.razon-social:Martín Alejandro Gómez}")
    private String emisorRazonSocial;

    @Value("${arca.emisor.condicion-iva:Responsable Monotributo}")
    private String emisorCondicionIva;

    @Value("${arca.emisor.ingresos-brutos:Exento}")
    private String emisorIngresosBrutos;

    @Value("${arca.emisor.inicio-actividades:01/01/2024}")
    private String emisorInicioActividades;

    public FacturaPdfService(FacturaVentaRepository facturaVentaRepository) {
        this.facturaVentaRepository = facturaVentaRepository;
    }

    public record PdfComprobante(byte[] bytes, String filename) {}

    @Transactional(readOnly = true)
    public PdfComprobante generarFacturaPdf(Long id) throws IOException {
        FacturaVenta factura = facturaVentaRepository.buscarConDetalles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La factura solicitada no existe"));

        byte[] bytes = renderizarPdf(factura);
        String filename = construirNombreArchivo(factura);
        return new PdfComprobante(bytes, filename);
    }

    public byte[] renderizarPdf(FacturaVenta factura) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        // Margen inferior de 38pt para alojar la numeración "Pág. X/Y" sin solapar el pie fiscal
        Document document = new Document(pdf, PageSize.A4, false);
        document.setMargins(26, 28, 38, 28);

        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont negrita = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        try {
            agregarBandaSuperior(document, factura, negrita);
            agregarCabeceraFiscal(document, factura, regular, negrita);
            agregarDatosReceptor(document, factura, regular, negrita);
            agregarTablaDetalles(document, factura, regular, negrita);
            agregarBloqueTotalesYObservaciones(document, factura, regular, negrita);
            agregarPieFiscalYQr(document, pdf, factura, regular, negrita);
            estamparNumeracionPaginas(document, pdf, regular);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void agregarBandaSuperior(Document document, FacturaVenta factura, PdfFont negrita) {
        EstadoFactura estado = factura.getEstado();
        if (estado != null && estado != EstadoFactura.AUTORIZADA) {
            boolean esAnulada = estado == EstadoFactura.ANULADA;
            DeviceRgb bg = esAnulada ? COLOR_ALERTA_ANULADA_BG : COLOR_ALERTA_BORRADOR_BG;
            DeviceRgb fg = esAnulada ? COLOR_ALERTA_ANULADA_FG : COLOR_ALERTA_BORRADOR_FG;

            String textoAviso = switch (estado) {
                case ANULADA -> "COMPROBANTE ANULADO" + (factura.getFechaAnulacion() != null
                        ? " EL " + formatearFecha(factura.getFechaAnulacion()) : "");
                case RECHAZADA -> "COMPROBANTE RECHAZADO POR ARCA — NO VÁLIDO COMO FACTURA";
                case EN_PROCESO -> "COMPROBANTE EN PROCESO DE AUTORIZACIÓN — SIN CAE";
                default -> "COMPROBANTE EN BORRADOR — NO VÁLIDO COMO FACTURA";
            };

            Table banner = new Table(UnitValue.createPercentArray(1)).setWidth(UnitValue.createPercentValue(100));
            banner.addCell(new Cell()
                    .add(new Paragraph(textoAviso).setFont(negrita).setFontSize(9f).setFontColor(fg))
                    .setBackgroundColor(bg)
                    .setBorder(new SolidBorder(fg, 0.7f))
                    .setPadding(5f)
                    .setTextAlignment(TextAlignment.CENTER));
            banner.setMarginBottom(6f);
            document.add(banner);
        }

        Table bandaOriginal = new Table(UnitValue.createPercentArray(1)).setWidth(UnitValue.createPercentValue(100));
        bandaOriginal.addCell(new Cell()
                .add(new Paragraph("ORIGINAL").setFont(negrita).setFontSize(9.5f).setFontColor(COLOR_PRIMARIO))
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBackgroundColor(COLOR_FONDO_SUAVE)
                .setPaddingTop(4f)
                .setPaddingBottom(4f)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(bandaOriginal);
    }

    private void agregarCabeceraFiscal(Document document, FacturaVenta factura,
                                       PdfFont regular, PdfFont negrita) {
        PuntoVenta pv = factura.getPuntoVenta();
        int ptoVtaNum = pv != null ? pv.getNumero() : 1;
        String ptoVtaStr = FormatoComprobante.formatearPuntoVenta(ptoVtaNum);
        String nroCmpStr = factura.getNumero() != null
                ? FormatoComprobante.formatearNumero8Digitos(factura.getNumero())
                : "PROVISORIO";
        String domicilioComercial = pv != null && pv.getDomicilioComercial() != null && !pv.getDomicilioComercial().isBlank()
                ? pv.getDomicilioComercial()
                : "Domicilio Comercial no informado";
        String sucursal = pv != null && pv.getDescripcion() != null && !pv.getDescripcion().isBlank()
                ? " (" + pv.getDescripcion() + ")"
                : "";

        Table cabecera = new Table(UnitValue.createPercentArray(new float[]{44f, 12f, 44f}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell colIzq = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBorderTop(Border.NO_BORDER)
                .setPadding(10f)
                .setVerticalAlignment(VerticalAlignment.TOP);
        String tituloEmisor = (emisorNombreFantasia != null && !emisorNombreFantasia.isBlank())
                ? emisorNombreFantasia
                : emisorRazonSocial;
        colIzq.add(new Paragraph(tituloEmisor)
                .setFont(negrita).setFontSize(14f).setFontColor(COLOR_PRIMARIO).setMarginBottom(6f));
        colIzq.add(lineaDato("Razón Social: ", emisorRazonSocial, negrita, regular, 8.5f));
        colIzq.add(lineaDato("Domicilio Comercial: ", domicilioComercial + sucursal, negrita, regular, 8.5f));
        colIzq.add(lineaDato("Condición frente al IVA: ", emisorCondicionIva, negrita, regular, 8.5f));

        Cell colCentro = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBackgroundColor(COLOR_FONDO_SUAVE)
                .setPadding(6f)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.CENTER);

        Table recuadroLetra = new Table(UnitValue.createPercentArray(1))
                .setWidth(UnitValue.createPercentValue(100));
        Cell celdaLetra = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPaddingTop(4f)
                .setPaddingBottom(4f)
                .setTextAlignment(TextAlignment.CENTER);
        celdaLetra.add(new Paragraph("C")
                .setFont(negrita).setFontSize(24f).setFontColor(COLOR_PRIMARIO)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0f).setMultipliedLeading(1f));
        celdaLetra.add(new Paragraph("COD. 011")
                .setFont(negrita).setFontSize(7f).setFontColor(COLOR_GRIS_TEXTO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(2f));
        recuadroLetra.addCell(celdaLetra);
        colCentro.add(recuadroLetra);

        Cell colDer = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBorderTop(Border.NO_BORDER)
                .setPadding(10f)
                .setVerticalAlignment(VerticalAlignment.TOP);
        colDer.add(new Paragraph("FACTURA")
                .setFont(negrita).setFontSize(15f).setFontColor(COLOR_PRIMARIO).setMarginBottom(4f));
        colDer.add(lineaDato("Punto de Venta: ", ptoVtaStr + "   Comp. Nro: " + nroCmpStr, negrita, regular, 9f));
        colDer.add(lineaDato("Fecha de Emisión: ",
                factura.getFechaEmision() != null ? formatearFecha(factura.getFechaEmision()) : "Sin emitir",
                negrita, regular, 8.5f));
        colDer.add(lineaDato("CUIT: ", formatearCuitEmisor(), negrita, regular, 8.5f));
        colDer.add(lineaDato("Ingresos Brutos: ", emisorIngresosBrutos, negrita, regular, 8.5f));
        colDer.add(lineaDato("Fecha de Inicio de Actividades: ", emisorInicioActividades, negrita, regular, 8.5f));

        cabecera.addCell(colIzq);
        cabecera.addCell(colCentro);
        cabecera.addCell(colDer);
        document.add(cabecera);
    }

    private void agregarDatosReceptor(Document document, FacturaVenta factura, PdfFont regular, PdfFont negrita) {
        Cliente cliente = factura.getCliente();
        String denominacion = (cliente != null && cliente.getDenominacion() != null && !cliente.getDenominacion().isBlank())
                ? cliente.getDenominacion()
                : "Consumidor Final";
        String cuit = (cliente != null && cliente.getCuitCuil() != null && !cliente.getCuitCuil().isBlank())
                ? cliente.getCuitCuil()
                : "—";
        String condIva = factura.getCondicionIva() != null && factura.getCondicionIva().getDenominacion() != null
                ? factura.getCondicionIva().getDenominacion()
                : "Consumidor Final";
        String domicilioStr = formatearDomicilioCliente(cliente);
        TipoMoneda moneda = factura.getTipoMoneda();
        String monedaStr = moneda != null ? moneda.getDenominacion() + " (" + moneda.getSimbolo() + ")" : "Peso Argentino ($)";

        Table receptor = new Table(UnitValue.createPercentArray(new float[]{46f, 54f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6f);

        Cell izquierda = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBorderRight(Border.NO_BORDER)
                .setPadding(8f);
        izquierda.add(lineaDato("CUIT / CUIL: ", cuit, negrita, regular, 8.5f));
        izquierda.add(lineaDato("Condición frente al IVA: ", condIva, negrita, regular, 8.5f));
        izquierda.add(lineaDato("Condición de venta: ", "Contado", negrita, regular, 8.5f));

        Cell derecha = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBorderLeft(Border.NO_BORDER)
                .setPadding(8f);
        derecha.add(lineaDato("Apellido y Nombre / Razón Social: ", denominacion, negrita, regular, 8.5f));
        derecha.add(lineaDato("Domicilio Comercial: ", domicilioStr, negrita, regular, 8.5f));
        derecha.add(lineaDato("Moneda: ", monedaStr, negrita, regular, 8.5f));

        receptor.addCell(izquierda);
        receptor.addCell(derecha);
        document.add(receptor);
    }

    private void agregarTablaDetalles(Document document, FacturaVenta factura, PdfFont regular, PdfFont negrita) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{12f, 38f, 10f, 15f, 10f, 15f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(10f);

        tabla.addHeaderCell(celdaHeaderTabla("Código", negrita, TextAlignment.LEFT));
        tabla.addHeaderCell(celdaHeaderTabla("Producto / Servicio", negrita, TextAlignment.LEFT));
        tabla.addHeaderCell(celdaHeaderTabla("Cantidad", negrita, TextAlignment.RIGHT));
        tabla.addHeaderCell(celdaHeaderTabla("Precio Unit.", negrita, TextAlignment.RIGHT));
        tabla.addHeaderCell(celdaHeaderTabla("% Bonif.", negrita, TextAlignment.RIGHT));
        tabla.addHeaderCell(celdaHeaderTabla("Subtotal", negrita, TextAlignment.RIGHT));

        String simbolo = factura.getTipoMoneda() != null && factura.getTipoMoneda().getSimbolo() != null
                ? factura.getTipoMoneda().getSimbolo()
                : "$";

        if (factura.getDetalles() == null || factura.getDetalles().isEmpty()) {
            tabla.addCell(new Cell(1, 6)
                    .add(new Paragraph("Sin ítems registrados.").setFont(regular).setFontSize(8.5f).setFontColor(COLOR_GRIS_TEXTO))
                    .setPadding(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
        } else {
            int idx = 0;
            for (FacturaVentaDetalle det : factura.getDetalles()) {
                DeviceRgb fondo = (idx++ % 2 == 1) ? COLOR_FONDO_SUAVE : null;
                Articulo art = det.getListaPrecioArticulo() != null ? det.getListaPrecioArticulo().getArticulo() : null;
                String codigo = art != null && art.getCodigo() != null ? art.getCodigo() : "—";
                String desc = det.getDescripcion() != null && !det.getDescripcion().isBlank()
                        ? det.getDescripcion()
                        : (art != null ? art.getDenominacion() : "Artículo");

                tabla.addCell(celdaDetalle(codigo, regular, TextAlignment.LEFT, fondo));
                tabla.addCell(celdaDetalle(desc, regular, TextAlignment.LEFT, fondo));
                tabla.addCell(celdaDetalle(formatearCantidad(det.getCantidad()), regular, TextAlignment.RIGHT, fondo));
                tabla.addCell(celdaDetalle(formatearMoneda(simbolo, det.getPrecioUnitario()), regular, TextAlignment.RIGHT, fondo));
                tabla.addCell(celdaDetalle(String.format(Locale.forLanguageTag("es-AR"), "%.2f%%", det.getPorcentajeBonificacion()),
                        regular, TextAlignment.RIGHT, fondo));
                tabla.addCell(celdaDetalle(formatearMoneda(simbolo, det.getImporteSubtotal()), negrita, TextAlignment.RIGHT, fondo));
            }
        }

        document.add(tabla);
    }

    private void agregarBloqueTotalesYObservaciones(Document document, FacturaVenta factura,
                                                    PdfFont regular, PdfFont negrita) {
        String simbolo = factura.getTipoMoneda() != null && factura.getTipoMoneda().getSimbolo() != null
                ? factura.getTipoMoneda().getSimbolo()
                : "$";
        double total = factura.getImporteTotal();

        Table bloque = new Table(UnitValue.createPercentArray(new float[]{56f, 44f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(12f)
                .setKeepTogether(true);

        Cell obsCell = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE, 0.8f))
                .setPadding(8f)
                .setVerticalAlignment(VerticalAlignment.TOP);
        obsCell.add(new Paragraph("Observaciones:")
                .setFont(negrita).setFontSize(8.5f).setFontColor(COLOR_PRIMARIO).setMarginBottom(3f));
        String obs = factura.getObservaciones() != null && !factura.getObservaciones().isBlank()
                ? factura.getObservaciones()
                : "—";
        obsCell.add(new Paragraph(obs).setFont(regular).setFontSize(8.5f).setFontColor(COLOR_GRIS_TEXTO));

        Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(COLOR_BORDE_FUERTE, 1f))
                .setBackgroundColor(COLOR_FONDO_SUAVE)
                .setPadding(8f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Table tablaTotales = new Table(UnitValue.createPercentArray(new float[]{55f, 45f}))
                .setWidth(UnitValue.createPercentValue(100));

        tablaTotales.addCell(celdaTotalLabel("Subtotal:", regular, 8.5f));
        tablaTotales.addCell(celdaTotalValor(formatearMoneda(simbolo, total), regular, 8.5f));

        tablaTotales.addCell(celdaTotalLabel("Importe Otros Tributos:", regular, 8.5f));
        tablaTotales.addCell(celdaTotalValor(formatearMoneda(simbolo, 0.0), regular, 8.5f));

        tablaTotales.addCell(celdaTotalLabel("Importe Total:", negrita, 11f).setPaddingTop(5f));
        tablaTotales.addCell(celdaTotalValor(formatearMoneda(simbolo, total), negrita, 11f).setPaddingTop(5f));

        totalesCell.add(tablaTotales);

        bloque.addCell(obsCell);
        bloque.addCell(totalesCell);
        document.add(bloque);
    }

    private void agregarPieFiscalYQr(Document document, PdfDocument pdf, FacturaVenta factura,
                                     PdfFont regular, PdfFont negrita) {
        Table pie = new Table(UnitValue.createPercentArray(new float[]{22f, 42f, 36f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(14f)
                .setKeepTogether(true);

        boolean tieneCae = factura.getCae() != null && !factura.getCae().isBlank();

        if (tieneCae) {
            String urlQr = construirUrlQrArca(factura);
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            // 80pt x 80pt equivale a ~28.2mm x 28.2mm (supera el mínimo legal de 20mm x 20mm)
            Image qrImage = new Image(qrCode.createFormXObject(ColorConstants.BLACK, pdf))
                    .setWidth(80f)
                    .setHeight(80f)
                    .setHorizontalAlignment(HorizontalAlignment.LEFT);

            Cell celdaQr = new Cell()
                    .setBorder(new SolidBorder(COLOR_BORDE, 0.5f))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setPadding(5f)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(qrImage);

            Cell celdaArca = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPaddingLeft(10f);
            celdaArca.add(new Paragraph("ARCA Comprobante Autorizado")
                    .setFont(negrita).setFontSize(10f).setFontColor(COLOR_PRIMARIO).setMarginBottom(3f));
            celdaArca.add(new Paragraph("Esta Administración Federal no se responsabiliza por los datos ingresados en el detalle de la operación.")
                    .setFont(regular).setFontSize(7.5f).setFontColor(COLOR_GRIS_TEXTO));

            Cell celdaCae = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.RIGHT);
            celdaCae.add(new Paragraph("CAE N°: " + factura.getCae())
                    .setFont(negrita).setFontSize(9.5f).setFontColor(COLOR_PRIMARIO)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(4f));
            String vtoCae = factura.getCaeFechaVencimiento() != null
                    ? formatearFecha(factura.getCaeFechaVencimiento())
                    : "—";
            celdaCae.add(new Paragraph("Fecha de Vto. de CAE: " + vtoCae)
                    .setFont(negrita).setFontSize(9f).setFontColor(COLOR_PRIMARIO)
                    .setTextAlignment(TextAlignment.RIGHT));

            pie.addCell(celdaQr);
            pie.addCell(celdaArca);
            pie.addCell(celdaCae);
        } else {
            Cell celdaSinCae = new Cell(1, 3)
                    .setBorder(new SolidBorder(COLOR_BORDE, 0.8f))
                    .setBackgroundColor(COLOR_FONDO_SUAVE)
                    .setPadding(10f)
                    .setTextAlignment(TextAlignment.CENTER);
            celdaSinCae.add(new Paragraph("COMPROBANTE PROVISORIO — SIN AUTORIZACIÓN ELECTRÓNICA (CAE)")
                    .setFont(negrita).setFontSize(9.5f).setFontColor(COLOR_GRIS_TEXTO));
            celdaSinCae.add(new Paragraph("Documento generado únicamente con fines informativos. Informar a ARCA para obtener validez fiscal y Código QR.")
                    .setFont(regular).setFontSize(8f).setFontColor(COLOR_GRIS_TEXTO));
            pie.addCell(celdaSinCae);
        }

        document.add(pie);
    }

    private void estamparNumeracionPaginas(Document document, PdfDocument pdf, PdfFont regular) {
        int totalPaginas = pdf.getNumberOfPages();
        for (int i = 1; i <= totalPaginas; i++) {
            PdfPage page = pdf.getPage(i);
            Rectangle pageSize = page.getPageSize();
            float x = pageSize.getWidth() / 2f;
            float y = 16f;
            Paragraph pagTexto = new Paragraph("Pág. " + i + "/" + totalPaginas)
                    .setFont(regular)
                    .setFontSize(8f)
                    .setFontColor(COLOR_GRIS_TEXTO);
            document.showTextAligned(pagTexto, x, y, i, TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0);
        }
    }

    public String construirUrlQrArca(FacturaVenta factura) {
        String json = construirJsonQrArca(factura);
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return URL_QR_AFIP + base64;
    }

    /**
     * Construye el JSON con los 13 campos obligatorios según RG 4892 para Factura C en Pesos Argentinos.
     */
    public String construirJsonQrArca(FacturaVenta factura) {
        String fecha = factura.getFechaEmision() != null
                ? new SimpleDateFormat("yyyy-MM-dd").format(factura.getFechaEmision())
                : new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        long cuitEmisor = arcaCuit != null ? arcaCuit : 0L;
        int ptoVta = factura.getPuntoVenta() != null ? factura.getPuntoVenta().getNumero() : 1;
        long nroCmp = factura.getNumero() != null ? factura.getNumero() : 0L;
        String importe = String.format(Locale.US, "%.2f", factura.getImporteTotal());

        int tipoDocRec = 99;
        long nroDocRec = 0L;
        Cliente cliente = factura.getCliente();
        if (cliente != null && cliente.getCuitCuil() != null && !cliente.getCuitCuil().isBlank()) {
            String docLimpio = cliente.getCuitCuil().replaceAll("\\D", "");
            if (!docLimpio.isEmpty()) {
                nroDocRec = Long.parseLong(docLimpio);
                tipoDocRec = docLimpio.length() == 11 ? 80 : 96;
            }
        }

        String caeStr = factura.getCae() != null ? factura.getCae().replaceAll("\\D", "") : "0";
        String codAutJson = caeStr.isEmpty() ? "0" : caeStr;

        return String.format(Locale.US,
                "{\"ver\":1,\"fecha\":\"%s\",\"cuit\":%d,\"ptoVta\":%d,\"tipoCmp\":%d,"
                        + "\"nroCmp\":%d,\"importe\":%s,\"moneda\":\"PES\",\"ctz\":1,"
                        + "\"tipoDocRec\":%d,\"nroDocRec\":%d,\"tipoCodAut\":\"E\",\"codAut\":%s}",
                fecha, cuitEmisor, ptoVta, CODIGO_AFIP_FACTURA_C,
                nroCmp, importe, tipoDocRec, nroDocRec, codAutJson);
    }

    private String construirNombreArchivo(FacturaVenta factura) {
        if (factura.getNumero() != null) {
            int ptoVta = factura.getPuntoVenta() != null ? factura.getPuntoVenta().getNumero() : 1;
            return "factura_" + FormatoComprobante.formatearNumeroComprobante(ptoVta, factura.getNumero()) + ".pdf";
        }
        long id = factura.getId() != null ? factura.getId() : 0L;
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            return "factura_anulada_" + id + ".pdf";
        }
        if (factura.getEstado() == EstadoFactura.RECHAZADA) {
            return "factura_rechazada_" + id + ".pdf";
        }
        if (factura.getEstado() == EstadoFactura.EN_PROCESO) {
            return "factura_en_proceso_" + id + ".pdf";
        }
        return "factura_borrador_" + id + ".pdf";
    }

    private String formatearCuitEmisor() {
        if (arcaCuit == null || arcaCuit <= 0) {
            return "No configurado";
        }
        String raw = String.format("%011d", arcaCuit);
        return raw.substring(0, 2) + "-" + raw.substring(2, 10) + "-" + raw.substring(10);
    }

    private static String formatearDomicilioCliente(Cliente cliente) {
        if (cliente == null || cliente.getDomicilio() == null) {
            return "—";
        }
        Domicilio dom = cliente.getDomicilio();
        String calle = dom.getNombreCalle() != null ? dom.getNombreCalle().trim() : "";
        String numero = dom.getNumeroCalle() != null ? dom.getNumeroCalle().trim() : "";
        String combinado = (calle + " " + numero).trim();
        return combinado.isEmpty() ? "—" : combinado;
    }

    private static Paragraph lineaDato(String etiqueta, String valor, PdfFont negrita, PdfFont regular, float size) {
        return new Paragraph()
                .add(new Text(etiqueta).setFont(negrita).setFontSize(size).setFontColor(COLOR_PRIMARIO))
                .add(new Text(valor != null ? valor : "—").setFont(regular).setFontSize(size).setFontColor(COLOR_GRIS_TEXTO))
                .setMarginBottom(2.5f);
    }

    private static Cell celdaHeaderTabla(String texto, PdfFont negrita, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(texto).setFont(negrita).setFontSize(8.5f).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COLOR_PRIMARIO)
                .setBorder(Border.NO_BORDER)
                .setPadding(6f)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell celdaDetalle(String texto, PdfFont font, TextAlignment align, DeviceRgb fondo) {
        Paragraph p = new Paragraph(texto != null ? texto : "—")
                .setFont(font)
                .setFontSize(8.5f)
                .setFontColor(COLOR_PRIMARIO);

        Cell cell = new Cell()
                .add(p)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f))
                .setPaddingTop(5f)
                .setPaddingBottom(5f)
                .setPaddingLeft(6f)
                .setPaddingRight(6f)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (fondo != null) {
            cell.setBackgroundColor(fondo);
        }
        return cell;
    }

    private static Cell celdaTotalLabel(String texto, PdfFont font, float size) {
        return new Cell()
                .add(new Paragraph(texto).setFont(font).setFontSize(size).setFontColor(COLOR_PRIMARIO))
                .setBorder(Border.NO_BORDER)
                .setPadding(2f)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private static Cell celdaTotalValor(String texto, PdfFont font, float size) {
        return new Cell()
                .add(new Paragraph(texto).setFont(font).setFontSize(size).setFontColor(COLOR_PRIMARIO))
                .setBorder(Border.NO_BORDER)
                .setPadding(2f)
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private static String formatearFecha(Date fecha) {
        if (fecha == null) return "—";
        return new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    private static String formatearCantidad(double cantidad) {
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.forLanguageTag("es-AR"), "%.0f", cantidad);
        }
        return String.format(Locale.forLanguageTag("es-AR"), "%.2f", cantidad);
    }

    private static String formatearMoneda(String simbolo, double monto) {
        return simbolo + " " + String.format(Locale.forLanguageTag("es-AR"), "%,.2f", monto);
    }
}
