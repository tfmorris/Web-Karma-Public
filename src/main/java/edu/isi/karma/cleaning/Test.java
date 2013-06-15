package edu.isi.karma.cleaning;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class Test {
	public static void test1()
	{
		List<String[]> examples = new ArrayList<String[]>();
		String[] xStrings = {"<_START>Jan<_END>", ""};
		String[] yStrings = {"<_START>Feb<_END>", "02"};
		String[] zStrings = {"<_START>Mar<_END>", "03"};
		examples.add(xStrings);
		examples.add(yStrings);
		examples.add(zStrings);
		ProgSynthesis psProgSynthesis = new ProgSynthesis();
		psProgSynthesis.inite(examples);
		List<ProgramRule> pls = new ArrayList<ProgramRule>();
		Collection<ProgramRule> ps = psProgSynthesis.run_main();
		ProgramRule pr = ps.iterator().next();
		String val = "Jan";
		String val2 = "Feb";
		InterpreterType rule = pr.getRuleForValue(val);
		System.out.println(rule.execute(val));
		InterpreterType rule1 = pr.getRuleForValue(val2);
		System.out.println(rule1.execute(val2));
		
	}
	public static void test4(String dirpath) {
		HashMap<String, List<String>> records = new HashMap<String, List<String>>();
		File nf = new File(dirpath);
		File[] allfiles = nf.listFiles();
		// statistics
		DataCollection dCollection = new DataCollection();
		// list all the csv file under the dir
		for (File f : allfiles) {
			List<String[]> examples = new ArrayList<String[]>();
			List<String[]> addExamples = new ArrayList<String[]>();
			List<String[]> entries = new ArrayList<String[]>();
			try {
				
				if (f.getName().indexOf(".csv") == (f.getName().length() - 4)) {
					HashMap<String, String[]> xHashMap = new HashMap<String, String[]>();
					CSVReader cr = new CSVReader(new FileReader(f), ',','"','\0');
					String[] pair;
					int index = 0;
					while ((pair = cr.readNext()) != null) {
						if (pair == null || pair.length <= 1)
							break;
						entries.add(pair);
						String[] line = {pair[0],pair[1],"","","wrong"}; // org, tar, tarcode, label
						xHashMap.put(index + "", line);
						index++;
					}
					if (entries.size() <= 1)
						continue;
					ExampleSelection expsel = new ExampleSelection();
					expsel.firsttime = true;
					expsel.inite(xHashMap,null);
					int target = Integer.parseInt(expsel.Choose());
					String[] mt = {
							"<_START>" + entries.get(target)[0] + "<_END>",
							entries.get(target)[1] };
					examples.add(mt);
					while (true) // repeat as no correct answer appears.
					{
						long checknumber = 1;
						HashMap<String, List<String[]>> expFeData = new HashMap<String, List<String[]>>();
						List<String> resultString = new ArrayList<String>();
						xHashMap = new HashMap<String, String[]>();
						ProgSynthesis psProgSynthesis = new ProgSynthesis();
						psProgSynthesis.inite(examples);
						List<ProgramRule> pls = new ArrayList<ProgramRule>();
						Collection<ProgramRule> ps = psProgSynthesis.run_main();
						if (ps != null)
							pls.addAll(ps);
						else {
							System.out.println("Cannot find any rule");
						}
						String[] wexam = null;
						if (pls.size() == 0)
							break;
						long t1 = System.currentTimeMillis();
						
						for (int i = 0; i < pls.size(); i++) {
							ProgramRule script = pls.get(i);
							// System.out.println(script);
							String res = "";
							for (int j = 0; j < entries.size(); j++) {
								InterpreterType worker = script
										.getRuleForValue(entries.get(j)[0]);
								String classlabel = script.getClassForValue(entries.get(j)[0]);
								String tmps = worker.execute_debug(entries.get(j)[0]);
								HashMap<String, String> dict = new HashMap<String, String>();
								UtilTools.StringColorCode(entries.get(j)[0], tmps, dict);
								String s = dict.get("Tar");
								res += s+"\n";
								if (ConfigParameters.debug == 1)
									System.out.println("result:   " + dict.get("Tardis"));
								if (s == null || s.length() == 0) {
									String[] ts = {"<_START>" + entries.get(j)[0] + "<_END>","",tmps,classlabel,"wrong"};
									xHashMap.put(j + "", ts);
									wexam = ts;
									checknumber ++;
								}
								boolean isfind = false;
								for(String[] exppair:examples)
								{
									if(exppair[0].compareTo("<_START>"+dict.get("Org")+"<_END>")==0)
									{
										String[] exp = {s,tmps};
										if(!expFeData.containsKey(classlabel))
										{
											List<String[]> vstr = new ArrayList<String[]>();
											vstr.add(exp);
											expFeData.put(classlabel, vstr);
										}
										else
										{
											expFeData.get(classlabel).add(exp);
										}
										isfind = true;
									}
								}
								//update positive traing data with user specification
								for (String[] tmpx : addExamples) {
									if(tmpx[0].compareTo(dict.get("Org"))==0 && tmpx[1].compareTo(dict.get("Tar"))==0)
									{
										String[] exp = {s,tmps};
										if(!expFeData.containsKey(classlabel))
										{
											List<String[]> vstr = new ArrayList<String[]>();
											vstr.add(exp);
											expFeData.put(classlabel, vstr);
										}
										else
										{
											expFeData.get(classlabel).add(exp);
										}
										isfind = true;
									}
								}
								if (!isfind) {
									String[] ts = {"<_START>" + entries.get(j)[0] + "<_END>",s,tmps,classlabel,"right"};
									if(s.compareTo(entries.get(j)[1]) != 0) 
									{
										wexam = ts;
										ts[4] = "wrong";
									}
									xHashMap.put(j + "", ts);			
								}
							}
							if (wexam == null)
								break;
							resultString.add(res);
						}
						records.put(f.getName()+examples.size(), resultString);
						long t2 = System.currentTimeMillis();
						
						if (wexam != null) {
							String[] wexp = new String[2];							
							while(true)
							{
								expsel = new ExampleSelection();
								expsel.inite(xHashMap,expFeData);
								int e = Integer.parseInt(expsel.Choose());
								if(xHashMap.get(""+e)[4].compareTo("right")!=0)
								{
									wexp[0] = "<_START>" + entries.get(e)[0] + "<_END>";
									wexp[1] = 	entries.get(e)[1];
									break;
								}
								else
								{
									//update positive training data
									addExamples.add(entries.get(e));
									//update the rest dataset
									xHashMap.remove(""+e);
								}
								checknumber ++;
							}
							examples.add(wexp);
							FileStat fileStat = new FileStat(f.getName(),
									psProgSynthesis.learnspan,
									psProgSynthesis.genspan, (t2 - t1),
									examples.size(), examples,
									psProgSynthesis.ruleNo,checknumber, pls.get(0).toString());
							dCollection.addEntry(fileStat);
						} else {
							FileStat fileStat = new FileStat(f.getName(),
									psProgSynthesis.learnspan,
									psProgSynthesis.genspan, (t2 - t1),
									examples.size(), examples,
									psProgSynthesis.ruleNo,checknumber, pls.get(0).toString());
							dCollection.addEntry(fileStat);
							break;
						}
						
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		dCollection.print();
		dCollection.print1();
		//hashResultPrint(records);
	}
	public static void hashResultPrint(HashMap<String, List<String>> res)
	{
		String s = "";
		for(String key:res.keySet())
		{
			s += "=============="+key+"=============\n";
			for(String value: res.get(key))
			{
				s += value+"\n";
			}
		}
		System.out.println(""+s);
	}
	public static void test0_sub(List<String[]> all, List<String[]> cand,
			List<String[]> examples, int cnt) {
		if (cand.size() <= 0) {
			if (cnt > Test.MaximalNumber) {
				MaximalNumber = cnt;
				Test.larexamples = examples;
			}
			if (cnt < Test.MinimalNumber) {
				MinimalNumber = cnt;
				Test.smalexamples = examples;
			}
			//System.out.println("returned");
			return;
		}
		for (int p = 0; p < cand.size(); p++) {
			List<String[]> tmp = new ArrayList<String[]>();
			tmp.addAll(examples);
			String[] x = { "<_START>" + cand.get(p)[0] + "<_END>",
					cand.get(p)[1] };
			tmp.add(x);
			List<String[]> tmpxStrings = new ArrayList<String[]>();

			ProgSynthesis psProgSynthesis = new ProgSynthesis();
			psProgSynthesis.inite(tmp);
			List<ProgramRule> pls = new ArrayList<ProgramRule>();
			Collection<ProgramRule> ps = psProgSynthesis.run_main();
			if (ps != null)
				pls.addAll(ps);
			String[] wexam = null;
			if (pls.size() == 0)
				break;
			for (int i = 0; i < pls.size(); i++) {
				ProgramRule script = pls.get(i);
				for (int j = 0; j < all.size(); j++) {
					InterpreterType worker = script
							.getRuleForValue(all.get(j)[0]);
					String s = worker.execute(all.get(j)[0]);
					//System.out.println("result:   " + s);
					if (s == null || s.length() == 0) {
						wexam = all.get(j);
						String[] ep = { "<_START>" + wexam[0] + "<_END>",
								wexam[1] };
						tmpxStrings.add(ep);
						continue;
					}
					if (s.compareTo(all.get(j)[1]) != 0) {
						wexam = all.get(j);
						String[] ep = { "<_START>" + wexam[0] + "<_END>",
								wexam[1] };
						tmpxStrings.add(ep);
						continue;
					}
				}

			}
			test0_sub(all, tmpxStrings, tmp, cnt + 1);
		}
	}

	public static int MaximalNumber = -1;
	public static int MinimalNumber = 100;
	public static List<String[]> larexamples = new ArrayList<String[]>();
	public static List<String[]> smalexamples = new ArrayList<String[]>();

	public static void test0(String dirpath) {
		File nf = new File(dirpath);
		File[] allfiles = nf.listFiles();
		// statistics
		DataCollection dCollection = new DataCollection();
		// list all the csv file under the dir
		for (File f : allfiles) {
			List<String[]> examples = new ArrayList<String[]>();
			List<String[]> entries = new ArrayList<String[]>();
			try {
				if (f.getName().indexOf(".csv") == (f.getName().length() - 4)) {
					CSVReader cr = new CSVReader(new FileReader(f), ',', '"',
							'\0');
					String[] pair;
					while ((pair = cr.readNext()) != null) {
						if (pair == null || pair.length <= 1)
							break;
						entries.add(pair);
					}
					if (entries.size() <= 1)
						continue;
					int cnt = 0;
					List<String[]> candStrings = new ArrayList<String[]>();
					candStrings.addAll(entries);
					test0_sub(entries, candStrings, examples, cnt);
					System.out.println("File " + f.getName() + "\n");
					System.out.println("Max: " + Test.MaximalNumber);
					System.out.println("Min: " + Test.MinimalNumber);
					String str = "Larget number of Examples:\n";
					for (int x = 0; x < Test.larexamples.size(); x++) {
						str += String.format("exp: %s, %s\n",
								larexamples.get(x)[0], larexamples.get(x)[1]);
					}
					System.out.println("Largest: " + str);
					String str1 = "Smallest number of Examples:\n";
					for (int x = 0; x < Test.smalexamples.size(); x++) {
						str1 += String.format("exp: %s, %s\n",
								smalexamples.get(x)[0], smalexamples.get(x)[1]);
					}
					System.out.println("Smallest: " + str1);
					//clear
					Test.MaximalNumber = -1;
					Test.larexamples = new ArrayList<String[]>();
					Test.MinimalNumber = 200;
					Test.smalexamples = new ArrayList<String[]>();
				}
				//
			} catch (Exception e) {
				System.out.println("" + e.toString());
			}
		}
	}

	public static void main(String[] args) {
		// load parameters
		ConfigParameters cfg = new ConfigParameters();
		cfg.initeParameters();
		DataCollection.config = cfg.getString();
		//Test.test0("/Users/bowu/Research/testdata/TestSingleFile");
		Test.test4("/Users/bowu/Research/testdata/TestSingleFile");
		//Test.test1();
	}
}
