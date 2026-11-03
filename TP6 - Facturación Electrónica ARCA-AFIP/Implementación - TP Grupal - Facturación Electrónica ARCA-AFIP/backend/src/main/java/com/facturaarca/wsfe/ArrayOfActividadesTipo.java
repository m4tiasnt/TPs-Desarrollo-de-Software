
package com.facturaarca.wsfe;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ArrayOfActividadesTipo complex type.</p>
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.</p>
 * 
 * <pre>{@code
 * <complexType name="ArrayOfActividadesTipo">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="ActividadesTipo" type="{http://ar.gov.afip.dif.FEV1/}ActividadesTipo" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfActividadesTipo", propOrder = {
    "actividadesTipo"
})
public class ArrayOfActividadesTipo {

    @XmlElement(name = "ActividadesTipo", nillable = true)
    protected List<ActividadesTipo> actividadesTipo;

    /**
     * Gets the value of the actividadesTipo property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the actividadesTipo property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getActividadesTipo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ActividadesTipo }
     * </p>
     * 
     * 
     * @return
     *     The value of the actividadesTipo property.
     */
    public List<ActividadesTipo> getActividadesTipo() {
        if (actividadesTipo == null) {
            actividadesTipo = new ArrayList<>();
        }
        return this.actividadesTipo;
    }

}
