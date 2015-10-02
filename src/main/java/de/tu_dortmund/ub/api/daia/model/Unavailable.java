package de.tu_dortmund.ub.api.daia.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * JAXB model class for the Unavailable element of a DAIA response
 *
 * @author Dipl.-Math. Hans-Georg Becker (M.L.I.S.)
 * @version 2015-03-06
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "unavailable")
public class Unavailable {

    private String service;
    private String href;
    private String expected;
    private String queue;

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
    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    @XmlAttribute
    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    @XmlElement(name = "message")
    public ArrayList<Message> getMessage() {
        return message;
    }

    public void setMessages(ArrayList<Message> message) {
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
