/*
 * Created on Jun 17, 2005
 */
package org.reactome.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to define a Java class to feed it into FreeMarker templates. This is a
 * simple way to describe a Java class and should not be mixed with the full-blown Class in
 * java.lang package.
 * @author wgm
 */
public class JavaClassDefinition {
    // A list of super classes
    private Set superClassNames;
    // A list of implemented interface
    private Set interfaceNames;
    // A list of private variables
    private Set variableNames;
    // A variable type map
    private Map variableTypeMap;
    // name
    private String name;
    // package name
    private String packageName;
    
    public JavaClassDefinition() {
    }
    
    public JavaClassDefinition(String name) {
        this();
        setName(name);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setPackageName(String name) {
        this.packageName = name;
    }
    
    public String getPackageName() {
        return this.packageName;
    }
    
    public int getInterfaceCount() {
        return interfaceNames == null ? 0 : interfaceNames.size();
    }
    
    public Set getInterfaceNames() {
        return interfaceNames;
    }
    
    public void addInterfaceNames(String name) {
        if (interfaceNames == null)
            interfaceNames = new HashSet();
        interfaceNames.add(name);
    }
    
    public int getSuperClassCount() {
        return superClassNames == null ? 0 : superClassNames.size();
    }
    
    public Set getSuperClassNames() {
        return superClassNames;
    }
    
    public void addSuperClassName(String name) {
        if (superClassNames == null)
            superClassNames = new HashSet();
        superClassNames.add(name);
    }

    public int getVariableCount() {
        return variableNames == null ? 0 : variableNames.size();
    }
    
    public Set getVariableNames() {
        return variableNames;
    }
    
    public void addVariableName(String name) {
        if (variableNames == null)
            variableNames = new HashSet();
        variableNames.add(name);
    }
    
    public void setVariableType(String variableName, String typeName) {
        if (variableTypeMap == null)
            variableTypeMap = new HashMap();
        variableTypeMap.put(variableName, typeName);
    }
    
    public String getVariableType(String variableName) {
        // Use default Object
        String typeName = (String) variableTypeMap.get(variableName);
        if (typeName == null)
            return "Object";
        return typeName;
    }
    
    public Map getVariableTypeMap() {
        return variableTypeMap;
    }
    
}
