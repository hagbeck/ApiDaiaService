package de.tu_dortmund.ub.api.daia.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * JAXB model class for the Item element of a DAIA response
 *
 * @author Dipl.-Math. Hans-Georg Becker (M.L.I.S.)
 * @version 2015-03-06
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "item")
public class Item {

	private String id;
	private String href;
    private String part;

    private ArrayList<Message> messages;
	private String label;
	private Department department;
	private Storage storage;

	private ArrayList<Available> available;
	private ArrayList<Unavailable> unavailable;


    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @XmlAttribute
    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    @XmlElement(name = "message")
    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    @XmlElement(name = "label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @XmlElement(name = "department")
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @XmlElement(name = "storage")
    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    @XmlElement(name = "available")
    public ArrayList<Available> getAvailable() {
        return available;
    }

    public void setAvailable(ArrayList<Available> available) {
        this.available = available;
    }

    @XmlElement(name = "unavailable")
    public ArrayList<Unavailable> getUnavailable() {
        return unavailable;
    }

    public void setUnavailable(ArrayList<Unavailable> unavailable) {
        this.unavailable = unavailable;
    }
}
