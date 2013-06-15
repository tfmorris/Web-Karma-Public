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

import java.util.ArrayList;
import java.util.List;

public class FeatureVector {
	String[] symbol = {"#",";",",","!","~","@","$","%","^","&","*","(",")","_","-","{","}","[","]","\"","'",":","?","<",">","."};
	int[] types = {TNode.NUMTYP,TNode.SYBSTYP,TNode.LWRDTYP,TNode.UWRDTYP};
	List<RecFeature> x = new ArrayList<RecFeature>();
	public int size;
	
	public FeatureVector()
	{
		
		
	}
	public int size()
	{
		return symbol.length+types.length+1;
	}
	public List<RecFeature> createList(String raw,String color)
	{
		List<RecFeature> v = new ArrayList<RecFeature>();
		Ruler r = new Ruler();
		r.setNewInput(raw);
		List<TNode> vt = new ArrayList<TNode>();
		vt = r.vec;
		constructList(vt,color,v);
		return v;
	}
	public void constructList(List<TNode> t,String color,List<RecFeature> v)
	{
		
		for(String s:symbol)
		{
			Feature1 feature1 = new Feature1(s, t);
			v.add(feature1);
		}
		for(int type:types)
		{
			Feature2 feature2 = new Feature2(type, t);
			v.add(feature2);
		}
		Feature3 feature3 = new Feature3(color);
		v.add(feature3);
	}
}
