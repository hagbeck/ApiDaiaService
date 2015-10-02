package de.tu_dortmund.ub.api.daia.ils.model;

import de.tu_dortmund.ub.api.daia.model.Item;
import de.tu_dortmund.ub.api.daia.model.Message;

import java.util.ArrayList;
import java.util.HashMap;

public class Document {

    // DAIA parameters
	private String id;
	private String href;
	private ArrayList<Item> item;
    private ArrayList<Message> messages;

    // more parameter
    private String erschform;
    private String veroefart;
    private String issn;
    private HashMap<String,String> url;
    private HashMap<String,String> urlbem;
    private HashMap<String,String> lokaleurl;
    private HashMap<String,String> lokaleurlbem;
    private String zdbid;
    private String issnwww;
    private String issnprint;
    private String mediatype;
    private String ausgabe;
    private String erschjahr;
    private String umfang;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public ArrayList<Item> getItem() {
		return item;
	}

	public void setItem(ArrayList<Item> item) {
		this.item = item;
	}

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getErschform() {
        return erschform;
    }

    public void setErschform(String erschform) {
        this.erschform = erschform;
    }

    public String getVeroefart() {
        return veroefart;
    }

    public void setVeroefart(String veroefart) {
        this.veroefart = veroefart;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public HashMap<String,String> getUrl() {
        return url;
    }

    public void setUrl(HashMap<String,String> url) {
        this.url = url;
    }

    public HashMap<String,String> getUrlbem() {
        return urlbem;
    }

    public void setUrlbem(HashMap<String,String> urlbem) {
        this.urlbem = urlbem;
    }

    public HashMap<String,String> getLokaleurl() {
        return lokaleurl;
    }

    public void setLokaleurl(HashMap<String,String> lokaleurl) {
        this.lokaleurl = lokaleurl;
    }

    public HashMap<String,String> getLokaleurlbem() {
        return lokaleurlbem;
    }

    public void setLokaleurlbem(HashMap<String,String> lokaleurlbem) {
        this.lokaleurlbem = lokaleurlbem;
    }

    public String getZdbid() {
        return zdbid;
    }

    public void setZdbid(String zdbid) {
        this.zdbid = zdbid;
    }

    public String getIssnwww() {
        return issnwww;
    }

    public void setIssnwww(String issnwww) {
        this.issnwww = issnwww;
    }

    public String getIssnprint() {
        return issnprint;
    }

    public void setIssnprint(String issnprint) {
        this.issnprint = issnprint;
    }

    public String getMediatype() {
        return mediatype;
    }

    public void setMediatype(String mediatype) {
        this.mediatype = mediatype;
    }

    public String getAusgabe() {
        return ausgabe;
    }

    public void setAusgabe(String ausgabe) {
        this.ausgabe = ausgabe;
    }

    public String getErschjahr() {
        return erschjahr;
    }

    public void setErschjahr(String erschjahr) {
        this.erschjahr = erschjahr;
    }

    public String getUmfang() {
        return umfang;
    }

    public void setUmfang(String umfang) {
        this.umfang = umfang;
    }
}
