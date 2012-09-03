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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class Alignment {
	//generate preprcessing editsteps before generating rules
	public static Vector<EditOper> getPreprocessingEditOpers(Vector<TNode> a,Vector<TNode> b)
	{
		Vector<EditOper> inss = Alignment.insopers(a,b);
		return inss;
	}
	//follow the framework of INS MOV DEL
	//generat all possible edit operations
	//a target token sequence b orginal token sequence
	public static Vector<Vector<EditOper>> genEditOperation(Vector<TNode> a, Vector<TNode> b)
	{
		Alignment.paths.clear();
		//generate INS operation (only need to record the content)
		/*Vector<EditOper> inss = Alignment.insopers(a,b);
		for(EditOper eo:inss)
		{
			NonterminalValidator.applyins(eo, clonea);
			eo.before = a;
			eo.after = clonea;
		}*/
		Vector<TNode> clonea = (Vector<TNode>)a.clone();
		//generate MOV and DEL operation
		Vector<Vector<EditOper>> movdels = Alignment.movopers(clonea,b);
		//generate all combination of the two set of operation sequence
		Vector<Vector<EditOper>> res = new Vector<Vector<EditOper>>();
		for(Vector<EditOper> v:movdels)
		{
			Vector<EditOper> z = new Vector<EditOper>();
			z.addAll(v);
			res.add(z);
			System.out.println(""+z.toString());
		}
		Alignment.paths.clear();
		return res;
	}
	public static Vector<Integer> delopers(Vector<int[]> a,Vector<TNode> before)
	{
		Vector<Integer> v = new Vector<Integer>();
		for(int i = 0; i< before.size(); i++)
		{
			boolean isFind = false;
			for(int j = 0 ; j < a.size();j++)
			{
				if(i == a.get(j)[0])
				{
					isFind = true;
					break;
				}
			}
			if(!isFind)
			{
				v.add(i);
			}
		}
		return v;
	}
	public static boolean isAdjacent(int a,int b, Vector<Integer> sortedmks)
	{
		if(a==b)
			return true; // <S> <E> token
		int index = sortedmks.indexOf(a); 
		if(a<=b && index<sortedmks.size()-1)
		{
			if(sortedmks.get(index+1) == b || a==b)
				return true;
			else
				return false;			
		}
		else if(a>b &&index>=1)
		{
			return false;
		}
		return false;
	}
	//mks contains the positions need to be sorted
	//q contains the unsorted positions,q contains all orignal vector's positions
	//return all the segments start and end position
	public static Vector<int[]> deterContinus(Vector<Integer> q,Vector<Integer> mks)
	{
		Vector<int[]> segments = new Vector<int[]>();
		Vector<Integer> copymks = (Vector<Integer>)mks.clone();
		Collections.sort(copymks);
		int start = 0;// the start of the segment
		int pre = start;//the previous valid element in current segment
		boolean findBegin = false;
		for(int j = 0; j<q.size();j++)
		{
			//identify the beginning
			if(mks.contains(q.get(j))&&!findBegin)
			{
				findBegin = true;
				start = j;
				pre = start;
				continue;
			}
			//proceed
			if(findBegin&&mks.contains(q.get(j))&&isAdjacent(q.get(pre),q.get(j),copymks))
			{
				pre = j;
				continue;
			}
			else if(findBegin&&mks.contains(q.get(j))&&!isAdjacent(q.get(pre),q.get(j),copymks))// identify the end of a segment
			{
				int a[] = new int[2];
				a[0]= start;
				a[1] = pre;
				start = j;
				pre = j;
				segments.add(a); 
				continue;
			}
		}
		//output the last segment
		int b[] = new int[2];
		b[0] = start;
		b[1] = pre;
		segments.add(b);
		return segments;
	}
	public static Vector<Vector<EditOper>> movopers(Vector<TNode> a, Vector<TNode> b)
	{
		int stoken = 0;
		for(TNode t:a){
			if(t.type == TNode.STARTTYP)
			{
				break;
			}
			stoken ++;
		}
		int etoken = a.size()-1;
		Vector<Vector<int[]>> mapping = map(a,b);
		Vector<Vector<EditOper>> paths = new Vector<Vector<EditOper>>();
		for(int i= 0; i< mapping.size();i++)
		{
			Vector<EditOper> ev = new Vector<EditOper>();
			Vector<int[]> tmapping = mapping.get(i);
			Vector<Integer> positions =  new Vector<Integer>();
			//record the original order
			Vector<Integer> mks = new Vector<Integer>(b.size());
			for(int  n = 0;n<b.size();n++)
			{
				mks.add(-1);
			}
			for(int n=0; n< a.size();n++) //
			{
				positions.add(n);
				//check if n is in the mapping array
				boolean isfind = false;
				for(int m = 0; m<tmapping.size();m++)
				{
					if(tmapping.get(m)[0]==n)
					{
						mks.set(tmapping.get(m)[1], n);
						break;
					}
				}
			}
			//update the positions to be order of target sequence.
			for(int q = 0; q<mks.size();q++)
			{
				int cnt = -1;
				int value = 0;
				for(int p = 0;p<positions.size();p++)
				{
					for(int k = 0;k<mks.size();k++)
					{
						if(mks.get(k)==p)
						{
							cnt ++;
							break;
						}
					}
					if(cnt == q)
					{
						value = p;
						break;
					}
				}
				for(int p = 0;p<positions.size();p++)
				{
					if(p==mks.get(q))
					{
						positions.set(p, value);
					}
				}				
			}
			/*set the s/e token's position value the same as its adjacent token's position value
			in order to make them move with the adjacent token*/
			positions.set(stoken,positions.get(stoken+1));
			positions.set(etoken,positions.get(etoken-1));
			//detect the continous segments
			Vector<int[]> segments =  deterContinus(positions,mks);
			Vector<EditOper> xx = new Vector<EditOper>();
			Vector<Vector<Integer>> history = new Vector<Vector<Integer>>();
			try 
			{
				transMOVDEL(segments,positions,mks,xx,history,a,paths);
			} catch (Exception e) {
				System.out.println("transMOVDEL error"+e.toString());
			}
		}		
		return paths;
	}
	//
	public static int getReverseOrderNum(Vector<Integer> position)
	{
		int cnt = 0;
		for(int i = 0; i<position.size(); i++)
		{
			int a = position.get(i);
			for(int j = 0; j<i; j++)
			{
				int  b = position.get(j);
				if(a<b)
				{
					cnt++;
					continue;
				}
				
			}
		}
		return cnt;
	}
	public static Vector<Vector<EditOper>> paths = new Vector<Vector<EditOper>>();
	/*
	 * segments: all the continous part
	 * position: contains the unsorted positions
	 * x == mks: all the position need to be moved
	 * eo: for record the path for one move and delete sequence
	 * history is used to store all previous state to prevent visit some visited state dead loop
	 */
	public static void transMOVDEL(Vector<int[]> segments,Vector<Integer> positon,Vector<Integer> x,Vector<EditOper> eo,Vector<Vector<Integer>> history,Vector<TNode> a,Vector<Vector<EditOper>> paths)
	{
		if(eo.size()>positon.size()-1)//prune, the number of mov should be less than total size -1 
			return;
		if(history.size()>0)
		{
			for(Vector<Integer> l:history)
			{
				if(positon.equals(l))
				{
					return;// visited before
				}
			}
		}
		Vector<Vector<Integer>> history1 = (Vector<Vector<Integer>>)history.clone(); 
		history1.add(positon);
		boolean globalcontrary = false;
		boolean localcontrary = false;
		for(int i = 0; i<segments.size(); i++)
		{
			localcontrary = false;
			int minNum = Integer.MAX_VALUE;
			EditOper eox = new EditOper();
			Vector<Integer> positonx = new Vector<Integer>();
			//move to the back
			EditOper eo2 = new EditOper();
			for(int k=i+1;k<segments.size();k++)
			{
				if(positon.get(segments.get(i)[0])>positon.get(segments.get(k)[0]))
				{
					localcontrary = true;
					globalcontrary = true;
					eo2.starPos = segments.get(i)[0];
					eo2.endPos = segments.get(i)[1];
					eo2.oper = "mov";
					eo2.dest = segments.get(k)[1]+1;//after the first segment
				}
				
			}
			if(localcontrary)
			{
				Vector<Integer> positon2 = (Vector<Integer>)positon.clone();
				//update the position2
				List<Integer> tmp2 = positon.subList(eo2.starPos, eo2.endPos+1);
				positon2.removeAll(tmp2);
				//insert first then delete
				if((eo2.dest-tmp2.size())>=positon2.size())
				{
					positon2.addAll(tmp2);
				}
				else {
					positon2.addAll(eo2.dest-tmp2.size(), tmp2);
				}
				positonx = positon2;
				eox = eo2;
			}
			
			//move to the front
			/*for(int k=0;k<i;k++)
			{
				if(positon.get(segments.get(i)[0])<positon.get(segments.get(k)[0]))
				{
					localcontrary = true;
					globalcontrary = true;
					EditOper eo1 = new EditOper();
					eo1.starPos = segments.get(i)[0];
					eo1.endPos = segments.get(i)[1];
					eo1.oper = "mov";
					eo1.dest = segments.get(k)[0];// before the first segment
					//Vector<EditOper> seq1 = (Vector<EditOper>)eo.clone();
					Vector<Integer> positon1 = (Vector<Integer>)positon.clone();
					//seq1.add(eo1);
					List<Integer> tmp1 = positon.subList(eo1.starPos, eo1.endPos+1);
					//update positon1 array
					positon1.removeAll(tmp1);
					positon1.addAll(eo1.dest, tmp1);
					int score = Alignment.getReverseOrderNum(positon1);
					if(score <minNum)
					{
						minNum = score;
						eox = eo1;
						positonx = positon1;
					}
				}
			}*/
			if(localcontrary)
			{
				Vector<EditOper> seqx = (Vector<EditOper>)eo.clone();
				seqx.add(eox);			
				Vector<int[]> newsegments1 = deterContinus(positonx,x);
				transMOVDEL(newsegments1,positonx,x,seqx,history1,a,paths);
			}
		}
		if(!globalcontrary)
		{
			//add the delete operation
			int start = 0;
			int cnt = 0;//offset the influence of deleted element
			boolean started = false;
			for(int h = 0; h<positon.size();h++)
			{
				//start of segment
				if(!started&&!x.contains(positon.get(h))&&(a.get(positon.get(h)).type != TNode.STARTTYP)&&(a.get(positon.get(h)).type != TNode.ENDTYP))
				{
					start = h;
					started = true;
					continue;
				}
				else if(started&&x.contains(positon.get(h)))//reach the end
				{
					EditOper deo = new EditOper();
					deo.starPos = start-cnt;
					deo.endPos = h-1-cnt;
					deo.oper = "del";
					eo.add(deo);
					started = false;
					cnt += h-1-start+1;
					continue;
				}
			}
			//output the last segment
			if(started)
			{
				EditOper deo = new EditOper();
				deo.oper = "del";
				deo.starPos = start-cnt;
				deo.endPos = positon.size()-1-cnt;
				eo.add(deo);
				started = false;
			}
			paths.add(eo);
			return; // ordered and do need to sort now
		}
		return;
	}
	public static Vector<EditOper> insopers(Vector<TNode> a, Vector<TNode> b)
	{
		Vector<EditOper> eo = new Vector<EditOper>();
		boolean[] marks = new boolean[a.size()];
		for(int i= 0;i<marks.length;i++)
		{
			marks[i] = false;
		}
		for(int i=0;i<b.size();i++)
		{
			boolean isFind = false;
			for(int j = 0; j<a.size();j++)
			{
				if(b.get(i).sameNode(a.get(j))&&!marks[j])
				{
					marks[j]= true;
					isFind= true;
					break;
				}
			}
			if(!isFind)
			{
				//create a new insertion operation
				EditOper eox= new EditOper();
				eox.oper = "ins";
				eox.dest = 0;
				b.get(i).setColor(TNode.INSCOLOR);//set color for inserted tnodes.
				eox.tar.add(b.get(i));
				eo.add(eox);
			}
		}
		return eo;
	}
	//merge continouse operation together
	public static Vector<Vector<int[]>> mergeOperation(Vector<Vector<int[]>> res)
	{
		return null;
	}
	public static void alignment(Vector<AlignObj> a,Vector<AlignObj> b,boolean[] aind,boolean[] bind,String path,HashSet<String> res)
	{
		boolean isend = true;
		for(int i = 0; i< a.size(); i++)
		{
			for(int j = 0; j<b.size(); j++)
			{
				if(aind[i]&&bind[j]&&a.get(i).tNode.sameNode(b.get(j).tNode))
				{
					isend = false;
					String xString = path;
					String subs = "#"+a.get(i).index+","+b.get(j).index;
					int xind = xString.indexOf("#");
					//to remove rudun like 1,1 0,0 and 0,0 1,1
					while(xind!= -1 && a.get(i).index>Integer.parseInt(xString.substring(xind+1,xind+2)))
					{
						xind = xString.indexOf("#", xind+1);
					}
					if(xind == -1)
					{
						xString +=subs;
					}
					else 
					{
						xString = xString.substring(0, xind)+subs+xString.substring(xind);
					}
					System.out.println(""+xString);
					aind[i]=false;
					bind[j]=false;
					alignment(a, b, aind, bind, xString,res);
					aind[i]=true;
					bind[j]=true;
				}
			}	
		}
		if(isend)
		{
			//handle the rest blank spaces
			if(!res.contains(path))
			{
				res.add(path);
			}
				
		}
	}
	public static Vector<Vector<int[]>> map(Vector<TNode> a,Vector<TNode> b)
	{
		Vector<Vector<int[]>> res = new Vector<Vector<int[]>>();
		HashMap<String, Vector<AlignObj>> dic = new HashMap<String, Vector<AlignObj>>();
		HashMap<String, Vector<AlignObj>> revdic = new HashMap<String, Vector<AlignObj>>();
		String blankmapping = "";
		boolean[] aind = new boolean[a.size()];
		for(int i=0; i<aind.length;i++)
		{
			aind[i] = true;
		}
		boolean[] bind = new boolean[b.size()];
		for(int i =0; i<bind.length; i++)
		{
			bind[i] = true;
		}
		for(int i = 0; i<a.size(); i++)
		{
			for(int j=0;j<b.size();j++)
			{
				TNode mNode = a.get(i);
				TNode nNode = b.get(j);
				if(mNode.sameNode(nNode)&&aind[i]&&bind[j])
				{
					if(a.get(i).text.compareTo(" ")==0)
					{
						//if left side nodes are same
						if(i-1>=0 && j-1>=0)
						{
							if(!a.get(i-1).sameNode(b.get(j-1)))
							{
								continue;
							}
						}
						if(i+1<a.size() && j+1<b.size())//if right side nodes are same.
						{
							if(!a.get(i+1).sameNode(b.get(j+1)))
							{
								continue;
							}
						}
						blankmapping += "#"+i+","+j;
						aind[i] = false;
						bind[j] = false;
					}
					else 
					{
						
						String key = mNode.toString();
						if(dic.containsKey(key))
						{
							AlignObj aObj = new AlignObj(nNode, j);
							Vector<AlignObj> vao = dic.get(key);
							boolean isrun = false;
							for(int k = 0; k<vao.size(); k++)
							{
								if(vao.get(k).index == j)
								{
									isrun = true;
								}
							}
							if(!isrun)
								dic.get(key).add(aObj);
						}
						else {
							Vector<AlignObj> vec = new Vector<AlignObj>();
							AlignObj aObj = new AlignObj(nNode, j);
							vec.add(aObj);
							dic.put(key, vec);
						}
						if(revdic.containsKey(key))
						{
							AlignObj aObj = new AlignObj(mNode, i);
							Vector<AlignObj> vao = revdic.get(key);
							boolean isrun = false;
							for(int k = 0; k<vao.size(); k++)
							{
								if(vao.get(k).index == i)
								{
									isrun = true;
								}
							}
							if(!isrun)
								revdic.get(key).add(aObj);
						}
						else {
							Vector<AlignObj> vec = new Vector<AlignObj>();
							AlignObj aObj = new AlignObj(mNode, i);
							vec.add(aObj);
							revdic.put(key, vec);
						}
					}
				}
			}
		}
		//generate non-ambiguious mapping
		Set<String> keys = dic.keySet();
		Iterator<String> it = keys.iterator();
		String mappingprefix = "";
		while(it.hasNext())
		{
			String k = it.next();
			if(dic.get(k).size()==1&&revdic.get(k).size()==1)
			{
				mappingprefix +="#"+revdic.get(k).get(0).index+","+dic.get(k).get(0).index;
			}
		}
		//generate blank space mapping
		for(int i=0;i<a.size();i++)
		{
			Vector<int[]> tmp = new Vector<int[]>();
			for(int j=0; j<b.size(); j++)
			{
				//check whether two whitespaces are counterpart
				if(a.get(i).sameNode(b.get(j))&&a.get(i).text.compareTo(" ")==0&&aind[i]&&bind[j])
				{
					blankmapping +="#"+i+","+j;
					aind[i] = false;
					bind[j] = false;
				}
			}
		}
		String theprefix = mappingprefix+blankmapping;		
		//generate ambiguious mapping
		Vector<String> allpathes=new Vector<String>();
		allpathes.add(theprefix);
		Iterator<String> it1 = keys.iterator();
		while(it1.hasNext())
		{
			String k = it1.next();
			if((dic.get(k).size()>1||revdic.get(k).size()>1))
			{
				if(dic.get(k).get(0).tNode.type != TNode.BNKTYP)
				{
					String path = "";
					Vector<AlignObj> x1= dic.get(k);
					Vector<AlignObj> y1=revdic.get(k);
					HashSet<String> pathes = new HashSet<String>();
					boolean[] xind = new boolean[x1.size()];
					for(int i = 0; i<xind.length;i++)
					{
						xind[i] = true;
					}
					boolean[] yind = new boolean[y1.size()];
					for(int i = 0; i<yind.length;i++)
					{
						yind[i] = true;
					}
					alignment(y1,x1,yind,xind, path, pathes);
								
					int cnt = allpathes.size();
					while(cnt>0)
					{
						String pString = allpathes.elementAt(0);
						Iterator<String> ks = pathes.iterator();	
						while(ks.hasNext())
						{
							allpathes.add(pString+ks.next());
						}
						allpathes.remove(0);
						cnt--;
					}				
				}
			}
			
		}
		Iterator<String> iter = allpathes.iterator();
		while(iter.hasNext())
		{
			String p = iter.next().trim();
			if(p.length()==0)
				continue;
			Vector<int[]> line = new Vector<int[]>();
			String[] mps = p.trim().split("#");
			for(String str:mps)
			{
				String string = str.trim();
				if(string.length()==0)
					continue;
				String[] t = string.split(",");
				int[] q = {Integer.parseInt(t[0]),Integer.parseInt(t[1])};
				line.add(q);
			}
			res.add(line);
		}
		return res;
	}	
	/****************************************************************/
	
	// this method should be able to detect the operation range of three
	// kinds of operations "delete, insert, Move" '
	// this method could also be wrong, so the best would be this alignment 
	//just give weak supervision, the later component would adjust it self to learn the right rule
	public static Vector<EditOper> alignment1(Vector<TNode> a, Vector<TNode> b)
	{
		HashMap<Integer,Integer> res = new HashMap<Integer,Integer>();
		int matrix[][] = new int[a.size()+1][b.size()+1];// the first row and column is kept for empty
		// initialize the first row and column
		int stepCost = 1;
		for(int i=0;i<a.size()+1;i++)
		{
			for(int j=0;j<b.size()+1;j++)
			{
				if(i==0)
				{
					matrix[i][j] = j;
					continue;
				}
				if(j==0)
				{
					matrix[i][j] = i;
					continue;
				}
				int cost =stepCost;
				if(a.get(i-1).sameNode(b.get(j-1)))
				{
					cost = 0;
					matrix[i][j] = Math.min(matrix[i-1][j-1]+cost, Math.min(matrix[i-1][j]+1, matrix[i][j-1]+1));
				}
				else // No substitution
				{
					matrix[i][j] =  Math.min(matrix[i-1][j]+1, matrix[i][j-1]+1);
				}
			}
		}
		//find the edit operations and use move to replace the del and ins as many as possible
		int ptr1 = a.size();
		int ptr2 = b.size();
		//use the alignment to derive the edit operations
		Vector<EditOper> editv = new Vector<EditOper>();
		HashMap<String,Vector<Integer>> dtmp = new HashMap<String,Vector<Integer>>();
		HashMap<String,Vector<Integer>> itmp = new HashMap<String,Vector<Integer>>();
		// 0th column and 0th row always represents the epsilon
		while(!(ptr1==0&&ptr2==0))
		{
			int value = matrix[ptr1][ptr2];
			//search the three directions  
			if(ptr1-1>=0 && ptr2-1>=0 && matrix[ptr1-1][ptr2-1] == value && a.get(ptr1-1).sameNode(b.get(ptr2-1)))// matched
			{
				ptr1 -= 1;
				ptr2 -= 1;
			}
			else if(ptr1-1>=0 && matrix[ptr1-1][ptr2] == value-1) //del the node
			{
				
				EditOper eo = new EditOper();
				eo.oper = "del";
				eo.starPos = ptr1-1;
				eo.endPos = ptr1-1;
				editv.add(eo);
				if(dtmp.containsKey(a.get(ptr1-1).text))
				{
					dtmp.get(a.get(ptr1-1).text).add(editv.size()-1);
				}
				else
				{
					Vector<Integer> ax = new Vector<Integer>();
					ax.add(editv.size()-1); // add the current index into the sequence
					dtmp.put(a.get(ptr1-1).text, ax);
				}
				ptr1 -= 1;
			}
			else if(ptr2-1>=0 && matrix[ptr1][ptr2-1] == value-1) //ins a node
			{
				
				EditOper eo = new EditOper();
				eo.oper = "ins";
				Vector<TNode> t = new Vector<TNode>();
				t.add( b.get(ptr2-1));
				eo.tar = t;
				eo.dest = ptr1;
				editv.add(eo);
				if(itmp.containsKey(b.get(ptr2-1).text))
				{
					itmp.get(b.get(ptr2-1).text).add(editv.size()-1);
				}
				else
				{
					Vector<Integer> ax = new Vector<Integer>();
					ax.add(editv.size()-1); // add the current index into the sequence
					itmp.put(b.get(ptr2-1).text, ax);
				}
				ptr2 -= 1;
			}
		}
		//replace the del ins of same symbol with mov operation
		Vector<Object> rv = new Vector<Object>();
		
		for(String x:dtmp.keySet())
		{
			if(itmp.containsKey(x))
			{
				//p and q are the index of the edit operation sequene the bigger, the latter
				Vector<Integer> q = dtmp.get(x);
				Vector<Integer> p = itmp.get(x);
				Iterator<Integer> t1 = q.iterator();//delete
				Iterator<Integer> t2 = p.iterator();//insert
				while(t1.hasNext()&&t2.hasNext())
				{
					Integer i1 = t1.next();
					Integer i2 = t2.next();
					if(i1>i2)
					{
						//update the two operation with mov operation
						editv.get(i2).oper = "mov";
						int delcnt = 0;
						for(int m=0;m<editv.size();m++)
						{
							if((editv.get(m).oper.compareTo("ins")==0||editv.get(m).oper.compareTo("mov")==0)&& m< i2)
								delcnt ++;
						}
						editv.get(i2).dest = editv.get(i2).dest-delcnt;
						editv.get(i2).starPos = editv.get(i1).starPos;
						editv.get(i2).endPos = editv.get(i1).endPos;
						//editv.remove(y);
						rv.add(editv.get(i1));
						t1.remove();
						t2.remove();
					}
					else
					{
						editv.get(i2).oper = "mov";
						editv.get(i2).starPos = editv.get(i1).starPos;
						editv.get(i2).endPos = editv.get(i1).endPos;
						//editv.remove(z);
						rv.add(editv.get(i1));
						t1.remove();
						t2.remove();
					}
				}
			}
		}
		for(Object o:rv)
		{
			editv.remove(o);
		}
		//merge the continuous same type of operations
		Vector<EditOper> newEo = new Vector<EditOper>();
		int pre = 0;
		for(int ptr = 1; ptr <editv.size();ptr++)
		{
			EditOper eo = editv.get(ptr);
			if(eo.oper.compareTo(editv.get(pre).oper)==0)
			{
				if(eo.oper.compareTo("mov")==0)
				{
					if(((eo.starPos-1==editv.get(ptr-1).starPos&&eo.starPos>=eo.dest)||(eo.starPos+1==editv.get(ptr-1).starPos&&eo.starPos<=eo.dest)) && eo.dest == editv.get(ptr-1).dest)
					{
						if(ptr<editv.size()-1)
							continue;
						else
						{
							ptr = ptr+1;
						}
					}
					if(ptr-pre>1) // need to merge
					{
						EditOper e = editv.get(ptr-1);
						Vector<TNode> x = new Vector<TNode>();
						e.starPos = editv.get(pre).starPos<=e.starPos?editv.get(pre).starPos:e.starPos;
						e.endPos = editv.get(pre).endPos>=e.endPos?editv.get(pre).endPos:e.endPos;
						for(int k = pre;k<ptr;k++)
						{
							x.addAll(editv.get(k).tar); // concate the tar tokens
						}
						e.tar = x;
						newEo.add(e);
						continue;
					}
					newEo.add(editv.get(pre));
					pre = ptr;	
					continue;
				}
				else if(eo.oper.compareTo("ins")==0)
				{
					if(eo.dest == editv.get(ptr-1).dest)
					{
						if(ptr<editv.size()-1)
							continue;
						else
						{
							ptr = ptr+1;
						}
					}
					if(ptr-pre>1) // need to merge
					{
						EditOper e = editv.get(ptr-1);
						Vector<TNode> x = new Vector<TNode>();
						e.endPos = editv.get(pre).endPos;
						for(int k = pre;k<ptr;k++)
						{
							x.addAll(editv.get(k).tar); // concate the tar tokens
						}
						e.tar = x;
						newEo.add(e);
						continue;
					}
					newEo.add(editv.get(pre));
					pre = ptr;
					continue;
				}
				else if(eo.oper.compareTo("del")==0)
				{
					if(eo.starPos+1==editv.get(ptr-1).starPos)
					{
						if(ptr<editv.size()-1)
							continue;
						else
						{
							ptr = ptr+1;
						}
					}				
					if(ptr-pre>1) // need to merge
					{
						EditOper e = editv.get(ptr-1);
						Vector<TNode> x = new Vector<TNode>();
						e.endPos = editv.get(pre).starPos;
						for(int k = pre;k<ptr;k++)
						{
							x.addAll(editv.get(k).tar); // concate the tar tokens
						}
						e.tar = x;
						newEo.add(e);
						continue;
					}
					newEo.add(editv.get(pre));
					pre = ptr;
					continue;
				}
			}
			newEo.add(editv.get(pre));
			pre = ptr;
		}
		if(pre== editv.size()-1)
			newEo.add(editv.get(pre));
		return newEo;
	}
	//used to generate template candidate tree for sampling
	public static void generateSubtemplate(HashMap<String,Vector<GrammarTreeNode>> container)
	{
		
	}
	public static Vector<int[]> getParams(Vector<TNode> org,Vector<TNode> tar)
	{
		//specially for del,  
		int optr = 0;
		int tptr= 0;
		boolean start = false;
		int a[] = new int[2];
		Vector<int[]> poss = new Vector<int[]>();
		while(optr<org.size() && tptr<tar.size())
		{
			if(org.get(optr).sameNode(tar.get(tptr)))
			{
				optr ++ ;
				tptr ++ ;
				if(start)
				{
					a[1] = optr-1;
					start = false;
					poss.add(a);
				}
			}
			else
			{
				if(!start)
				{
					a = new int[2];
					a[0]=optr;
					start = true;
				}
				optr ++ ;
			}
		}
		if(tptr == tar.size() && optr<org.size())
		{
			int p[] = {tptr,org.size()-1};
			poss.add(p);
		}
		return poss;
	}
	public static Vector<int[]> getParams(String sorg,String star)
	{
		Ruler ru = new Ruler();
		ru.setNewInput(sorg);
		Ruler ru1 = new Ruler();
		ru1.setNewInput(star);
		Vector<TNode> org = ru.vec;
		Vector<TNode> tar = ru1.vec;
		//specially for del,  
		int optr = 0;
		int tptr= 0;
		boolean start = false;
		int a[] = new int[2];
		Vector<int[]> poss = new Vector<int[]>();
		while(optr<org.size() && tptr<tar.size())
		{
			if(org.get(optr).sameNode(tar.get(tptr)))
			{	
				if(start)
				{
					a[1] = optr-1;
					start = false;
					poss.add(a);
				}
				optr ++ ;
				tptr ++ ;
			}
			else
			{
				if(!start)
				{
					a = new int[2];
					a[0]=optr;
					start = true;
				}
				optr ++ ;
			}
		}
		if(tptr == tar.size() && optr<org.size())
		{
			int p[] = {tptr,org.size()-1};
			poss.add(p);
		}
		return poss;
	}
}
class AlignObj
{
	public TNode tNode;
	public int index;
	public AlignObj(TNode t,int index)
	{
		tNode = t;
		this.index = index;
	}
}
class Comparator1 implements Comparator<EditOper>{
	public int compare(EditOper x,EditOper y)
	{
		if(x.dest > y.dest)
		{
			return 1;
		}
		else
		{
			return -1;
		}
	}
}
