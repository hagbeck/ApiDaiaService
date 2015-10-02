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

/**
 * Hello world!
 *
 */
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

        logger.info("[" + config.getProperty("service.name") + "] " + "Starting 'DaiaService' ...");
        logger.info("[" + config.getProperty("service.name") + "] " + "conf-file = " + conffile);
        logger.info("[" + config.getProperty("service.name") + "] " + "log4j-conf-file = " + config.getProperty("service.log4j-conf"));

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
