
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
 *         <element name="FECAEASolicitarResult" type="{http://ar.gov.afip.dif.FEV1/}FECAEAGetResponse" minOccurs="0"/>
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
    "fecaeaSolicitarResult"
})
@XmlRootElement(name = "FECAEASolicitarResponse")
public class FECAEASolicitarResponse {

    @XmlElement(name = "FECAEASolicitarResult")
    protected FECAEAGetResponse fecaeaSolicitarResult;

    /**
     * Obtiene el valor de la propiedad fecaeaSolicitarResult.
     * 
     * @return
     *     possible object is
     *     {@link FECAEAGetResponse }
     *     
     */
    public FECAEAGetResponse getFECAEASolicitarResult() {
        return fecaeaSolicitarResult;
    }

    /**
     * Define el valor de la propiedad fecaeaSolicitarResult.
     * 
     * @param value
     *     allowed object is
     *     {@link FECAEAGetResponse }
     *     
     */
    public void setFECAEASolicitarResult(FECAEAGetResponse value) {
        this.fecaeaSolicitarResult = value;
    }

}
