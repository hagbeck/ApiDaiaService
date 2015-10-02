package de.tu_dortmund.ub.api.daia.jop.model;

import de.tu_dortmund.ub.api.daia.model.Item;
import de.tu_dortmund.ub.api.daia.model.Message;

import java.util.ArrayList;

public class Document {

    // DAIA parameters
	private String id;
	private String href;
	private ArrayList<Item> item;
    private ArrayList<Message> messages;

    // more parameter
    private boolean existsDigitalItems;
    private int countDigitlItems;

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

    public boolean isExistsDigitalItems() {
        return existsDigitalItems;
    }

    public void setExistsDigitalItems(boolean existsDigitalItems) {
        this.existsDigitalItems = existsDigitalItems;
    }

    public void setCountDigitlItems(int countDigitlItems) {
        this.countDigitlItems = countDigitlItems;
    }

    public int getCountDigitlItems() {
        return countDigitlItems;
    }
}
