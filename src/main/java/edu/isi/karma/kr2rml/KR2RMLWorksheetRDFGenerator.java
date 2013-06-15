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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.kr2rml.ErrorReport.Priority;
import edu.isi.karma.modeling.Uris;
import edu.isi.karma.modeling.ontology.OntologyManager;
import edu.isi.karma.rep.Node;
import edu.isi.karma.rep.RepFactory;
import edu.isi.karma.rep.Row;
import edu.isi.karma.rep.Table;
import edu.isi.karma.rep.Worksheet;

public class KR2RMLWorksheetRDFGenerator {
	
	private RepFactory factory;
	private Worksheet worksheet;
	private String outputFileName;
	private OntologyManager ontMgr;
	private ErrorReport errorReport;
	private KR2RMLMappingAuxillaryInformation auxInfo;
	private Map<String, String> prefixToNamespaceMap;
	
	private PrintWriter outWriter;
	
	private Logger logger = LoggerFactory.getLogger(KR2RMLWorksheetRDFGenerator.class);
	public static String BLANK_NODE_PREFIX = "_:";

	public KR2RMLWorksheetRDFGenerator(Worksheet worksheet, RepFactory factory, 
			OntologyManager ontMgr, String outputFileName, 
			KR2RMLMappingAuxillaryInformation auxInfo, ErrorReport errorReport) {
		super();
		this.ontMgr = ontMgr;
		this.auxInfo = auxInfo;
		this.factory = factory;
		this.worksheet = worksheet;
		this.outputFileName = outputFileName;
		this.errorReport = errorReport;
		this.prefixToNamespaceMap = new HashMap<String, String>();
		
		populatePrefixToNamespaceMap();
	}
	
	public KR2RMLWorksheetRDFGenerator(Worksheet worksheet, RepFactory factory, 
			OntologyManager ontMgr, PrintWriter writer, KR2RMLMappingAuxillaryInformation auxInfo, 
			ErrorReport errorReport) {
		super();
		this.ontMgr = ontMgr;
		this.auxInfo = auxInfo;
		this.factory = factory;
		this.worksheet = worksheet;
		this.outWriter = writer;;
		this.errorReport = errorReport;
		this.prefixToNamespaceMap = new HashMap<String, String>();
	
		populatePrefixToNamespaceMap();
	}
	
	
	
	public void generateRDF() throws IOException {
		// Prepare the output writer
		BufferedWriter bw = null;
		try {
			if(this.outputFileName != null){
				bw = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(this.outputFileName),"UTF-8"));
				outWriter = new PrintWriter (bw);
			}else{
				outWriter = new PrintWriter (System.out);			
			}
			
