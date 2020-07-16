package org.gk.HyperGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Test;

import edu.reed.HyperGraph;
import edu.reed.HyperNode;


/**
 * (1) Get the diagram from invoking caller.
 * (2) Read in pathway from diagram reader.
 * (3) Iterate over all rendered components.
 * (4) If component is a Reactome HyperEdge, convert to their HyperEdge.
 *      (*) By converting inputs, catalysts, inhibitors, activators to 'tail'.
 *      (*) By converting output to 'head'.
 * (5) Add edges to HyperGraph.
 */
public class HyperGraphConverter {
    private Map<Long, HyperNode> hyperNodes;

    public HyperGraphConverter() {
        hyperNodes = new HashMap<Long, HyperNode>();
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
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(diagram);
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return null;

        edu.reed.HyperEdge edge = null;
        HyperNode node = null;
        HyperGraph graph = new HyperGraph();

        for (Renderable component : components) {
            // Currently, the only nodes included in the HyperGraph are those that are connected to an edge.
            if (component instanceof org.gk.render.HyperEdge) {
                edge = createHyperEdge((org.gk.render.HyperEdge) component);
                if (edge != null)
                    graph.edges.add(edge);
            }
        }

        return graph;
    }

    /**
     * Convert a Reaction Like Event to a HyperEdge.
     *
     * @param edge
     * @return HyperEdie
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private edu.reed.HyperEdge createHyperEdge(org.gk.render.HyperEdge edge) throws InvalidAttributeException, Exception {
        if (edge == null)
            return null;

        // Handle conversions to Head.
        List<Node> outputs = edge.getOutputNodes();
        HashSet<HyperNode> head = createHyperNodeSet(outputs);

        // Handle conversions to Tail.
        List<Node> inputs = edge.getOutputNodes();
        List<Node> activators = edge.getActivatorNodes();
        List<Node> catalysts= edge.getHelperNodes();
        List<Node> inhibitors= edge.getInhibitorNodes();
        HashSet<HyperNode> tail = createHyperNodeSet(inputs);
        tail.addAll(createHyperNodeSet(activators));
        tail.addAll(createHyperNodeSet(catalysts));
        tail.addAll(createHyperNodeSet(inhibitors));

        // Handle conversions to HyperEdge.
        edu.reed.HyperEdge hyperEdge = new edu.reed.HyperEdge();
        hyperEdge.setHead(head);
        hyperEdge.setTail(tail);

        return hyperEdge;
    }

    /**
     * Convert a list of Nodes to a set of HyperNodes.
     *
     * @param rle
     * @param attName
     * @return HashSet
     * @throws InvalidAttributeException
     * @throws Exception
     */
    HashSet<HyperNode> createHyperNodeSet(List<Node> nodes) throws InvalidAttributeException, Exception {
        HyperNode hyperNode = null;
        GKInstance instance = null;
        HashSet<HyperNode> set = new HashSet<HyperNode>();

        for (Node node : nodes) {
            // Check if a new node needs to be created.
            instance = node.getInstance();
            if (instance == null)
                continue;

            Long dbid = instance.getDBID();
            if (hyperNodes.containsKey(dbid))
                hyperNode = hyperNodes.get(dbid);

            else {
                hyperNode = createHyperNode(node);
                hyperNodes.put(dbid, hyperNode);
            }

            set.add(hyperNode);
        }

        return set;
    }

    /**
     * Convert a Node to a HyperNode.
     *
     * @param node
     * @return HyperNode
     * @throws Exception
     * @throws InvalidAttributeException
     */
    private HyperNode createHyperNode(Node node) throws InvalidAttributeException, Exception {
        if (node == null)
            return null;

        GKInstance instance = node.getInstance();
        if (instance == null)
            return null;

        Map<String, String> attributes = new HashMap<String, String>();
        SchemaClass cls = instance.getSchemClass();

	    for (Iterator<?> it = cls.getAttributes().iterator(); it.hasNext();) {
	        SchemaAttribute att = (SchemaAttribute) it.next();
	        String attName = att.getName();
	        Object attValue = instance.getAttributeValue(attName);

	        if (attValue == null)
	            continue;

	        attributes.put(attName, attValue.toString());
	    }

        HyperNode hyperNode = new HyperNode(instance.getDBID().toString());
	    hyperNode.setAttributes(attributes);
        return hyperNode;
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
        HyperGraph graph = createGraph(diagram);
        System.out.println("Edges:");
        graph.printEdges();
        System.out.println("Nodes:");
        graph.printNodes();
    }

}
