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

import de.tu_dortmund.ub.api.daia.jop.JournalOnlinePrintService;
import de.tu_dortmund.ub.util.impl.Lookup;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.Properties;

public class DaiaService {

    private static String conffile  = "conf/daia.properties";

    public static void main( String[] args ) throws Exception {

        if (args.length == 1) {
            conffile = args[0];
        }

        // Init properties
        Properties config = new Properties();
        try {
            InputStream inputStream = new FileInputStream(conffile);

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
            System.out.println("FATAL ERROR: Die Datei '" + conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(config.getProperty("service.log4j-conf"));
        Logger logger = Logger.getLogger(DaiaService.class.getName());

        logger.info("Starting 'DaiaService' ...");
        logger.info("conf-file = " + conffile);
        logger.info("log4j-conf-file = " + config.getProperty("service.log4j-conf"));

        // Server
        Server server = new Server(new Integer(config.getProperty("service.port")));

        ServletContextHandler context = new ServletContextHandler();

        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase", config.getProperty("service.resourceBase"));
        context.addServlet(holderHome, "/*");

        context.setContextPath(config.getProperty("service.contextPath"));
        server.setHandler(context);

        context.addServlet(new ServletHolder(new PingEndpoint(conffile)), config.getProperty("service.endpoint.ping"));

        context.addServlet(new ServletHolder(new HealthEndpoint(conffile)), config.getProperty("service.endpoint.health"));

        context.addServlet(new ServletHolder(new DaiaEndpoint(conffile)), config.getProperty("service.endpoint.daia"));

        if (Lookup.lookupAll(JournalOnlinePrintService.class).size() > 0) {
            context.addServlet(new ServletHolder(new DaiaOpenUrlEndpoint(conffile)), config.getProperty("service.endpoint.openurl"));
        }
        else {
            logger.info("JournalOnlinePrintService not configured!");
        }

        server.start();
        server.join();
    }
}
