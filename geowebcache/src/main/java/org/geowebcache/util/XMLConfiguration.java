package org.geowebcache.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.cache.CacheFactory;
import org.geowebcache.cache.*;
import org.geowebcache.layer.Grid;
import org.geowebcache.mime.MimeType;

import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class XMLConfiguration implements Configuration, ApplicationContextAware {
    private static Log log = LogFactory
            .getLog(org.geowebcache.util.XMLConfiguration.class);

    private static final String CONFIGURATION_FILE_NAME = "geowebcache.xml";
    
    private WebApplicationContext context;

    private CacheFactory cacheFactory = null;

    private String absPath = null;

    private String relPath = null;

    private File configDirH = null;

    /**
     * XMLConfiguration class responsible for reading/writing layer
     * configurations to and from XML file
     * 
     * @param cacheFactory
     */
    public XMLConfiguration(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    public XMLConfiguration() {
    }
    
    private File findConfFile() throws GeoWebCacheException {
        if (configDirH == null) {
            determineConfigDirH();
        }

        File xmlFile = null;
        if (configDirH != null) {
            // Find the property file
            xmlFile = new File(configDirH.getAbsolutePath() + File.separator + CONFIGURATION_FILE_NAME);
        } else {
            throw new GeoWebCacheException("Unable to determine configuration directory.");
        }

        if (xmlFile != null) {
            log.trace("Found configuration file in "
                    + configDirH.getAbsolutePath());
        } else {
            throw new GeoWebCacheException("Found no configuration file in "+ configDirH.getAbsolutePath());
        }
        
        return xmlFile;
    }

    /**
     * Method responsible for loading XML configuration file
     * 
     */
    public Map<String, TileLayer> getTileLayers() throws GeoWebCacheException {
        File xmlFile = findConfFile();
        
        HashMap<String, TileLayer> layers = new HashMap<String, TileLayer>();

        // load configurations into Document
        Document docc = loadIntoDocument(xmlFile);
        Element root = docc.getDocumentElement();
        NodeList allLayerNodes = root.getChildNodes();

        XStream xs = getConfiguredXStream(new XStream());

        TileLayer result = null;
        for (int i = 0; i < allLayerNodes.getLength(); i++) {
            if (allLayerNodes.item(i) instanceof Element) {
                Element e = (Element) allLayerNodes.item(i);
                if (e.getTagName().equalsIgnoreCase("wmslayer"))
                    result = (WMSLayer) xs.unmarshal(new DomReader(
                            (Element) allLayerNodes.item(i)));
                result.setCacheFactory(this.cacheFactory);
                layers.put(result.getName(), result);
            }
        }

        return layers;
    }

    public XStream getConfiguredXStream(XStream xstream) {
        XStream xs = xstream;

        xs.alias("layer", TileLayer.class);
        xs.alias("wmslayer", WMSLayer.class);
        xs.aliasField("layer-name", TileLayer.class, "name");
        xs.alias("grid", Grid.class);
        xs.alias("format", String.class);

        return xs;
    }

    /**
     * Method responsible for writing out TileLayer objects
     * 
     * @param tl
     *            a new TileLayer object to be serialized to XML
     * @return true if operation succeeded, false otherwise
     */

    public boolean createLayer(TileLayer tl) throws GeoWebCacheException {
        File xmlFile = findConfFile();

        // load configurations into Document
        Document docc = loadIntoDocument(xmlFile);
        Element root = docc.getDocumentElement();

        // create the XStream for serializing tileLayers to XML
        XStream xs = getConfiguredXStream( new XStream());
        // sent to XML
        xs.marshal(tl, new DomWriter((Element) root));

        try {
            DOMSource source = new DOMSource(docc);
            StreamResult result = new StreamResult(xmlFile);

            // write the DOM to the file
            Transformer xformer = TransformerFactory.newInstance()
                    .newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
        } catch (TransformerException e) {
        }

        return true;

    }

    /**
     * Method responsible for modifying an existing layer.
     * 
     * @param currentLayer
     *            the name of the layer to be modified
     * @param tl
     *            the new layer to overwrite the existing layer
     * @return true if operation succeeded, false otherwise
     */

    public boolean modifyLayer(String currentLayer, TileLayer tl) throws GeoWebCacheException {

        return deleteLayer(currentLayer) && createLayer(tl);

    }

    /**
     * Method responsible for deleting existing layers
     * 
     * @param layerName the name of the layer to be deleted
     * @return true if operation succeeded, false otherwise
     */
    public boolean deleteLayer(String layerName) {
        if (configDirH == null) {
            determineConfigDirH();
        }

        File xmlFile = null;
        if (configDirH != null) {
            // Find the property file and process each one into a TileLayer
            xmlFile = new File(configDirH.getAbsolutePath() + File.separator + "geowebcache.xml");
        }

        if (xmlFile != null) {
            log.trace("Found configuration file in "+ configDirH.getAbsolutePath());
        } else {
            log.error("Found no configuration file in "+ configDirH.getAbsolutePath());
            return false;
        }

        // load configurations into Document
        Document docc = loadIntoDocument(xmlFile);
        Element root = docc.getDocumentElement();
        // find the layer to delete This assumes that ALL layer names are
        // distinct
        NodeList nl = docc.getElementsByTagName("layer-name");

        if (nl.getLength() == 0)
            return false;
        else {
            Element toDelete = null;
            for (int i = 0; i < nl.getLength(); i++) {
                Node tmp = (Node) nl.item(i).getFirstChild();
                if (tmp.getNodeValue().equals(layerName)) {
                    toDelete = (Element) nl.item(i);
                    break;
                } else {
                    continue;
                }
            }

            if (toDelete != null) {
                root.removeChild(toDelete.getParentNode());

                try {
                    DOMSource source = new DOMSource(docc);
                    StreamResult result = new StreamResult(xmlFile);

                    // write the DOM to the file
                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                    xformer.transform(source, result);
                } catch (TransformerConfigurationException e) {
                    log.error("XMLConfiguration encountered "+e.getMessage());
                } catch (TransformerException e) {
                    log.error("XMLConfiguration encountered "+e.getMessage());
                }
                return true;
            }
            
            return false;
        }
    }

    /**
     * Method responsible for loading xml configuration file and parsing it into
     * a W3C DOM Document
     * 
     * @param file
     *            the file contaning the layer configurations
     * @return W3C DOM Document
     */
    private org.w3c.dom.Document loadIntoDocument(File file) {
        org.w3c.dom.Document document = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(file);
        } catch (ParserConfigurationException pce) {
            log.error(pce.getMessage());
            System.err.println(pce.getMessage());
            pce.printStackTrace();
        } catch (IOException ei) {
            log.error("Exception occured while creating documet from file " + file.getAbsolutePath());
            ei.printStackTrace(System.err);
        } catch (SAXException saxe) {
            log.error(saxe.getMessage());
            saxe.printStackTrace();
        }
        return document;
    }

    /**
     * 
     * Method finds the layer configuration XML file
     * 
     * @param configDirH
     * @return
     */
    private File findPropFile(File configDirH) {
        FilenameFilter select = new ExtensionFileLister("geowebcache", "xml");
        File[] f = configDirH.listFiles(select);

        if (f == null) {
            log.error("Unable to find configuration file in "
                    + this.configDirH.getAbsolutePath() + " !! ");
            return null;
        } else {
            return f[0];
        }
    }


    public void determineConfigDirH() {
        if (absPath != null) {
            configDirH = new File(absPath);
            return;
        }

        /* Only keep going for relative directory */
        if (relPath == null) {
            log.warn("No configuration directory was specified,"
                    + " reverting to default: ");
            relPath = "";
        } else {
            if (File.separator.equals("\\")
                    && relPath.equals("/WEB-INF/classes")) {
                log.warn("You seem to be running on windows, changing search path to \\WEB-INF\\classes");
                relPath = "\\WEB-INF\\classes";
            }
        }

        String baseDir = context.getServletContext().getRealPath("");

        configDirH = new File(baseDir + relPath);

        log.info("Configuration directory set to: "
                + configDirH.getAbsolutePath());

        if (!configDirH.exists() || !configDirH.canRead()) {
            log.error(configDirH.getAbsoluteFile()
                    + " cannot be read or does not exist!");
        }
    }

    public String getIdentifier() {
        return configDirH.getAbsolutePath();
    }

    public void setRelativePath(String relPath) {
        this.relPath = relPath;
    }

    public void setAbsolutePath(String absPath) {
        this.absPath = absPath;
    }

    public void setApplicationContext(ApplicationContext arg0)
            throws BeansException {
        context = (WebApplicationContext) arg0;
    }

    public CacheFactory getCacheFactory() {
        return this.cacheFactory;
    }

}