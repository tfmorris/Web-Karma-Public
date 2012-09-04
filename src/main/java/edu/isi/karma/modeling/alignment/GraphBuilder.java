/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/
package edu.isi.karma.modeling.alignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.semantictypes.SemanticType;

public class GraphBuilder {

	static Logger logger = Logger.getLogger(GraphBuilder.class);
	

	private List<SemanticType> semanticTypes;
	private List<Vertex> semanticNodes;
	private DirectedWeightedMultigraph<Vertex, LabeledWeightedEdge> graph;
	private OntologyManager ontologyManager;
	private boolean separateDomainInstancesForSameDataProperties;
	
	private HashMap<String, Integer> nodesLabelCounter;
	private HashMap<String, Integer> linksLabelCounter;
	
	private static String THING_URI = "http://www.w3.org/2002/07/owl#Thing";
	private static String THING_NS = "http://www.w3.org/2002/07/owl#";
	private static String THING_PREFIX = "owl";
	
	private static String SUBCLASS_URI = "hasSubClass";
	private static String SUBCLASS_NS = "http://example.com#";
	private static String SUBCLASS_PREFIX = "";

	public static double DEFAULT_WEIGHT = 1.0;	
	public static double MIN_WEIGHT = 0.000001; // need to be fixed later	
	public static double MAX_WEIGHT = 1000000;
	
	public GraphBuilder(OntologyManager ontologyManager, List<SemanticType> semanticTypes, boolean separateDomainInstancesForSameDataProperties) {
		this.ontologyManager = ontologyManager;
		this.separateDomainInstancesForSameDataProperties = separateDomainInstancesForSameDataProperties;
		
		nodesLabelCounter = new HashMap<String, Integer>();
		linksLabelCounter = new HashMap<String, Integer>();

		long start = System.currentTimeMillis();
		
		this.semanticTypes = semanticTypes;
		graph = new DirectedWeightedMultigraph<Vertex, LabeledWeightedEdge>(LabeledWeightedEdge.class);
		semanticNodes = new ArrayList<Vertex>();
			
		buildInitialGraph();

		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis/1000F;

		logger.info("total number of nodes in the graph: " + this.graph.vertexSet().size());
		logger.info("total number of links in the graph: " + this.graph.edgeSet().size());
		logger.info("total time to build the graph: " + elapsedTimeSec);
	}

	
	private String createNodeID(String label) {
		
		int index;
		String id;
		
		if (nodesLabelCounter.containsKey(label)) {
			index = nodesLabelCounter.get(label).intValue();
			nodesLabelCounter.put(label, ++index);
			id = label + "" + index;
		} else {
			index = 1;
			nodesLabelCounter.put(label, index);
//			id = label + "" + index;
			id = label;
		}
		return id;
	}
	
	private String getLastID(String label) {
		
		int index;
		
		if (nodesLabelCounter.containsKey(label)) {
			index = nodesLabelCounter.get(label).intValue();
			if (index == 1){
				return label;
			}else{
				return (label + "" + index);
			}
		}else{
			return null;
		}
	}
	
	private String createLinkID(String label) {

		String id;
		int index;
		
		if (linksLabelCounter.containsKey(label)) {
			index = linksLabelCounter.get(label).intValue();
			linksLabelCounter.put(label, ++index);
			id = label + "" + index;
		} else {
			index = 1;
			linksLabelCounter.put(label, index);
//			id = label + "" + index;
			id = label;
		}
		return id;
	}
	
	private void addSemanticTypesToGraph() {
		
		logger.debug("<enter");
		String id;
		SemanticType semanticType;
		NodeType nodeType;
		String label;
		
		for (int i = 0; i < this.semanticTypes.size(); i++) {
			
			semanticType =semanticTypes.get(i); 
			label = semanticType.getType().getUriString();
			id = createNodeID(label);
			
			if (ontologyManager.isClass(label)){
				nodeType = NodeType.Class;
			}else if (ontologyManager.isDataProperty(label)){
				nodeType = NodeType.DataProperty;
			}else{
				nodeType = null;
			}
			
			if (nodeType == null) {
				logger.debug("could not find type of " + label + " in the ontology.");
				continue;
			}
			
			Vertex v = new Vertex(id, semanticType.getType(), semanticType, nodeType);
			semanticNodes.add(v);
			graph.addVertex(v);
		}


		// Add Thing to Graph if it is not added before.
		// Preventing from have an unconnected graph
		if (!nodesLabelCounter.containsKey(THING_URI)) {
			Vertex v = new Vertex(createNodeID(THING_URI), new URI(THING_URI, THING_NS, THING_PREFIX), NodeType.Class);			
			this.graph.addVertex(v);
		}
		
		logger.debug("exit>");
	}
	
