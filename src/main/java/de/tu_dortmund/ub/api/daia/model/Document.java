package de.tu_dortmund.ub.api.daia.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * JAXB model class for the Document element of a DAIA response
 *
 * @author Dipl.-Math. Hans-Georg Becker (M.L.I.S.)
 * @version 2015-03-06
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "document")
public class Document {

	private String id;
	private String href;

    private String edition;
	
	private ArrayList<Item> item;

    private ArrayList<Message> messages;

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

    @XmlElement(name = "edition")
    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    @XmlElement(name = "item")
	public ArrayList<Item> getItem() {
		return item;
	}

	public void setItem(ArrayList<Item> item) {
		this.item = item;
	}

    @XmlElement(name = "message")
    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }
}
