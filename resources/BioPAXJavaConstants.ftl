/*
 * This file is generated from a FreeMarker template. 
 */
<#-- package name -->
package org.reactome.biopax;

<#if isForBioPAX>
import org.jdom.Namespace;

public class BioPAXJavaConstants {

    // A list of name spaces
    public static final String BIOPAX_NS = "http://www.biopax.org/release/biopax-level2.owl#";
    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
    public static final String BIOPAX_DOWNLOAD_URI = "http://www.biopax.org/Downloads/Level2v0.91/biopax-level2.owl";
    
    // Reused NameSpaces
    public static final Namespace bpNS = Namespace.getNamespace(BioPAXJavaConstants.BIOPAX_NS);
    public static final Namespace rdfNS = Namespace.getNamespace("rdf", BioPAXJavaConstants.RDF_NS);
    public static final Namespace rdfsNS = Namespace.getNamespace("rdfs", BioPAXJavaConstants.RDFS_NS);
    public static final Namespace owlNS = Namespace.getNamespace("owl", BioPAXJavaConstants.OWL_NS);
    public static final Namespace xsdNS = Namespace.getNamespace("xsd", BioPAXJavaConstants.XSD_NS);
<#else>
public class ReactomeJavaConstants {
</#if>
    
    // A list of class names
    <#list classNames as c>
    public static final String ${c} = "${c}";
    </#list>
    
    // A list of property names
    <#list propertyNames as p>
    public static final String ${p?replace("-", "_")} = "${p}";
    </#list>
    
    <#if (dataTypeNames)?has_content>
    // A list of data type names
    <#list dataTypeNames as d>
    public static final String XSD_${d?upper_case} = XSD_NS + "${d}"; 
    </#list>
    </#if>
}