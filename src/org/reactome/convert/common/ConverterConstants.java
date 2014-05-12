package org.reactome.convert.common;

/**
 * Setup class for Converters.<br>
 * If you want to add a new Converter, add the Class Name,
 * Model Name, model Version and auto-detection path to the
 * data structure. Auto-detection and instancing is only
 * possible if all the information is provided.
 * @author andreash
 */
public class ConverterConstants {
	/**
	 * Class path to the converter.<br>
	 * Example: <code>org.reactome.panther.PantherToReactomeConverter</code>
	 */
	public static String[] classNames = {
		"org.reactome.panther.PantherToReactomeConverter",
		"org.reactome.panther.PantherToReactomeConverter",
		"org.reactome.panther.PantherToReactomeConverterV25",
		"org.reactome.panther.PantherToReactomeConverterV25"
	};
	
	/**
	 * Model you want to convert. A key to get to your converter.<br>
	 * Examples: <code>"celldesigner"</code>, <code>"biomart"</code>, ...
	 */
	public static String[] modelNames = {
		"celldesigner",
		"celldesigner",
		"celldesigner",
		"celldesigner"
	};
	
	/**
	 * Version of the XML-File used for conversion.<br>
	 * Example: <code>"1.0"</code>
	 */
	public static String[] modelVersions = {
		"2.2",
		"2.3",
		"2.5",
		"4.0"
	};
	
	/**
	 * X-Path compliant path to the Model Version of the XML-File for Model Auto
	 * detection. If auto detection is not required or possible (e.g. due to missing
	 * version in the xml file), use the String "" instead.<br>
	 * Auto detection will only work if sets consisting of modelVersions and
	 * autoDetectVersionPath are unique. Please take into account that the detector
	 * only uses a simplified xPath language. If the version number is an attribute,
	 * the attribute has to be included in the XPath expression (See Example 3).<br><br>
	 * Example 1: <code>"/sbml/model/annotation/celldesigner:modelVersion"</code><br>
	 * Exampl3 2: <code>"/root/element/namespace:versionElement"</code><br>
	 * Example 3: <code>"/root/element[&#x0040;versionAttribute]"</code><br>
	 */
	public static String[] autoDetectVersionPath = {
		"//sbml/model/annotation/celldesigner:modelVersion",
		"//sbml/model/annotation/celldesigner:modelVersion",
		"//sbml/model/annotation/celldesigner:modelVersion",
		"//sbml/model/annotation/celldesigner:modelVersion"
	};
}
