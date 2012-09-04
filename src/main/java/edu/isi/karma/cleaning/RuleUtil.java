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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import au.com.bytecode.opencsv.CSVReader;
import edu.isi.karma.cleaning.changed_grammar.INSInterpreterLexer;
import edu.isi.karma.cleaning.changed_grammar.INSInterpreterParser;
import edu.isi.karma.cleaning.changed_grammar.INSInterpreterTree;
import edu.isi.karma.cleaning.changed_grammar.MOVInterpreterLexer;
import edu.isi.karma.cleaning.changed_grammar.MOVInterpreterParser;
import edu.isi.karma.cleaning.changed_grammar.MOVInterpreterTree;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterLexer;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterParser;
import edu.isi.karma.cleaning.changed_grammar.RuleInterpreterTree;

public class RuleUtil {
	public static String tokens2str(List<TNode> x)
	{
		String resString = "";
		for(TNode t:x)
		{
			resString += t.text;
		}
		return resString;
	}
	public static String tokens2strwithStyle(List<TNode> x)
	{
		String resString = "";
		for(TNode t:x)
		{
			resString += "<span class='"+t.getColor()+"'>"+t.text+"</span>";
		}
		return resString;
	}
	public  static void write2file(Collection<String> x,String fname)
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
	// 
	public static List<String> applyRuleF(List<String> rules,String fpath0)
	{
		try
		{
			List<String> res = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new FileReader(fpath0));
			String line = "";
			while((line = br.readLine())!= null)
			{
				if(line.length() == 0){
					continue;
				}
				line = "%"+line+"@";
				System.out.println(""+line);
				line = RuleUtil.applyRule(rules, line);
				res.add(line);
			}
			br.close();
			return res;
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
			return null;
		}
	}
	//apply a sequence of rules
	public static String applyRulewithStyle(List<String> rules, String s)
	{
		try
		{
		Ruler r = new Ruler();
		r.setNewInput(s);
		List<TNode> x = r.vec;
		/*value preprocessing starts*/
		for(EditOper eo:preEditOpers)
		{
			if (eo.oper.compareTo("ins") == 0) {
				NonterminalValidator.applyins(eo, x);
			}
		}
		/*value preprocessing ends*/
		for(String rule:rules)
		{
			x = applyRule(rule,x);
		}
		return RuleUtil.tokens2strwithStyle(x);
		}
		catch(Exception e)
		{
			//System.out.println(""+e.toString());
			return "";
		}
	}
	public static String applyRule(List<String> rules, String s)
	{
		try
		{
		Ruler r = new Ruler();
		r.setNewInput(s);
		List<TNode> x = r.vec;
		/*value preprocessing starts*/
		for(EditOper eo:preEditOpers)
		{
			if (eo.oper.compareTo("ins") == 0) {
				NonterminalValidator.applyins(eo, x);
			}
		}
		/*value preprocessing ends*/
		for(String rule:rules)
		{
			x = applyRule(rule,x);
		}
		return RuleUtil.tokens2str(x);
		}
		catch(Exception e)
		{
			//System.out.println(""+e.toString());
			return "";
		}
	}
	//apply a rule on a token sequence and return a token sequence
	public static List<TNode> applyRule(String rule,List<TNode> before)
	{
		try 
		{
			CharStream cs =  new ANTLRStringStream(rule);
			Ruler r = new Ruler();
			//decide which class of rule interpretor apply
			rule = rule.trim();
			if(rule.indexOf("mov")==0)
			{
				MOVInterpreterLexer lexer = new MOVInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);	        
		        MOVInterpreterParser parser= new MOVInterpreterParser(tokens);
		        CommonTree x = (CommonTree)parser.rule().getTree();
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream(x);
		        nodes.setTokenStream(tokens);
		        MOVInterpreterTree evaluator = new MOVInterpreterTree(nodes);  
		        r.setNewInput(before);
		        evaluator.setRuler(r);
		        evaluator.rule();
		        
			}
			else if(rule.indexOf("del")==0)
			{
				RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        nodes.setTokenStream(tokens);
		        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
		        r.setNewInput(before);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
			else if(rule.indexOf("ins")==0)
			{
				INSInterpreterLexer lexer = new INSInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        INSInterpreterParser parser= new INSInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        nodes.setTokenStream(tokens);
		        INSInterpreterTree evaluator = new INSInterpreterTree(nodes);
		        r.setNewInput(before);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
	        return r.vec;
			
		} catch (Exception e) {
			//System.out.println(""+e.toString());
			return null;
		}
		
	}
	//apply a rule on String and return a token sequence
	public static List<TNode> applyRulet(String rule,String s)
	{
		try 
		{
			CharStream cs =  new ANTLRStringStream(rule);
			Ruler r = new Ruler();
			//decide which class of rule interpretor apply
			rule = rule.trim();
			if(rule.indexOf("mov")==0)
			{
				MOVInterpreterLexer lexer = new MOVInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);	        
		        MOVInterpreterParser parser= new MOVInterpreterParser(tokens);
		        CommonTree x = (CommonTree)parser.rule().getTree();
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream(x);
		        MOVInterpreterTree evaluator = new MOVInterpreterTree(nodes);  
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
		        
			}
			else if(rule.indexOf("del")==0)
			{
				RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
			else if(rule.indexOf("ins")==0)
			{
				INSInterpreterLexer lexer = new INSInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        INSInterpreterParser parser= new INSInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        INSInterpreterTree evaluator = new INSInterpreterTree(nodes);
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
	        return r.vec;
			
		} catch (Exception e) {
			return null;
		}
	}
	//apply a rule on a string
	public static String applyRuleS(String rule,String s)
	{
		try 
		{
			CharStream cs =  new ANTLRStringStream(rule);
			Ruler r = new Ruler();
			//decide which class of rule interpretor apply
			rule = rule.trim();
			if(rule.indexOf("mov")==0)
			{
				MOVInterpreterLexer lexer = new MOVInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);	        
		        MOVInterpreterParser parser= new MOVInterpreterParser(tokens);
		        CommonTree x = (CommonTree)parser.rule().getTree();
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream(x);
		        MOVInterpreterTree evaluator = new MOVInterpreterTree(nodes);  
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
		        
			}
			else if(rule.indexOf("del")==0)
			{
				RuleInterpreterLexer lexer = new RuleInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        RuleInterpreterParser parser= new RuleInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        RuleInterpreterTree evaluator = new RuleInterpreterTree(nodes);
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
			else if(rule.indexOf("ins")==0)
			{
				INSInterpreterLexer lexer = new INSInterpreterLexer(cs);
		        CommonTokenStream tokens = new CommonTokenStream(lexer);
		        INSInterpreterParser parser= new INSInterpreterParser(tokens);
		        CommonTreeNodeStream nodes = new CommonTreeNodeStream((CommonTree)parser.rule().getTree());
		        INSInterpreterTree evaluator = new INSInterpreterTree(nodes);
		        r.setNewInput(s);
		        evaluator.setRuler(r);
		        evaluator.rule();
			}
	        return r.toString();
			
		} catch (Exception e) {
			return "";
		}
		
	}
	public static String applyRule(String rule, String fpath)
	{
		 ResultViewer rv = new ResultViewer();
		 try
		 {
			
			String xline = "";
			File f = new File(fpath);
			BufferedReader xbr = new BufferedReader(new FileReader(f));
			while((xline=xbr.readLine())!=null)
			{
				List<String> xrow = new ArrayList<String>();
				if(xline.compareTo("")==0){
					break;
				}
				String s = RuleUtil.applyRuleS(rule, xline);
		        System.out.println(s);
		        xrow.add(xline);
			    xrow.add(s);
			    //xrow.add(r.toString());
				rv.addRow(xrow);
			}
			xbr.close();
			rv.print(f.getAbsolutePath()+"_pair.txt");
			return f.getAbsolutePath()+"_pair.txt";
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
			return "";
		}	 
	}
	public static List<List<String>> getLitervalue(List<TNode> example, int sPos, int ePos,RuleGenerator gen)
	{
		//tokenize the example
		//identify the variance part
		int length = ePos - sPos +1; // the deleted number of Tnodes
		List<String> wNum = new ArrayList<String>();
		wNum.add(String.valueOf(length));
		//wNum.add("1"); // all the token to be deleted
		List<String> wToken = new ArrayList<String>();
		wToken = gen.replacetokspec(gen.printRules("tokenspec",(ePos-sPos)), example, sPos, ePos);
		//remove
		List<String> sNum = new ArrayList<String>();
		sNum.add(String.valueOf(sPos+1));
		sNum.add("0");
		sNum.add(String.valueOf(example.size()-sPos-1));
		List<String> sToken = new ArrayList<String>();
		sToken.add("\""+example.get(sPos).text+"\"");
		List<String> eNum = new ArrayList<String>();
		eNum.add(String.valueOf(ePos));
		eNum.add("0");
		eNum.add(String.valueOf(example.size()-ePos-1));
		List<String> eToken = new ArrayList<String>();
		eToken.add("\""+example.get(ePos).text+"\"");
		List<List<String>> vs = new ArrayList<List<String>>();
		vs.add(wNum);
		vs.add(wToken);
		vs.add(sNum);
		vs.add(sToken);
		vs.add(eNum);
		vs.add(eToken);
		return vs;
	}
	public static void filter(String nonterm, List<String> rules,List<TNode> org,List<TNode> tar,List<EditOper> eos)
	{
		return;
	}
	public static int sgsnum = 0;
	//ops is corresponding editoperation of multiple sequence
	public static List<EditOper> preEditOpers = new ArrayList<EditOper>();
	public static List<String> genRule(List<String[]> examples)
	{	
//		List<String> rules = new ArrayList<String>();
		try
		{	
			List<List<TNode>> org = new ArrayList<List<TNode>>();
			List<List<TNode>> tar = new ArrayList<List<TNode>>();
			for(int i =0 ; i<examples.size();i++)
			{
				Ruler r = new Ruler();
				r.setNewInput(examples.get(i)[0]);
				org.add(r.vec);
				Ruler r1 = new Ruler();
				r1.setNewInput(examples.get(i)[1]);
				tar.add(r1.vec);
			}
			/*examples preprocessing starts*/
			preEditOpers.clear();
			preEditOpers = Alignment.getPreprocessingEditOpers(org.get(0), tar.get(0));
			for(int i= 0; i<examples.size();i++)
			{
				for (EditOper eo : preEditOpers) {
					if (eo.oper.compareTo("ins") == 0) {
						NonterminalValidator.applyins(eo, org.get(i));
					}
				}
			}
			/*example preprocessing ends*/
			List<List<GrammarParseTree>> trees = RuleUtil.genGrammarTrees(org, tar);
			sgsnum = trees.size();
			List<Integer> l = new ArrayList<Integer>();
			List<Integer> sr = new ArrayList<Integer>();
			List<String> pls = new ArrayList<String>();
			for(List<GrammarParseTree> gt:trees)
			{
				l.add(gt.size());
				sr.add(1);
			}
			int lhod = 8;
			int uhod = 50;
			int deadend = 0;
			if(l.size()*0.5>lhod &&l.size()*0.5<uhod){
				deadend = (int) (l.size()*0.5);
			}else if(l.size()*0.5<lhod){
				deadend = lhod;
			}else{
				deadend = uhod;
			}
			for(int c=0; c<deadend;c++)
			{
				int index = MarkovDP.sampleByScore(l,sr);
				//Random r = new Random();
				//int index = r.nextInt(1);
				List<GrammarParseTree> gt = trees.get(index);
				System.out.print(gt.size()+","+index+"\n");	
				HashMap<MDPState,MDPState> his = new HashMap<MDPState,MDPState>();
				int sccnt = 0;
				for(int ct = 0;ct <200;ct++)
				{
					MarkovDP mdp = new MarkovDP(org,tar,gt);
					mdp.setHistory(his);
					mdp.run();
					if(mdp.isValid())
					{
						pls.add(mdp.getPolicies());
						sccnt += 1;
					}
				}
				sr.set(index, sccnt);
			}
			return pls;
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
			return null;
		}
	}
	
	public static List<List<List<EditOper>>> genEditOpers(List<List<TNode>> orgs,List<List<TNode>> tars) throws Throwable
	{
		List<List<List<EditOper>>> tmp =new ArrayList<List<List<EditOper>>>();
		for(int i=0;i<orgs.size();i++)
		{
			List<TNode> x = orgs.get(i);
			List<TNode> y = tars.get(i);
			List<List<EditOper>> ops = Alignment.genEditOperation(x, y);//ops contains multiple edit sequence
			List<List<EditOper>> tx = new ArrayList<List<EditOper>>();
			for(int j = 0; j<ops.size();j++)
			{
//				String sign = "";
				List<TNode> cur = x;
				for(EditOper xeo:ops.get(j))
				{
//					sign+=xeo.oper;
					xeo.before = cur;
					Ruler r = new Ruler();
					r.setNewInput(new ArrayList<TNode>(cur));
					if(xeo.oper.compareTo("del")==0)
					{
						r.doOperation("del", "1", null, xeo.starPos, xeo.endPos);
					}
					else if(xeo.oper.compareTo("mov")==0)
					{
						r.doOperation("mov", ""+xeo.dest, null, xeo.starPos, xeo.endPos);
					}
					else if(xeo.oper.compareTo("ins")==0)
					{
						r.doOperation("ins", "1", xeo.tar, xeo.dest, 0);
					}
					xeo.after = new ArrayList<TNode>(r.vec);
					cur = r.vec;
				}
				tx.add(ops.get(j));
			}
			tmp.add(tx);
		}
		return tmp;
	}
	//edit operation number is the same
	//edit operation type is the same
	//move direction is the same
	//param: curSeqs edit sequence 
	//return: the signature of current  edit sequence
	public static String getSign(List<EditOper> curSeq)
	{
		String sign = "";
		//size
		int size = curSeq.size();
		sign = String.valueOf(size);
		// type and direction
		for(EditOper eo:curSeq)
		{
			sign += eo.oper;
			if(eo.oper.compareTo("mov")==0)
			{
				//find the direction left or right
				if(eo.dest>eo.endPos)
				{
					sign += "";
				}
				else if(eo.dest<eo.starPos)
				{
					sign += "";
				}
			}
		}
		return sign;
	}
	public static List<List<GrammarParseTree>> genGrammarTrees(List<List<TNode>> orgs,List<List<TNode>> tars)
	{
		List<List<List<EditOper>>> tmp = new ArrayList<List<List<EditOper>>>();
		try
		{	
			tmp = genEditOpers(orgs,tars);
		}
		catch(Throwable ex)
		{
			System.out.println("genEditOpers error: "+ex.toString());
		}
		// do cross join between the editsequence between different examples
		//if number of operation is not the same continue
		//       (one edit)
		HashMap<String,GrammarTreeNode> global_temp = new HashMap<String,GrammarTreeNode>();
		List<List<List<HashSet<String>>>> descriptions = new ArrayList<List<List<HashSet<String>>>>();//store description for multiple sequence
		Description dcrpt = new Description();
		List<List<List<Tuple>>> sequences = new ArrayList<List<List<Tuple>>>();
		List<String> sign = new ArrayList<String>();
		HashSet<String> hs = new HashSet<String>();
		for(int i=0;i<tmp.size();i++)
		{
			List<String> newsign = new ArrayList<String>();
			List<List<EditOper>> curSeqs = tmp.get(i);
			if(descriptions.size() == 0)
			{		
				for(List<EditOper> eos: curSeqs)
				{
					List<List<Tuple>> seq = new ArrayList<List<Tuple>>();
					List<TNode> p = new ArrayList<TNode>(orgs.get(i));
					List<List<HashSet<String>>> seqscurExamp = new ArrayList<List<HashSet<String>>>(); //store the description for one editsequence 
					for(EditOper eo:eos)//
					{
						List<Tuple> vt = new ArrayList<Tuple>();
						vt.add(new Tuple(eo.before,eo.after));
						seq.add(vt);
						List<HashSet<String>> allParams = new ArrayList<HashSet<String>>();//store the all description for one operation
						HashSet<String> set1 = NonterminalValidator.genendendContext(eo, p);
						for(String s: set1)
						{
							NonterminalValidator.genTemplate("etokenspec", s, global_temp);
						}
						allParams.add(set1);
						HashSet<String> set2 = NonterminalValidator.genstartContext(eo, p);
						for(String s: set2)
						{
							NonterminalValidator.genTemplate("stokenspec", s, global_temp);
						}
						allParams.add(set2);
						HashSet<String> set3 = NonterminalValidator.generateWhatTemp(eo, p);
						for(String s: set3)
						{
							NonterminalValidator.genTemplate("tokenspec", s, global_temp);
						}
						allParams.add(set3);
						HashSet<String> set4 = NonterminalValidator.genNum(eo, p);
						for(String s: set4)
						{
							NonterminalValidator.genTemplate("qnum", s, global_temp);
						}
						allParams.add(set4);
						HashSet<String> set5 = NonterminalValidator.genPostion(eo, p, "start");
						for(String s: set5)
						{
							NonterminalValidator.genTemplate("snum", s, global_temp);
						}

						allParams.add(set5);
						HashSet<String> set6 = NonterminalValidator.genPostion(eo, p, "end");
						for(String s: set6)
						{
							NonterminalValidator.genTemplate("tnum", s, global_temp);
						}
						allParams.add(set6);
						HashSet<String> set7 = NonterminalValidator.genPostion(eo, p, "dest");
						for(String s: set7)
						{
							NonterminalValidator.genTemplate("dnum", s, global_temp);
						}
						allParams.add(set7);
						HashSet<String> set8 = NonterminalValidator.gendestContext(eo, p);
						for(String s: set8)
						{
							NonterminalValidator.genTemplate("dtokenspec", s, global_temp);
						}
						allParams.add(set8);
						//add the name of the operater 
						HashSet<String> set9 = new HashSet<String>();
						set9.add(eo.oper);
						allParams.add(set9);
						NonterminalValidator.applyoper(eo, p, eo.oper);		
						seqscurExamp.add(allParams);
					}
					if(hs.contains(seqscurExamp.toString()))
					{
						continue;
					}
					else
					{
						newsign.add(RuleUtil.getSign(eos));
						dcrpt.addDesc(seqscurExamp);
						dcrpt.addSeqs(seq);
						hs.add(seqscurExamp.toString());
					}
				}
			}
			else
			{		
				for(int j = 0;j<descriptions.size(); j++)
				{
					//iterate through all possible edit sequence for current example
					for(int l = 0; l<curSeqs.size();l++)
					{
						//signature are the same
						if(RuleUtil.getSign(curSeqs.get(l)).compareTo(sign.get(j))==0)
						{
							List<TNode> p = new ArrayList<TNode>(orgs.get(i));
							List<List<HashSet<String>>> seqscurExamp = new ArrayList<List<HashSet<String>>>();
							//used to keep the edit history
							List<List<Tuple>> seq = new ArrayList<List<Tuple>>();
							//clone the history
							for(int k = 0;k<sequences.get(j).size(); k++)
							{
								seq.add(new ArrayList<Tuple>(sequences.get(j).get(k)));
							}
							//List<List<Tuple>> seq = (List<List<Tuple>>)sequences.get(j).clone();
							for(int m = 0; m<curSeqs.get(l).size();m++)
							{
								//add before and after here
								seq.get(m).add(new Tuple(curSeqs.get(l).get(m).before,curSeqs.get(l).get(m).after));
								List<HashSet<String>> allParams = new ArrayList<HashSet<String>>();
								HashSet<String> set1 = NonterminalValidator.genendendContext(curSeqs.get(l).get(m), p);
								set1.retainAll(descriptions.get(j).get(m).get(0));
								System.out.println(set1);
								allParams.add(set1);
								HashSet<String> set2 = NonterminalValidator.genstartContext(curSeqs.get(l).get(m), p);
								set2.retainAll(descriptions.get(j).get(m).get(1));
								allParams.add(set2);
								HashSet<String> set3 = NonterminalValidator.generateWhatTemp(curSeqs.get(l).get(m), p);
								set3.retainAll(descriptions.get(j).get(m).get(2));
								allParams.add(set3);							
								HashSet<String> set4 = NonterminalValidator.genNum(curSeqs.get(l).get(m), p);
								set4.retainAll(descriptions.get(j).get(m).get(3));
								allParams.add(set4);
								HashSet<String> set5 = NonterminalValidator.genPostion(curSeqs.get(l).get(m), p, "start");
								set5.retainAll(descriptions.get(j).get(m).get(4));
								allParams.add(set5);
								HashSet<String> set6 = NonterminalValidator.genPostion(curSeqs.get(l).get(m), p, "end");
								set6.retainAll(descriptions.get(j).get(m).get(5));
								allParams.add(set6);
								HashSet<String> set7 = NonterminalValidator.genPostion(curSeqs.get(l).get(m), p, "dest");
								set7.retainAll(descriptions.get(j).get(m).get(6));
								allParams.add(set7);
								HashSet<String> set8 = NonterminalValidator.gendestContext(curSeqs.get(l).get(m), p);
								set8.retainAll(descriptions.get(j).get(m).get(7));
								allParams.add(set8);
								HashSet<String> set9 = new HashSet<String>();
								set9.add(curSeqs.get(l).get(m).oper);
								allParams.add(set9);
								NonterminalValidator.applyoper(curSeqs.get(l).get(m), p,curSeqs.get(l).get(m).oper);
								seqscurExamp.add(allParams);
							}
							if(hs.contains(seqscurExamp.toString()))
							{
								continue;
							}
							else
							{
								dcrpt.addDesc(seqscurExamp);	
								dcrpt.addSeqs(seq);
								newsign.add(RuleUtil.getSign(curSeqs.get(l)));
								hs.add(seqscurExamp.toString());
							}
						}
					}
				}		
			}
			sign = newsign;
			descriptions = dcrpt.getDesc();
			sequences = dcrpt.getSeqs();
			dcrpt.newDesc();
			dcrpt.newSeqs();
			hs.clear();//
			//descriptions = filterDescription(descriptions,sign);
		}
		dcrpt.sequences = sequences;
		dcrpt.desc = descriptions;
		try {
			dcrpt.writeJSONString();
		} catch (Exception e) {
		}
		
		//descriptions = filterDescription(descriptions,sign); // many descriptoins
		//prepare three kind of rule generator
		List<List<GrammarParseTree>> gps = new ArrayList<List<GrammarParseTree>>();
		//inite parse before all the object are initialized
		List<List<List<Tuple>>> seqs = dcrpt.getSeqs();
		GrammarParseTree.initGrammarParserTrees();
		int validCnt = 0;
		for(int i=0; i<dcrpt.getDesc().size(); i++)
		{
			List<List<HashSet<String>>> curSeq = dcrpt.getDesc().get(i);
			List<List<Tuple>> seq = seqs.get(i);
			// iterates through all the operations
			List<GrammarParseTree> tmptrees = new ArrayList<GrammarParseTree>();		
			boolean isvalid = true;
			for(int j=0; j<curSeq.size();j++)
			{
				List<HashSet<String>> oper = curSeq.get(j);
				String type = oper.get(8).iterator().next();
				
				GrammarParseTree gt = new GrammarParseTree(type);
				gt.setExample(seq.get(j));//set the before and after for this edit component
				isvalid = gt.initalSubtemplete(oper,global_temp);
				if(!isvalid){
					break;
				}
				tmptrees.add(gt);
				//gt.diagPrint();
			}
			if(isvalid)
			{
				gps.add(tmptrees);
				System.out.println("======="+i+"/"+descriptions.size()+"===========");
				validCnt ++ ;
			}
			else
			{
				//System.out.println("==INVALID=="+i+"/"+descriptions.size()+"===========");
			}
		}
		System.out.println("Valid Count: "+validCnt);
		//generate grammar tree sequence from a sequence of descriptions			
		return gps;
	}
	// filter  descriptions
	public static void filter(Description desc)
	{
		int i = 0;
		List<List<List<HashSet<String>>>> dsc = desc.getDesc();
		while(i < dsc.size())
		{
			List<List<HashSet<String>>> seq = dsc.get(i);//get one sequence
			boolean isvalid = true;
			//iter through all edit operation
			for(List<HashSet<String>> s:seq)
			{
				if((s.get(0).size()==0&&s.get(4).size()==0)||(s.get(1).size()==0&&s.get(3).size()==0)||(s.get(6).size()==0&&s.get(7).size()==0))
				{
					isvalid = false;
					break;
				}
			}
			if(isvalid)
			{
				desc.delComponent(i);
			}
			else
			{
				i++;
			}
		}
	}
	public static void main(String[] args)
	{
		try
		{
			String fp = "/Users/bowu/Research/dataclean/data/RuleData/FullChange.csv";
			CSVReader cr = new CSVReader(new FileReader(new File(fp)),'\t');
			String x[];
			List<List<TNode>> orgs = new ArrayList<List<TNode>>();
			List<List<TNode>> tars = new ArrayList<List<TNode>>();
			String[] exmp = {"2011-07-09","07/09/2011"};
			while((x=cr.readNext())!=null)
			{
				Ruler r1 = new Ruler();
				Ruler r2 = new Ruler();
				String s1 = x[0];
				r1.setNewInput(s1);
				String s2 = x[1];
				r2.setNewInput(s2);
				System.out.println(s1+" ===== "+s2);
				//System.out.println(Alignment.alignment1(r1.vec, r2.vec).toString());
				//List<List<EditOper>> res = Alignment.genEditOperation(r1.vec,r2.vec);
				// prepare description for sub tree
				List<TNode> p = r1.vec;
				orgs.add(p);
				List<TNode> q = r2.vec;
				tars.add(q);
			}
			cr.close();
			List<List<GrammarParseTree>> trees = RuleUtil.genGrammarTrees(orgs, tars);
			List<List<String>> ress = new ArrayList<List<String>>();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/bowu/mysoft/restransit.txt")));
			int corrNum = 0;
			int cnt = 10;
			List<int[]> resut = new ArrayList<int[]>();
			for(int b = 1; b<21; b++)
			{
				corrNum = 0;
				for(List<GrammarParseTree> ts:trees)
				{
					if(ts.size()>=7){
						continue;
					}
					cnt = 0;
					while(cnt < b*10)
					{
						String tar = exmp[0];
						List<String> res = new ArrayList<String>();
						List<String> ruls = new ArrayList<String>();
						for(GrammarParseTree t:ts)
						{
							GrammarTreeNode gn = new GrammarTreeNode("");
							t.buildTree("rule", gn);
							t.root = gn;
							String r = t.toString();
							tar = RuleUtil.applyRuleS(r, tar);
							res.add(tar);
							ruls.add(r);
							if(tar.compareTo(exmp[1])==0)
							{
								corrNum++;
							}
						}
						ress.add(res);
						bw.write(res.toString()+"\n");
						bw.write(ruls.toString()+"\n");
						cnt ++ ;
					}
				}
				int[] elem = {b*10,corrNum};
				resut.add(elem);
			}
			for(int[] xx:resut)
			{
				System.out.println(xx[0]+" "+xx[1]+"\n");
			}
			bw.close();
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
}
