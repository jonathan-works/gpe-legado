
package br.quartzo.soap.ws.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de anonymous complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conte�do esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Jsonin" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "jsonin"
})
@XmlRootElement(name = "WSControleAcessoGPE.GETMENUGXUI")
public class WSControleAcessoGPEGETMENUGXUI {

    @XmlElement(name = "Jsonin", required = true)
    protected String jsonin;

    /**
     * Obt�m o valor da propriedade jsonin.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJsonin() {
        return jsonin;
    }

    /**
     * Define o valor da propriedade jsonin.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJsonin(String value) {
        this.jsonin = value;
    }

}
