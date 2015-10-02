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
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Hans-Georg on 24.07.2015.
 */
public class HealthEndpoint extends HttpServlet {

    private String conffile  = "";
    private Properties config = new Properties();
    private Logger logger = Logger.getLogger(HealthEndpoint.class.getName());

    public HealthEndpoint() throws IOException {

        this("conf/api-test.properties");
    }

    public HealthEndpoint(String conffile) throws IOException {

        this.conffile = conffile;

        // Init properties
        try {
            InputStream inputStream = new FileInputStream(this.conffile);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                try {
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
            System.out.println("FATAL ERROR: Die Datei '" + this.conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(this.config.getProperty("service.log4j-conf"));

        this.logger.info("Starting 'HealthEndpoint' ...");
        this.logger.info("conf-file = " + this.conffile);
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

            String json = "{ ";

            json += "\"name\" : \"" + config.getProperty("service.name") + "\",";
            json += "\"timestamp\" : \"" + LocalDateTime.now() + "\"";

            if (health != null && health.size() > 0) {

                json += ", \"dependencies\" : { ";

                for (String dependency : health.keySet()) {

                    json += "\"" + dependency + "\" : \"" + health.get(dependency) + "\", ";
                }

                json = json.substring(0, json.length() - 2);

                json += "}";
            }

            json += " }";

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(json);
        }
        catch (Exception e) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void writeToSocket(Socket socket, String message) throws IOException {

        OutputStream outStream = socket.getOutputStream();
        outStream.write(message.getBytes("UTF-8"));
        outStream.flush();
    }

    private String readFromSocket(Socket socket) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        char[] buffer = new char[200];
        int length = bufferedReader.read(buffer, 0, 200); // blockiert bis Nachricht empfangen
        String nachricht = new String(buffer, 0, length);
        return nachricht;
    }

}
