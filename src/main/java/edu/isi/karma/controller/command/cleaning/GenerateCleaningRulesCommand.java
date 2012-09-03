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
package edu.isi.karma.controller.command.cleaning;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVReader;
import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.WorksheetCommand;
import edu.isi.karma.controller.update.CleaningResultUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.rep.HNodePath;
import edu.isi.karma.rep.Node;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.cleaning.RamblerTransformationExample;
import edu.isi.karma.rep.cleaning.RamblerTransformationInputs;
import edu.isi.karma.rep.cleaning.RamblerTransformationOutput;
import edu.isi.karma.rep.cleaning.RamblerValueCollection;
import edu.isi.karma.rep.cleaning.TransformationExample;
import edu.isi.karma.rep.cleaning.ValueCollection;
import edu.isi.karma.view.VWorkspace;

public class GenerateCleaningRulesCommand extends WorksheetCommand {
	final String hNodeId;
	private List<TransformationExample> examples;
	RamblerTransformationInputs inputs;

	public GenerateCleaningRulesCommand(String id, String worksheetId, String hNodeId, String examples) {
		super(id, worksheetId);
		this.hNodeId = hNodeId;
		this.examples = this.parseExample(examples);
		
	}
	public List<TransformationExample> parseExample(String example)
	{
		List<TransformationExample> x = new ArrayList<TransformationExample>();
		try
		{
			JSONArray jsa = new JSONArray(example);
			for(int i=0;i<jsa.length();i++)
			{
				String[] ary = new String[3];
				JSONObject jo = (JSONObject) jsa.get(i);
				String nodeid = (String)jo.get("nodeId");
				String before = (String)jo.getString("before");
				String after = (String)jo.getString("after");
				ary[0] = nodeid;
				ary[1] = "<_START>"+before+"<_END>";
				ary[2] = after;
				TransformationExample re = new RamblerTransformationExample(ary[1], ary[2], ary[0]);
				x.add(re);
			}
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
		return x;
	}
	@Override
	public String getCommandName() {
		return GenerateCleaningRulesCommand.class.getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Generate Cleaning Rules";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.undoable;
	}
	
	private static List<String> getTopK(Set<String> res,int k)
	{
		//
		String[] x = (String[])res.toArray(new String[res.size()]);
		/*System.out.println(""+x);
		String trainPath = "./grammar/features.arff";
		List<String> vs = new ArrayList<String>();
		List<Double> scores = UtilTools.getScores(x, trainPath);
		System.out.println("Scores: "+scores);
		List<Integer> ins =UtilTools.topKindexs(scores, k);
		System.out.println("Indexs: "+ins);*/
		List<String> y = new ArrayList<String>();
		for(int i = 0; i<k&&i<x.length;i++)
		{
			y.add(x[i]);
		}
		return y;
	}
	@Override
	public UpdateContainer doIt(VWorkspace vWorkspace) throws CommandException {
		Worksheet wk = vWorkspace.getRepFactory().getWorksheet(worksheetId);
		// Get the HNode
		HNodePath selectedPath = null;
		List<HNodePath> columnPaths = wk.getHeaders().getAllPaths();
		for (HNodePath path : columnPaths) {
			if (path.getLeaf().getId().equals(hNodeId)) {
				selectedPath = path;
			}
		}
		Collection<Node> nodes = new ArrayList<Node>();
		wk.getDataTable().collectNodes(selectedPath, nodes);
		HashMap<String, String> rows = new HashMap<String,String>();
		//obtain original rows
		for (Node node : nodes) {
			String id = node.getId();
			String originalVal = "<_START>"+node.getValue().asString()+"<_END>";
			//System.out.println(id+","+originalVal);
			rows.put(id, originalVal);
		}
		RamblerValueCollection vc = new RamblerValueCollection(rows);
		inputs = new RamblerTransformationInputs(examples, vc);
		//generate the program
		boolean results = false;
		int iterNum = 0;
		RamblerTransformationOutput rtf = null;
		while(iterNum<10 && !results) // try to find any rule during 5 times running
		{
			rtf = new RamblerTransformationOutput(inputs);
			if(rtf.getTransformations().keySet().size()>0)
			{
				results = true;
			}
			iterNum ++;
		}
		Iterator<String> iter = rtf.getTransformations().keySet().iterator();
		List<ValueCollection> vvc = new ArrayList<ValueCollection>();
		HashMap<String,List<String>> js2tps = new HashMap<String,List<String>>();
//		int index = 0;
		while(iter.hasNext())
		{
			String tpid = iter.next();
			ValueCollection rvco = rtf.getTransformedValues(tpid);
			vvc.add(rvco);
			String reps = rvco.getJson().toString();
			if(js2tps.containsKey(reps))
			{
				js2tps.get(reps).add(tpid); // update the variance dic
			}
			else
			{
				List<String> tps = new ArrayList<String>();
				tps.add(tpid);
				js2tps.put(reps, tps);
			}
		}
		////////
		List<String> jsons = new ArrayList<String>();
		if(js2tps.keySet().size()!=0)
		{
			jsons = getTopK(js2tps.keySet(), 50);
		}
		else
		{
			System.out.println("Didn't find any transformation programs");
		}
		
		return new UpdateContainer(new CleaningResultUpdate(hNodeId, jsons,js2tps));
	}

	@Override
	public UpdateContainer undoIt(VWorkspace vWorkspace) {
		// TODO Auto-generated method stub
		return null;
	}
	public static void main(String[] args)
	{
		String dirpath = "/Users/bowu/Research/testdata/TestSingleFile";
		File nf = new File(dirpath);
		File[] allfiles = nf.listFiles();
		for(File f:allfiles)
		{
			try
			{
				if(f.getName().indexOf(".csv")==(f.getName().length()-4))
				{
					
					CSVReader cr = new CSVReader(new FileReader(f),'\t');
					String[] pair;
					int isadded = 0;
					HashMap<String,String> tx = new HashMap<String,String>();
					int i = 0;
					List<TransformationExample> vrt = new ArrayList<TransformationExample>();
					while ((pair=cr.readNext())!=null)
					{
						
						pair[0] = "<_START>"+pair[0]+"<_END>";
						tx.put(i+"", pair[0]);
						if(isadded<2)
						{
							RamblerTransformationExample tmp = new RamblerTransformationExample(pair[0], pair[1], i+"");
							vrt.add(tmp);
							isadded ++;
						}
						i++;
					}
					cr.close();
					RamblerValueCollection vc = new RamblerValueCollection(tx);
					RamblerTransformationInputs inputs = new RamblerTransformationInputs(vrt, vc);
					//generate the program
					RamblerTransformationOutput rtf = new RamblerTransformationOutput(inputs);
					HashMap<String,List<String>> js2tps = new HashMap<String,List<String>>();
					Iterator<String> iter = rtf.getTransformations().keySet().iterator();
					List<ValueCollection> vvc = new ArrayList<ValueCollection>();
//					int index = 0;
					while(iter.hasNext())
					{
						String tpid = iter.next();
						ValueCollection rvco = rtf.getTransformedValues(tpid);
						vvc.add(rvco);
						String reps = rvco.getJson().toString();
						if(js2tps.containsKey(reps))
						{
							js2tps.get(reps).add(tpid); // update the variance dic
						}
						else
						{
							List<String> tps = new ArrayList<String>();
							tps.add(tpid);
							js2tps.put(reps, tps);
						}
					}
					////////
					if(js2tps.keySet().size() == 0)
					{
						System.out.println("No Rules have been found");
						return; 
					}
					List<String> jsons = getTopK(js2tps.keySet(), 3);
					for(String s:jsons)
					{
						System.out.println(""+s);
					}
				}			
			}
			catch(Exception ex)
			{
				System.out.println(""+ex.toString());
			}	
		}
	}
}
