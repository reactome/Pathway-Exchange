package org.reactome.convert.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * A handler for converters. For Configuration of the handler, please
 * modify the Class <code>ConverterConstants</code>. The Handler is
 * able to automatically detect the version of a model used in an XML
 * file, but only if the configuration is correct.
 * @author andreash
 */
public class ConverterHandler {
	private List<ConverterClass> converters = new ArrayList<ConverterClass>();
	private static ConverterHandler instance = new ConverterHandler();
	
	public static ConverterHandler getInstance() {
		return instance;
	}
	
	private ConverterHandler() {
		init();
	}
	
	/**
	 * initializes the Converter by using <code>ConverterConstants</code>
	 */
	private void init() {
		for (int i = 0; i < ConverterConstants.classNames.length; i++) {
			String className = ConverterConstants.classNames[i];
			String modelName = ConverterConstants.modelNames[i];
			String modelVersion = ConverterConstants.modelVersions[i];
			String autoDetect = ConverterConstants.autoDetectVersionPath[i];
			try {
				Class clazz = Class.forName(className);
				ConverterClass converter = new ConverterClass(clazz, className, modelName, modelVersion, autoDetect);
				converters.add(converter);
			} catch(ClassNotFoundException ex) {
				System.err.println("Initialising failed for Converter " + modelName + " Version " + modelVersion);
			}
		}
	}
	
	/**
	 * Detects the model name and model version of a given XML-File. For a successful auto-detect,
	 * the converter and it's appropriate information has to be available in <code>ConverterConstants</code>.
	 * @param fileName - XML-File to detect
	 * @return A model name and model version Pair or <code>null</code> if autoDetect failed 
	 * @throws <b>IOException</b> The XML document could not be opened. Check the filename.
	 * @throws <b>JDOMException</b> The document could not be parsed. Please check if the XML-File is valid
	 * @throws <b>JaxenException</b> The XPath-Expression could not be parsed. please check the expression 
	 * in <code>ConverterConstants</code>
	 */
	public Pair<String, String> autoDetect(String fileName) throws JDOMException, IOException, JaxenException {
		SAXBuilder builder = new SAXBuilder();
		Document xmlDoc = builder.build(new File(fileName));
		for (ConverterClass converter : converters) {	
			String xPath = converter.getAutoDetect();
			String versionString = getXPathNodes(xPath, xmlDoc);
			if (versionString.equals(converter.getModelVersion())) {
				return new Pair<String, String>(converter.getModelName(), versionString);
			}
		}
		return null;
	}
	
	/**
	 * This method looks for a certain xPath expression in an xml Document while taking care of 
	 * xPath default namespace problematics
	 * @param xPath - xPath query to be used on the document
	 * @param xmlDoc - Document that is to be detected
	 * @return List of nodes found with the expression. 
	 * @throws <b>IOException</b> The XML document could not be opened. Check the filename.
	 * @throws <b>JDOMException</b> The document could not be parsed. Please check if the XML-File is valid
	 * @throws <b>JaxenException</b> The XPath-Expression could not be parsed. please check the expression 
	 * in <code>ConverterConstants</code>
	 */
	private String getXPathNodes(String xPath, Document xmlDoc) throws JDOMException, IOException, JaxenException {
		XPath myPath = getXPathObject(xPath, xmlDoc.getRootElement());
		List nodes = myPath.selectNodes(xmlDoc);
		if (nodes.size() == 0) {
			return "";
		}
		Object nodeEntry = myPath.selectNodes(xmlDoc).get(0);
		String lastAttributeName = extractLastAttributeName(xPath);
		if (lastAttributeName.length()==0) {
			return ((Element)nodeEntry).getTextTrim();
		} else {
			return ((Element)nodeEntry).getAttributeValue(lastAttributeName);
		}
	}

	/**
	 * Will extract an XML attribute from an xPath expression if it's available in the last element only
	 * @param xPath - xPath expression
	 * @return - attribute name or empty String if no Attribute is available
	 */
	private String extractLastAttributeName(String xPath) {
		String lastElement = xPath.substring(xPath.lastIndexOf("/"));
		int attributeStartIndex = lastElement.indexOf("@")+1;
		if (attributeStartIndex >= 0) {
			String attributePart = lastElement.substring(attributeStartIndex);
			if (lastElement.contains("=")) {
				return lastElement.substring(attributeStartIndex, lastElement.indexOf("="));
			}
			if (lastElement.contains("]")) {
				return lastElement.substring(attributeStartIndex, lastElement.indexOf("]"));
			}
		}
		return "";
	}
	
