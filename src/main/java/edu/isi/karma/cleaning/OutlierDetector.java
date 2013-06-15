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

import java.util.HashMap;
import java.util.List;

public class OutlierDetector {
	public HashMap<String,double[]> rLists = new HashMap<String,double[]>();
	public double currentMax = -1;
	public OutlierDetector()
	{
		
	}
	public double getDistance(double[] x,double[] y)
	{
		if(x.length != y.length){
			return Double.MAX_VALUE;
		}
		double value = 0.0;
		for(int i = 0; i<x.length; i++)
		{
			value += Math.pow(x[i]-y[i], 2);
		}
		return Math.sqrt(value);
	}
	// find outliers for one partition
	//simple 2d distance
	//testdata rowid:{tar, tarcolor}
	public String getOutliers(HashMap<String,String[]> testdata,double[] meanList,double Max)
	{
		String Id = "";
		for(String key: testdata.keySet())
		{
			String[] vpair = testdata.get(key);
			FeatureVector fvFeatureVector = new FeatureVector();
			List<RecFeature> vRecFeatures = fvFeatureVector.createList(vpair[0], vpair[1]);
			double[] x = new double[fvFeatureVector.size()];
			for(int i = 0; i< vRecFeatures.size(); i++)
			{
				x[i] = vRecFeatures.get(i).computerScore();
			}
			double value = this.getDistance(x, meanList);
			/*System.out.println("current: "+ vpair[0]+ " "+Max);
			System.out.println("=======\n"+this.test(x)+"\n"+this.test(meanList));
			System.out.println("distance: "+value);*/
			if(value > Max)
			{
				Max = value;
				this.currentMax = Max;
				Id = key;
			}
		}
		return Id;
	}
	// pid: [{rawstring, code}]
	public void buildMeanList(HashMap<String,List<String[]>> data)
	{
		if(data == null){
			return;
		}
		for(String key:data.keySet())
		{
			List<String[]> vs = data.get(key);
			FeatureVector fList = new FeatureVector();
			double[] dvec = new double[fList.size()];
			for (int i = 0; i < dvec.length; i++) {
				dvec[i] = 0;
			}
			for(String[] elem:vs)
			{
				List<RecFeature> sFeatures = fList.createList(elem[0], elem[1]);
				for(int j = 0; j<sFeatures.size();j++)
				{
					dvec[j] += sFeatures.get(j).computerScore();
				}
			}
			//get average size
			for(int i = 0; i<dvec.length; i++)
			{
				dvec[i] = dvec[i]*1.0/ vs.size();
			}
			rLists.put(key, dvec);
		}
	}
	public String test(double[] row)
	{
		String string="";
		for(double d:row)
		{
			string += d+",";
		}
		return string.substring(1,string.length()-1);
	}

}
