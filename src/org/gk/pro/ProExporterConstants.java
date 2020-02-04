package org.gk.pro;

import java.util.Arrays;
import java.util.List;

public class ProExporterConstants {
    public static final String plus = "+";
    public static final String equals = "=";
    public static final String unknown = "?";
    public static final String colon = ":";
    public static final String at = "@";
    public static final String leftBracket = "[";
    public static final String rightBracket = "]";
    public static final String delimiter = "\t";
    public static final String freeTextDelimiter = "^|^";
    public static final String ewas = "EWAS";
    public static final String mod = "=MOD:";
    public static final String chebi = "=CHEBI:";
    public static final String uniprot = "=UniProt:";
    public static final String reactome = "=Reactome:";
    public static final String stableIdPrefix= "R-HSA-";
    public static final String insertion = "INSERTION";
    public static final String deletion = "DELETION";
    public static final String replaced = "REPLACED";
    public static final String crosslink = "CROSSLINK";

    public static final String entityType = "Type";
    public static final String identifier = "Identifier";
    public static final String location= "Location";
    public static final String accession = "Accession";
    public static final String startPosition = "Start";
    public static final String endPosition = "End";
    public static final String modifications = "Modifications";
    public static final String freeText = "Free Text";

    public static final List<String> COLUMNS = Arrays.asList(entityType,
                                                             identifier,
                                                             location,
                                                             accession,
                                                             startPosition,
                                                             endPosition,
                                                             modifications,
                                                             freeText);
}
