
package com.facturaarca.wsfe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para anonymous complex type.</p>
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.</p>
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="FECompConsultarResult" type="{http://ar.gov.afip.dif.FEV1/}FECompConsultaResponse" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "feCompConsultarResult"
})
@XmlRootElement(name = "FECompConsultarResponse")
public class FECompConsultarResponse {

    @XmlElement(name = "FECompConsultarResult")
    protected FECompConsultaResponse feCompConsultarResult;

    /**
     * Obtiene el valor de la propiedad feCompConsultarResult.
     * 
     * @return
     *     possible object is
     *     {@link FECompConsultaResponse }
     *     
     */
    public FECompConsultaResponse getFECompConsultarResult() {
        return feCompConsultarResult;
    }

    /**
     * Define el valor de la propiedad feCompConsultarResult.
     * 
     * @param value
     *     allowed object is
     *     {@link FECompConsultaResponse }
     *     
     */
    public void setFECompConsultarResult(FECompConsultaResponse value) {
        this.feCompConsultarResult = value;
    }

}
