/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbgn;

import java.io.File;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.sbgn.bindings.Sbgn;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Dump SBGN diagrams to strings, streams or files.
 * 
 * @author David Croft
 *
 */
public class Dumper {
	public static void dumpToFile(Sbgn sbgn) {
		dumpToFile(sbgn, "sbgn.xml");
	}
	
	/**
	 * Dump to the named file.  If filename is null, dump to STDOUT.
	 * 
	 * @param filename
	 */
	public static void dumpToFile(Sbgn sbgn, String filename) {
		if (filename == null)
			System.out.print(dumpToString(sbgn));
		else
			dumpToFile(sbgn, new File(filename));
	}
	
	public static void dumpToFile(Sbgn sbgn, File file) {
		try {
			JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(sbgn, file);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public static String dumpToString(Sbgn sbgn) {
		try {
			JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			marshaller.marshal(sbgn, doc);			

		    NodeList nodes = doc.getElementsByTagName("sbgn");
		    if (nodes == null)
	    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - nodes is null");
		    else if (nodes.getLength() < 1)
	    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - there are no nodes in the doc");
		    else {
		    	Node node = nodes.item(0);
		    	if (node == null)
		    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - node is null");
		    	else {
			    	String nodeString = nodeToString(node);
			    	if (nodeString == null)
			    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - nodeString is null");
			    	else if (nodeString.isEmpty())
			    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - nodeString is empty");
			    	else
			    		return nodeString;
		    	}
		    }
		} catch (Exception e) {
    		System.err.println("LayoutGeneratorSBGNPD.dumpToString: WARNING - problem converting SBGN to string");
			e.printStackTrace(System.err);
		}
		
		return null;
	}

	private static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = SAXTransformerFactory.newInstance().newTransformer();
			//t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException e) {
    		System.err.println("LayoutGeneratorSBGNPD.nodeToString: WARNING - problem converting node to string");
			e.printStackTrace(System.err);
		}
		return sw.toString();
	}
}
