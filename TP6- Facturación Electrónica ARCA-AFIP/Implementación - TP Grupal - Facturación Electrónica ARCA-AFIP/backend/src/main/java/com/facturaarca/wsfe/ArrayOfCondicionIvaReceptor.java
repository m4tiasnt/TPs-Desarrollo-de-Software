
package com.facturaarca.wsfe;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ArrayOfCondicionIvaReceptor complex type.</p>
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.</p>
 * 
 * <pre>{@code
 * <complexType name="ArrayOfCondicionIvaReceptor">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="CondicionIvaReceptor" type="{http://ar.gov.afip.dif.FEV1/}CondicionIvaReceptor" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfCondicionIvaReceptor", propOrder = {
    "condicionIvaReceptor"
})
public class ArrayOfCondicionIvaReceptor {

    @XmlElement(name = "CondicionIvaReceptor", nillable = true)
    protected List<CondicionIvaReceptor> condicionIvaReceptor;

    /**
     * Gets the value of the condicionIvaReceptor property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the condicionIvaReceptor property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getCondicionIvaReceptor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CondicionIvaReceptor }
     * </p>
     * 
     * 
     * @return
     *     The value of the condicionIvaReceptor property.
     */
    public List<CondicionIvaReceptor> getCondicionIvaReceptor() {
        if (condicionIvaReceptor == null) {
            condicionIvaReceptor = new ArrayList<>();
        }
        return this.condicionIvaReceptor;
    }

}
