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
import de.tu_dortmund.ub.api.daia.model.*;
import de.tu_dortmund.ub.api.daia.ils.ILSException;
import de.tu_dortmund.ub.api.daia.ils.IntegratedLibrarySystem;
import de.tu_dortmund.ub.api.daia.jop.JOPException;
import de.tu_dortmund.ub.api.daia.jop.JournalOnlinePrintService;
import de.tu_dortmund.ub.api.daia.linkresolver.LinkResolver;
import de.tu_dortmund.ub.api.daia.linkresolver.LinkResolverException;
import de.tu_dortmund.ub.util.impl.Lookup;
import de.tu_dortmund.ub.util.impl.Mailer;
import net.sf.saxon.s9api.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom2.*;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class DaiaEndpoint extends HttpServlet {

    private Properties config = null;
    private Logger logger = null;

	public DaiaEndpoint() throws IOException {

		this("conf/api-test.properties");
	}

	public DaiaEndpoint(String conffile) throws IOException {
        
        // Init properties
        try {
            InputStream inputStream = new FileInputStream(conffile);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                try {

                    this.config = new Properties();
                    this.config.load(reader);

                } finally {
                    reader.close();
                }
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException e) {
            System.out.println("FATAL ERROR: Die Datei '" + conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(this.config.getProperty("service.log4j-conf"));
        this.logger = Logger.getLogger(DaiaEndpoint.class.getName());

        this.logger.info("Starting 'DaiaEndpoint' ...");
        this.logger.info("conf-file = " + conffile);
        this.logger.info("log4j-conf-file = " + this.config.getProperty("service.log4j-conf"));
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

        boolean isTUintern = false;
        boolean isUBintern = false;

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

        response.setHeader("Access-Control-Allow-Origin", config.getProperty("Access-Control-Allow-Origin"));

        try {
            String format = "html";

            if (request.getParameter("format") != null && !request.getParameter("format").equals("")) {

                format = request.getParameter("format");
            }
            else {

                Enumeration<String> headerNames = request.getHeaderNames();
                while ( headerNames.hasMoreElements() ) {
                    String headerNameKey = headerNames.nextElement();

                    if (headerNameKey.equals("Accept")) {

                        this.logger.debug("headerNameKey = " + request.getHeader( headerNameKey ));

                        if (request.getHeader( headerNameKey ).contains("text/html")) {
                            format = "html";
                        }
                        else if (request.getHeader( headerNameKey ).contains("application/xml")) {
                            format = "xml";
                        }
                        else if (request.getHeader( headerNameKey ).contains("application/json")) {
                            format = "json";
                        }
                    }
                }
            }

            if (format.equals("")) {

                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid {FORMAT} requested.");
            }
		    else if (request.getParameter("id") == null || request.getParameter("id").equals("")) {

                this.logger.debug("Query: " + request.getQueryString());

                if (format.equals("html")) {

                    response.sendRedirect(this.config.getProperty("service.doc"));
                }
                else {

                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Usage: http://api.ub-tu-dortmund.de.de/daia/?id={ID}&format={FORMAT}.");
                }
            }
            else {

                if (request.getParameter("id").contains(":")) {

                    String[] tmp = request.getParameter("id").split(":");
                    this.logger.debug("id = " + request.getParameter("id") + "' - " + new Date());
                    String idtype = tmp[0];
                    String localpart = "";
                    if (tmp.length > 1) {
                        for (int i = 1; i < tmp.length; i++) {
                            localpart += tmp[i];

                            if (i < tmp.length-1) {
                                localpart += ":";
                            }
                        }
                    }

                    if (!(idtype.equals("ilsid") || idtype.equals("isbn") || idtype.equals("issn") || idtype.equals("verbundid") || idtype.equals("eki") || idtype.equals("zdbid"))) {

                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid {IDTYPE} requested.");
                    }
                    else {

                        try {

                            // Wenn via META-INF/services eine Implementierung zum interface "IntegratedLibrarySystem" erfolgt ist, dann frage das System ab.
                            if (Lookup.lookupAll(IntegratedLibrarySystem.class).size() > 0) {

                                IntegratedLibrarySystem integratedLibrarySystem = Lookup.lookup(IntegratedLibrarySystem.class);
                                // init ILS
                                integratedLibrarySystem.init(this.config);

                                ArrayList<de.tu_dortmund.ub.api.daia.ils.model.Document> documents = null;
                                documents = integratedLibrarySystem.items(idtype, localpart);

                                if (documents != null && documents.size() > 0) {

                                    ArrayList<de.tu_dortmund.ub.api.daia.model.Document> daiaDocuments = new ArrayList<de.tu_dortmund.ub.api.daia.model.Document>();

                                    ArrayList<Integer> indexToRemove = new ArrayList<Integer>();

                                    for (de.tu_dortmund.ub.api.daia.ils.model.Document document : documents) {

                                        if (document == null) {

                                            // Wie kann das sein?
                                            this.logger.error("document = null for '" + idtype + ":" + localpart + "'");
                                        }
                                        else {

                                            if ( document.getItem() == null ||
                                                    (document.getItem() != null && document.getItem().size() == 0) ||
                                                    (document.getErschform() != null && document.getErschform().equals("p") && document.getVeroefart() != null && !document.getVeroefart().equals("da")) ||
                                                    (document.getErschform() != null && document.getErschform().equals("z"))) {

                                                // TODO 2015-03-06: HttpClient-Aufruf an den OpenURL-DaiaService
                                                if (Lookup.lookupAll(JournalOnlinePrintService.class).size() > 0) {

                                                    String issn = null;
                                                    if (document.getIssn() != null) {
                                                        issn = document.getIssn();
                                                    }
                                                    else if (document.getIssnprint() != null) {
                                                        issn = document.getIssnprint().replaceAll("ISSN ","");
                                                    }
                                                    else if (document.getIssnwww() != null) {
                                                        issn = document.getIssnwww().replaceAll("ISSN ","");
                                                    }

                                                    this.logger.debug("JOP mit ISSN = " + issn);
                                                    this.logger.debug("JOP mit ZDB-ID = " + document.getZdbid());

                                                    if (issn != null) {
                                                        JournalOnlinePrintService journalOnlinePrintService = Lookup.lookup(JournalOnlinePrintService.class);
                                                        // init JOP
                                                        journalOnlinePrintService.init(this.config);

                                                        // get items
                                                        ArrayList<de.tu_dortmund.ub.api.daia.jop.model.Document> jopDocuments = journalOnlinePrintService.items("issn", issn);

                                                        if (jopDocuments != null && jopDocuments.size() > 0) {

                                                            de.tu_dortmund.ub.api.daia.model.Document daiaDocument = new de.tu_dortmund.ub.api.daia.model.Document();

                                                            if (idtype.equals("verbundid")) {

                                                                daiaDocument.setId(this.config.getProperty("daia.document.baseurl") + localpart);
                                                                daiaDocument.setHref(this.config.getProperty("daia.document.baseurl") + localpart);
                                                            }
                                                            else if (idtype.equals("issn")) {

                                                                if (jopDocuments.get(0).getId() != null) {
                                                                    daiaDocument.setId(jopDocuments.get(0).getId());
                                                                }
                                                                else {
                                                                    daiaDocument.setId("urn:issn:" + localpart);
                                                                }

                                                                if (jopDocuments.get(0).getHref() != null) {
                                                                    daiaDocument.setId(jopDocuments.get(0).getHref());
                                                                }
                                                            }
                                                            else {

                                                                if (jopDocuments.get(0).getId() != null) {
                                                                    daiaDocument.setId(jopDocuments.get(0).getId());
                                                                }
                                                                else {
                                                                    daiaDocument.setId(this.config.getProperty("daia.document.baseurl") + localpart);
                                                                }

                                                                if (jopDocuments.get(0).getHref() != null) {
                                                                    daiaDocument.setId(jopDocuments.get(0).getHref());
                                                                }
                                                                else {
                                                                    daiaDocument.setHref(this.config.getProperty("daia.document.baseurl") + localpart);
                                                                }
                                                            }

                                                            // print
                                                            if (jopDocuments.get(0).getItem() != null && jopDocuments.get(0).getItem().size() > 0) {
                                                                daiaDocument.setItem(jopDocuments.get(0).getItem());
                                                            }

                                                            // digital
                                                            if (jopDocuments.get(0).isExistsDigitalItems() && Lookup.lookupAll(LinkResolver.class).size() > 0) {

                                                                String openurl = "";

                                                                if (document.getMediatype() != null && document.getMediatype().equals("g")) {

                                                                    this.logger.debug("document.getMediatype().equals(\"g\")");

                                                                    openurl += "&rft.genre=journal&rft.eissn=" + issn;

                                                                    if (document.getIssnprint() != null) {
                                                                        if (document.getIssnprint().startsWith("ISSN ")) {
                                                                            openurl += "&rft.issn=" + document.getIssnprint().replaceAll("ISSN ","");
                                                                        }
                                                                        else {

                                                                            openurl += "&rft.issn=" + document.getIssnprint();
                                                                        }
                                                                    }
                                                                }
                                                                else {

                                                                    if (document.getIssnprint() != null) {
                                                                        if (document.getIssnprint().startsWith("ISSN ")) {
                                                                            openurl += "&rft.genre=journal&rft.issn=" + document.getIssnprint().replaceAll("ISSN ","");
                                                                        }
                                                                        else {

                                                                            openurl += "&rft.genre=journal&rft.issn=" + document.getIssnprint();
                                                                        }

                                                                        openurl += "&rft.eissn=" + document.getIssn();
                                                                    }
                                                                    else if (document.getIssnwww() != null) {
                                                                        if (document.getIssnwww().startsWith("ISSN ")) {
                                                                            openurl += "&rft.genre=journal&rft.eissn=" + document.getIssnwww().replaceAll("ISSN ","");
                                                                        }
                                                                        else {
                                                                            openurl += "&rft.genre=journal&rft.eissn=" + document.getIssnwww();
                                                                        }
                                                                        openurl += "&rft.issn=" + document.getIssn();
                                                                    }
                                                                    else {
                                                                        openurl += "&rft.genre=journal&&rft.issn=" + document.getIssn();
                                                                    }

                                                                    // TODO reicht das aus?
                                                                    //openurl += "&rft.genre=journal&&rft.issn=" + issn;
                                                                }

                                                                LinkResolver linkResolver = Lookup.lookup(LinkResolver.class);
                                                                // init Linkresolver
                                                                linkResolver.init(this.config);

                                                                // get items
                                                                ArrayList<de.tu_dortmund.ub.api.daia.model.Document> linkresolverDocument = linkResolver.items("openurl", openurl);

                                                                if (linkresolverDocument != null && linkresolverDocument.size() > 0) {
                                                                    this.logger.debug(linkresolverDocument.get(0).getItem().size() + ", " + jopDocuments.get(0).getCountDigitlItems());
                                                                }

                                                                if (linkresolverDocument != null && linkresolverDocument.size() > 0 && linkresolverDocument.get(0).getItem().size() >= jopDocuments.get(0).getCountDigitlItems()) {

                                                                    if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                        daiaDocument.setItem(linkresolverDocument.get(0).getItem());
                                                                    }
                                                                    else {
                                                                        daiaDocument.getItem().addAll(linkresolverDocument.get(0).getItem());
                                                                    }
                                                                }
                                                                else {

                                                                    this.logger.debug("Ja, der Fall ist das!");
                                                                    openurl = "";

                                                                    if (document.getIssnprint() != null) {
                                                                        if (document.getIssnprint().startsWith("ISSN ")) {
                                                                            openurl += "&rft.genre=journal&&rft.issn=" + document.getIssnprint().replaceAll("ISSN ","");
                                                                        }
                                                                    }
                                                                    else if (document.getIssnwww() != null) {
                                                                        if (document.getIssnwww().startsWith("ISSN ")) {
                                                                            openurl += "&rft.genre=journal&&rft.issn=" + document.getIssnwww().replaceAll("ISSN ","");
                                                                        }
                                                                    }

                                                                    if (!openurl.equals("")) {
                                                                        // get items
                                                                        linkresolverDocument = linkResolver.items("openurl", openurl);

                                                                        if (linkresolverDocument != null && linkresolverDocument.size() > 0) {
                                                                            this.logger.debug(linkresolverDocument.get(0).getItem().size() + ", " + jopDocuments.get(0).getCountDigitlItems());
                                                                        }

                                                                        if (linkresolverDocument != null && linkresolverDocument.size() > 0 && linkresolverDocument.get(0).getItem().size() >= jopDocuments.get(0).getCountDigitlItems()) {

                                                                            if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                                daiaDocument.setItem(linkresolverDocument.get(0).getItem());
                                                                            }
                                                                            else {
                                                                                daiaDocument.getItem().addAll(linkresolverDocument.get(0).getItem());
                                                                            }
                                                                        }
                                                                        else {

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
                                                                                        }
                                                                                        else {
                                                                                            daiaDocument.getItem().addAll(jopDocuments.get(0).getItem());
                                                                                        }

                                                                                    }
                                                                                }
                                                                                else {

                                                                                    if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                                        daiaDocument = null;
                                                                                    }
                                                                                }
                                                                            }
                                                                            else {

                                                                                if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                                    daiaDocument = null;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    else {

                                                                        daiaDocument = null;

                                                                        /* TODO tue nix?
                                                                        if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {

                                                                            if (daiaDocument.getItem() == null) {
                                                                                daiaDocument.setItem(new ArrayList<Item>());
                                                                            }
                                                                            daiaDocument.setItem(linkresolverDocument.get(0).getItem());
                                                                        }
                                                                        else {
                                                                            daiaDocument.getItem().addAll(linkresolverDocument.get(0).getItem());
                                                                        }
                                                                        */
                                                                    }

                                                                }
                                                            }

                                                            if (daiaDocument != null) {

                                                                daiaDocuments.add(daiaDocument);
                                                            }
                                                        }
                                                    }
                                                    else if (document.getZdbid() != null) {

                                                        JournalOnlinePrintService journalOnlinePrintService = Lookup.lookup(JournalOnlinePrintService.class);
                                                        // init JOP
                                                        journalOnlinePrintService.init(this.config);

                                                        // get items
                                                        ArrayList<de.tu_dortmund.ub.api.daia.jop.model.Document> jopDocuments = journalOnlinePrintService.items("zdbid", document.getZdbid());

                                                        if (jopDocuments != null && jopDocuments.size() > 0) {

                                                            de.tu_dortmund.ub.api.daia.model.Document daiaDocument = new de.tu_dortmund.ub.api.daia.model.Document();

                                                            if (idtype.equals("verbundid")) {

                                                                daiaDocument.setId(this.config.getProperty("daia.document.baseurl") + localpart);
                                                                daiaDocument.setHref(this.config.getProperty("daia.document.baseurl") + localpart);
                                                            }
                                                            else {

                                                                if (jopDocuments.get(0).getId() != null) {
                                                                    daiaDocument.setId(jopDocuments.get(0).getId());
                                                                }
                                                                else {
                                                                    daiaDocument.setId(this.config.getProperty("daia.document.baseurl") + document.getZdbid());
                                                                }

                                                                if (jopDocuments.get(0).getHref() != null) {
                                                                    daiaDocument.setHref(jopDocuments.get(0).getHref());
                                                                }
                                                                else {
                                                                    daiaDocument.setHref(this.config.getProperty("daia.document.baseurl") + document.getZdbid());
                                                                }
                                                            }
                                                            // print
                                                            if (jopDocuments.get(0).getItem() != null && jopDocuments.get(0).getItem().size() > 0) {
                                                                daiaDocument.setItem(jopDocuments.get(0).getItem());
                                                            }

                                                            // digital - nicht möglich, da ISSN fehlt
                                                            if (jopDocuments.get(0).isExistsDigitalItems()) {

                                                                jopDocuments = journalOnlinePrintService.eonly("zdbid", document.getZdbid());

                                                                if (jopDocuments != null && jopDocuments.size() > 0) {
                                                                    if (daiaDocument.getItem() == null || daiaDocument.getItem().size() == 0) {
                                                                        daiaDocument.setItem(jopDocuments.get(0).getItem());
                                                                    }
                                                                    else {
                                                                        daiaDocument.getItem().addAll(jopDocuments.get(0).getItem());
                                                                    }
                                                                }
                                                            }

                                                            daiaDocuments.add(daiaDocument);
                                                        }
                                                    }
                                                    else {
                                                        // evtl. Serienaufnahme
                                                        indexToRemove.add(documents.indexOf(document));
                                                    }
                                                }
                                            }
                                            else {
                                                de.tu_dortmund.ub.api.daia.model.Document daiaDocument = new de.tu_dortmund.ub.api.daia.model.Document();
                                                daiaDocument.setId(document.getId());
                                                daiaDocument.setHref(document.getHref());

                                                if (document.getErschform() != null && document.getErschform().equals("p") && document.getVeroefart() != null && document.getVeroefart().equals("da")) {

                                                    // TODO inhaltlich falsch
                                                    if (document.getLokaleurl() != null && !document.getLokaleurl().equals("")) {

                                                        //this.logger.info(document.getItem().size() + " Items");
                                                        document.getItem().get(0).setHref(document.getLokaleurl().get(0));
                                                    }
                                                    else if (document.getUrl() != null && !document.getUrl().equals("")) {
                                                        document.getItem().get(0).setHref(document.getUrl().get(0));
                                                    }
                                                }

                                                daiaDocument.setItem(document.getItem());

                                                String editionStatement = "";
                                                if (document.getAusgabe() != null && !document.getAusgabe().equals("")) {
                                                    editionStatement += document.getAusgabe() + ". ";
                                                }
                                                if (document.getErschjahr() != null && !document.getErschjahr().equals("")) {
                                                    editionStatement += document.getErschjahr();
                                                }
                                                if (document.getUmfang() != null && !document.getUmfang().equals("")) {
                                                    editionStatement += ". - " + document.getUmfang();
                                                }
                                                daiaDocument.setEdition(editionStatement);
                                                daiaDocuments.add(daiaDocument);
                                            }
                                        }
                                    }

                                    if (indexToRemove.size() > 0) {

                                        for (Integer i : indexToRemove) {

                                            documents.remove(Integer.valueOf(i));
                                        }

                                    }

                                    if (daiaDocuments.size() > 0) {

                                        Daia daia = new Daia();
                                        daia.setVersion(this.config.getProperty("daia.version"));
                                        daia.setSchema(this.config.getProperty("daia.schema"));

                                        GregorianCalendar gc = new GregorianCalendar();
                                        gc.setTimeInMillis(new java.util.Date().getTime());
                                        try {
                                            DatatypeFactory df = DatatypeFactory.newInstance();

                                            daia.setTimestamp(df.newXMLGregorianCalendar(gc).toString());

                                        } catch (DatatypeConfigurationException dce) {
                                            this.logger.error("ERROR: Service unavailable.");
                                        }

                                        Institution institution = new Institution();
                                        institution.setId(this.config.getProperty("daia.institution.id"));
                                        institution.setHref(this.config.getProperty("daia.institution.href"));
                                        institution.setContent(this.config.getProperty("daia.institution.content"));
                                        daia.setInstitution(institution);

                                        daia.setDocument(daiaDocuments);

                                        // Ausgabe
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

                                                Document doc = new SAXBuilder().build(new StringReader(stringWriter.toString()));

                                                HashMap<String,String> parameters = new HashMap<String,String>();
                                                parameters.put("lang", "de");
                                                parameters.put("isTUintern", Boolean.toString(isTUintern));
                                                parameters.put("isUBintern", Boolean.toString(isUBintern));

                                                this.logger.debug("idtype = " + idtype);
                                                parameters.put("id", idtype + ":" + localpart);

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
                                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                                            } catch (JAXBException e) {
                                                this.logger.error(e.getMessage(), e.getCause());
                                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                                            } catch (JDOMException e) {
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
                                    else {

                                        // Document not found
                                        if (format.equals("html")) {

                                            try {

                                                JAXBContext context = JAXBContext.newInstance(Daia.class);
                                                Marshaller m = context.createMarshaller();
                                                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                                                // Write to HttpResponse
                                                Document doc = new Document();
                                                doc.setRootElement(new Element("daia"));

                                                HashMap<String,String> parameters = new HashMap<String,String>();
                                                parameters.put("lang", "de");
                                                parameters.put("isTUintern", Boolean.toString(isTUintern));
                                                parameters.put("isUBintern", Boolean.toString(isUBintern));

                                                parameters.put("id", idtype + ":" + localpart);

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
                                }
                                else {

                                    // Document not found
                                    if (format.equals("html")) {

                                        try {

                                            JAXBContext context = JAXBContext.newInstance(Daia.class);
                                            Marshaller m = context.createMarshaller();
                                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                                            // Write to HttpResponse
                                            Document doc = new Document();
                                            doc.setRootElement(new Element("daia"));

                                            HashMap<String,String> parameters = new HashMap<String,String>();
                                            parameters.put("lang", "de");
                                            parameters.put("isTUintern", Boolean.toString(isTUintern));
                                            parameters.put("isUBintern", Boolean.toString(isUBintern));

                                            parameters.put("id", idtype + ":" + localpart);

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
                            }
                            else {
                                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable (ILS): No ILS configured!");
                            }
                        } catch (ILSException e) {
                            e.printStackTrace();
                            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable (ILS).");
                        } catch (LinkResolverException e) {
                            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable (LinkResolver).");
                        } catch (JOPException e) {
                            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable (JOP).");
                        }
                    }
                }
            }
        }
        catch (Exception e) {

            this.logger.error(new Date() + "[DAIA] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable.");
            this.logger.error(e.getMessage(), e.getCause());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                this.logger.error("\t" + stackTraceElement.toString());
            }

            Mailer mailer = new Mailer(this.config.getProperty("service.mailer.conf"));

            try {

                String referer = "'none'";
                if (request.getHeader("referer") != null) {

                    referer = request.getHeader("referer");
                }
                mailer.postMail("[DAIA] Exception: " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + " Service unavailable.", e.getMessage() + "\n" + request.getRequestURL() + "\n" + ip + "\n" + referer + "\n" + "\n" + e.getCause().toString());

            } catch (MessagingException e1) {

                this.logger.error(e1.getMessage(), e1.getCause());
            }

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable.");
        }
	}

    private String htmlOutputter(Document doc, String xslt, HashMap<String,String> params) throws IOException {

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

            this.logger.error("SaxonApiException: " + e.getMessage());
        }

        return result;
    }

    private String xmlOutputter(Document doc, String xslt, HashMap<String,String> params) throws IOException {

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

            this.logger.error("SaxonApiException: " + e.getMessage());
        }

        return result;
    }
}
