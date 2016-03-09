/*
 * Created on Jun 18, 2009
 *
 */
package org.gk.biosystems;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.PathwayDiagramGeneratorViaAT;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.RenderablePathway;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.junit.Test;
import org.reactome.convert.common.AbstractConverterFromReactome;
import org.reactome.convert.common.FrontPageItemHelper;
import org.reactome.convert.common.PathwayReferenceEntityHelper;

/**
 * This class is used to convert Reactome pathways onto BioSystems required XML files. See examples in
 * http://www.ncbi.nlm.nih.gov/biosystems/.
 * The application based on this class should be run at the dev machine during release time so that
 * a downloadable file can be placed at the download directory for the NCBI people to use.
 * @author wgm
 *
 */
public class ReactomeToBioSystemsConverter extends AbstractConverterFromReactome {
    private String optionalPathwayUrl = "http://www.reactome.org/content/detail/";
    // Default should use stable_id
    private String pathwayUrl = "http://www.reactome.org/content/detail/";
    private String sourceUrl = "http://www.reactome.org";
    // This is for chicken
    ///public String pathwayUrl = "http://gallus.reactome.org/cgi-bin/eventbrowser?DB=test_gallus_reactome_release_0&ID=";
    //private String sourceUrl = "http://gallus.reactome.org";
    // A flag to indicate if the pathway hierarchy should be used.
    private boolean needHierarchy = false;
    // Used to add helper attribute
    private final String SUPER_PATHWAY = "superPatway";
    
    public ReactomeToBioSystemsConverter() {
    }
    
    public void setNeedHierarchy(boolean need) {
        this.needHierarchy = need;
    }
    
    public Document convertPathway(GKInstance pathway) throws Exception {
        List<GKInstance> pathways = new ArrayList<GKInstance>();
        pathways.add(pathway);
        return convertPathways(pathways);
    }
    
    /**
     * Convert a list of pathways represented by DB_IDs to a XML document.
     * @param pathwayIds
     * @return
     * @throws Exception
     */
    public Document convertPathways(List<GKInstance> pathways) throws Exception {
        if (dbAdaptor == null) {
            throw new IllegalStateException("ReactomeToBioSystemConverter.convertPathways(): no database connection.");
        }
        if (targetDir == null) {
            targetDir = new File(".");
        }
        Document document = new Document();
        DocType docType = new DocType(BioSystemsConstants.biosystems, "rssm.dtd");
        document.setDocType(docType);
        Element bsElm = new Element(BioSystemsConstants.biosystems);
        document.setRootElement(bsElm);
        // Add source and sourceurl
        Element sourceElm = new Element(BioSystemsConstants.source);
        sourceElm.setText("Reactome");
        bsElm.addContent(sourceElm);
        Element sourceUrlElm = new Element(BioSystemsConstants.sourceurl);
        //sourceUrlElm.setText("http://www.reactome.org");
        sourceUrlElm.setText(sourceUrl);
        bsElm.addContent(sourceUrlElm);
        // Add reactome citation
        Element reactomeCitationsElm = createReactomeCitations();
        bsElm.addContent(reactomeCitationsElm);
        Set<GKInstance> parsedPathways = new HashSet<GKInstance>();
        // Prepare the pathways for sup-set information
        if (needHierarchy) {
            createSubPathwayOfAttributes(pathways);
        }
        // Start working with pathways
        for (GKInstance pathway : pathways) {
            if (debug)
                System.out.println("Converting " + pathway);
            Element pathwayElm = createPathwayElm(pathway);
            bsElm.addContent(pathwayElm);
            parsedPathways.add(pathway);
            if (needHierarchy) {
                createPathwayElmForSub(pathway,
                                       pathwayElm,
                                       bsElm,
                                       parsedPathways);
            }
        }
        return document;
    }
    
    /**
     * This helper method is used to fill-up a helper attribute to be used
     * to create sup-set links.
     * @param pathways
     * @throws Exception
     */
    private void createSubPathwayOfAttributes(List<GKInstance> pathways) throws Exception {
        for (GKInstance pathway : pathways) {
            createSubPathwayOfAttributes(pathway);
        }
    }
    