	private void addDomainsOfDataPropertyNodesToGraph() {
		
		logger.debug("<enter");
		String id;
		
		List<String> visitedDataProperties = new ArrayList<String>();
		Vertex[] vertexList = this.graph.vertexSet().toArray(new Vertex[0]);
		for (Vertex v : vertexList) {
			
			if (v.getNodeType() != NodeType.DataProperty){
				continue;
			}
			
			if (v.getSemanticType() == null){
				continue;
			}

			URI domainURI = v.getSemanticType().getDomain();
			if (domainURI == null || domainURI.getUriString() == null || domainURI.getUriString().trim().length() == 0){
				continue;
			}
			
			String domainClass = v.getSemanticType().getDomain().getUriString();
		
			if (!ontologyManager.isClass(domainClass)){
				return;
			}
			
			if (!separateDomainInstancesForSameDataProperties) {
				if (nodesLabelCounter.get(domainClass) == null) {
					id = createNodeID(domainClass);
					Vertex domain = new Vertex(id, domainURI, NodeType.Class);
					graph.addVertex(domain);
					v.setDomainVertexId(domain.getID());
				}else{
					v.setDomainVertexId(getLastID(domainClass));
				}
			} else {
				if (visitedDataProperties.indexOf(domainClass + v.getUriString()) != -1 || nodesLabelCounter.get(domainClass) == null) {
					id = createNodeID(domainClass);
					Vertex domain = new Vertex(id, domainURI, NodeType.Class);
					graph.addVertex(domain);
					v.setDomainVertexId(domain.getID());
				}else{
					v.setDomainVertexId(getLastID(domainClass));
				}
				visitedDataProperties.add(domainClass + v.getUriString());
			}
		}
		
		logger.debug("exit>");
	}
	
	private void addNodesClosure() {
		
		logger.debug("<enter");

		String label;
		List<Vertex> recentlyAddedNodes = new ArrayList<Vertex>(graph.vertexSet());
		List<Vertex> newNodes;
		List<String> dpDomainClasses = new ArrayList<String>();
		List<String> opDomainClasses = new ArrayList<String>();
		List<String> superClasses = new ArrayList<String>();
		List<String> newAddedClasses = new ArrayList<String>();

		// We don't need to add subclasses of each class separately.
		// The only place in which we add children is where we are looking for domain class of a property.
		// In this case, in addition to the domain class, we add all of its children too.
		
		List<String> processedLabels = new ArrayList<String>();
		while (recentlyAddedNodes.size() > 0) {
			
			newNodes = new ArrayList<Vertex>();
			for (int i = 0; i < recentlyAddedNodes.size(); i++) {
				
				label = recentlyAddedNodes.get(i).getUriString();
				if (processedLabels.indexOf(label) != -1){
					continue;
				}
				
				processedLabels.add(label);
				
				if (recentlyAddedNodes.get(i).getNodeType() == NodeType.Class) {
					opDomainClasses = ontologyManager.getDomainsGivenRange(label, true);
					superClasses = ontologyManager.getSuperClasses(label, false);
				} else if (recentlyAddedNodes.get(i).getNodeType() == NodeType.DataProperty) {
					dpDomainClasses = ontologyManager.getDomainsGivenProperty(label, true);
				}
				
				if (opDomainClasses != null){
					newAddedClasses.addAll(opDomainClasses);
				}
				if (dpDomainClasses != null){
					newAddedClasses.addAll(dpDomainClasses);
				}
				if (superClasses != null){
					newAddedClasses.addAll(superClasses);
				}
				
				for (int j = 0; j < newAddedClasses.size(); j++) {
					if (!nodesLabelCounter.containsKey(newAddedClasses.get(j))) { // if node is not in graph yet
						label = newAddedClasses.get(j);
						Vertex v = new Vertex(createNodeID(label), ontologyManager.getURIFromString(newAddedClasses.get(j)), NodeType.Class);
						newNodes.add(v);
						this.graph.addVertex(v);
					}
				}
			}
			
			recentlyAddedNodes = newNodes;
			newAddedClasses.clear();
		}

		logger.debug("exit>");
	}
	
//	private void addUnaddedDomainsToGraph() {
//		
//		logger.debug("<enter");
//		String id;
//		
//		for (int i = 0; i < this.semanticTypes.size(); i++) {
//			
//			if (!ontologyManager.isDataProperty(semanticTypes.get(i).getType().trim()))
//				continue;
//
//			String domainClass = semanticTypes.get(i).getDomain().trim();
//			if (domainClass == null || domainClass.trim().length() == 0)
//				continue;
//		
//			if (!ontologyManager.isClass(domainClass))
//				return;
//			
//			if (nodesLabelCounter.get(domainClass) == null) {
//				id = createNodeID(domainClass);
//				Vertex v = new Vertex(id, ontologyManager.getNameFromURI(domainClass), NodeType.Class);
//				semanticNodes.add(v);
//				graph.addVertex(v);
//				
//			}
//		}
//		
//		logger.debug("exit>");
//	}
	

