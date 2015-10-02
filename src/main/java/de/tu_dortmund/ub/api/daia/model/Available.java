package de.tu_dortmund.ub.api.daia.model;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * JAXB model class for the Available element of a DAIA response
 *
 * @author Dipl.-Math. Hans-Georg Becker (M.L.I.S.)
 * @version 2015-03-06
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "available")
public class Available {

    private String service;
    private String href;
    private String delay;

    private ArrayList<Message> message;
    private ArrayList<Limitation> limitation;

    @XmlAttribute
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @XmlAttribute
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @XmlAttribute
    public String getDelay() {
        return delay;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    @XmlElement(name = "message")
    public ArrayList<Message> getMessage() {

        return message;
    }

    public void setMessage(ArrayList<Message> message) {
        this.message = message;
    }

    @XmlElement(name = "limitation")
    public ArrayList<Limitation> getLimitation() {

        return limitation;
    }

    public void setLimitation(ArrayList<Limitation> limitation) {
        this.limitation = limitation;
    }
}
