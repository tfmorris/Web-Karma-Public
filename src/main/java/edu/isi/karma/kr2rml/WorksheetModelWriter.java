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

package edu.isi.karma.kr2rml;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.modeling.Namespaces;
import edu.isi.karma.modeling.Prefixes;
import edu.isi.karma.modeling.Uris;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.HNode;
import edu.isi.karma.rep.RepFactory;

public class WorksheetModelWriter {
	private PrintWriter writer;
	private RepFactory factory;
	private OntologyManager ontMgr;
	private Repository myRepository;
	
	// Internal instance variables
	private RepositoryConnection con;
	private ValueFactory f;
	// Add a blank node of R2RML mapping
	private Resource mappingRes;
	private static Logger logger = LoggerFactory
			.getLogger(WorksheetModelWriter.class);
	
	public WorksheetModelWriter(PrintWriter writer, RepFactory factory, OntologyManager ontMgr, 
			String worksheetName)
			throws RepositoryException {
		this.writer = writer;
		this.factory = factory;
		this.ontMgr = ontMgr;
		
		/** Initialize an in-memory sesame triple store **/
		myRepository = new SailRepository(new MemoryStore());
		myRepository.initialize();
		con = myRepository.getConnection();
		f = myRepository.getValueFactory();
		
		/** Create resource for the mapping as a blank node **/
		URI r2rmlMapUri = f.createURI(Uris.KM_R2RML_MAPPING_URI);
		URI sourceNameUri = f.createURI(Uris.KM_SOURCE_NAME_URI);
		mappingRes = f.createBNode();
		con.add(mappingRes, RDF.TYPE, r2rmlMapUri);
		Value srcNameVal = f.createLiteral(worksheetName);
		con.add(mappingRes, sourceNameUri, srcNameVal);
	}
	
