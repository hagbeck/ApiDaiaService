/*
The MIT License (MIT)

Copyright (c) 2015, Hans-Georg Becker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

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