    private void createSubPathwayOfAttributes(GKInstance pathway) throws Exception {
        List<GKInstance> hasEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (hasEvents == null || hasEvents.size() == 0)
            return;
        for (GKInstance subPathway : hasEvents) {
            if (!subPathway.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                continue;
            Set<GKInstance> sup = (Set<GKInstance>) subPathway.getAttributeValueNoCheck(SUPER_PATHWAY);
            if (sup == null) {
                sup = new HashSet<GKInstance>();
                subPathway.setAttributeValueNoCheck(SUPER_PATHWAY, sup);
            }
            sup.add(pathway);
            createSubPathwayOfAttributes(subPathway);
        }
    }
    
    private void createPathwayElmForSub(GKInstance container,
                                        Element pathwayElm,
                                        Element bsElm,
                                        Set<GKInstance> parsedPathways) throws Exception {
        // Get contained sub-pathways
        List subpathways = container.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (subpathways == null || subpathways.size() == 0)
            return;
        Element linkedsystems = null;
        List<GKInstance> subPathways = new ArrayList<GKInstance>();
        // Create subset links for the passed pathwayElm and container.
        for (Iterator it = subpathways.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            // Should work with Pathways only, not Reactions.
            if (!event.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                continue;
            // Create subset first
            if (linkedsystems == null) {
                linkedsystems = new Element(BioSystemsConstants.linkedsystems);
            }
            Element linkedsystem = createLinkedSystemElement(event, 
                                                             BioSystemsConstants.subset);
            linkedsystems.addContent(linkedsystem);
            subPathways.add(event);
        }
        // For orthologous event
        Element linkedsystem = createLinkedSystemForOrthologousSource(container);
        if (linkedsystem != null) {
            if (linkedsystems == null)
                linkedsystems = new Element(BioSystemsConstants.linkedsystems);
            linkedsystems.addContent(linkedsystem);
        }
        if (linkedsystems != null)
            pathwayElm.addContent(linkedsystems);
        // Create elements for sub pathways and sup-set links
        for (GKInstance subPathway : subPathways) {
            if (parsedPathways.contains(subPathway))
                continue; // Don't need to recursive down
            if (debug)
                System.out.println("Converting " + subPathway);
            Element subPathwayElm = createPathwayElm(subPathway);
            bsElm.addContent(subPathwayElm);
            parsedPathways.add(subPathway);
            createPathwayElmForSub(subPathway,
                                   subPathwayElm,
                                   bsElm,
                                   parsedPathways);
            // Need to add sup for this sub-pathway
            Element subLinkedsystems = subPathwayElm.getChild(BioSystemsConstants.linkedsystems);
            if (subLinkedsystems == null) {
                subLinkedsystems = new Element(BioSystemsConstants.linkedsystems);
                subPathwayElm.addContent(subLinkedsystems);
            }
            // This subPathway may be a child of multiple containers. All these containser should be listed
            // as its superset.
            Set<GKInstance> containers = (Set<GKInstance>) subPathway.getAttributeValueNoCheck(SUPER_PATHWAY);
            if (containers == null || containers.size() == 0) {
                throw new IllegalStateException(subPathway + " should not have empty super pathway values!");
            }
            for (GKInstance sup : containers) {
                linkedsystem = createLinkedSystemElement(sup, BioSystemsConstants.superset);
                subLinkedsystems.addContent(linkedsystem);
            }
        }
    }
    
    private Element createLinkedSystemElement(GKInstance event, 
                                              String type) throws Exception {
        Element linkedsystem = new Element(BioSystemsConstants.linkedsystem);
        String eventId = getReactomeId(event);
        Element externalId = createElment(BioSystemsConstants.externalid, eventId);
        linkedsystem.addContent(externalId);
        Element linkedsystemtype = new Element(BioSystemsConstants.linkedsystemtype);
        Element typeElm = new Element(type);
        linkedsystemtype.addContent(typeElm);
        linkedsystem.addContent(linkedsystemtype);
        return linkedsystem;
    }
    
    @Override
    protected String getReactomeId(GKInstance pathway) throws Exception {
        //GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
        //if (species != null && species.getDisplayName().equals("Homo sapiens")) {
            return super.getReactomeId(pathway);
        //}
        //return pathway.getDBID() + "";
    }
    
    private Element createLinkedSystemForOrthologousSource(GKInstance event) throws Exception {
        GKInstance orthologousFrom = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.inferredFrom);
        if (orthologousFrom == null)
            return null;
        return createLinkedSystemElement(orthologousFrom,
                                         BioSystemsConstants.linked);
    }
    
    private String checkPathwayDescription(String desc) {
        // Change a long description from Reactome to a shorter version for BioSystems
        if (desc.startsWith("This event has been computationally inferred from an event"))
            return "computationally inferred pathway (not manually curated)";
        return desc;
    }
    
    private Element createPathwayElm(GKInstance pathway) throws Exception {
        Element pathwayElm = new Element(BioSystemsConstants.biosystem);
        //TODO: This should be changed to statble_id in the future for human pathways.
        String pathwayId = getReactomeId(pathway);
        Element externalid = createElment(BioSystemsConstants.externalid, 
                                          pathwayId);
        pathwayElm.addContent(externalid);
        Element name = createElment(BioSystemsConstants.name,
                                    pathway.getDisplayName());
        pathwayElm.addContent(name);
        Element biosystemtype = new Element(BioSystemsConstants.biosystemtype);
        pathwayElm.addContent(biosystemtype);
        Element elm = new Element(BioSystemsConstants.organism_specific_biosystem);
        biosystemtype.addContent(elm);
        // Use summation for description
        GKInstance summation = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.summation);
        if (summation != null) {
            String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
            text = checkPathwayDescription(text);
            elm = createElment(BioSystemsConstants.description, text);
            pathwayElm.addContent(elm);
        }
        if (!needHierarchy) { // thunmbmail should be disabled in this case
            Element thumbnail = createThumbnail(pathway);
            if (thumbnail != null)
                pathwayElm.addContent(thumbnail);
        }
        // Pathway URL
        elm = createElment(BioSystemsConstants.url,
                           pathwayUrl + getReactomeId(pathway));
        pathwayElm.addContent(elm);
        // TODO: hiliteurl is not available. It should be added after the new Web ELV available.
        // Taxonomy
        Element taxonomy = new Element(BioSystemsConstants.taxonomy);
        pathwayElm.addContent(taxonomy);
        List speciesList = pathway.getAttributeValuesList(ReactomeJavaConstants.species);
        for (int i = 0; i < speciesList.size(); i++) {
            GKInstance species = (GKInstance) speciesList.get(i);
            // Get taxid
            GKInstance crossReference = (GKInstance) species.getAttributeValue(ReactomeJavaConstants.crossReference);
            String id = (String) crossReference.getAttributeValue(ReactomeJavaConstants.identifier);
            Element taxnode = new Element(BioSystemsConstants.taxnode);
            taxonomy.addContent(taxnode);
            elm = createElment(BioSystemsConstants.taxid, id);
            taxnode.addContent(elm);
            elm = createElment(BioSystemsConstants.taxonomyname, species.getDisplayName());
            taxnode.addContent(elm);
        }
        // Handle pathway components
        handlePathwayComponents(pathwayElm, pathway);
        Element citations = createPathwayCitations(pathway);
        if (citations != null)
            pathwayElm.addContent(citations);
        return pathwayElm;
    }
    