	/**
	 * converts an xPath String and a root Element into a valid Jaxen XPath Object
	 * @param xPath - xPath String
	 * @param rootElement - root Element of the XML Document
	 * @return Jaxen XPath object
	 * @throws JaxenException - XPath Expression seems to be broken
	 */
	private XPath getXPathObject(String xPath, Element rootElement) throws JaxenException {
		String defaultNamespace = "defaultNamespaceForXPath";
		xPath = addDefaultNamespace(xPath, defaultNamespace);
		SimpleNamespaceContext context = getNamespacesFromXPath(xPath, rootElement, defaultNamespace);
		XPath myPath = new JDOMXPath(xPath);
		myPath.setNamespaceContext(context);
		return myPath;
	}
	
	/**
	 * Transforms an xPath String and adds changes default namespaces to the string
	 * defined in <code>defaultNamespace</code>. This is required as xPath cannot
	 * handle default namespaces.
	 * @param xPath - the XPath entry to modify
	 * @param defaultNamespace - the name of the default namespace to be inserted
	 * @return String - modified xPath String
	 */
	private String addDefaultNamespace(String xPath, String defaultNamespace) {
		String myPath = xPath;
		String outPath = "";
		while(myPath.length()!=0) {
			int startIndex = myPath.indexOf("/");
			if (startIndex == 0) {
				myPath = myPath.substring(startIndex+1);
				outPath += "/";
				continue;
			}
			if (startIndex == -1) {
				startIndex = myPath.length();
			}
			String pathPart = myPath.substring(0, startIndex);
			if (pathPart.indexOf(":") == -1) {
				outPath += defaultNamespace+":";
			}
			outPath += pathPart;
			myPath = myPath.substring(startIndex);
		}
		return outPath;
	}
	
	/**
	 * Method to create a <code>SimpleNamespaceContext</code> to be used with
	 * Jaxen, especially for usage with XPath. This method works together with
	 * <code>addDefaultNamespace</code> which should always be called before
	 * with the same <code>defaultNamespace</code> parameter to create a working
	 * xPath String.
	 * @param xPath - XPath String to extract the Context of 
	 * @param xml - root element that holds the required namespaces
	 * @return SimpleNamespaceContext - A xpath namespace context used for Jaxen
	 */
	private SimpleNamespaceContext getNamespacesFromXPath(String xPath, Element rootElm, String defaultNamespace) {
		HashMap<String, String> map = new HashMap<String, String>();
		String myPath = xPath;
		
		while(myPath.length()!=0) {
			int startIndex = myPath.indexOf("/");
			if (startIndex == 0) {
				myPath = myPath.substring(startIndex+1);
				continue;
			}
			if (startIndex == -1) {
				startIndex = myPath.length();
			}
			String pathPart = myPath.substring(0, startIndex);
			int nsEndIndex = pathPart.indexOf(":");
			if (nsEndIndex != -1) {
				String ns = pathPart.substring(0, nsEndIndex);
				
				if (ns.equals(defaultNamespace)) {
					map.put(ns, rootElm.getNamespace("").getURI());
				} else {
					map.put(ns, rootElm.getNamespace(ns).getURI());
				}
			}
			myPath = myPath.substring(startIndex);
		}
		return new SimpleNamespaceContext(map);
	}
	
	/**
	 * Returns a new object of the class <code>Converter</code>
	 * @param modelName The name/key for the XML-Converter
	 * @param modelVersion The model Version of the XML-Format
	 * @return A new Converter of the given class or <code>null</code> if converter was not found 
	 */
	public Converter getConverter(String modelName, String modelVersion) {
		if (modelName != null && modelName.length() != 0 && modelVersion != null && modelVersion.length() != 0) {
			for (ConverterClass converter : converters) {
				if (converter.getModelName().equals(modelName) && converter.getModelVersion().equals(modelVersion)) {
					return converter.newInstance();
				}
			}
		}
		return null;
	}
	
	/**
	 * Search for an appropriate Converter from the registered Converter class.
	 * @param modelFileName
	 * @return
	 */
	public Converter getConverter(String modelFileName) throws Exception {
	    Pair<String, String> desc = autoDetect(modelFileName);
	    return getConverter(desc);
	}
	
	/**
	 * Returns a new object of the class <code>Converter</code>
	 * @param modelNameVersionPair - a <code>Pair</code> of modelName and modelVersion 
	 * @return A new Converter of the given class or <code>null</code> if converter was not found
	 */
	public Converter getConverter(Pair modelNameVersionPair) {
		if (modelNameVersionPair!=null) {
			return getConverter((String)modelNameVersionPair.getMember1(), (String)modelNameVersionPair.getMember2());
		}
		return null;
		
	}
}
