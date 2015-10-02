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

package de.tu_dortmund.ub.api.daia;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tu_dortmund.ub.api.daia.acm.AcmResolver;
import de.tu_dortmund.ub.api.daia.ieee.IeeeResolver;
import de.tu_dortmund.ub.api.daia.ils.IntegratedLibrarySystem;
import de.tu_dortmund.ub.api.daia.jop.JOPException;
import de.tu_dortmund.ub.api.daia.jop.JournalOnlinePrintService;
import de.tu_dortmund.ub.api.daia.linkresolver.LinkResolver;
import de.tu_dortmund.ub.api.daia.linkresolver.LinkResolverException;
import de.tu_dortmund.ub.api.daia.lncs.LncsResolver;
import de.tu_dortmund.ub.api.daia.model.*;
import de.tu_dortmund.ub.api.daia.model.Document;
import de.tu_dortmund.ub.util.impl.Lookup;
import de.tu_dortmund.ub.util.impl.Mailer;
import net.sf.saxon.s9api.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom2.*;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class DaiaOpenUrlEndpoint extends HttpServlet {

    private String conffile  = "";

    private Properties config = new Properties();

    private Logger logger = Logger.getLogger(DaiaOpenUrlEndpoint.class.getName());

    private boolean isTUintern = false;
    private boolean isUBintern = false;

    public DaiaOpenUrlEndpoint() throws IOException {

		this("conf/api-test.properties");
	}

	public DaiaOpenUrlEndpoint(String propfile_api) throws IOException {

        this.conffile = propfile_api;

        // Init properties
        try {
            InputStream inputStream = new FileInputStream(this.conffile);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                try {
                    config.load(reader);

                } finally {
                    reader.close();
                }
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException e) {
            System.out.println("FATAL ERROR: Die Datei '" + this.conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(config.getProperty("service.log4j-conf"));

        logger.info("[" + config.getProperty("service.name") + "] " + "Starting 'DaiaService OpenURL Endpoint' ...");
        logger.info("[" + config.getProperty("service.name") + "] " + "conf-file = " + conffile);
        logger.info("[" + config.getProperty("service.name") + "] " + "log4j-conf-file = " + config.getProperty("service.log4j-conf"));
	}

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Methods", config.getProperty("Access-Control-Allow-Methods"));
        response.addHeader("Access-Control-Allow-Headers", config.getProperty("Access-Control-Allow-Headers"));
        response.setHeader("Accept", config.getProperty("Accept"));
        response.setHeader("Access-Control-Allow-Origin", config.getProperty("Access-Control-Allow-Origin"));

        response.getWriter().println();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String ip = request.getHeader("X-Forwarded-For");

        try {
            if (ip != null) {

                String[] ranges = config.getProperty("service.iprange.tu").split("\\|");
                for (String range : ranges) {

                    if (ip.matches(range)) {

                        isTUintern = true;
                        break;
                    }
                }

                String[] exceptions = config.getProperty("service.iprange.tu.exceptions").split("\\|");
                if (isTUintern) {

                    for (String exception : exceptions) {

                        if (ip.matches(exception)) {

                            isTUintern = false;
                            break;
                        }
                    }
                }

                ranges = config.getProperty("service.iprange.ub").split("\\|");
                for (String range : ranges) {

                    if (ip.matches(range)) {

                        isUBintern = true;
                        break;
                    }
                }
                exceptions = config.getProperty("service.iprange.ub.exceptions").split("\\|");
                if (isUBintern) {

                    for (String exception : exceptions) {

                        if (ip.matches(exception)) {

                            isUBintern = false;
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {

            this.logger.error(e.getMessage(), e.getCause());
        }
        this.logger.info("Where is it from? " + request.getHeader("X-Forwarded-For") + ", " + isTUintern + ", " + isUBintern);

        // format
        String format = "html";

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerNameKey = headerNames.nextElement();

            if (headerNameKey.equals("Accept")) {

                if (request.getHeader(headerNameKey).contains("text/html")) {
                    format = "html";
                } else if (request.getHeader(headerNameKey).contains("application/xml")) {
                    format = "xml";
                } else if (request.getHeader(headerNameKey).contains("application/json")) {
                    format = "json";
                }
            }
        }

        // read query
        this.logger.debug("TEST 'getQueryString': " + request.getQueryString());

        String queryString = null;
        if (request.getQueryString() == null || request.getQueryString().equals("")) {

            if (format.equals("html")) {

                try {

                    JAXBContext context = JAXBContext.newInstance(Daia.class);
                    Marshaller m = context.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                    // Write to HttpResponse
                    org.jdom2.Document doc = new org.jdom2.Document();
                    doc.setRootElement(new Element("daia"));

                    HashMap<String,String> parameters = new HashMap<String,String>();
                    parameters.put("lang", "de");
                    parameters.put("isTUintern", Boolean.toString(isTUintern));
                    parameters.put("isUBintern", Boolean.toString(isUBintern));

                    String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                    response.setContentType("text/html;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(html);
                }
                catch (PropertyException e) {
                    this.logger.error(e.getMessage(), e.getCause());
                } catch (JAXBException e) {
                    this.logger.error(e.getMessage(), e.getCause());
                }
            }
            else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query is empty.");
            }
        }
        else {

            if (request.getQueryString().contains("%26")) {
                this.logger.debug("TEST 'getQueryString': " + request.getQueryString());

                queryString = request.getQueryString().replaceAll("%26", "xXx");
                this.logger.debug("TEST 'getQueryString': " + queryString);

            } else {
                queryString = request.getQueryString();
            }

            if (queryString.contains("%0A")) {

                queryString = queryString.replaceAll("%0A", "");
                this.logger.debug("TEST 'queryString': " + queryString);
            }

            if ((queryString.contains("__char_set=latin1") && !queryString.contains("sid=semantics")) || queryString.contains("sid=GBI:wiwi")) {

                queryString = URLDecoder.decode(queryString, "ISO-8859-1");
            } else if (queryString.contains("__char_set=latin1") && queryString.contains("sid=semantics")) {

                // Tue erstmal nix
                this.logger.debug("semantics?");
            } else {
                queryString = URLDecoder.decode(queryString, "UTF-8");
            }

            HashMap<String, String> latinParameters = new HashMap<String, String>();

            for (String param : queryString.split("&")) {

                String[] tmp = param.split("=");
                try {

                    if (tmp[1].contains("xXx")) {
                        latinParameters.put(tmp[0], tmp[1].replaceAll("xXx", "&"));
                    } else {
                        latinParameters.put(tmp[0], tmp[1]);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    latinParameters.put(tmp[0], "");
                }

            }

            if (latinParameters.get("format") != null && !latinParameters.get("format").equals("")) {

                format = latinParameters.get("format");
            }

            this.logger.debug("format = " + format);

            // build response
            response.setHeader("Access-Control-Allow-Origin", config.getProperty("Access-Control-Allow-Origin"));

            if (format.equals("")) {

                if (format.equals("html")) {

                    try {

                        JAXBContext context = JAXBContext.newInstance(Daia.class);
                        Marshaller m = context.createMarshaller();
                        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                        // Write to HttpResponse
                        org.jdom2.Document doc = new org.jdom2.Document();
                        doc.setRootElement(new Element("daia"));

                        HashMap<String,String> parameters = new HashMap<String,String>();
                        parameters.put("lang", "de");
                        parameters.put("isTUintern", Boolean.toString(isTUintern));
                        parameters.put("isUBintern", Boolean.toString(isUBintern));

                        String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                        response.setContentType("text/html;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().println(html);
                    }
                    catch (PropertyException e) {
                        this.logger.error(e.getMessage(), e.getCause());
                    } catch (JAXBException e) {
                        this.logger.error(e.getMessage(), e.getCause());
                    }
                }
                else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid {FORMAT} requested.");
                }

            } else {

                this.daia(request, response, format, latinParameters);
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String ip = request.getHeader("X-Forwarded-For");

        try {
            if (ip != null) {

                String[] ranges = config.getProperty("service.iprange.tu").split("\\|");
                for (String range : ranges) {

                    if (ip.matches(range)) {

                        isTUintern = true;
                        break;
                    }
                }

                String[] exceptions = config.getProperty("service.iprange.tu.exceptions").split("\\|");
                if (isTUintern) {

                    for (String exception : exceptions) {

                        if (ip.matches(exception)) {

                            isTUintern = false;
                            break;
                        }
                    }
                }

                ranges = config.getProperty("service.iprange.ub").split("\\|");
                for (String range : ranges) {

                    if (ip.matches(range)) {

                        isUBintern = true;
                        break;
                    }
                }
                exceptions = config.getProperty("service.iprange.ub.exceptions").split("\\|");
                if (isUBintern) {

                    for (String exception : exceptions) {

                        if (ip.matches(exception)) {

                            isUBintern = false;
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {

            this.logger.error(e.getMessage(), e.getCause());
        }
        this.logger.debug("Where is it from? " + request.getHeader("X-Forwarded-For") + ", " + isTUintern + ", " + isUBintern);

        // read query
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();

            while ((line = reader.readLine()) != null) {

                jb.append(line);
            }

            this.logger.debug("POST-INPUT: \n\t" + jb);

        } catch (Exception e) {

            this.logger.error(e.getMessage(), e.getCause());
        }

        // format
        String format = "html";

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {

            String headerNameKey = headerNames.nextElement();

            if (headerNameKey.equals("Accept")) {

                if (request.getHeader(headerNameKey).contains("text/html")) {
                    format = "html";
                } else if (request.getHeader(headerNameKey).contains("application/xml")) {
                    format = "xml";
                } else if (request.getHeader(headerNameKey).contains("application/json")) {
                    format = "json";
                }
            }
        }

        // read query
        String queryString = null;

        if (jb.length() == 0) {

            if (format.equals("html")) {

                try {

                    JAXBContext context = JAXBContext.newInstance(Daia.class);
                    Marshaller m = context.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                    // Write to HttpResponse
                    org.jdom2.Document doc = new org.jdom2.Document();
                    doc.setRootElement(new Element("daia"));

                    HashMap<String,String> parameters = new HashMap<String,String>();
                    parameters.put("lang", "de");
                    parameters.put("isTUintern", Boolean.toString(isTUintern));
                    parameters.put("isUBintern", Boolean.toString(isUBintern));

                    String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                    response.setContentType("text/html;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(html);
                }
                catch (PropertyException e) {
                    this.logger.error(e.getMessage(), e.getCause());
                } catch (JAXBException e) {
                    this.logger.error(e.getMessage(), e.getCause());
                }
            }
            else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query is empty.");
            }
        }
        else {

            if (jb.toString().contains("__char_set=latin1") || jb.toString().contains("sid=semantics") || jb.toString().contains("sid=GBI:wiwi")) {
                this.logger.debug("TEST 'getQueryString': " + URLDecoder.decode(jb.toString(), "ISO-8859-1"));

                queryString = URLDecoder.decode(jb.toString(), "ISO-8859-1");

            } else {
                queryString = URLDecoder.decode(jb.toString(), "UTF-8");
            }

            HashMap<String, String> latinParameters = new HashMap<String, String>();

            for (String param : queryString.split("&")) {

                String[] tmp = param.split("=");
                try {
                    latinParameters.put(tmp[0], tmp[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    latinParameters.put(tmp[0], "");
                }
            }

            if (latinParameters.get("format") != null && !latinParameters.get("format").equals("")) {

                format = latinParameters.get("format");
            }

            this.logger.debug("format = " + format);

            // build response
            response.setHeader("Access-Control-Allow-Origin", config.getProperty("Access-Control-Allow-Origin"));

            if (format.equals("")) {

                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid {FORMAT} requested.");
            } else {

                this.daia(request, response, format, latinParameters);
            }
        }
    }

    private void daia(HttpServletRequest request, HttpServletResponse response, String format, HashMap<String, String> latinParameters) throws IOException {

        String openurl = "";

        try {

            // build openurl
            for (String k : latinParameters.keySet()) {

                // repeat input-openurl
                if (!k.equals("format") && !k.equals("system") && !latinParameters.get(k).equals("")) {

                    if (latinParameters.get(k).contains("/>")) {
                        latinParameters.put(k, latinParameters.get(k).replaceAll("/>",""));
                    }
                    if (latinParameters.get(k).contains(">")) {
                        latinParameters.put(k, latinParameters.get(k).replaceAll(">",""));
                    }

                    String prefix = "";
                    if (!k.startsWith("rft")) {
                        prefix = "rft.";
                    }

                    String value = "";

                    if (k.contains("title") || k.contains("au")) {
                        value = latinParameters.get(k);
                    }
                    else if (k.contains("issn") && !latinParameters.get(k).equals("")) {

                        if (latinParameters.get(k).contains("-")) {
                            value = latinParameters.get(k);
                        }
                        else {
                            value = latinParameters.get(k).subSequence(0,4) + "-" + latinParameters.get(k).subSequence(4,8);
                        }
                    }
                    else {
                        value = latinParameters.get(k);
                    }

                    openurl += "&" + prefix + k + "=" + URLEncoder.encode(value,"UTF-8");
                }
            }
            this.logger.debug("\n" + "\tOpenURL-Parameter = " + this.config.getProperty("linkresolver.baseurl") + this.config.getProperty("linkresolver.parameters") + openurl);

            ArrayList<Document> daiaDocuments = new ArrayList<Document>();

            // falls OpenURL contains isbn: Anfrage an "normalen DaiaService Endpoint
            String isbn = "";
            if (latinParameters.get("rft.isbn") != null && !latinParameters.get("rft.isbn").equals("")) {
                isbn = latinParameters.get("rft.isbn");
            }
            else if (latinParameters.get("isbn") != null && !latinParameters.get("isbn").equals("")) {
                isbn = latinParameters.get("isbn");
            }
            else if (latinParameters.get("id") != null && latinParameters.get("id").contains("isbn:")) {
                isbn = latinParameters.get("id").split(":")[1];
            }
            this.logger.debug("ISBN = " + isbn);

            if (!isbn.equals("")) {

                if (Lookup.lookupAll(IntegratedLibrarySystem.class).size() > 0) {
                    // erst DAIA isbn fragen
                    if (isbn.contains("; ")) {
                        isbn = isbn.replaceAll("; ", ",");
                    }
                    String daiaUrl = "http://" + request.getServerName() + ":" + request.getServerPort() + "/daia/?id=isbn:" + isbn;
                    this.logger.debug("daiaUrl = " + daiaUrl);
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet(daiaUrl);
                    httpGet.addHeader("Accept", "application/xml");

                    CloseableHttpResponse httpResponse = httpclient.execute(httpGet);

                    try {

                        int statusCode = httpResponse.getStatusLine().getStatusCode();
                        HttpEntity httpEntity = httpResponse.getEntity();

                        switch (statusCode) {

                            case 200: {

                                JAXBContext jaxbContext = JAXBContext.newInstance(Daia.class);
                                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                                Daia daia = (Daia) unmarshaller.unmarshal(httpEntity.getContent());

                                if (daia.getDocument() != null) {

                                    daiaDocuments.addAll(daia.getDocument());
                                }

                                break;
                            }
                            default: {

                                // TODO Evaluieren: Das müssten die Faälle sein, in denen E-Books in der Knowledgebase eingetragen sind
                                if (Lookup.lookupAll(LinkResolver.class).size() > 0) {

                                    Document daiaDocument = null;

                                    LinkResolver linkResolver = Lookup.lookup(LinkResolver.class);
                                    // init Linkresolver
                                    linkResolver.init(this.config);

                                    // get items
                                    ArrayList<Document> linkresolverDocument = linkResolver.items("openurl", openurl);

                                    if (linkresolverDocument != null && linkresolverDocument.size() > 0) {

                                        daiaDocument = new Document();

                                        daiaDocument.setId("urn:isbn:" + isbn);

                                        if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {

                                            daiaDocument.setItem(linkresolverDocument.get(0).getItem());
                                        } else {
                                            daiaDocument.getItem().addAll(linkresolverDocument.get(0).getItem());
                                        }

                                        if (daiaDocument != null) {

                                            daiaDocuments.add(daiaDocument);
                                        }
                                    }
                                }
                            }
                        }

                        EntityUtils.consume(httpEntity);
                    } finally {
                        httpResponse.close();
                    }
                }
            }
            else {

                // Wenn JOP registriert ist
                if (Lookup.lookupAll(JournalOnlinePrintService.class).size() > 0) {

                    // build OpenURL for JOP
                    String issn = "";
                    if (latinParameters.get("rft.issn") != null && !latinParameters.get("rft.issn").equals("")) {
                        issn = latinParameters.get("rft.issn");
                    }
                    else if (latinParameters.get("issn") != null && !latinParameters.get("issn").equals("")) {
                        issn = latinParameters.get("issn");
                    }
                    if (latinParameters.get("rft.eissn") != null && !latinParameters.get("rft.eissn").equals("")) {
                        issn = latinParameters.get("rft.eissn");
                    }
                    else if (latinParameters.get("eissn") != null && !latinParameters.get("eissn").equals("")) {
                        issn = latinParameters.get("eissn");
                    }
                    else if (latinParameters.get("id") != null && latinParameters.get("id").contains("issn:")) {
                        issn = latinParameters.get("id").split(":")[1];
                    }
                    this.logger.debug("ISSN = " + issn);

                    String jop_openurl = "";

                    for (String k : latinParameters.keySet()) {

                        // mit rft
                        if ((latinParameters.keySet().contains("rft.atitle") || latinParameters.keySet().contains("atitle")) && k.equals("rft.date") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&date=" + latinParameters.get(k);
                        }
                        if (k.equals("rft.volume") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&volume=" + latinParameters.get(k);
                        }
                        if (k.equals("rft.issue") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&issue=" + latinParameters.get(k);
                        }

                        // ohne rft
                        if ((latinParameters.keySet().contains("rft.atitle") || latinParameters.keySet().contains("atitle")) && k.equals("date") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&date=" + latinParameters.get(k);
                        }
                        if (k.equals("volume") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&volume=" + latinParameters.get(k);
                        }
                        if (k.equals("issue") && !latinParameters.get(k).equals("")) {
                            jop_openurl += "&issue=" + latinParameters.get(k);
                        }
                    }

                    if (!issn.equals("")) {

                        if (issn.contains("-")) {
                            jop_openurl += "&issn=" + issn;
                        } else {
                            jop_openurl += "&issn=" + issn.subSequence(0, 4) + "-" + issn.subSequence(4, 8);
                        }
                    }

                    if (latinParameters.keySet().contains("rft.atitle") || latinParameters.keySet().contains("atitle")) {
                        jop_openurl += "&genre=article";
                    }
                    else {
                        jop_openurl += "&genre=journal";
                    }

                    this.logger.debug("\n" + jop_openurl + "\tOpenURL-Parameter (JOP) = " + this.config.getProperty("jop.url.openurl") + jop_openurl);

                    if (!jop_openurl.equals("&genre=journal") && (jop_openurl.contains("&title=") || jop_openurl.contains("&issn=")) ) {

                        // get data
                        try {

                            JournalOnlinePrintService journalOnlinePrintService = Lookup.lookup(JournalOnlinePrintService.class);
                            // init JOP
                            journalOnlinePrintService.init(this.config);

                            // get items
                            ArrayList<de.tu_dortmund.ub.api.daia.jop.model.Document> jopDocuments = journalOnlinePrintService.items("openurl", jop_openurl);

                            if (jopDocuments != null && jopDocuments.size() > 0) {

                                Document daiaDocument = new Document();

                                this.logger.debug("JOP hits: " + jopDocuments.size());

                                if (jopDocuments.get(0).getId() != null && jopDocuments.get(0).getHref() != null) {
                                    daiaDocument.setId(jopDocuments.get(0).getId());
                                    daiaDocument.setHref(jopDocuments.get(0).getHref());
                                } else {
                                    daiaDocument.setId("urn:issn:" + issn);
                                }

                                // print
                                if (jopDocuments.get(0).getItem() != null && jopDocuments.get(0).getItem().size() > 0) {
                                    daiaDocument.setItem(jopDocuments.get(0).getItem());
                                }

                                // digital
                                if (jopDocuments.get(0).isExistsDigitalItems() && Lookup.lookupAll(LinkResolver.class).size() > 0) {

                                    // TODO define a boolean variable for executing a linkresolver request
                                    // TODO auslagern!
                                    LinkResolver linkResolver = Lookup.lookup(LinkResolver.class);
                                    // init Linkresolver
                                    linkResolver.init(this.config);

                                    // get items
                                    ArrayList<Document> linkresolverDocument = linkResolver.items("openurl", openurl);

                                    if (linkresolverDocument != null && linkresolverDocument.size() > 0 && linkresolverDocument.get(0).getItem().size() >= jopDocuments.get(0).getCountDigitlItems()) {

                                        if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                            daiaDocument.setItem(linkresolverDocument.get(0).getItem());
                                        } else {
                                            daiaDocument.getItem().addAll(linkresolverDocument.get(0).getItem());
                                        }
                                    } else {

                                        // TODO Ticket 11679

                                        // E-Mail an katalogplus@ub.tu-dortmund.de mit Betreff-Prefix [Content]

                                        boolean isNatLic = true;

                                        if (isNatLic) {

                                            if (!issn.equals("")) {

                                                // request JOP again with only ISSN
                                                jopDocuments = journalOnlinePrintService.eonly("issn", issn);

                                                if (jopDocuments != null && jopDocuments.size() > 0) {

                                                    this.logger.debug("JOP hits: " + jopDocuments.size());

                                                    if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                        daiaDocument.setItem(jopDocuments.get(0).getItem());
                                                    } else {
                                                        if (jopDocuments.get(0).getItem() != null) {
                                                            daiaDocument.getItem().addAll(jopDocuments.get(0).getItem());
                                                        } else {

                                                            // Error-E-Mail "JOP<>LinkResolver: Not an NatLic"
                                                            Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

                                                            try {

                                                                int cnt = 0;
                                                                if (linkresolverDocument != null) {
                                                                    cnt = linkresolverDocument.get(0).getItem().size();
                                                                }

                                                                mailer.postMail("[DAIAopenurl] JOP-Document ohne Items ", "JOP-Link " + jopDocuments.get(0).getCountDigitlItems() + ": " +
                                                                        this.config.getProperty("jop.url.openurl") + jop_openurl + ".\n");

                                                            } catch (MessagingException e) {

                                                                this.logger.error(e.getMessage(), e.getCause());
                                                                this.logger.debug("[DAIAopenurl] CONTENT-ERROR");
                                                                this.logger.debug("OpenUrl: " + openurl + "\n\n\tJOP-URL: " + jop_openurl);
                                                            }

                                                            if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                daiaDocument = null;
                                                            }
                                                        }
                                                    }

                                                }
                                            } else {

                                                // Error-E-Mail "JOP<>LinkResolver: Not an NatLic"
                                                Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

                                                try {

                                                    int cnt = 0;
                                                    if (linkresolverDocument != null) {
                                                        cnt = linkresolverDocument.get(0).getItem().size();
                                                    }

                                                    mailer.postMail("[DAIAopenurl] JOP<>LinkResolver: NatLic without ISSN ", "Laut ZDB/EZB gibt es " + jopDocuments.get(0).getCountDigitlItems() + "-mal elektronischen Bestand (vgl. " +
                                                            this.config.getProperty("jop.url.openurl") + jop_openurl + ").\n" +
                                                            "Laut 360 Link aber " + cnt + "-mal (vgl. " +
                                                            this.config.getProperty("linkresolver.baseurl") + this.config.getProperty("linkresolver.parameters") + openurl + ").");

                                                } catch (MessagingException e) {

                                                    this.logger.error(e.getMessage(), e.getCause());
                                                    this.logger.debug("[DAIAopenurl] CONTENT-ERROR");
                                                    this.logger.debug("OpenUrl: " + openurl + "\n\n\tJOP-URL: " + jop_openurl);
                                                }

                                                if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                    daiaDocument = null;
                                                }
                                            }
                                        } else {
                                            // Hier kann man nix machen!
                                        }
                                    }
                                }

                                if (daiaDocument != null) {

                                    daiaDocuments.add(daiaDocument);
                                }
                            }

                        } catch (LinkResolverException e) {

                            // daiaDocuments bleibt leer
                            this.logger.error("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable (LinkResolver).");
                            this.logger.error(e.getMessage(), e.getCause());
                            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                                this.logger.error("\t" + stackTraceElement.toString());
                            }

                            Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

                            try {
                                mailer.postMail("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable (LinkResolver).", e.getMessage() + "\n" + this.config.getProperty("linkresolver.baseurl") + this.config.getProperty("linkresolver.parameters") + openurl);

                            } catch (MessagingException e1) {

                                this.logger.error(e1.getMessage(), e1.getCause());
                            }

                        } catch (JOPException e) {

                            // daiaDocuments bleibt leer
                            this.logger.error("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable (Journals Online & Print).");
                            this.logger.error(e.getMessage(), e.getCause());
                            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                                this.logger.error("\t" + stackTraceElement.toString());
                            }

                            Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

                            try {
                                mailer.postMail("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable (Journals Online & Print).", e.getMessage() + "\n" + this.config.getProperty("jop.url.openurl") + jop_openurl);

                            } catch (MessagingException e1) {

                                this.logger.error(e1.getMessage(), e1.getCause());
                            }
                        }
                    }
                    else {

                        // tue nix: daiaDocuments bleibt leer
                    }
                }
            }

            // Abschlusskorrektur für ACM, IEEE und LNCS vor dem response
            // LNCS
            if (Lookup.lookupAll(LncsResolver.class).size() > 0 && Boolean.parseBoolean(this.config.getProperty("licenced.lncs"))) {

                if (daiaDocuments.size() > 0) {

                    for (Document daiaDocument : daiaDocuments) {

                        if (daiaDocument != null && daiaDocument.getId() != null && daiaDocument.getId().contains("urn:issn:03029743") &&
                        ((latinParameters.keySet().contains("id") && latinParameters.get("id").startsWith("doi:10.1007")) ||
                                (latinParameters.keySet().contains("rft.id") && latinParameters.get("rft.id").startsWith("doi:10.1145"))) ) {

                            daiaDocument.setItem(null);

                            LncsResolver lncsResolver = Lookup.lookup(LncsResolver.class);
                            lncsResolver.init(this.config);

                            Document lncsDocument = lncsResolver.items(latinParameters);

                            daiaDocument.setId(lncsDocument.getId());
                            daiaDocument.setHref(lncsDocument.getHref());
                            daiaDocument.setItem(lncsDocument.getItem());
                        }
                    }
                }
            }

            // ACM
            if (Lookup.lookupAll(AcmResolver.class).size() > 0 && Boolean.parseBoolean(this.config.getProperty("licenced.acm"))) {

                if ((latinParameters.keySet().contains("id") && latinParameters.get("id").contains("10.1145")) || (latinParameters.keySet().contains("rft.id") && latinParameters.get("rft.id").contains("10.1145"))) {

                    if (daiaDocuments.size() > 0) {

                        for (Document daiaDocument : daiaDocuments) {

                            daiaDocument.setItem(null);

                            AcmResolver acmResolver = Lookup.lookup(AcmResolver.class);
                            acmResolver.init(this.config);

                            Document acmDocument = acmResolver.items(latinParameters);

                            daiaDocument.setId(acmDocument.getId());
                            daiaDocument.setHref(acmDocument.getHref());

                            if (daiaDocument.getItem() == null) {
                                daiaDocument.setItem(acmDocument.getItem());
                            }
                            else {
                                daiaDocument.getItem().addAll(acmDocument.getItem());
                            }
                        }
                    }
                    else {

                        Document daiaDocument = new Document();

                        AcmResolver acmResolver = Lookup.lookup(AcmResolver.class);
                        acmResolver.init(this.config);

                        Document acmDocument = acmResolver.items(latinParameters);

                        daiaDocument.setId(acmDocument.getId());
                        daiaDocument.setHref(acmDocument.getHref());

                        if (daiaDocument.getItem() == null) {
                            daiaDocument.setItem(acmDocument.getItem());
                        }
                        else {
                            daiaDocument.getItem().addAll(acmDocument.getItem());
                        }

                        daiaDocuments.add(daiaDocument);
                    }
                }
            }

            // IEEE
            if (Lookup.lookupAll(IeeeResolver.class).size() > 0 && Boolean.parseBoolean(this.config.getProperty("licenced.ieee"))) {

                if ((latinParameters.keySet().contains("id") && latinParameters.get("id").contains("10.1109")) || (latinParameters.keySet().contains("rft.id") && latinParameters.get("rft.id").contains("10.1109"))) {

                    if (daiaDocuments.size() > 0) {

                        for (Document daiaDocument : daiaDocuments) {

                            daiaDocument.setItem(null);

                            IeeeResolver ieeeResolver = Lookup.lookup(IeeeResolver.class);
                            ieeeResolver.init(this.config);

                            Document ieeeDocument = ieeeResolver.items(latinParameters);

                            daiaDocument.setId(ieeeDocument.getId());
                            daiaDocument.setHref(ieeeDocument.getHref());

                            if (daiaDocument.getItem() == null) {
                                daiaDocument.setItem(ieeeDocument.getItem());
                            }
                            else {
                                daiaDocument.getItem().addAll(ieeeDocument.getItem());
                            }
                        }
                    }
                    else {

                        Document daiaDocument = new Document();

                        IeeeResolver ieeeResolver = Lookup.lookup(IeeeResolver.class);
                        ieeeResolver.init(this.config);

                        Document ieeeDocument = ieeeResolver.items(latinParameters);

                        daiaDocument.setId(ieeeDocument.getId());
                        daiaDocument.setHref(ieeeDocument.getHref());

                        if (daiaDocument.getItem() == null) {
                            daiaDocument.setItem(ieeeDocument.getItem());
                        }
                        else {
                            daiaDocument.getItem().addAll(ieeeDocument.getItem());
                        }

                        daiaDocuments.add(daiaDocument);
                    }
                }
            }

            if (daiaDocuments.size() > 0) {

                this.logger.debug("200 Document Found");

                // TODO query footnotes from ils if configured

                // Ausgabe
                Daia daia = new Daia();
                daia.setVersion(this.config.getProperty("daia.version"));
                daia.setSchema(this.config.getProperty("daia.schema"));

                GregorianCalendar gc = new GregorianCalendar();
                gc.setTimeInMillis(new Date().getTime());
                try {
                    DatatypeFactory df = DatatypeFactory.newInstance();

                    daia.setTimestamp(df.newXMLGregorianCalendar(gc).toString());

                } catch (DatatypeConfigurationException dce) {
                    this.logger.error("ERROR: Service unavailable.", dce.getCause());
                }

                Institution institution = new Institution();
                institution.setId(this.config.getProperty("daia.institution.id"));
                institution.setHref(this.config.getProperty("daia.institution.href"));
                institution.setContent(this.config.getProperty("daia.institution.content"));
                daia.setInstitution(institution);

                daia.setDocument(daiaDocuments);

                // Ausgabe
                if (daia.getDocument() == null || daia.getDocument().size() == 0) {

                    // HTML-Ausgabe via XSLT
                    if (format.equals("html")) {

                        try {

                            JAXBContext context = JAXBContext.newInstance(Daia.class);
                            Marshaller m = context.createMarshaller();
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                            // Write to HttpResponse
                            StringWriter stringWriter = new StringWriter();
                            m.marshal(daia, stringWriter);

                            org.jdom2.Document doc = new org.jdom2.Document();
                            doc.setRootElement(new Element("daia"));

                            HashMap<String,String> parameters = new HashMap<String,String>();
                            parameters.put("lang", "de");
                            parameters.put("isTUintern", Boolean.toString(isTUintern));
                            parameters.put("isUBintern", Boolean.toString(isUBintern));

                            parameters.put("id", "openurl:" + URLDecoder.decode(openurl,"UTF-8"));

                            ObjectMapper mapper = new ObjectMapper();
                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, daia);
                            parameters.put("json", json.toString());

                            String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                            response.setContentType("text/html;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().println(html);
                        }
                        catch (PropertyException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering a HTML message. Message is 'Document not found'.");
                        } catch (JAXBException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering a HTML message. Message is 'Document not found'.");
                        }
                    }
                    else {

                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found.");
                    }
                }
                else {

                    this.logger.debug("format = " + format);

                    // HTML-Ausgabe via XSLT
                    if (format.equals("html")) {

                        try {

                            JAXBContext context = JAXBContext.newInstance(Daia.class);
                            Marshaller m = context.createMarshaller();
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                            // Write to HttpResponse
                            StringWriter stringWriter = new StringWriter();
                            m.marshal(daia, stringWriter);

                            org.jdom2.Document doc = new SAXBuilder().build(new StringReader(this.cleanup(stringWriter.toString())));

                            HashMap<String,String> parameters = new HashMap<String,String>();
                            parameters.put("lang", "de");
                            parameters.put("isTUintern", Boolean.toString(isTUintern));
                            parameters.put("isUBintern", Boolean.toString(isUBintern));

                            parameters.put("id", "openurl:" + this.cleanup(URLDecoder.decode(openurl, "UTF-8")));

                            ObjectMapper mapper = new ObjectMapper();
                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, daia);
                            parameters.put("json", json.toString());

                            String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                            response.setContentType("text/html;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().println(html);
                        } catch (PropertyException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (JAXBException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (JDOMException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (IOException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        }
                    }

                    // XML-Ausgabe mit JAXB
                    if (format.equals("xml")) {

                        try {

                            JAXBContext context = JAXBContext.newInstance(Daia.class);
                            Marshaller m = context.createMarshaller();
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                            // Write to HttpResponse
                            response.setContentType("application/xml;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_OK);
                            m.marshal(daia, response.getWriter());
                        }
                        catch (PropertyException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (JAXBException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        }
                    }

                    // JSON-Ausgabe mit Jackson
                    if (format.equals("json")) {

                        ObjectMapper mapper = new ObjectMapper();

                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_OK);

                        mapper.writeValue(response.getWriter(), daia);
                    }

                    // RDF-Ausgabe mit XSLT auf XML-Ausgabe
                    if (format.equals("rdf")) {

                        try {
                            JAXBContext context = JAXBContext.newInstance(Daia.class);
                            Marshaller m = context.createMarshaller();
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                            // Write to HttpResponse
                            response.setContentType("application/xml;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_OK);

                            StringWriter xml = new StringWriter();
                            m.marshal(daia, xml);

                            XMLOutputter out = new XMLOutputter();
                            out.output(new SAXBuilder().build(new StringReader(xmlOutputter(new SAXBuilder().build(new StringReader(xml.toString())), config.getProperty("xslt_xml2rdf"), null))), response.getWriter());

                        } catch (JDOMException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (PropertyException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        } catch (JAXBException e) {
                            this.logger.error(e.getMessage(), e.getCause());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                        }
                    }
                }
            }
            else {

                this.logger.debug("404 Document Not Found + (format=" + format + ")");

                if (format.equals("html")) {

                    try {

                        JAXBContext context = JAXBContext.newInstance(Daia.class);
                        Marshaller m = context.createMarshaller();
                        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                        // Write to HttpResponse
                        org.jdom2.Document doc = new org.jdom2.Document();
                        doc.setRootElement(new Element("daia"));

                        HashMap<String,String> parameters = new HashMap<String,String>();
                        parameters.put("lang", "de");
                        parameters.put("isTUintern", Boolean.toString(isTUintern));
                        parameters.put("isUBintern", Boolean.toString(isUBintern));

                        if (openurl.contains("__char_set=latin1")) {
                            parameters.put("id", "openurl:" + request.getQueryString()); // TODO TESTEN für andere Fälle!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            this.logger.debug("OpenUrl für 404-HTML: " + openurl);
                        }
                        else {
                            parameters.put("id", "openurl:" + URLDecoder.decode(openurl,"UTF-8"));
                        }

                        String html = htmlOutputter(doc, this.config.getProperty("linkresolver.html.xslt"), parameters);

                        response.setContentType("text/html;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().println(html);
                    } catch (PropertyException e) {
                        this.logger.error(e.getMessage(), e.getCause());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering a HTML message. Message is 'Document not found'.");
                    } catch (JAXBException e) {
                        this.logger.error(e.getMessage(), e.getCause());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering a HTML message. Message is 'Document not found'.");
                    }
                }
                else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found.");
                }
            }
        }
        catch (Exception e) {

            this.logger.error("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable.");
            this.logger.error(e.getMessage(), e.getCause());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                this.logger.error("\t" + stackTraceElement.toString());
            }

            Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

            try {
                mailer.postMail("[DaiaService OpenUrl Endpoint] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable.", e.getMessage() + "\n" + request.getRequestURL());

            } catch (MessagingException e1) {

                this.logger.error(e1.getMessage(), e1.getCause());
            }

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable.");
        }
	}

    private String htmlOutputter(org.jdom2.Document doc, String xslt, HashMap<String,String> params) throws IOException {

        String result = null;

        try {

            // Init XSLT-Transformer
            Processor processor = new Processor(false);
            XsltCompiler xsltCompiler = processor.newXsltCompiler();
            XsltExecutable xsltExecutable = xsltCompiler.compile(new StreamSource(xslt));


            XdmNode source = processor.newDocumentBuilder().build(new JDOMSource( doc ));
            Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");

            StringWriter buffer = new StringWriter();
            out.setOutputWriter(new PrintWriter( buffer ));

            XsltTransformer trans = xsltExecutable.load();
            trans.setInitialContextNode(source);
            trans.setDestination(out);

            if (params != null) {
                for (String p : params.keySet()) {
                    trans.setParameter(new QName(p), new XdmAtomicValue(params.get(p).toString()));
                }
            }

            trans.transform();

            result = buffer.toString();

        } catch (SaxonApiException e) {

            e.printStackTrace();
            this.logger.error("SaxonApiException: " + e.getMessage());
        }

        return result;
    }

    private String xmlOutputter(org.jdom2.Document doc, String xslt, HashMap<String,String> params) throws IOException {

        String result = null;

        try {

            // Init XSLT-Transformer
            Processor processor = new Processor(false);
            XsltCompiler xsltCompiler = processor.newXsltCompiler();
            XsltExecutable xsltExecutable = xsltCompiler.compile(new StreamSource(xslt));


            XdmNode source = processor.newDocumentBuilder().build(new JDOMSource( doc ));
            Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.METHOD, "xml");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");

            StringWriter buffer = new StringWriter();
            out.setOutputWriter(new PrintWriter( buffer ));

            XsltTransformer trans = xsltExecutable.load();
            trans.setInitialContextNode(source);
            trans.setDestination(out);

            if (params != null) {
                for (String p : params.keySet()) {
                    trans.setParameter(new QName(p), new XdmAtomicValue(params.get(p).toString()));
                }
            }

            trans.transform();

            result = buffer.toString();

        } catch (SaxonApiException e) {

            e.printStackTrace();
            this.logger.error("SaxonApiException: " + e.getMessage());
        }

        return result;
    }

    private String cleanup(String input) {

        String output = "";

        try {
            // Quelle: http://la.remifa.so/unicode/latin1.html oder http://www.starkeffects.com/html-symbol-codes.shtml
            output = input;
            output = output.replaceAll("&#x79;", "");
            output = output.replaceAll("&#127;", "'");

            output = output.replaceAll("&#x80;", " EUR");
            output = output.replaceAll("&#128;", " EUR");

            output = output.replaceAll("&#x81;", "");
            output = output.replaceAll("&#129;", "");

            output = output.replaceAll("&#x82;", ",");
            output = output.replaceAll("&#130;", ",");

            output = output.replaceAll("&#x83;", "");
            output = output.replaceAll("&#131;", "");

            output = output.replaceAll("&#x84;", "'");
            output = output.replaceAll("&#132;", "'");

            output = output.replaceAll("&#x85;", "...");
            output = output.replaceAll("&#133;", "...");

            output = output.replaceAll("&#x86;", "");
            output = output.replaceAll("&#134;", "");

            output = output.replaceAll("&#x87;", "");
            output = output.replaceAll("&#135;", "");

            output = output.replaceAll("&#x88;", "");
            output = output.replaceAll("&#136;", "");

            output = output.replaceAll("&#x89;", "");
            output = output.replaceAll("&#137;", "");

            output = output.replaceAll("&#x90;", "");
            output = output.replaceAll("&#144;", "");

            output = output.replaceAll("&#x91;", "'");
            output = output.replaceAll("&#145;", "'");

            output = output.replaceAll("&#x92;", "'");
            output = output.replaceAll("&#146;", "'");

            output = output.replaceAll("&#x93;", "'");
            output = output.replaceAll("&#147;", "'");

            output = output.replaceAll("&#x94;", "'");
            output = output.replaceAll("&#148;", "'");

            output = output.replaceAll("&#x95;", "*");
            output = output.replaceAll("&#149;", "*");
            output = output.replaceAll("\u0095","*");

            output = output.replaceAll("&#x96;", "-");
            output = output.replaceAll("&#150;", "-");
            output = output.replaceAll("\u0096","-");

            output = output.replaceAll("&#x97;", "--");
            output = output.replaceAll("&#151;", "--");
            output = output.replaceAll("\u0097","--");

            output = output.replaceAll("&#x98;", "~");
            output = output.replaceAll("&#152;", "~");
            output = output.replaceAll("\u0098","~");

            output = output.replaceAll("&#x99;", "&#x2122;");
            output = output.replaceAll("&#153;", "&#x2122;");
            output = output.replaceAll("\u0099","&#x2122;");

            output = output.replaceAll("&#x9A;", "S");
            output = output.replaceAll("&#154;", "S");
            output = output.replaceAll("\u009A","S");

            output = output.replaceAll("&#x9B;", ">");
            output = output.replaceAll("&#155;", ">");
            output = output.replaceAll("\u009B",">");

            output = output.replaceAll("&#x9C;", "");
            output = output.replaceAll("&#156;", "");
            output = output.replaceAll("\u009C","");

            output = output.replaceAll("&#x9D;", " ");
            output = output.replaceAll("&#156;", " ");
            output = output.replaceAll("\u009D"," ");

            output = output.replaceAll("&#x9E;", "Z");
            output = output.replaceAll("&#157;", "Z");
            output = output.replaceAll("\u009E","Z");

            output = output.replaceAll("&#x9F;", "Y");
            output = output.replaceAll("&#158;", "Y");
            output = output.replaceAll("\u009F","Y");
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return output;
    }
}
