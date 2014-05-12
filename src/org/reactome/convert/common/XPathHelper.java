package org.reactome.convert.common;

import java.util.HashMap;
import java.util.Random;

import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Element;

/**
 * This class exists as a helper class for XPath. 
 * The class exists because normal XPath does not handle default namespaces.
 * This class prepares the XPath-Object and transforms default namespaces into
 * a fixed namespace so default namepsaces in xPath queries can be used.<br><br>
 * <b>Warning:</b><br>
 * As a restriction, the default namespace of <code>rootElement</code> should have
 * the same default namespace as the <code>Element</code>-Objects it is used on later.
 * If this is not the case, a successful usage cannot be guaranteed.<br>
 * Works with <code>org.jaxen.XPath</code>, incompatible with
 * <code>org.jdom.xpath.XPath</code>.
 * @author andreash
 */
public class XPathHelper {
	// the default namespace used will be randomized to avoid accidential similarities
	private static Random myRand = new Random();
	
	/**
	 * Creates a new XPathHelperObject
	 */
	public XPathHelper() {
	}
	
	/**
	 * Converts an xPath String and a root Element into a valid Jaxen XPath Object
	 * @param xPath xPath String
	 * @param rootElement root Element of the XML Document
	 * @return Jaxen XPath object
	 * @throws JaxenException XPath Expression seems to be broken
	 */
	public static XPath getXPathObject(String xPath, Element rootElement) throws JaxenException {
		String path = xPath;
		String defaultNamespace = "defaultNamespaceForXPath"+new Integer(myRand.nextInt(Integer.MAX_VALUE)).toString();
		path = addDefaultNamespace(path, defaultNamespace);
		SimpleNamespaceContext context = getNamespacesFromXPath(path, rootElement, defaultNamespace);
		XPath myPath = new JDOMXPath(path);
		myPath.setNamespaceContext(context);
		return myPath;
	}
	
	/**
	 * Transforms an xPath String and adds changes default namespaces to the string
	 * defined in <code>defaultNamespace</code>. This is required as xPath cannot
	 * handle default namespaces.
	 * @param xPath the XPath entry to modify
	 * @param defaultNamespace the name of the default namespace to be inserted
	 * @return String modified xPath String
	 */
	private static String addDefaultNamespace(String xPath, String defaultNamespace) {
		String myPath = xPath;
		String outPath = "";
		if (myPath.substring(0,1).equals(".")) {
			outPath += ".";
			myPath = myPath.substring(1);
		}
		while(myPath!=null && myPath.length()!=0) {
			
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
	 * @param xPath XPath String to extract the Context of 
	 * @param rootElm root element that holds the required namespaces
	 * @param defaultNamespace the default Namespace used for this context
	 * @return SimpleNamespaceContext A xpath namespace context used for Jaxen
	 */
	private static SimpleNamespaceContext getNamespacesFromXPath(String xPath, Element rootElm, String defaultNamespace) {
		HashMap<String, String> map = new HashMap<String, String>();
		String myPath = xPath;
		if (myPath.substring(0,1).equals(".")) {
			myPath = myPath.substring(1);
		}
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
}
