package edu.isi.karma.cleaning.features;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
public class Data2Features {
	//convert the csv file to arff file
	//return the fpath of arff file
	public static void csv2arff(String csvpath,String arfpath)
	{
		try
		{
			CSVLoader loader = new CSVLoader();
		    loader.setSource(new File(csvpath));
		    Instances data = loader.getDataSet();
		    // save ARFF
		    ArffSaver saver = new ArffSaver();
		    saver.setInstances(data);
		    saver.setFile(new File(arfpath));
		    saver.setDestination(new File(arfpath));
		    saver.writeBatch();
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
	//cpath is the original data file, label is the class label
	//
	public static List<String> data2CSVfeaturefile(String cpath,String label)
	{
		//test the class
		try
		{
			File f = new File(cpath);
			List<String> row = new ArrayList<String>();
			List<String> oexamples = new ArrayList<String>();
			List<String> examples = new ArrayList<String>();
			CSVReader re = new CSVReader(new FileReader(f), '\t');
			String[] line = null;
			re.readNext();//discard the first line
			while((line=re.readNext() )!= null)
			{
				oexamples.add(line[0]);
				examples.add(line[1]);
			}
			re.close();
			RegularityFeatureSet rf = new RegularityFeatureSet();
			Collection<Feature> cf = rf.computeFeatures(oexamples,examples);
			Feature[] x = cf.toArray(new Feature[cf.size()]);
			//row.add(f.getName());
			for(int k=0;k<cf.size();k++)
			{
				row.add(String.valueOf(x[k].getScore()));
			}
			row.add(label); // change this according to the dataset.
			return row;
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
			return null;
		}
	}
	public static void main(String[] args)
	{
		//create a new CSVfile 
		String fpath = "/Users/bowu/Research/featureValue.csv";
		String pdir = "/Users/bowu/Research/dataclean/data/RuleData/rawdata/pairs/pos";
		//String pdir = "/Users/bowu/Research/dataclean/data/RuleData/rawdata/pairs/neg";
		String ndir = "/Users/bowu/Research/dataclean/data/RuleData/rawdata/pairs/neg";
		try
		{
			CSVWriter cw = new CSVWriter(new FileWriter(new File(fpath)),',');
			//wrtie header into the csvfile
			
			//xyz[0] = "name";
			
			List<String> oexamples = new ArrayList<String>();
			List<String> examples = new ArrayList<String>();
			RegularityFeatureSet rfs = new RegularityFeatureSet();
			Collection<Feature> cols = rfs.computeFeatures(examples, oexamples);
			String[] xyz = new String[rfs.fnames.size()+1];
			for(int i = 0; i<xyz.length-1; i++)
			{
				xyz[i] ="a_"+i;
			}
			xyz[xyz.length-1] = "label";
			cw.writeNext(xyz);
			//write positive rows into csvfile
			File p = new File(pdir);
			File[] pfs = p.listFiles();
			for(int i=0; i<pfs.length; i++)
			{
				File f = pfs[i];
				if(f.getName().indexOf(".csv")==(f.getName().length()-4))
				{
					List<String> row = Data2Features.data2CSVfeaturefile(f.getAbsolutePath(), "1");
					cw.writeNext((String[])row.toArray(new String[row.size()]));
				}
			}
			//write negative rows into csvfile
			File n = new File(ndir);
			File[] nfs = n.listFiles();
			for(int i=0; i<nfs.length; i++)
			{
				File f = nfs[i];
				if(f.getName().indexOf(".csv")==(f.getName().length()-4))
				{
					List<String> row = Data2Features.data2CSVfeaturefile(f.getAbsolutePath(), "-1");
					cw.writeNext((String[])row.toArray(new String[row.size()]));
				}
			}
			cw.flush();
			cw.close();
			//convert the csv file into arff file
			String arfpath = "/Users/bowu/Research/features.arff";
			Data2Features.csv2arff(fpath, arfpath);
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
	}
}