    private Element createPathwayCitations(GKInstance pathway) throws Exception {
        List list = pathway.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
        if (list == null || list.size() == 0)
            return null;
        Element citations = new Element(BioSystemsConstants.citations);
        // Add citation
        for (Iterator it = list.iterator(); it.hasNext();) {
            GKInstance lr = (GKInstance) it.next();
            // Check if there is pmid
            Integer pmid = null;
            if (lr.getSchemClass().isValidAttribute(ReactomeJavaConstants.pubMedIdentifier))
                pmid = (Integer) lr.getAttributeValue(ReactomeJavaConstants.pubMedIdentifier);
            if (pmid != null) {
                Element pmidElm = createPMIDCitation(pmid + "");
                citations.addContent(pmidElm);
            }
            else {
                // Create textcitation element.
                Element textcitation = createTextCitation(lr);
                citations.addContent(textcitation);
            }
        }
        return citations;
    }
    
    private Element createTextCitation(GKInstance lr) throws Exception {
        Element citation = new Element(BioSystemsConstants.citation);
        // Convert lr to a text
        List authors = lr.getAttributeValuesList(ReactomeJavaConstants.author);
        StringBuilder builder = new StringBuilder();
        for (Iterator it = authors.iterator(); it.hasNext();) {
            GKInstance author = (GKInstance) it.next();
            builder.append(author.getDisplayName());
            if (it.hasNext())
                builder.append(", ");
        }
        builder.append(". ");
        // Get year
        if (lr.getSchemClass().isValidAttribute(ReactomeJavaConstants.year)) {
            Integer year = (Integer) lr.getAttributeValue(ReactomeJavaConstants.year);
            if (year != null)
                builder.append(year).append(". ");
        }
        String title = (String) lr.getAttributeValue(ReactomeJavaConstants.title);
        if (title != null)
            builder.append(title);
        // Check if page
        if (lr.getSchemClass().isValidAttribute(ReactomeJavaConstants.pages)) {
            String pages = (String) lr.getAttributeValue(ReactomeJavaConstants.pages);
            if (pages != null) {
                builder.append(": ").append(pages);
            }
        }
        builder.append(".");
        Element textcitation = createElment(BioSystemsConstants.textcitation, builder.toString());
        citation.addContent(textcitation);
        return citation;
    }
    
