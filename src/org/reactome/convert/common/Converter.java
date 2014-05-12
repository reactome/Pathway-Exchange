/**
 * 
 */
package org.reactome.convert.common;

import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;

/**
 * Interface for Converters into Reactome. This interface has to be
 * implemented if Usage of the ConverterHandler is intended<br>
 * This interface was created with the panther converter in mind.
 * If changes are required, please check if compatibility is still
 * assured wherever it was used 
 * @author andreash
 */
public interface Converter {
	/**
	 * Converts a file into the Reactome data format
	 * @param fileName the name of the file to convert
	 * @return The top level Pathway Instance
	 * @throws Exception
	 */
	public GKInstance convert(String fileName) throws Exception;
	
	/**
	 * The converting is usually divided into two steps: this is the first step.
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public GKInstance convertBeforePost(String fileName) throws Exception;
	
	/**
	 * Converts a list of files into the Reactome data format 
	 * @param fileNames The list of Filenames used
	 * @return List of top level Pathways created by the converter
	 * @throws Exception
	 */
	public List<GKInstance> convert(List<String> fileNames) throws Exception;
	
	/**
	 * This method saves the converted Pathways into a Reactome Curator Tool File
	 * @param projectFileName the project filename of the curator tool project
	 * @throws Exception
	 */
	public void save(String projectFileName) throws Exception;
	
	/**
	 * Sets the database adaptor used by the converter
	 * @param dbAdaptor The database adaptor
	 */
	public void setDatabaseAdaptor(MySQLAdaptor dbAdaptor);
	
	/**
	 * Sets the file adaptor used by the converter
	 * @param fileAdaptor The file Adaptor
	 */
	public void setFileAdaptor(XMLFileAdaptor fileAdaptor);
}
