package org.gk.HyperGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Test;

import edu.reed.HyperEdge;
import edu.reed.HyperGraph;
import edu.reed.HyperNode;


public class Converter {

    public Converter() {
    }

    /**
     * Convert a Pathway Diagram to a HyperGraph.
     *
     * @param diagram
     * @return HyperGraph
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public HyperGraph createGraph(GKInstance diagram) throws InvalidAttributeException, Exception {
        GKInstance pathway = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        List<GKInstance> events = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);

        if (events == null || events.size() == 0)
            return null;

        HyperEdge edge = null;
        HyperNode node = null;
        HyperGraph graph = new HyperGraph();

        for (GKInstance event : events) {
            if (event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                edge = createEdge(event);
                graph.edges.add(edge);
            }
            else if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                node = createNode(event);
                graph.nodes.add(node);
            }
        }

        return graph;
    }

    /**
     * Convert a Reaction Like Event to a HyperEdge.
     *
     * @param rle
     * @return HyperEdie
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private HyperEdge createEdge(GKInstance rle) throws InvalidAttributeException, Exception {
        if (rle == null)
            return null;

        // Handle conversions to Head.
        HashSet<HyperNode> head = createNodeSet(rle, ReactomeJavaConstants.input);

        // Handle conversions to Tail.
        HashSet<HyperNode> tail = createNodeSet(rle, ReactomeJavaConstants.input);

        // Handle conversions to HyperEdge.
        HyperEdge edge = new HyperEdge();
        edge.setHead(head);
        edge.setTail(tail);

        return edge;
    }

    /**
     * Add all instances of a given RLE's attribute list to a HyperNode set (e.g. input and output).
     *
     * @param rle
     * @param attName
     * @return HashSet
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private HashSet<HyperNode> createNodeSet(GKInstance rle, String attName) throws InvalidAttributeException, Exception {
        HashSet<HyperNode> nodes = new HashSet<HyperNode>();
        List<GKInstance> instances = rle.getAttributeValuesList(attName);
        for (GKInstance instance : instances) {
            HyperNode node = createNode(instance);
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Convert a GKInstance to a HyperNode.
     *
     * @param instance
     * @return HyperNode
     * @throws Exception
     * @throws InvalidAttributeException
     */
    private HyperNode createNode(GKInstance instance) throws InvalidAttributeException, Exception {
        if (instance == null)
            return null;

        HyperNode node = new HyperNode(instance.getDBID().toString());
        SchemaClass cls = instance.getSchemClass();
        Map<String, String> attributes = new HashMap<String, String>();

	    for (Iterator<?> it = cls.getAttributes().iterator(); it.hasNext();) {
	        SchemaAttribute att = (SchemaAttribute) it.next();
	        String attName = att.getName();
	        Object attValue = instance.getAttributeValue(attName);

	        if (attValue == null)
	            continue;

	        attributes.put(attName, attValue.toString());
	    }

	    node.setAttributes(attributes);
        return node;
    }

    @Test
    public void testConvertDiagram() throws Exception {
        // Simple test pathway.
        // Name: CYP3A43 6b-hydroxylates TEST
        // Id: R-HSA-211959.1
        // Url: https://reactome.org/PathwayBrowser/#/R-HSA-211945&SEL=R-HSA-211959&PATH=R-HSA-1430728,R-HSA-211859
        Long diagramDbId = 9676704L;
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "central",
                                            "liam",
                                            ")8J7m]!%[<");
        GKInstance diagram = dba.fetchInstance(diagramDbId);
        HyperGraph convertedDiagram = createGraph(diagram);
    }

}
