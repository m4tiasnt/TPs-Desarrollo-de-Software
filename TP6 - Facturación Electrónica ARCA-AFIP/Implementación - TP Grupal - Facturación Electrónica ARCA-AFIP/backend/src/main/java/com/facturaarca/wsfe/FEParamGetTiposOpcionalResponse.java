
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
 *         <element name="FEParamGetTiposOpcionalResult" type="{http://ar.gov.afip.dif.FEV1/}OpcionalTipoResponse" minOccurs="0"/>
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
    "feParamGetTiposOpcionalResult"
})
@XmlRootElement(name = "FEParamGetTiposOpcionalResponse")
public class FEParamGetTiposOpcionalResponse {

    @XmlElement(name = "FEParamGetTiposOpcionalResult")
    protected OpcionalTipoResponse feParamGetTiposOpcionalResult;

    /**
     * Obtiene el valor de la propiedad feParamGetTiposOpcionalResult.
     * 
     * @return
     *     possible object is
     *     {@link OpcionalTipoResponse }
     *     
     */
    public OpcionalTipoResponse getFEParamGetTiposOpcionalResult() {
        return feParamGetTiposOpcionalResult;
    }

    /**
     * Define el valor de la propiedad feParamGetTiposOpcionalResult.
     * 
     * @param value
     *     allowed object is
     *     {@link OpcionalTipoResponse }
     *     
     */
    public void setFEParamGetTiposOpcionalResult(OpcionalTipoResponse value) {
        this.feParamGetTiposOpcionalResult = value;
    }

}