	private void addLinks() {
		
		logger.debug("<enter");

		Vertex[] vertices = this.graph.vertexSet().toArray(new Vertex[0]);
		List<String> objectProperties = new ArrayList<String>();
		//List<String> dataProperties = new ArrayList<String>();
		
		Vertex source;
		Vertex target;
		String sourceLabel;
		String targetLabel;
		
		String id;
		String label;
		
		for (int i = 0; i < vertices.length; i++) {
			for (int j = 0; j < vertices.length; j++) {
				
				if (j == i){
					continue;
				}
				
				source = vertices[i];
				target = vertices[j];
				sourceLabel = source.getUriString();
				targetLabel = target.getUriString();

				// There is no outgoing link from DataProperty nodes
				if (source.getNodeType() == NodeType.DataProperty){
					break;
				}
				
				// create a link from the domain and all its subclasses of this DataProperty to range
				if (target.getNodeType() == NodeType.DataProperty) {
					
					String domain = "";
					if (target.getSemanticType() != null && 
							target.getSemanticType().getDomain() != null){
						domain = target.getSemanticType().getDomain().getUriString();
					}
					
					if (domain != null && domain.trim().equalsIgnoreCase(sourceLabel.trim()))
					
					//dataProperties = ontologyManager.getDataProperties(sourceLabel, targetLabel, true);
					//for (int k = 0; k < dataProperties.size(); k++) 
					
					{
						// label of the data property nodes is equal to name of the data properties
						label = targetLabel; // dataProperties.get(k);
						id = createLinkID(label);
						LabeledWeightedEdge e = new LabeledWeightedEdge(id, ontologyManager.getURIFromString(label), LinkType.DataProperty);
						this.graph.addEdge(source, target, e);
						this.graph.setEdgeWeight(e, DEFAULT_WEIGHT);

					}
				}

				boolean inherited = true;
				// create a link from the domain and all its subclasses of ObjectProperties to range and all its subclasses
				if (target.getNodeType() == NodeType.Class) {
					objectProperties = ontologyManager.getObjectProperties(sourceLabel, targetLabel, true);
					
					for (int k = 0; k < objectProperties.size(); k++) {
						label = objectProperties.get(k);
						
						List<String> dirDomains = ontologyManager.getOntCache().getPropertyDirectDomains().get(label);
						List<String> dirRanges = ontologyManager.getOntCache().getPropertyDirectRanges().get(label);
				
						if (dirDomains != null && dirDomains.indexOf(sourceLabel) != -1 &&
								dirRanges != null && dirRanges.indexOf(targetLabel) != -1){
							inherited = false;
						}
						
						id = createLinkID(label);
						LabeledWeightedEdge e = new LabeledWeightedEdge(id, ontologyManager.getURIFromString(label), LinkType.ObjectProperty);
						this.graph.addEdge(source, target, e);
						
						// prefer the links which are actually defined between source and target in ontology over inherited ones.
						if (inherited){
							this.graph.setEdgeWeight(e, DEFAULT_WEIGHT + MIN_WEIGHT);
						}else{
							this.graph.setEdgeWeight(e, DEFAULT_WEIGHT);
						}
					}
				}
				
				if (target.getNodeType() == NodeType.Class) {
					// we have to check both sides.
					if (ontologyManager.isSubClass(targetLabel, sourceLabel, false) ||
							ontologyManager.isSuperClass(sourceLabel, targetLabel, false)) {
						id = createLinkID(SUBCLASS_URI);
						LabeledWeightedEdge e = new LabeledWeightedEdge(id, 
								new URI(SUBCLASS_URI, SUBCLASS_NS, SUBCLASS_PREFIX), 
								LinkType.HasSubClass);
						this.graph.addEdge(source, target, e);
						this.graph.setEdgeWeight(e, MAX_WEIGHT);					
					}
				}
				
			}
		}
		
//		logger.info("number of links added to graph: " + this.graph.edgeSet().size());
		logger.debug("exit>");
	}
	
