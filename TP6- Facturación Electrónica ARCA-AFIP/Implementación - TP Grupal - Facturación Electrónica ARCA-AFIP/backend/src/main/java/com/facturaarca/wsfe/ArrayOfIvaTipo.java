
package com.facturaarca.wsfe;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ArrayOfIvaTipo complex type.</p>
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.</p>
 * 
 * <pre>{@code
 * <complexType name="ArrayOfIvaTipo">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="IvaTipo" type="{http://ar.gov.afip.dif.FEV1/}IvaTipo" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfIvaTipo", propOrder = {
    "ivaTipo"
})
public class ArrayOfIvaTipo {

    @XmlElement(name = "IvaTipo", nillable = true)
    protected List<IvaTipo> ivaTipo;

    /**
     * Gets the value of the ivaTipo property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ivaTipo property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getIvaTipo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IvaTipo }
     * </p>
     * 
     * 
     * @return
     *     The value of the ivaTipo property.
     */
    public List<IvaTipo> getIvaTipo() {
        if (ivaTipo == null) {
            ivaTipo = new ArrayList<>();
        }
        return this.ivaTipo;
    }

}