	public boolean writeR2RMLMapping(OntologyManager ontManager, KR2RMLMappingGenerator mappingGen)
			throws RepositoryException, JSONException {
		/** Get the required data structures of R2RML **/
		R2RMLMapping mapping = mappingGen.getR2RMLMapping();
		KR2RMLMappingAuxillaryInformation auxInfo = mappingGen.getMappingAuxillaryInformation();
		List<TriplesMap> triplesMapList = mapping.getTriplesMapList();
		
		try {
			
			URI trTypeUri = f.createURI(Uris.RR_TRIPLESMAP_CLASS_URI);
			URI templateUri = f.createURI(Uris.RR_TEMPLATE_URI);
			URI subjMapUri = f.createURI(Uris.RR_SUBJECTMAP_URI);
			URI predUri = f.createURI(Uris.RR_PREDICATE_URI);
			URI objectMapUri = f.createURI(Uris.RR_OBJECTMAP_URI);
			URI columnUri = f.createURI(Uris.RR_COLUMN_URI);
			URI rfObjClassUri = f.createURI(Uris.RR_REF_OBJECT_MAP_URI);
			URI parentTriplesMapUri = f.createURI(Uris.RR_PARENT_TRIPLE_MAP_URI);
			URI predObjMapMapUri = f.createURI(Uris.RR_PRED_OBJ_MAP_URI);
			URI blankNodeUri = f.createURI(Uris.RR_BLANK_NODE_URI);
			URI termTypeUri = f.createURI(Uris.RR_TERM_TYPE_URI);
			
			URI coversColUri = f.createURI(Uris.KM_BLANK_NODE_COVERS_COLUMN_URI);
			URI bnNamePrefixUri = f.createURI(Uris.KM_BLANK_NODE_PREFIX_URI);
			URI nodeIdUri = f.createURI(Uris.KM_NODE_ID_URI);
			URI steinerTreeRootNodeUri = f.createURI(Uris.KM_STEINER_TREE_ROOT_NODE);
			URI hasTrMapUri = f.createURI(Uris.KM_HAS_TRIPLES_MAP_URI);
			
			
			/** Add all the triple maps **/
			for (TriplesMap trMap:triplesMapList) {
				URI trMapUri = f.createURI(Namespaces.KARMA_DEV + trMap.getId());
				// Add the triples map type statement
				con.add(trMapUri, RDF.TYPE, trTypeUri);
				// Associate it with the source mapping URI
				con.add(mappingRes, hasTrMapUri, trMapUri);
				
				// Add the subject map statements
				SubjectMap sjMap = trMap.getSubject();
				BNode sjBlankNode = f.createBNode();
				Value templVal = f.createLiteral(sjMap.getTemplate()
						.getR2rmlTemplateString(factory));
				con.add(sjBlankNode, templateUri, templVal);
				con.add(trMapUri, subjMapUri, sjBlankNode);

				// Add the node id for the subject
				Value nodeIdVal = f.createLiteral(sjMap.getId());
				con.add(sjBlankNode, nodeIdUri, nodeIdVal);
				
				// Add the type for subject maps
				List<TemplateTermSet> rdfsTypes = sjMap.getRdfsType();
				for (TemplateTermSet typeTermSet:rdfsTypes) {
					if (typeTermSet.isSingleUriString()) {
						URI sjTypeUri = f.createURI(typeTermSet.getR2rmlTemplateString(factory));
						con.add(sjBlankNode, RDF.TYPE, sjTypeUri);
					}
				}
				
				// Check if the subject map is a blank node
				if (sjMap.isBlankNode()) {
					con.add(sjBlankNode, termTypeUri, blankNodeUri);
					// Add the information about the columns that it covers.
					// This info is used in constructing the blank node's URI.
					List<String> columnsCovered = auxInfo.getBlankNodesColumnCoverage().
							get(sjMap.getId());
					for (String colHnodeId:columnsCovered) {
						HNode hNode = factory.getHNode(colHnodeId);
						if (hNode != null) {
							Value colNameVal = f.createLiteral(getR2RMLColNameRepresentation(hNode));
							con.add(sjBlankNode, coversColUri, colNameVal);	
						}
					}
					// Add the prefix name for the blank node
					String prefix = auxInfo.getBlankNodesUriPrefixMap().get(sjMap.getId());
					Value prefixVal = f.createLiteral(prefix);
					con.add(sjBlankNode, bnNamePrefixUri, prefixVal);
				}
				
				// Mark as Steiner tree root node if required
				if (sjMap.isSteinerTreeRootNode()) {
					con.add(sjBlankNode, RDF.TYPE, steinerTreeRootNodeUri);
				}
				
				// Add the predicate object maps
				for (PredicateObjectMap pom:trMap.getPredicateObjectMaps()) {
					BNode pomBlankNode = f.createBNode();
					// Add the predicate
					TemplateTermSet predTermSet = pom.getPredicate().getTemplate();
					if (predTermSet.isSingleUriString()) {
						URI predValUri = f.createURI(predTermSet
								.getR2rmlTemplateString(factory));
						con.add(pomBlankNode, predUri, predValUri);
					} else {
						Value predValLiteratl = f.createLiteral(predTermSet.
								getR2rmlTemplateString(factory));
						con.add(pomBlankNode, predUri, predValLiteratl);
					}
					
					// Add the object: Could be RefObjectMap or simple object with column values
					if (pom.getObject().hasRefObjectMap()) {
						RefObjectMap rfMap = pom.getObject().getRefObjectMap();
						URI rfUri = f.createURI(Namespaces.KARMA_DEV + rfMap.getId());
						con.add(rfUri, RDF.TYPE, rfObjClassUri);
						
						TriplesMap prMap = rfMap.getParentTriplesMap();
						URI prMapUri = f.createURI(Namespaces.KARMA_DEV + prMap.getId());
						con.add(rfUri, parentTriplesMapUri, prMapUri);
						
						// Add the RefObjectMap as the object map of current POMap
						con.add(pomBlankNode, objectMapUri, rfUri);
					} else {
						TemplateTermSet objTermSet = pom.getObject().getTemplate();
						if (objTermSet.isSingleColumnTerm()) {
							BNode cnBnode = f.createBNode();
							Value cnVal = f.createLiteral(objTermSet.
									getColumnNameR2RMLRepresentation(factory));
							con.add(cnBnode, columnUri, cnVal);
							
							// Add the link b/w blank node and object map
							con.add(pomBlankNode, objectMapUri, cnBnode);
						}
					}
					con.add(trMapUri, predObjMapMapUri, pomBlankNode);
				}
			}
			
			Map<String, String> prefixMap = ontMgr.getPrefixMap(); 
			for (String ns:prefixMap.keySet()) {
				String prefix = prefixMap.get(ns);
				con.setNamespace(prefix, ns);
			}
			con.setNamespace(Prefixes.RR, Namespaces.RR);
			con.setNamespace(Prefixes.KARMA_DEV, Namespaces.KARMA_DEV);
			
			RDFHandler rdfxmlWriter = new TurtleWriter(writer);
			con.export(rdfxmlWriter);
			
		} catch (OpenRDFException e) {
			logger.error("Error occured while generating RDF representation of R2RML data " +
					"structures.", e);
		} finally {
			con.close();
		}
		return true;
	}

	private String getR2RMLColNameRepresentation(HNode hNode) throws JSONException {
		String colNameStr = "";
		JSONArray colNameArr = hNode.getJSONArrayRepresentation(factory);
		if (colNameArr.length() == 1) {
			colNameStr = (String) 
					(((JSONObject)colNameArr.get(0)).get("columnName"));
		} else {
			JSONArray colNames = new JSONArray();
			for (int i=0; i<colNameArr.length();i++) {
				colNames.put((String)
						(((JSONObject)colNameArr.get(i)).get("columnName")));
			}
			colNameStr = colNames.toString();
		}
		return colNameStr;
	}

	public boolean writeTransformationHistory(List<String> commandsJSON) throws RepositoryException {
		URI hasTransformationUri = f.createURI(Uris.KM_HAS_TRANSFORMATION_URI);
		for (String commandJson : commandsJSON) {
			Value commandLiteral = f.createLiteral(commandJson);
			con.add(mappingRes, hasTransformationUri, commandLiteral);
		}
		return true;
	}

	public void close() throws RepositoryException {
		con.close();
		myRepository.shutDown();
	}
}
