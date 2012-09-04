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
package edu.isi.karma.cleaning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterLexer;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterParser;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterTree;
import edu.isi.karma.cleaning.features.Feature;
import edu.isi.karma.cleaning.features.RegularityFeatureSet;

public class Main {
	 public void Evaluation()
	 {
		 String fpath = "/Users/bowu/Research/dataclean/data/rule.txt";
		 String xline = "";
		 String fpath0 = "/Users/bowu/Research/dataclean/data/30addresslist.txt";
		 String fpath1 = "/Users/bowu/Research/dataclean/data/eval.txt";
		
		 try
		 {
			 	BufferedWriter bres = new BufferedWriter(new FileWriter(fpath1));
			 	BufferedReader br = new BufferedReader(new FileReader(fpath));
				String line = "";
				while((line=br.readLine())!=null)
				{
					if(line.compareTo("")==0){
						break;
					}
					
			        Evaluator eva = new Evaluator();
			        BufferedReader xbr = new BufferedReader(new FileReader(fpath0));
					while(true)
					{
						xline=xbr.readLine();
						if(xline == null)
						{
							double d1 = eva.calShannonEntropy(eva.pos);
							double d2 = eva.calShannonEntropy(eva.cht);
							//String l = "rule:::::::::"+line+"\n"+"epos:"+d1+" ecnt:"+d2;
							String l = d1+"	"+d2+" ";
							bres.write(l+"\n");
							bres.flush();
							System.out.println(d1+" "+d2);
							break;
						}
						if(xline.compareTo("")==0)
						{
							break;
						}
						Ruler r = new Ruler();
						r.setNewInput(xline);
						eva.setRuler(r);	
						CharStream cs =  new ANTLRStringStream(line);
						RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
				        CommonTokenStream tokens = new CommonTokenStream(lexer);
				        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
				        CommonTreeNodeStream nodes = new CommonTreeNodeStream(parser.rule().getTree());
					    RuleInterpreterTree inter = new RuleInterpreterTree(nodes);
					    inter.setRuler(r);
					    inter.rule();
					    eva.addCnt(r.whats);
					    eva.addPos(r.positions);
					   // System.out.println(r.print());
					    
						br.close();
						bres.close();
					}
					//output evaluation result
					
					xbr.close();
					
				}
		}
		catch(Exception ex)
		{
				System.out.println(""+ex.toString());
		}	 
	}
	public void autogeneratetestFeaturesFile()
	{
		String fpath = "/Users/bowu/Research/dataclean/data/testrule.txt";
		 ResultViewer rv = new ResultViewer();
		 //ResultViewer rv1 = new ResultViewer();
		 List<CommonTree> ps = new ArrayList<CommonTree>();
		 String orgin = "";
		 try
		 {	
				BufferedReader br = new BufferedReader(new FileReader(fpath));
				String line = "";
				List<String> row = new ArrayList<String>();
				row.add("rows");
				HashMap<String,List<String>> hm = new HashMap<String,List<String>>(); 
				
				while((line=br.readLine())!=null)
				{
					if(line.compareTo("")==0){
						break;
					}
					
			        //CommonTree t  = (CommonTree) parser.rule().getTree();
			        String[] xline;
					String fpath0 = "/Users/bowu/Research/dataclean/data/RuleData/50_address_pair.csv";
					CSVReader xbr = new CSVReader(new FileReader(fpath0),'\t');
					xbr.readNext();
					String s = "";
					orgin = "";
					boolean isrit = true;
					while((xline=xbr.readNext())!=null)
					{
						Ruler r = new Ruler();
						List<String> xrow = new ArrayList<String>();
						xrow.add(xline[0]);
						r.setNewInput(xline[0]);	
						CharStream cs =  new ANTLRStringStream(line);
						RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
				        CommonTokenStream tokens = new CommonTokenStream(lexer);
				        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
				        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
				        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
				        evaluator.setRuler(r);
				        evaluator.rule();
				        String rvalue = "";
				        s+= r.toString()+"\n";
				        orgin += xline[0]+"\n";
				        if(r.toString().compareTo(xline[1])==0)
				        {
				        		xrow.add(r.toString());
				        }
				        else
				        {
				        		xrow.add("<font color='#FF0000'>"+r.toString()+"</font>");
				        		isrit = false;
				        } 
					}
					br.close();
					xbr.close();
					if(hm.containsKey(s))
	        			{
	        				hm.get(s).add(line);
	        			}
	        			else
	        			{
	        				List<String> examples = Arrays.asList(s.split("\n"));
	        				//RegularityClassifer.Add2FeatureFile(examples, "", isrit);
	        				List<String> vr = new ArrayList<String>();
	        				vr.add(line);
	        				hm.put(s,vr); 
	        			}
					
				}
		 }
		 catch(Exception ex)
		 {
			 System.out.println(""+ex.toString());
		 }
	}
	//fpath rule file loaction
	//fpath0 ground through file loaction
	//ofpath outout location
	public static String[] exper1_cluster(String fpath,String fpath0,String ofpath)
	{
		// ResultViewer rv = new ResultViewer();
		 List<CommonTree> ps = new ArrayList<CommonTree>();
		 String orgin = "";
		 try
		 {	
				BufferedReader br = new BufferedReader(new FileReader(fpath));
				String line = "";
				List<String> row = new ArrayList<String>();
				row.add("rows");
				HashMap<String,List<String>> hm = new HashMap<String,List<String>>(); 
				
				while((line=br.readLine())!=null)
				{
					if(line.compareTo("")==0){
						break;
					}
					
			        //CommonTree t  = (CommonTree) parser.rule().getTree();
			        String[] xline;
					CSVReader xbr = new CSVReader(new FileReader(fpath0),'\t');
					//xbr.readNext();
					String s = "";//the string contain all the data
					orgin = ""; //the string contain all the original data
					while((xline=xbr.readNext())!=null)
					{
						Ruler r = new Ruler();
						List<String> xrow = new ArrayList<String>();
						xrow.add(xline[0]);
						r.setNewInput(xline[0]);	
						CharStream cs =  new ANTLRStringStream(line);
						RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
				        CommonTokenStream tokens = new CommonTokenStream(lexer);
				        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
				        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
				        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
				        evaluator.setRuler(r);
				        evaluator.rule();
				        String rvalue = "";
				        s+= r.toString()+"\n";
				        orgin += xline[0]+"\n";
				        if(r.toString().compareTo(xline[1])==0)
				        {
				        		xrow.add(r.toString());
				        }
				        else
				        {
				        		xrow.add("<font color='#FF0000'>"+r.toString()+"</font>");
				        } 
					}
					xbr.close();
					// hm is the data to rules
					if(hm.containsKey(s))
	        			{
	        				hm.get(s).add(line);
	        			}
	        			else
	        			{
	        				List<String> vr = new ArrayList<String>();
	        				vr.add(line);
	        				hm.put(s,vr); 
	        			}
				}
				br.close();

				//output the hash table
				// output all the data sets
//				String[] a = new String[hm.keySet().size()];
				int cnt = 0;
				//ResultViewer rx = new ResultViewer();
				boolean isfirstRun = true;
				List<String> rawAddr = new ArrayList<String>();
				for(String ks:orgin.split("\n"))
				{
					rawAddr.add(ks);
				}
				double lowest = 10000;
				String result =""; 
				for(String xs:hm.keySet())
				{
//					List<String> vs1 = hm.get(xs);
					RegularityFeatureSet rf = new RegularityFeatureSet();
					List<String> addr = new ArrayList<String>();
					for(String is:xs.split("\n"))
					{
						addr.add(is);
					}				
					Collection<Feature> cf = rf.computeFeatures(rawAddr,addr);
					Feature[] x = new Feature[cf.size()];
					cf.toArray(x);
					List<String> xrow = new ArrayList<String>();
					if(isfirstRun)
					{
						xrow.add("Featurename");
						for(int l=0;l<x.length;l++)
						{
							xrow.add(x[l].getName());
						}
						isfirstRun = false;
						//rx.addRow(xrow);
						xrow = new ArrayList<String>();
					}
					if(!isfirstRun)
					{
						xrow.add(String.valueOf(cnt));
						double sc = 0;
						for(int k=0;k<cf.size();k++)
						{
							sc += x[k].getScore();
							xrow.add(String.valueOf(x[k].getScore()));
						}
						if(sc<lowest)
						{
							lowest = sc;
							result = xs;
						}
					}
					//rx.addRow(xrow);
					//write2file(vs1,"/Users/bowu/Research/dataclean/data/cluster_rset"+cnt+".txt");
					cnt ++;
				}
				String [] res = visualResult(result, fpath0);
				//rx.print(ofpath);
				return res;
		}
		catch(Exception ex)
		{
				System.out.println(""+ex.toString());
				return null;
		}	 
	}
	public void write2file(Collection<String> x,String fname)
	{
		try
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fname)));
			for(String s:x)
			{
				bw.write(s+"\n");
			}
			bw.close();
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
	public void applyRule(String rule, String fpath)
	{
		 ResultViewer rv = new ResultViewer();
		 try
		 {
			
	        Ruler r = new Ruler();
			String xline = "";
			File f = new File(fpath);
			BufferedReader xbr = new BufferedReader(new FileReader(f));
			while((xline=xbr.readLine())!=null)
			{
				List<String> xrow = new ArrayList<String>();
				if(xline.compareTo("")==0){
					break;
				}
				//xrow.add(xline);
				CharStream cs =  new ANTLRStringStream(rule);
				RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
				r.setNewInput(xline);
		        evaluator.setRuler(r);
		        evaluator.rule();
		        System.out.println(r.toString());
		        xrow.add(xline);
			    xrow.add(r.toString());
			    //xrow.add(r.toString());
				rv.addRow(xrow);
			}
			xbr.close();
			rv.print(f.getAbsolutePath()+"_res.csv");
		}
		catch(Exception ex)
		{
				System.out.println(""+ex.toString());
		}	 
	}
	// for generating training data
	public void genTrainingdata()
	{
		File dir = new File("/Users/bowu/Research/dataclean/data/RuleData");
		File[] flist = dir.listFiles();
		try
		{
			for(int i=0;i<flist.length;i++)
			{
				if(flist[i].getName().contains(".csv"))
				{
					CSVReader cr = new CSVReader(new FileReader(flist[i]),'\t');
					BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/bowu/Research/dataclean/data/"+flist[i].getName()+"_res.txt")));
					String[] eles = cr.readNext();
					while((eles=cr.readNext())!=null)
					{
						bw.write(eles[1]+"\n");
					}
					cr.close();
					bw.close();
				}
			}
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
	static List<Integer> ruleNum1 = new ArrayList<Integer>();
	static List<Integer> ruleNum2 = new ArrayList<Integer>();
	// output for generating all the results of all rules
	//fpath rule file path fpath0 data file
	public static String[] output(String fpath,String fpath0)
	{
			 //ResultViewer rv = new ResultViewer();
			 List<CommonTree> ps = new ArrayList<CommonTree>();
			 try
			 {	
					BufferedReader br = new BufferedReader(new FileReader(fpath));
					String line = "";
					List<String> row = new ArrayList<String>();
					row.add("rows");
					while((line=br.readLine())!=null)
					{
						if(line.compareTo("")==0){
							break;
						}
						row.add(line);
						CharStream cs =  new ANTLRStringStream(line);
						RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
				        CommonTokenStream tokens = new CommonTokenStream(lexer);
				        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
				        //CommonTree t  = (CommonTree) parser.rule().getTree();
				        ps.add((CommonTree)parser.rule().getTree());
					}
					br.close();
					
					//rv.addRow(row);
			        //System.out.println("hello");
			        // apply the rule
					String [] wrongExample = null;
			        HashSet<Integer> hs = new HashSet<Integer>();
					String[] xline;
					CSVReader xbr = new CSVReader(new FileReader(fpath0),'\t');
					xbr.readNext();
					while((xline=xbr.readNext())!=null)
					{
						Ruler r = new Ruler();
						List<String> xrow = new ArrayList<String>();
						xrow.add(xline[0]);
						for(int k = 0;k<ps.size();k++)
					    {
							r.setNewInput(xline[0]);
							CommonTreeNodeStream nodes = new CommonTreeNodeStream(ps.get(k));
					        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
					        evaluator.setRuler(r);
					        evaluator.rule();
						    System.out.println("function output "+k);
					        String rvalue = "";
					        if(r.toString().compareTo(xline[1])==0)
					        {
					        		xrow.add(r.toString());
					        }
					        else
					        {
					        		xrow.add("<font color='#FF0000'>"+r.toString()+"</font>");
					        		if(!hs.contains(k))
					        		{
					        			hs.add(k);
					        			wrongExample = xline;
					        		}
					        } 
						    
					    }
						//rv.addRow(xrow);
					}
					xbr.close();
				//rv.print("xx.csv");
				//rv.publishHTML("/Users/bowu/Research/dataclean/data/ResVisual.htm");
				System.out.println("Right Rules: "+(ps.size()-hs.size())+"   Total Rules: "+ps.size());
				ruleNum1.add(ps.size());
				return wrongExample;
			}
			catch(Exception ex)
			{
					System.out.println(""+ex.toString());
					return null;
			}	 
	}
	public void visualFile()
	{
		try 
		{
			String gtruth = "/Users/bowu/Research/dataclean/data/RuleData/50_address_pair.csv";
			String file = "/Users/bowu/Research/dataclean/data/cluster1.csv";
			CSVReader cr = new CSVReader(new FileReader(new File(gtruth)),'\t');
			List<String[]> lines = cr.readAll();
			cr.close();
			lines.remove(0);
			CSVReader cr1 = new CSVReader(new FileReader(new File(file)),'\t');
			String[] ds = cr1.readNext();
			cr1.close();
			ResultViewer rv = new ResultViewer();
			for(int p=0;p<ds.length;p++)
			{
				String[] res = ds[p].split("\n");
				List<String> tr = new ArrayList<String>();
				for(int k=0;k<res.length;k++)
				{
					if(res[k].compareTo(lines.get(k)[1])==0)
					{
						tr.add(res[k]);
					}
					else
					{
						tr.add("<font color='#FF0000'>"+res[k]+"</font>");
					}
				}
				rv.addColumn(tr);
			}
			rv.publishHTML("/Users/bowu/Research/dataclean/data/cluster1.htm");
		} catch (Exception e) {
			// TODO: handle exception
		}	
	}
	public static String[] visualResult(String xs, String gtruth)
	{
		try 
		{
			CSVReader cr = new CSVReader(new FileReader(new File(gtruth)),'\t');
			List<String[]> lines = cr.readAll();
			cr.close();
			//lines.remove(0);
			ResultViewer rv = new ResultViewer();
			String[] res = xs.split("\n");
			String[] wrongExample = null;//use for experiment to record incorrect transoformated result
			List<String> tr = new ArrayList<String>();
			List<String> org = new ArrayList<String>();
			for(String[] tmp:lines)
			{
				org.add(tmp[0]);
			}
			rv.addColumn(org);
			for(int k=0;k<res.length;k++)
			{
				if(res[k].compareTo(lines.get(k)[1])==0)
				{
					tr.add(res[k]);
				}
				else
				{
					tr.add("<font color='#FF0000'>"+res[k]+"</font>");
					wrongExample = lines.get(k);
				}
			}
			rv.addColumn(tr);
			rv.publishHTML("/Users/bowu/Research/dataclean/data/cluster1.htm");
			return wrongExample;
		} catch (Exception e) {
			System.out.println("error in the visualResult");
			return null;
		}
		
	}
	public void genNegRes()
	{
		String dpath = "/Users/bowu/Research/dataclean/data/RuleData/rawdata";
		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/bowu/Research/dataclean/data/RuleData/rawdata/negarules.in")));
			File f = new File(dpath);
			File[] fs = f.listFiles();
			for(File fe:fs)
			{
				if(fe.getName().contains("txt"))
				{
					String rule = br.readLine();
					this.applyRule(rule, fe.getAbsolutePath());
				}
			}
			br.close();			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	public static void fun1()
	{
		try
		{
			BufferedReader br1 = new BufferedReader(new FileReader(new File("/Users/bowu/Research/dataclean/data/RuleData/rawdata/trans/50_data2.txt")));
			BufferedReader br2 = new BufferedReader(new FileReader(new File("/Users/bowu/Research/dataclean/data/RuleData/rawdata/trans/50_data2_res.txt")));
			CSVWriter cw = new CSVWriter(new FileWriter(new File("/Users/bowu/Research/dataclean/data/RuleData/rawdata/trans/50_data2.csv")),'\t');
			String line1 = "";
			String line2 = "";
			while(((line1=br1.readLine())!=null)&&((line2=br2.readLine())!=null))
			{
//				line2.replaceAll("", "\""); // BUG: This line doesn't actually do anything
				String[] l = {line1,line2};
				cw.writeNext(l);
			}
			cw.close();
			br2.close();
			br1.close();
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
//	public void exper_2(String dirpath)
//	{
//		File nf = new File(dirpath);
//		File[] allfiles = nf.listFiles();
//		//statistics
//		List<String> names = new ArrayList<String>();
//		List<Integer> exampleCnt = new ArrayList<Integer>();
//		List<Double> timeleng = new ArrayList<Double>();
//		List<Integer> cRuleNum = new ArrayList<Integer>();
//		List<List<String>> ranks = new ArrayList<List<String>>();
//		List<List<Integer>> consisRules = new ArrayList<List<Integer>>();
//		List<String> cRules = new ArrayList<String>();
//		//list all the csv file under the dir
//		for(File f:allfiles)
//		{
//			String cx = "";
//			
//			List<String[]> examples = new ArrayList<String[]>();
//			List<String[]> entries = new ArrayList<String[]>();	
//			List<String> rank = new ArrayList<String>();
//			List<Integer> consRule = new ArrayList<Integer>();
//			try
//			{
//				if(f.getName().indexOf(".csv")==(f.getName().length()-4))
//				{					
//					CSVReader cr = new CSVReader(new FileReader(f),'\t');
//					String[] pair;
//					String corrResult = "";
//					while ((pair=cr.readNext())!=null)
//					{
//						pair[0] = "<_START>"+pair[0]+"<_END>";
//						entries.add(pair);
//						corrResult += pair[1]+"\n";
//					}
//					cr.close();
//					HashMap<Integer,Boolean> indicators = new HashMap<Integer,Boolean>();
//					examples.add(entries.get(0));
//					boolean isend = false;
//					double timespan = 0.0;
//					int Ranktermin = -1;
//					while(Ranktermin == -1) // repeat as no correct answer appears.
//					{
//						cx = "";
//						HashMap<String,Integer> dic = new HashMap<String,Integer>();
//						long st = System.currentTimeMillis();
//						List<String> pls = RuleUtil.genRule(examples);
//						System.out.println("Consistent Rules :"+pls.size());
//						for(int k = 0; k<examples.size();k++)
//						{
//							System.out.println(examples.get(k)[0]+"    "+examples.get(k)[1]);
//						}
//						int corrNum = 0;
//						String[] wexam = null;
//						if(pls.size()==0){
//							continue;
//						}
//						for(int i = 0; i<pls.size(); i++)
//						{		
//							String tranresult = "";
//							cx +="\n\n"+pls.get(i);
//							String[] rules = pls.get(i).split("<RULESEP>");
//							//System.out.println(""+s1);
//							List<String> xr = new ArrayList<String>();
//							for(int t = 0; t< rules.length; t++)
//							{
//								if(rules[t].length()!=0){
//									xr.add(rules[t]);
//								}
//							}
//							isend = true;
//							for(int j = 0; j<entries.size(); j++)
//							{
//								String s = RuleUtil.applyRule(xr, entries.get(j)[0]);
//								if(s== null||s.length()==0)
//								{
//									isend = false;
//									wexam = entries.get(j);
//									s = entries.get(j)[0];
//									//break;
//								}
//								if(s.compareTo(entries.get(j)[1])!=0)
//								{
//									isend = false;
//									wexam = entries.get(j);
//									//break;
//								}
//								tranresult += s+"\n";								
//							}
//							if(dic.containsKey(tranresult))
//							{
//								dic.put(tranresult, dic.get(tranresult)+1);
//							}
//							else
//							{
//								dic.put(tranresult, 1);
//							}
//							if(isend){
//								corrNum++;
//							}
//						}	
//						long ed = System.currentTimeMillis();
//						timespan = (ed -st)*1.0/60000;
//						
//						String trainPath = "./grammar/features.arff";
//						// TODO: Weka dependency
////						int trnk = UtilTools.rank(dic, corrResult, trainPath);
//						rank.add(trnk+"/"+dic.keySet().size());
//						Ranktermin = trnk;
//						if(!indicators.containsKey(examples.size()))
//						{
//							if(!recdic.containsKey(f.getName()))
//							{
//								HashMap<String,List<Double>> tmp = new HashMap<String,List<Double>>();
//								if(tmp.containsKey(""+examples.size()))
//								{
//									List<Double> x = tmp.get(""+examples.size());
//									x.set(0, x.get(0)+RuleUtil.sgsnum);
//									x.set(1, x.get(1)+pls.size());
//									x.set(2, x.get(2)+corrNum);
//									if(trnk<=3 && trnk>=0)
//									{
//										x.set(3, x.get(3)+1);
//										x.set(4, x.get(4)+dic.keySet().size());
//									}
//									x.set(5,x.get(5)+1);
//								}
//								else
//								{
//									List<Double> x = new ArrayList<Double>();
//									x.add(1.0*RuleUtil.sgsnum);
//									x.add(1.0*pls.size());
//									x.add(1.0*corrNum);
//									if(trnk<=3 && trnk>=0)
//									{	
//										x.add(1.0);
//										x.add(1.0*dic.keySet().size());
//									}
//									else
//									{
//										x.add(0.0);
//										x.add(0.0);
//									}
//									x.add(1.0);
//									tmp.put(""+examples.size(), x);
//								}
//								recdic.put(f.getName(), tmp);
//							}
//							else
//							{
//								HashMap<String,List<Double>> tmp = recdic.get(f.getName());
//								if(tmp.containsKey(""+examples.size()))
//								{
//									List<Double> x = tmp.get(""+examples.size());
//									x.set(0, x.get(0)+RuleUtil.sgsnum);
//									x.set(1, x.get(1)+pls.size());
//									x.set(2, x.get(2)+corrNum);
//									if(trnk<=3 && trnk>=0)
//									{
//										x.set(3, x.get(3)+1);
//										x.set(4, x.get(4)+dic.keySet().size());
//									}
//									x.set(5,x.get(5)+1);
//								}
//								else
//								{
//									List<Double> x = new ArrayList<Double>();
//									x.add(1.0*RuleUtil.sgsnum);
//									x.add(1.0*pls.size());
//									x.add(1.0*corrNum);
//									if(trnk<=3 && trnk>=0)
//									{	
//										x.add(1.0);
//										x.add(1.0*dic.keySet().size());
//									}
//									else
//									{
//										x.add(0.0);
//										x.add(0.0);
//									}
//									
//									x.add(1.0);
//									tmp.put(""+examples.size(), x);
//								}
//							}
//						}
//						indicators.put(examples.size(), true); 
//						String[] choice = UtilTools.results.get(UtilTools.index).split("\n");
//						for(int n = 0; n<choice.length;n++)
//						{
//							if(choice[n].compareTo(entries.get(n)[1])!=0)
//							{
//								wexam = entries.get(n);
//								break;
//							}
//						}
//						if(wexam!=null)
//						{
//							//if(examples.size()<=3)
//							//{
//								examples.add(wexam);
//							//}
//						}
//						names.add(f.getName());
//						exampleCnt.add(examples.size());
//						timeleng.add(timespan);
//						cRules.add(cx);
//						ranks.add(rank);
//						consisRules.add(consRule);
//					}							
//				}
//			}
//			catch(Exception ex)
//			{
//				System.out.println(""+ex.toString());
//			}
//		}
////		Random r = new Random();
//		try
//		{
//			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/bowu/mysoft/xx/logx.txt")));
//			for(int x = 0; x<names.size();x++)
//			{
//				bw.write(names.get(x)+":"+exampleCnt.get(x)+","+timeleng.get(x));
//				bw.write("\n");
//				System.out.println(names.get(x)+":"+exampleCnt.get(x)+","+timeleng.get(x));
//				bw.write("\n");
////				System.out.println(consisRules.get(x));
//			}
//			bw.flush();
//			bw.close();
//		}
//		catch(Exception ex)
//		{
//			System.out.println(""+ex.toString());
//		}
//		
//	}
	public HashMap<String,HashMap<String,List<Double>>> recdic = new HashMap<String,HashMap<String,List<Double>>>();
	public void write2CSV()
	{
		try
		{
			CSVWriter cw = new CSVWriter(new FileWriter(new File("./exper.csv")));
			Set<String> sy = recdic.keySet();
			Iterator<String> iter = sy.iterator();
			while(iter.hasNext())
			{
				String key = iter.next();
				System.out.println(""+key);
				HashMap<String,List<Double>> values = recdic.get(key);
				Set<String> ks = values.keySet();
				Iterator<String> iter1 = ks.iterator();
				while(iter1.hasNext())
				{
					String[] row = new String[8];
					String expcnt = iter1.next();
					List<Double> vs = values.get(expcnt);
					for(int j = 0; j<vs.size();j++)
					{
						row[j+2] = vs.get(j)+"";
					}
					row[0] = key;
					row[1] = expcnt;
					System.out.println(""+row);
					cw.writeNext(row);
				}
			}
			cw.flush();
			cw.close();
		}
		catch(Exception e)
		{
			System.out.println(""+e.toString());
		}
	}
	public static void main(String[] args)
	{
		Main m = new Main();
		List<Double> xy = new ArrayList<Double>();
		for(int x = 0;x <1;x++)
		{
			double st = System.currentTimeMillis();
			// TODO: Weka dependency
//			m.exper_2("/Users/bowu/Research/testdata/TestSingleFile");
			double ed = System.currentTimeMillis();
			xy.add((ed-st)*1.0/60000);
			
		}
		m.write2CSV();
		for(int i= 0; i<xy.size();i++)
		{
			System.out.println(""+xy.get(i));
		} 
		System.out.println("hello");
	}
}