	private void addLinksFromThing() {
		
		logger.debug("<enter");

		Vertex[] vertices = this.graph.vertexSet().toArray(new Vertex[0]);
		
		Vertex source;
		Vertex target;
		String sourceLabel;
		String targetLabel;
		
		String id;

		for (int i = 0; i < vertices.length; i++) {
			for (int j = 0; j < vertices.length; j++) {
				
				if (j == i){
					continue;
				}
				
				source = vertices[i];
				target = vertices[j];
				sourceLabel = source.getUriString();
				targetLabel = target.getUriString();
				
				// There is no outgoing link from DataProperty nodes
				if (source.getNodeType() != NodeType.Class){
					break;
				}
				
				if (target.getNodeType() != NodeType.Class){
					continue;
				}
				
				if (!sourceLabel.equalsIgnoreCase(THING_URI)){
					continue;
				}
				
				if (ontologyManager.getSuperClasses(targetLabel, false).size() != 0){
					continue;
				}
				
				// create a link from all Thing nodes to nodes who don't have any superclasses
				if (target.getNodeType() == NodeType.Class) {
					id = createLinkID(SUBCLASS_URI);
					LabeledWeightedEdge e = new LabeledWeightedEdge(id, 
							new URI(SUBCLASS_URI, SUBCLASS_NS, SUBCLASS_PREFIX), 
							LinkType.HasSubClass);
					this.graph.addEdge(source, target, e);
					this.graph.setEdgeWeight(e, MAX_WEIGHT);					
				}
			}
		}
		
		logger.debug("exit>");

	}
	
	public Vertex copyNode(Vertex node) {
		
		if (node.getNodeType() != NodeType.Class) {
			logger.debug("nodes other than type of Class cannot be duplicated.");
			return null;
		}
		
		String id;
		String label;
		
		label = node.getUriString();
		id = createNodeID(label);
		
		Vertex newNode = new Vertex(id, ontologyManager.getURIFromString(label), node.getNodeType());
		
		this.graph.addVertex(newNode);
	
		return newNode;
	}
	