			// RDF Generation starts at the top level rows
			ArrayList<Row> rows = this.worksheet.getDataTable().getRows(0, 
					this.worksheet.getDataTable().getNumRows());
			int i=1;
			for (Row row:rows) {
				Set<String> rowTriplesSet = new HashSet<String>();
				Set<String> rowPredicatesCovered = new HashSet<String>();
				Map<String, ReportMessage> predicatesFailed = new HashMap<String,ReportMessage>();
				generateTriplesForRow(row, rowTriplesSet, rowPredicatesCovered, predicatesFailed);
				outWriter.println();
				if (i++%2000 == 0){
					logger.info("Done processing " + i + " rows");
				}
				for (ReportMessage errMsg:predicatesFailed.values()){
					this.errorReport.addReportMessage(errMsg);
				}
			}
		} finally {
			outWriter.flush();
			outWriter.close();
			if (bw != null) {
				bw.close();
			}
		}
		// An attempt to prevent an occasional error that occurs on Windows platform
		// The requested operation cannot be performed on a file with a user-mapped section open
		System.gc();
	}
	
	private void generateTriplesForRow(Row row, Set<String> existingTopRowTriples, 
			Set<String> predicatesCovered, Map<String, ReportMessage> predicatesFailed) {
		Map<String, Node> rowNodes = row.getNodesMap();
		for (String hNodeId:rowNodes.keySet()) {
			Node rowNode = rowNodes.get(hNodeId);
			if (rowNode.hasNestedTable()) {
				Table rowNodeTable = rowNode.getNestedTable();
				if (rowNodeTable != null) {
					for (Row nestedTableRow:rowNodeTable.getRows(0, rowNodeTable.getNumRows())) {
						Set<String> rowPredicatesCovered = new HashSet<String>();
						generateTriplesForRow(nestedTableRow, existingTopRowTriples, 
								rowPredicatesCovered, predicatesFailed);
					}
				}
			} else {
				generateTriplesForCell(rowNode, existingTopRowTriples, hNodeId, 
						predicatesCovered, predicatesFailed);
			}
		}
	}
	
	private void generateTriplesForCell(Node node, Set<String> existingTopRowTriples, 
			String hNodeId, Set<String> predicatesCovered, 
			Map<String, ReportMessage> predicatesFailed) {
		Map<String, String> columnValues = node.getColumnValues();
		List<PredicateObjectMap> pomList = this.auxInfo.getHNodeIdToPredObjLinks().get(hNodeId);
		if (pomList == null || pomList.isEmpty()){
			return;
		}
		
		List<TriplesMap> toBeProcessedTriplesMap = new LinkedList<TriplesMap>();
		for (PredicateObjectMap pom:pomList) {
			toBeProcessedTriplesMap.add(pom.getTriplesMap());
		}
		
		Set<String> alreadyProcessedTriplesMapIds = new HashSet<String>();
		while (!toBeProcessedTriplesMap.isEmpty()) {
			TriplesMap trMap = toBeProcessedTriplesMap.remove(0);
			boolean dontAddNeighboringMaps = false;
			
			// Generate properties for the triple maps
			for (PredicateObjectMap pom:trMap.getPredicateObjectMaps()) {
				if (!predicatesCovered.contains(pom.getPredicate().getId())) {
					generatePropertyForPredObjMap(pom, columnValues, predicatesCovered, 
							existingTopRowTriples, hNodeId, predicatesFailed);
				}
			}
			
			// Need to stop at the root
			if (trMap.getSubject().isSteinerTreeRootNode()) {
				dontAddNeighboringMaps = true;
			}
			
			List<TriplesMapLink> neighboringLinks = this.auxInfo.getTriplesMapGraph()
					.getAllNeighboringTriplesMap(trMap.getId());
			
			for (TriplesMapLink trMapLink:neighboringLinks) {
				if (predicatesCovered.contains(trMapLink.getPredicateObjectMapLink().getPredicate().getId())){
					continue;
				}
				
				// Add the other triplesMap in queue to be processed later
				if (!alreadyProcessedTriplesMapIds.contains(trMapLink.getSourceMap().getId())
						&& !dontAddNeighboringMaps) {
					toBeProcessedTriplesMap.add(trMapLink.getSourceMap());	
				}
					
				if (!alreadyProcessedTriplesMapIds.contains(trMapLink.getTargetMap().getId())
						&& !dontAddNeighboringMaps) {
					toBeProcessedTriplesMap.add(trMapLink.getTargetMap());
				}
			}
			alreadyProcessedTriplesMapIds.add(trMap.getId());
		}
	}
	

	private void generatePropertyForPredObjMap(PredicateObjectMap pom,
			Map<String, String> columnValues, Set<String> predicatesCovered, 
			Set<String> existingTopRowTriples, String hNodeId, 
			Map<String, ReportMessage> predicatesFailed) {
		
		// Generate subject RDF
		SubjectMap subjMap = pom.getTriplesMap().getSubject();
		String subjUri = "";
		try {
			subjUri = generateSubjectMapRDF(subjMap, existingTopRowTriples, columnValues);
		} catch (ValueNotFoundKarmaException ve) {
			ReportMessage msg = createReportMessage("Could not generate subject's RDF and URI for <i>predicate:" + 
					 pom.getPredicate().getTemplate().toString().replaceAll("<", "{").replaceAll(">", "}") +
					 ", subject node: " + subjMap.getId()+"</i>", ve, 
					 this.factory.getHNode(hNodeId).getColumnName());
			predicatesFailed.put(pom.getPredicate().getId(), msg);
			return;
		} catch (NoValueFoundInNodeException e) {
			logger.debug("No value found in a node required to generate subject's RDF or URI.");
		}
		
		
		// Generate the predicate RDF
		String predicateUri = "";
		try {
			predicateUri = getTemplateTermSetPopulatedWithValues(columnValues,  
					pom.getPredicate().getTemplate()).replaceAll(" ", "");
		} catch (ValueNotFoundKarmaException ve) {
			ReportMessage msg = createReportMessage("Could not generate predicate's URI for <i>predicate:" + 
					pom.getPredicate().getTemplate().toString().replaceAll("<", "{").replaceAll(">", "}") + 
					", subject node: " + subjMap.getId() + "</i>",  ve, 
					this.factory.getHNode(hNodeId).getColumnName());
			predicatesFailed.put(pom.getPredicate().getId(), msg);
			return;
		} catch (NoValueFoundInNodeException e) {
			logger.debug("No value found in a node required to generate predicate's URI.");
		}
		
		// Object property
		if (pom.getObject().hasRefObjectMap()) {
			// Generate the object URI
			TriplesMap objPropertyObjectTriplesMap = pom.getObject().getRefObjectMap().
					getParentTriplesMap();
			String objUri = "";
			try {
				objUri = generateSubjectMapRDF(objPropertyObjectTriplesMap.getSubject(), 
						existingTopRowTriples, columnValues);
			} catch (ValueNotFoundKarmaException ve) {
				ReportMessage msg = createReportMessage("Could not generate object's URI for <i>predicate:" + 
						pom.getPredicate().getTemplate().toString()
						.replaceAll("<", "{").replaceAll(">", "}") + 
						", subject node: " + pom.getTriplesMap().getSubject().getId()+"</i>", ve
						, this.factory.getHNode(hNodeId).getColumnName());
				predicatesFailed.put(pom.getPredicate().getId(), msg);
				return;
			} catch (NoValueFoundInNodeException e) {
				logger.debug("No value found in a node required to generate object's URI for a predicate.");
			}
			
			String triple = constructTripleWithURIObject(subjUri, predicateUri, objUri);
			if (!existingTopRowTriples.contains(triple)) {
				outWriter.println(triple);
				existingTopRowTriples.add(triple);
			}
		} 
		// Data Property
		else {
			// Get the value
			String value = "";
			try {
				value = getTemplateTermSetPopulatedWithValues(columnValues, pom.getObject().getTemplate());
				if (value == null || value.trim().equals("")){
					return;
				}
			} catch (ValueNotFoundKarmaException ve) {
				ReportMessage msg = createReportMessage("Could not retrieve value for the <i>predicate:" + 
						pom.getPredicate().getTemplate().toString().replaceAll("<", "{").replaceAll(">", "}") +
						", subject node: " + subjMap.getId()+"</i>", ve, 
						this.factory.getHNode(hNodeId).getColumnName());
				predicatesFailed.put(pom.getPredicate().getId(), msg);
				return;
			} catch (NoValueFoundInNodeException e) {
				logger.debug("No value found in a node required to generate value for a predicate.");
			}
			String triple = constructTripleWithLiteralObject(subjUri, predicateUri, value, "");
			if (!existingTopRowTriples.contains(triple)) {
				existingTopRowTriples.add(triple);
				outWriter.println(triple);
			}
		}
		predicatesCovered.add(pom.getPredicate().getId());
		if (predicatesFailed.containsKey(pom.getPredicate().getId())){
			predicatesFailed.remove(pom.getPredicate().getId());
		}
	}

	private ReportMessage createReportMessage(String title, ValueNotFoundKarmaException ve, 
			String cellColumnName) {
		ReportMessage msg = new ReportMessage(title, ve.getMessage() 
				+ " from column: <i>" +  cellColumnName + "</i>", Priority.high);
		return msg;
	}

	private String generateSubjectMapRDF(SubjectMap subjMap, Set<String> existingTopRowTriples, Map<String, 
			String> columnValues) throws ValueNotFoundKarmaException, NoValueFoundInNodeException {
		// Generate URI for subject
		String uri = "";
		if (subjMap.isBlankNode()) {
			uri = getBlankNodeUri(subjMap.getId(), columnValues).replaceAll(" ", "")
					.replaceAll("[,`']", "_");
		}else{
			uri = getTemplateTermSetPopulatedWithValues(columnValues, subjMap.getTemplate())
				.replaceAll(" ", "").replaceAll("[,`']", "_");
		}
		
		// Generate triples for specifying the types
		for (TemplateTermSet typeTerm:subjMap.getRdfsType()) {
			String typeUri = getTemplateTermSetPopulatedWithValues(columnValues, typeTerm);
			String triple = constructTripleWithURIObject(uri, Uris.RDF_TYPE_URI, typeUri);
			if (!existingTopRowTriples.contains(triple)) {
				existingTopRowTriples.add(triple);
				outWriter.println(triple);
			}
		}
		return uri;
	}
	
	private String constructTripleWithURIObject(String subjUri, String predicateUri, String objectUri) {
		if (!subjUri.startsWith(BLANK_NODE_PREFIX)){
			subjUri = "<" + subjUri + ">";
		}
		
		if (!objectUri.startsWith(BLANK_NODE_PREFIX)){
			objectUri = "<" + objectUri + ">";
		}
		
		return subjUri + " " 
				+ getNormalizedPredicateUri(predicateUri) + " " 
				+ objectUri.replaceAll(" ", "").replaceAll("[,`']", "_") + " .";
	}
	
	private String constructTripleWithLiteralObject(String subjUri, String predicateUri, String value, 
			String literalType) {
		if (!subjUri.startsWith(BLANK_NODE_PREFIX)){
			subjUri = "<" + subjUri + ">";
		}
		
		// Escaping the quotes
		value = value.replaceAll("\\\\", "\\\\\\\\");
		value = value.replaceAll("\"", "\\\\\"");
		
		// Take care of the new lines that may appear iin text
		if (value.contains("\n") || value.contains("\r")) {
			value = "\"\"\"" + value + "\"\"\"";
		} else {
			value = "\"" + value + "\"";
		}
		
		// Add the RDF literal type to the literal if present
		if (literalType != null && !literalType.equals("")) {
			return subjUri + " " + getNormalizedPredicateUri(predicateUri) + " \"" + value + 
					"\"" + "^^" + literalType + " .";
		}
		return subjUri + " " + getNormalizedPredicateUri(predicateUri) + " " + value + " .";
	}

	private String getBlankNodeUri(String subjMapid, Map<String, String> columnValues) 
			throws ValueNotFoundKarmaException {
//		System.out.println("Column values: " + columnValues);
		StringBuilder output = new StringBuilder();
		// Add the blank namespace
		output.append(BLANK_NODE_PREFIX);
		
		// Add the class node prefix
		output.append(this.auxInfo.getBlankNodesUriPrefixMap().get(subjMapid).replaceAll(":", "_"));
		
		// Add the node ids for tha columns covered
		List<String> hNodeIdsCovered = this.auxInfo.getBlankNodesColumnCoverage().get(subjMapid);
//		System.out.println("Blank node coverage: " + this.auxInfo.getBlankNodesColumnCoverage().get(subjMapid));
		if (hNodeIdsCovered != null && !hNodeIdsCovered.isEmpty()) {
			for (int i=0; i<hNodeIdsCovered.size(); i++) {
				String hNodeId = hNodeIdsCovered.get(i);
				if (columnValues.containsKey(hNodeId)) {
					output.append("_" + columnValues.get(hNodeId));
				} else {
					String columnName = this.factory.getHNode(hNodeId).getColumnName();
//					System.out.println("Value not found for " + hNodeId + " Column name:" + columnName);
					throw new ValueNotFoundKarmaException("Could not retrieve value while constructing " +
							"blank URI of column:" + columnName + ". ", hNodeId);
				}
			}
		}
		return output.toString();
	}

	private String getTemplateTermSetPopulatedWithValues(Map<String, String> columnValues, 
			TemplateTermSet termSet) throws ValueNotFoundKarmaException, NoValueFoundInNodeException {
		StringBuilder output = new StringBuilder();
		for (TemplateTerm term:termSet.getAllTerms()) {
			// String template term
			if (term instanceof StringTemplateTerm) {
				output.append(term.getTemplateTermValue());
			} 
			// Column template term
			else if (term instanceof ColumnTemplateTerm) {
				String hNodeId = term.getTemplateTermValue();
				if (columnValues.containsKey(hNodeId)) {
					Node node = factory.getNode(columnValues.get(hNodeId));
					if (node != null) {
						if (node.getValue().asString() == null 
								|| node.getValue().asString().equals("")) {
							throw new NoValueFoundInNodeException();
						}
						output.append(node.getValue().asString());
					}
				} else {
					String columnName = this.factory.getHNode(hNodeId).getColumnName();
					throw new ValueNotFoundKarmaException("Could not retrieve value from column: " + 
							columnName + ".", hNodeId);
				}
			}
		}
		return output.toString();
	}
	
	private String getNormalizedPredicateUri(String predicate) {
		// Check if the predicate contains a predicate.
		if (!predicate.startsWith("http:") && predicate.contains(":")) {
			// Replace the prefix with proper namespace by looking into the ontology manager
			String prefix = predicate.substring(0, predicate.indexOf(":"));
			
			String namespace = this.prefixToNamespaceMap.get(prefix);
			if (namespace == null || namespace.isEmpty()) {
				this.errorReport.createReportMessage("Error creating predicate's URI: " + predicate, 
						"No namespace found for the prefix: " + prefix, Priority.high);
//				logger.error("No namespace found for the predicate prefix: " + prefix);
			} else {
				predicate = namespace + predicate.substring(predicate.indexOf(":")+1);
			}
		}
		return "<" + predicate.replaceAll(" ", "").replaceAll("[,`']", "_") + ">";
	}
	
	private void populatePrefixToNamespaceMap() {
		Map<String, String> prefixMapOntMgr = this.ontMgr.getPrefixMap(); 
		for (String ns:prefixMapOntMgr.keySet()) {
			String prefix = prefixMapOntMgr.get(ns);
			this.prefixToNamespaceMap.put(prefix, ns);
		}
	}
}

class ValueNotFoundKarmaException extends Exception{

	private static final long serialVersionUID = 1L;
	private String offendingColumnHNodeId;
	
	//constructor without parameters
	public ValueNotFoundKarmaException() {}

	//constructor for exception description
	public ValueNotFoundKarmaException(String description, String offendingColumnHNodeId) {
	    super(description);
	    this.offendingColumnHNodeId = offendingColumnHNodeId;
	}
	
	public String getOffendingColumnHNodeId() {
		return this.offendingColumnHNodeId;
	}
}

class NoValueFoundInNodeException extends Exception{

	private static final long serialVersionUID = 1L;
	
	//constructor without parameters
	public NoValueFoundInNodeException() {}

	//constructor for exception description
	public NoValueFoundInNodeException(String description) {
	    super(description);
	}
}
