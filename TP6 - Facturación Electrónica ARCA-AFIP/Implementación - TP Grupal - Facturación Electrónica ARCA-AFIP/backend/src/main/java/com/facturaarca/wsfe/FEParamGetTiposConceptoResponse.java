
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
 *         <element name="FEParamGetTiposConceptoResult" type="{http://ar.gov.afip.dif.FEV1/}ConceptoTipoResponse" minOccurs="0"/>
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
    "feParamGetTiposConceptoResult"
})
@XmlRootElement(name = "FEParamGetTiposConceptoResponse")
public class FEParamGetTiposConceptoResponse {

    @XmlElement(name = "FEParamGetTiposConceptoResult")
    protected ConceptoTipoResponse feParamGetTiposConceptoResult;

    /**
     * Obtiene el valor de la propiedad feParamGetTiposConceptoResult.
     * 
     * @return
     *     possible object is
     *     {@link ConceptoTipoResponse }
     *     
     */
    public ConceptoTipoResponse getFEParamGetTiposConceptoResult() {
        return feParamGetTiposConceptoResult;
    }

    /**
     * Define el valor de la propiedad feParamGetTiposConceptoResult.
     * 
     * @param value
     *     allowed object is
     *     {@link ConceptoTipoResponse }
     *     
     */
    public void setFEParamGetTiposConceptoResult(ConceptoTipoResponse value) {
        this.feParamGetTiposConceptoResult = value;
    }

}
