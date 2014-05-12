/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import java.io.PrintStream;

import javax.xml.stream.XMLStreamException;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Writer;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;

/**
 * Wraps org.sbml.jsbml.Writer.
 * 
 * @author David Croft
 *
 */
public class JsbmlWriter implements Writer {
	@Override
	public boolean writeSBML(Document document) {
		SBMLWriter sbmlWriter = new SBMLWriter();
		
		try {
			PrintStream outputStream = System.out;
			sbmlWriter.write(((org.gk.sbml.model.jsbml.JsbmlDocument)document).getSbmlElement(), outputStream);
			outputStream.println("");
		} catch (SBMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
}