    private Element createThumbnail(GKInstance pathway) throws Exception {
        RenderablePathway rPathway = queryPathwayDiagram(pathway);
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(rPathway);
        PathwayDiagramGeneratorViaAT generator = new PathwayDiagramGeneratorViaAT();
        File imgFile = generator.saveAsThumbnail(600,
                                                 editor,
                                                 targetDir);
        Element thumbnail = new Element(BioSystemsConstants.thumbnail);
        Element image = new Element(BioSystemsConstants.image);
        thumbnail.addContent(image);
        Element type = new Element(BioSystemsConstants.type);
        image.addContent(type);
        type.addContent(new Element(BioSystemsConstants.png));
        // Base64 encoding image
        FileInputStream fis = new FileInputStream(imgFile);
        FileChannel fileChannel = fis.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)fileChannel.size());
        fileChannel.read(byteBuffer);
        byte[] in = byteBuffer.array();
        byte[] out = Base64.encodeBase64(in);
        // Convert out to a string
        String text = new String(out);
        // For testing
        //System.out.println(text);
        Element encodedimage = createElment(BioSystemsConstants.encodedimage, text);
        image.addContent(encodedimage);
        imgFile.deleteOnExit();
        return thumbnail;
    }
    
//    private void addEntrezGeneIdentifier(GKInstance geneProduct,
//                                         Element entity) throws Exception {
//        // Add only ENTREZ gene identifier since only Entrez gene has been marked in otherIdentifiers
//        List otherIdentifiers = geneProduct.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier);
//        if (otherIdentifiers == null || otherIdentifiers.size() == 0)
//            return;
//        for (Iterator it = otherIdentifiers.iterator(); it.hasNext();) {
//            String id = (String) it.next();
//            if (!id.startsWith("EntrezGene:"))
//                continue;
//            id = id.substring("EntrezGene:".length());
//            // Use geneid for ids fromEntrezGene
//            entity.addContent(createElment(BioSystemsConstants.geneid, id));
//            //Element xref = new Element(BioSystemsConstants.xref);
//            //protein.addContent(xref);
//            //xref.addContent(createElment(BioSystemsConstants.sourcename, "EntrezGene"));
//            //xref.addContent(createElment(BioSystemsConstants.externalid, id));
//        }
//    }
    
    private List<String> getEntrezGeneIdentifiers(GKInstance geneProduct) throws Exception {
        // Add only ENTREZ gene identifier since only Entrez gene has been marked in otherIdentifiers
        List otherIdentifiers = geneProduct.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier);
        if (otherIdentifiers == null || otherIdentifiers.size() == 0)
            return null;
        List<String> rtn = new ArrayList<String>();
        for (Iterator it = otherIdentifiers.iterator(); it.hasNext();) {
            String id = (String) it.next();
            if (!id.startsWith("EntrezGene:"))
                continue;
            id = id.substring("EntrezGene:".length());
            if (Pattern.matches("\\A\\d+\\z", id))
            	rtn.add(id);
        }
        return rtn;
    }

    private void handlePathwayComponents(Element pathwayElm,
                                         GKInstance pathway) throws Exception {
        //System.out.println("Handling pathway: " + pathway);
        PathwayReferenceEntityHelper refEntityHelper = new PathwayReferenceEntityHelper();
        Set<GKInstance> referenceEntities = refEntityHelper.grepReferenceEntitiesInPathway(pathway);
        Set<GKInstance> smallMolecules = new HashSet<GKInstance>();
        Set<GKInstance> geneProducts = new HashSet<GKInstance>();
        refEntityHelper.sortReferenceEntities(referenceEntities, 
                                              smallMolecules,
                                              geneProducts);
        if (geneProducts.size() > 0) {
            Element genes = new Element(BioSystemsConstants.genes);
            pathwayElm.addContent(genes);
            // For gene products
            for (GKInstance geneProduct : geneProducts) {
                Element gene = new Element(BioSystemsConstants.gene);
                genes.addContent(gene);
                Element extenalId = createElment(BioSystemsConstants.externalid, 
                                                 geneProduct.getDBID() + "");
                gene.addContent(extenalId);
                String geneName = (String) geneProduct.getAttributeValue(ReactomeJavaConstants.geneName);
                if (geneName != null) {
                    Element name = createElment(BioSystemsConstants.name, geneName);
                    gene.addContent(name);
                }
                List<String> geneIds = getEntrezGeneIdentifiers(geneProduct);
                // It may have multiple geneids. Need to create multiple entity
                if (geneIds == null || geneIds.size() == 0) {
                    Element entity = new Element(BioSystemsConstants.entity);
                    gene.addContent(entity);
                    //                // Check if there is a gene identifier available
                    //                // Use xref to add identifier
                    //                addEntrezGeneIdentifier(geneProduct, 
                    //                                        entity);
                    createProteinElement(geneProduct, entity);
                }
                else {
                    for (String geneId : geneIds) {
                        Element entity = new Element(BioSystemsConstants.entity);
                        gene.addContent(entity);
                        entity.addContent(createElment(BioSystemsConstants.geneid, geneId));
                        createProteinElement(geneProduct, entity);
                    }
                }
            }
        }
        // For small molecules
        if (smallMolecules.size() > 0) {
            Element smallMoleculesElm = new Element(BioSystemsConstants.smallmolecules);
            pathwayElm.addContent(smallMoleculesElm);
            for (GKInstance smallMolecule : smallMolecules) {
                Element sm = new Element(BioSystemsConstants.smallmolecule);
                smallMoleculesElm.addContent(sm);
                Element externalId = createElment(BioSystemsConstants.externalid,
                                                  smallMolecule.getDBID().toString());
                sm.addContent(externalId);
                String name = (String) smallMolecule.getAttributeValue(ReactomeJavaConstants.name);
                if (name != null) {
                    Element nameElm = createElment(BioSystemsConstants.name, name);
                    sm.addContent(nameElm);
                }
                String identifier = (String) smallMolecule.getAttributeValue(ReactomeJavaConstants.identifier);
                if (identifier != null) {
                    Element xref = new Element(BioSystemsConstants.xref);
                    Element sourcename = createElment(BioSystemsConstants.sourcename, "ChEBI");
                    xref.addContent(sourcename);
                    Element cid = createElment(BioSystemsConstants.externalid, 
                                               identifier);
                    xref.addContent(cid);
                    sm.addContent(xref);
                }
            }
        }
    }

    private void createProteinElement(GKInstance geneProduct, Element entity)
            throws InvalidAttributeException, Exception {
        // Create a protein
        Element protein = new Element(BioSystemsConstants.protein);
        entity.addContent(protein);
        GKInstance dbRef = (GKInstance) geneProduct.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        String dbName = dbRef.getDisplayName();
        Element xref = new Element(BioSystemsConstants.xref);
        protein.addContent(xref);
        xref.addContent(createElment(BioSystemsConstants.sourcename, dbName));
        String identifier = getProteinId(geneProduct);
        xref.addContent(createElment(BioSystemsConstants.externalid, identifier));
    }
    
    /**
     * NCBI cannot support protein isoforms. So only UniProt accession numbers are used.
     * @param refEntity
     * @return
     * @throws Exception
     */
    private String getProteinId(GKInstance refEntity) throws Exception {
        String identifier = null;
//        if (refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
//            identifier = (String) refEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
//        if (identifier == null)
            identifier = (String) refEntity.getAttributeValue(ReactomeJavaConstants.identifier);
        return identifier;
    }
    
    private Element createElment(String elmName, String text) {
        Element elm = new Element(elmName);
        elm.setText(text);
        return elm;
    }
    
    private Element createReactomeCitations() {
        Element citationsElm = new Element(BioSystemsConstants.citations);
        String[] pmids = new String[] {
                "18981052",
                "17367534",
                "15338623"
        };
        for (String pmid : pmids) {
            Element citationElm = createPMIDCitation(pmid);
            citationsElm.addContent(citationElm);
        }
        return citationsElm;
    }
    
    private Element createPMIDCitation(String pmid) {
        Element pmidElm = createElment(BioSystemsConstants.pmid, pmid);
        Element citationElm = new Element(BioSystemsConstants.citation);
        citationElm.addContent(pmidElm);
        return citationElm;
    }
    
    @Test
    public void testLoadThumnails() throws Exception {
        String fileName = "tmp/chicken_BioSys.xml";
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new File(fileName));
        Element root = document.getRootElement();
        String path = "//" + BioSystemsConstants.encodedimage;
        List list = XPath.selectNodes(root, path);
        System.out.println("List: " + list.size());
        Element elm = (Element) list.get(0);
        String text = elm.getText();
        // Convert to byte array
        byte[] in = text.getBytes();
        byte[] out = Base64.decodeBase64(in);
        ByteBuffer byteBuffer = ByteBuffer.allocate(out.length);
        byteBuffer.put(out);
        System.out.println("Out: " + out.length);
        // Output out
        FileOutputStream fos = new FileOutputStream("tmp/test.png");
        FileChannel channel = fos.getChannel();
        byteBuffer.position(0);
        channel.write(byteBuffer);
        channel.close();
        fos.close();
    }
    
    @Test
    public void checkOutput() throws Exception {
//        String fileName = "/Users/wgm/Desktop/ReactomeToBioSystems_34/Influenza A virus.xml";
//        String fileName = "/Users/wgm/Desktop/ReactomeToBioSystems_35/Influenza A virus.xml";
        String fileName = "/Users/wgm/Desktop/ReactomeToBioSystems_33/Influenza A virus.xml";
        SAXBuilder builder = new SAXBuilder(false);
        Document doc = builder.build(fileName);
        Element root = doc.getRootElement();
        List<?> list = root.getChildren(BioSystemsConstants.biosystem);
        Set<String> geneIds = new HashSet<String>();
        Set<String> entityGeneIds = new HashSet<String>();
        Set<String> uniprotIds = new HashSet<String>();
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String text = elm.getChildText(BioSystemsConstants.externalid);
            if (text.equals("REACT_6152")) {
                System.out.println(elm.getChildText(BioSystemsConstants.name));
                Element genes = elm.getChild(BioSystemsConstants.genes);
                List<?> geneList = genes.getChildren(BioSystemsConstants.gene);
                for (Iterator<?> it1 = geneList.iterator(); it1.hasNext();) {
                    Element geneElm = (Element) it1.next();
                    geneIds.add(geneElm.getChildText(BioSystemsConstants.externalid));
                    List<?> entityList = geneElm.getChildren(BioSystemsConstants.entity);
                    for (Iterator<?> it2 = entityList.iterator(); it2.hasNext();) {
                        Element entityElm = (Element) it2.next();
                        entityGeneIds.add(entityElm.getChildText(BioSystemsConstants.geneid));
//                        <entity>
//                        <protein>
//                          <xref>
//                            <sourcename>UniProt</sourcename>
//                            <externalid>P49207</externalid>
//                          </xref>
//                        </protein>
//                      </entity>
                        Element uniProtElm = (Element) XPath.selectSingleNode(entityElm, "protein/xref/externalid");
                        uniprotIds.add(uniProtElm.getTextTrim());
                    }
                }
                break;
            }
        }
        System.out.println("FileName: " + fileName);
        System.out.println("Total gene ids: " + geneIds.size());
        System.out.println("Total entity gene ids: " + entityGeneIds.size());
        System.out.println("Total UniProt ids: " + uniprotIds.size());
    }
    
    @Test
    public void testOtherIdentifiers() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver32",
                                            "root",
                                            "macmysql01");
        Collection collection = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        dba.loadInstanceAttributeValues(collection, new String[]{ReactomeJavaConstants.otherIdentifier});
        Set<String> dbNames = new HashSet<String>();
        int index = 0;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List otherIds = inst.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier);
            if (otherIds == null)
                continue;
            for (Iterator it1 = otherIds.iterator(); it1.hasNext();) {
                String id = (String) it1.next();
                index = id.indexOf(":");
                if (index > 0)
                    dbNames.add(id.substring(0, index));
            }
        }
        index = 1;
        for (String dbName : dbNames) {
            System.out.println(index + ": " + dbName);
            index ++;
        }
    }
    
    public void setPathwayUrl(String url) {
        this.pathwayUrl = url;
    }
    
    public void setSourceUrl(String url) {
        this.sourceUrl = url;
    }
    
    /**
     * This method is used to convert pathways in all species. Top pathways are based on FrontPageItems
     * and their orthologousEvents.
     * @param dirName
     * @throws IOException
     */
    public void convertPathwaysInAllSpecies(String dirName) throws Exception {
        // Make sure dir is there
        File dir = new File(dirName);
        if (!dir.exists())
            dir.mkdirs();
        FrontPageItemHelper helper = new FrontPageItemHelper();
        Map<GKInstance, List<GKInstance>> speciesToItems = helper.generateSpeciesToFrontPageItemMap(dbAdaptor);
        setNeedHierarchy(true);
        String stableUrl = pathwayUrl;
        String dbUrl = optionalPathwayUrl;
        for (GKInstance species : speciesToItems.keySet()) {
//            System.out.println("Species: " + species.getDisplayName());
            GKInstance crossReference = (GKInstance) species.getAttributeValue(ReactomeJavaConstants.crossReference);
            if (crossReference == null) {
                System.err.println(species + " has no crossReference value. No pathways will be exported for it!");
                // There is a bug regarding this species. Do a manual fix
                if (species.getDisplayName().equals("Staphylococcus aureus N315")) {
                    fixCrossReferenceForSpecies(species, "158879");
                }
                else if (species.getDisplayName().equals("Danio rerio")) {
                    fixCrossReferenceForSpecies(species, "7955");
                }
                else
                    continue;
            }
            // Escape the chicken pathways. These pathways will be handled differently.
            if (species.getDBID().equals(49591L)) // || // Chicken DB_ID
//                species.getDBID().equals(1265767L) ||
//                species.getDBID().equals(1141091L) ||
//                species.getDBID().equals(1097953L) ||
//                species.getDBID().equals(1117238L)) // Staphylococcus aureus N315: there is a bug in the database
                continue; 
            // Only human has stable ids.
            if (species.getDisplayName().equals("Homo sapiens"))
                setPathwayUrl(stableUrl);
            else
                setPathwayUrl(dbUrl);
            List<GKInstance> list = speciesToItems.get(species);
            Document doc = convertPathways(list);
            // Output
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            String fileName = species.getDisplayName();
            fileName = fileName.replaceAll("/", "_");
            File file = new File(dir, fileName + ".xml");
            FileWriter fileWriter = new FileWriter(file);
            outputter.output(doc, fileWriter);
        }
    }

    private void fixCrossReferenceForSpecies(GKInstance species,
                                             String id)
            throws InvalidAttributeException, InvalidAttributeValueException {
        GKInstance crossReference;
        crossReference = new GKInstance();
        SchemaClass cls = species.getDbAdaptor().getSchema().getClassByName(ReactomeJavaConstants.DatabaseIdentifier);
        crossReference.setSchemaClass(cls);
        crossReference.setDbAdaptor(species.getDbAdaptor());
        crossReference.setAttributeValue(ReactomeJavaConstants.identifier,
                                         id);
        species.setAttributeValue(ReactomeJavaConstants.crossReference, crossReference);
    }
    
    public static void main(String[] args) {
        // Need the following arguments in order: dbHost, dbName, dbUser, dbPwd, dbPort, fileForPathwayList, fileForXMLOutput, pathwayURL
        if (args.length < 6) {
            System.err.println("Usage: java -Xmx1024m -Dfile.encoding=UTF-8 " +
            		"org.gk.biosystems.ReactomeToBioSystemsConverters " +
            		"dbHost " +
            		"dbName " +
            		"dbUser " +
            		"dbPwd " +
            		"dbPort " +
            		"fileOrDirNameForXMLOutput " +
            		"{fileForPathwayList} " + 
            		"{pathwayURL} " + 
            		"{sourceURL}. " +
            		"Note: if fileForPathwayList is provided, file name should be used for XML output (argument 6). " +
            		"If pathways in all species should be dumped, just provide a dir name for output, and no more arguments should be provided.");
            return;
        }
        try {
            ReactomeToBioSystemsConverter converter = new ReactomeToBioSystemsConverter();
            converter.setMySQLAdaptor(args[0],
                                      args[1],
                                      args[2],
                                      args[3],
                                      Integer.parseInt(args[4]));
//            // Check species
//            Collection<?> species = converter.getMySQLAdaptor().fetchInstancesByClass(ReactomeJavaConstants.Species);
//            for (Iterator<?> it = species.iterator(); it.hasNext();) {
//                GKInstance tmp = (GKInstance) it.next();
//                GKInstance crossReference = (GKInstance) tmp.getAttributeValue(ReactomeJavaConstants.crossReference);
//                if (crossReference == null) {
//                    System.out.println(tmp + " has no cross reference!");
//                }
//            }
//            if (true)
//                return;
            if (args.length == 6) {
                // Output for all species
                converter.convertPathwaysInAllSpecies(args[5]);
                zipAllFiles(args[5]);
            }
            else {
                // Used as working directory
                File file = new File(args[5]);
                converter.setTargetDir(file.getParentFile());
                String fileName = args[6];
                List<GKInstance> pathways = null;
                if (fileName.equals("true"))
                    pathways = converter.loadFrontPageItems(converter.getMySQLAdaptor());
                else
                    pathways = converter.loadPathways(fileName, 
                                                      converter.getMySQLAdaptor());
                // args[6] can be true to use front page item for human pathways.
                if (args[6].equals("true"))
                    converter.setNeedHierarchy(true);
                // Check if a pathway url is provided, which can be used for pathways don't have DB_IDs
                if (args.length >= 8) {
                    String pathwayUrl = args[7];
                    converter.setPathwayUrl(pathwayUrl);
                }
                if (args.length >= 9) 
                    converter.setSourceUrl(args[8]);
                Document doc = converter.convertPathways(pathways);
                // Output
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                FileWriter fileWriter = new FileWriter(file);
                outputter.output(doc, fileWriter);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Zip all output into one file.
     * @param dirName
     * @throws IOException
     */
    private static void zipAllFiles(String dirName) throws IOException {
        File dir = new File(dirName);
        File dest = new File(dir, "ReactomeToBioSystems.zip");
        GKApplicationUtilities.zipDir(dir, dest, ".xml", true);
    }
}
