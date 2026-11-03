
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
 *         <element name="FEParamGetTiposCbteResult" type="{http://ar.gov.afip.dif.FEV1/}CbteTipoResponse" minOccurs="0"/>
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
    "feParamGetTiposCbteResult"
})
@XmlRootElement(name = "FEParamGetTiposCbteResponse")
public class FEParamGetTiposCbteResponse {

    @XmlElement(name = "FEParamGetTiposCbteResult")
    protected CbteTipoResponse feParamGetTiposCbteResult;

    /**
     * Obtiene el valor de la propiedad feParamGetTiposCbteResult.
     * 
     * @return
     *     possible object is
     *     {@link CbteTipoResponse }
     *     
     */
    public CbteTipoResponse getFEParamGetTiposCbteResult() {
        return feParamGetTiposCbteResult;
    }

    /**
     * Define el valor de la propiedad feParamGetTiposCbteResult.
     * 
     * @param value
     *     allowed object is
     *     {@link CbteTipoResponse }
     *     
     */
    public void setFEParamGetTiposCbteResult(CbteTipoResponse value) {
        this.feParamGetTiposCbteResult = value;
    }

}
