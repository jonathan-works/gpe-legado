
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
 *         &lt;element name="Registroin" type="{br.com.abaco.sisbase}SDTRegistro"/&gt;
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
    "registroin"
})
@XmlRootElement(name = "WSControleAcessoGPE.GETORGANOGRAMAUSUARIOAUT")
public class WSControleAcessoGPEGETORGANOGRAMAUSUARIOAUT {

    @XmlElement(name = "Registroin", required = true)
    protected SDTRegistro registroin;

    /**
     * Obt�m o valor da propriedade registroin.
     * 
     * @return
     *     possible object is
     *     {@link SDTRegistro }
     *     
     */
    public SDTRegistro getRegistroin() {
        return registroin;
    }

    /**
     * Define o valor da propriedade registroin.
     * 
     * @param value
     *     allowed object is
     *     {@link SDTRegistro }
     *     
     */
    public void setRegistroin(SDTRegistro value) {
        this.registroin = value;
    }

}
