package de.tu_dortmund.ub.api.daia.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * JAXB model class for the root element of a DAIA response
 *
 * @author Dipl.-Math. Hans-Georg Becker (M.L.I.S.)
 * @version 2015-03-06
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "daia")
public class Daia {

    private String version;
    private String schema;
    private String timestamp;
    private Institution institution;
    private ArrayList<Document> document;

    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @XmlAttribute
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @XmlAttribute
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @XmlElement(name = "institution")
    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @XmlElement(name = "document")
    public ArrayList<Document> getDocument() {
        return document;
    }

    public void setDocument(ArrayList<Document> document) {
        this.document = document;
    }
}
