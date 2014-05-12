package org.reactome.convert.common;

/**
 * A helper Class to store Converters, their indentifiable information and the according auto detection xPath
 * String
 * @author andreash
 * 
 */
public class ConverterClass {
	private String className;
	private String modelName;
	private String modelVersion;
	private String autoDetect;
	
	private Class converterClass;
	
	/**
	 * Creates a Converter class with the given parameters.
	 * @param converter The class path of the converter
	 * @param className The given name of the converter
	 * @param modelName The model name this converter represents
	 * @param modelVersion The model Version this converter represents
	 * @param autoDetect The autoDetect XPath String where the modelVersion can be found at
	 */
	public ConverterClass(Class converter, String className, String modelName, String modelVersion, String autoDetect) {
		this.className = className;
		this.modelName = modelName;
		this.modelVersion = modelVersion;
		this.autoDetect = autoDetect;
		this.converterClass = converter;
	}
	
	/**
	 * Returns a new instance of the converter
	 * @return A new converter instance or null if the instance could not be created
	 */
	public Converter newInstance() {
		try {
			return (converterClass==null) ? null : (Converter)converterClass.newInstance();
		} catch (InstantiationException ex) {
			return null;
		} catch (IllegalAccessException ex) {
			return null;
		}
	}

	/**
	 * returns the String representation of a Converter class, consisting of the model name, the model Version
	 * and the class name.
	 * @return the String representation of the ConverterClass
	 */
	public String toString() {
		return modelName+":"+modelVersion+"@"+className;
	}
	
	/**
	 * @return the classname
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * @return the model version
	 */
	public String getModelVersion() {
		return modelVersion;
	}

	/**
	 * @return the auto detection xPath String
	 */
	public String getAutoDetect() {
		return autoDetect;
	}

	/**
	 * Sets the class name
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * Sets the model name
	 * @param modelName the modelName to set
	 */
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	/**
	 * sets the model version
	 * @param modelVersion the modelVersion to set
	 */
	public void setModelVersion(String modelVersion) {
		this.modelVersion = modelVersion;
	}

	/**
	 * sets the auto detect xPath String
	 * @param autoDetect the autoDetect to set
	 */
	public void setAutoDetect(String autoDetect) {
		this.autoDetect = autoDetect;
	}
}
