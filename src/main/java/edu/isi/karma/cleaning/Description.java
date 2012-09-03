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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
class Tuple
{
	//(beforetoks, aftertoks)
	private List<List<TNode>> tuple = new ArrayList<List<TNode>>();
	public Tuple(List<TNode> bef,List<TNode> aft)
	{
		tuple.add(bef);
		tuple.add(aft);
	}
	public List<TNode> getBefore()
	{
		return tuple.get(0);
	}
	public List<TNode> getafter()
	{
		return tuple.get(1);
	}
	public String toString()
	{
		List<TNode> bef = this.getBefore();
		List<TNode> aft = this.getafter();
		String orgString = "";
		String aftString = "";
		for(TNode t:bef)
		{
			orgString += t.text;
		}
		for(TNode t:aft)
		{
			aftString += t.text;
		}
		return orgString+" === "+aftString;
	}
}
public class Description 
{
	public List<List<List<HashSet<String>>>> desc = new ArrayList<List<List<HashSet<String>>>>();
	//((tup11,tup21),(tup12,tup22)....)
	public List<List<List<Tuple>>> sequences = new ArrayList<List<List<Tuple>>>();
	public Description()
	{
	}
	public Description(List<List<List<HashSet<String>>>> desc)
	{
		this.desc = desc;
	}
	public void clear()
	{
		desc.clear();
		sequences.clear();
	}
	public void delComponent(int index)
	{
		desc.remove(index);
		sequences.remove(index);
	}
	public List<List<List<HashSet<String>>>> getDesc()
	{
		return desc;
	}
	public List<List<List<Tuple>>> getSeqs()
	{
		return this.sequences;
	}
	
	public void addSeqs(List<List<Tuple>> seq)
	{
		sequences.add(seq);
	}
	public void addDesc(List<List<HashSet<String>>> seq)
	{
		desc.add(seq);
	}
	public void newDesc()
	{
		this.desc = new ArrayList<List<List<HashSet<String>>>>();
	}
	public void newSeqs()
	{
		this.sequences = new ArrayList<List<List<Tuple>>>();
	}
	public void writeJSONString() throws Exception
	{
		
//		String rep = "";
		if(this.desc.size() != this.sequences.size())
		{
			CleaningLogger.write("description toString error");
			return ;
			
		}
		JSONArray jx = new JSONArray();
		for(int i = 0; i<this.desc.size(); i++)
		{
			JSONArray jaArray = new JSONArray();
			for(int j = 0; j<this.desc.get(i).size();j++)
			{
				JSONObject jObject = new JSONObject();
				JSONObject js1 = new JSONObject();
				JSONArray js2 = new JSONArray();
				for(int k = 0; k<this.desc.get(i).get(j).size();k++)
				{
					if(k==0)
					{
						js1.put("etokenspec",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==1)
					{
						js1.put("stokenspec",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==2)
					{
						js1.put("tokenspec",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==3)
					{
						js1.put("qnum",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==4)
					{
						js1.put("snum",this.desc.get(i).get(j).get(k).toString());
					}
					else if( k ==5)
					{
						js1.put("tnum",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==6)
					{
						js1.put("dnum",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k ==7)
					{
						js1.put("dtokenspec",this.desc.get(i).get(j).get(k).toString());
					}
					else if(k==8)
					{
						js1.put("operator",this.desc.get(i).get(j).get(k).toString());
					}
				}
				for(int k = 0;k<this.sequences.get(i).get(j).size();k++)
				{
					js2.put(this.sequences.get(i).get(j).get(k).toString());
				}
				jObject.put("Description", js1);
				jObject.put("Sequence", js2);
				jaArray.put(jObject);
			}
			jx.put(jaArray);
		}
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(new File("./tmp/sgsdebug.json")));
		bWriter.write(jx.toString());//this one might cause out of heap space.
		bWriter.close();
	}
}
