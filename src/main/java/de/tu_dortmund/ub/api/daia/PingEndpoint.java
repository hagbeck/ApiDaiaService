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

import de.tu_dortmund.ub.api.daia.ils.IntegratedLibrarySystem;
import de.tu_dortmund.ub.api.daia.jop.JournalOnlinePrintService;
import de.tu_dortmund.ub.api.daia.linkresolver.LinkResolver;
import de.tu_dortmund.ub.util.impl.Lookup;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class PingEndpoint extends HttpServlet {

    private Properties config = null;
    private Logger logger = null;

    public PingEndpoint() throws IOException {

        this("conf/api-test.properties");
    }

    public PingEndpoint(String conffile) throws IOException {

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
        this.logger = Logger.getLogger(PingEndpoint.class.getName());

        this.logger.info("Starting 'PingEndpoint' ...");
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

        response.setHeader("Access-Control-Allow-Origin", "*");

        try {

            HashMap<String,String> health = null;

            // ILS
            if (Lookup.lookupAll(IntegratedLibrarySystem.class).size() > 0) {

                IntegratedLibrarySystem integratedLibrarySystem = Lookup.lookup(IntegratedLibrarySystem.class);
                // init ILS
                integratedLibrarySystem.init(this.config);

                health = integratedLibrarySystem.health(this.config);
            }
            // JOP
            if (Lookup.lookupAll(JournalOnlinePrintService.class).size() > 0) {

                JournalOnlinePrintService journalOnlinePrintService = Lookup.lookup(JournalOnlinePrintService.class);
                // init JOP
                journalOnlinePrintService.init(this.config);

                if (health == null) {

                    health = journalOnlinePrintService.health(this.config);
                }
                else {

                    health.putAll(journalOnlinePrintService.health(this.config));
                }
            }
            // Linkresolver
            if (Lookup.lookupAll(LinkResolver.class).size() > 0) {

                LinkResolver linkResolver = Lookup.lookup(LinkResolver.class);
                // init Linkresolver
                linkResolver.init(this.config);

                if (health == null) {

                    health = linkResolver.health(this.config);
                }
                else {

                    health.putAll(linkResolver.health(this.config));
                }
            }

            if (health.containsValue("failed")) {

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println("One or more dependencies unavailable!");
            }
            else {

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println("pong");
            }
        }
        catch (Exception e) {

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("Could not check system health!");
        }
    }

}