	public void copyLinks(Vertex source, Vertex target) {
		
		LabeledWeightedEdge[] outgoing =  graph.outgoingEdgesOf(source).toArray(new LabeledWeightedEdge[0]);
		LabeledWeightedEdge[] incoming = graph.incomingEdgesOf(source).toArray(new LabeledWeightedEdge[0]);
		
		String id;
		String label;
		
		Vertex s, t;
		
		if (outgoing != null){
			for (int i = 0; i < outgoing.length; i++) {
				label = outgoing[i].getUriString();
				id = createLinkID(label);
				LabeledWeightedEdge e = new LabeledWeightedEdge(id, 
						new URI(outgoing[i].getUriString(), 
								outgoing[i].getNs(),
								outgoing[i].getPrefix()), outgoing[i].getLinkType());
				s = target;
				t = outgoing[i].getTarget();
				this.graph.addEdge(s, t, e);
				this.graph.setEdgeWeight(e, outgoing[i].getWeight());
			}
		}
		
		if (incoming != null){
			for (int i = 0; i < incoming.length; i++) {
				label = incoming[i].getUriString();
				id = createLinkID(label);
				LabeledWeightedEdge e = new LabeledWeightedEdge(id, 
						new URI(incoming[i].getUriString(), 
								incoming[i].getNs(),
								incoming[i].getPrefix()), incoming[i].getLinkType());
				s = incoming[i].getSource();
				t = target;
				this.graph.addEdge(s, t, e);
				this.graph.setEdgeWeight(e, incoming[i].getWeight());
			}
		}
		
		if (source.getNodeType() != NodeType.Class || target.getNodeType() != NodeType.Class){
			return;
		}

		// interlinks from source to target
		s = source; t= target;
		List<String> objectProperties = ontologyManager.getObjectProperties(s.getUriString(), t.getUriString(), true);
			
		for (int k = 0; k < objectProperties.size(); k++) {
			label = objectProperties.get(k);
			id = createLinkID(label);
			LabeledWeightedEdge e = new LabeledWeightedEdge(id, ontologyManager.getURIFromString(label), LinkType.ObjectProperty);
			this.graph.addEdge(s, t, e);
			this.graph.setEdgeWeight(e, DEFAULT_WEIGHT);
		}

		// interlinks from target to source
		s = target; t= source;
		objectProperties = ontologyManager.getObjectProperties(s.getUriString(), t.getUriString(), true);
			
		for (int k = 0; k < objectProperties.size(); k++) {
			label = objectProperties.get(k);
			id = createLinkID(label);
			LabeledWeightedEdge e = new LabeledWeightedEdge(id, ontologyManager.getURIFromString(label), LinkType.ObjectProperty);
			this.graph.addEdge(s, t, e);
			this.graph.setEdgeWeight(e, DEFAULT_WEIGHT);
		}

	}
	
	private void buildInitialGraph() {

		if (this.semanticTypes == null) {
			logger.debug("semantic types list is null.");
			return;
		}

		long start = System.currentTimeMillis();
		float elapsedTimeSec;
		
		addSemanticTypesToGraph();
		long addSemanticTypes = System.currentTimeMillis();
		elapsedTimeSec = (addSemanticTypes - start)/1000F;
		logger.info("number of initial nodes: " + this.graph.vertexSet().size());
		logger.info("time to add initial semantic types: " + elapsedTimeSec);

		addDomainsOfDataPropertyNodesToGraph();
		long addDomainsOfDataPropertyNodes = System.currentTimeMillis();
		elapsedTimeSec = (addDomainsOfDataPropertyNodes - addSemanticTypes)/1000F;
		logger.info("time to add domain of data property nodes to graph: " + elapsedTimeSec);

		addNodesClosure();
		long addNodesClosure = System.currentTimeMillis();
		elapsedTimeSec = (addNodesClosure - addDomainsOfDataPropertyNodes)/1000F;
		logger.info("time to add nodes closure: " + elapsedTimeSec);
		
//		addUnaddedDomainsToGraph();
		addLinks();
		long addLinks = System.currentTimeMillis();
		elapsedTimeSec = (addLinks - addNodesClosure)/1000F;
		logger.info("time to add links to graph: " + elapsedTimeSec);

		addLinksFromThing();
		long addLinksFromThing = System.currentTimeMillis();
		elapsedTimeSec = (addLinksFromThing - addLinks)/1000F;
//		logger.info("time to add links from Thing (root): " + elapsedTimeSec);

	}

	public DirectedWeightedMultigraph<Vertex, LabeledWeightedEdge> getGraph() {
		return this.graph;
	}
	
	public List<Vertex> getSemanticNodes() {
		return this.semanticNodes;
	}

}
