package org.geowebcache.jetty;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Jetty starter, will run GeoWebCache inside the Jetty web container.<br>
 * Useful for debugging, especially in IDE were you have direct dependencies between the sources of
 * the various modules (such as Eclipse).
 * 
 * @author wolf
 * 
 */
public class Start {
    private static final Log log = LogFactory.getLog(Start.class);

    public static void main(String[] args) {
        // Use this to set a limit on the number of threads used to respond requests
        QueuedThreadPool tp = new QueuedThreadPool();
        tp.setMinThreads(50);
        tp.setMaxThreads(50);
        
        final Server jettyServer = new Server(tp);

        try {
            ServerConnector conn = new ServerConnector(jettyServer);
//            SocketConnector conn = new SocketConnector();
            String portVariable = System.getProperty("jetty.port");
            int port = parsePort(portVariable);
            if (port <= 0)
                port = 8080;
            conn.setPort(port);
            conn.setAcceptQueueSize(100);
            jettyServer.addConnector(conn);
//            jettyServer.setConnectors(new Connector[] { conn });

            WebAppContext wah = new WebAppContext();
            wah.setContextPath("/geowebcache");
            wah.setWar("src/main/webapp");
            jettyServer.setHandler(wah);
            wah.setTempDirectory(new File("target/work"));


            jettyServer.start();

            /*
             * Reads from System.in looking for the string "stop\n" in order to gracefully terminate
             * the jetty server and shut down the JVM. This way we can invoke the shutdown hooks
             * while debugging in eclipse. Can't catch CTRL-C to emulate SIGINT as the eclipse
             * console is not propagating that event
             */
            Thread stopThread = new Thread() {
                @Override
                public void run() {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String line;
                    try {
                        while (true) {
                            line = reader.readLine();
                            if ("stop".equals(line)) {
                                jettyServer.stop();
                                System.exit(0);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            };
            stopThread.setDaemon(true);
            stopThread.run();
        } catch (Exception e) {
            log.error("Could not start the Jetty server: " + e.getMessage(), e);

            if (jettyServer != null) {
                try {
                    jettyServer.stop();
                } catch (Exception e1) {
                    log.error("Unable to stop the " + "Jetty server:" + e1.getMessage(), e1);
                }
            }
        }
    }

    private static int parsePort(String portVariable) {
        if (portVariable == null)
            return -1;
        try {
            return Integer.valueOf(portVariable).intValue();
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}