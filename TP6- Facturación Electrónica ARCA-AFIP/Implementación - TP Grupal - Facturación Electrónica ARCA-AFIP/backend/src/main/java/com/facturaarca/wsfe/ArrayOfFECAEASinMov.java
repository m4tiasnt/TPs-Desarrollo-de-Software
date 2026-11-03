
package com.facturaarca.wsfe;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ArrayOfFECAEASinMov complex type.</p>
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.</p>
 * 
 * <pre>{@code
 * <complexType name="ArrayOfFECAEASinMov">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="FECAEASinMov" type="{http://ar.gov.afip.dif.FEV1/}FECAEASinMov" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfFECAEASinMov", propOrder = {
    "fecaeaSinMov"
})
public class ArrayOfFECAEASinMov {

    @XmlElement(name = "FECAEASinMov", nillable = true)
    protected List<FECAEASinMov> fecaeaSinMov;

    /**
     * Gets the value of the fecaeaSinMov property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fecaeaSinMov property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getFECAEASinMov().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FECAEASinMov }
     * </p>
     * 
     * 
     * @return
     *     The value of the fecaeaSinMov property.
     */
    public List<FECAEASinMov> getFECAEASinMov() {
        if (fecaeaSinMov == null) {
            fecaeaSinMov = new ArrayList<>();
        }
        return this.fecaeaSinMov;
    }

}
