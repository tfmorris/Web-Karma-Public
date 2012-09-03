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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;
public class Ruler {
	String Org = "";
	String Trgt = "";
	StringTokenizer st = null;
	String[] seperator = {" ",","};
	public List<TNode> vec;
	int curPos = 0;
	List<Object[]> operators = new ArrayList<Object[]>();
	List<Integer> positions;
	List<TNode> whats;
	List<Integer> consPos;
	public Ruler()
	{
		positions = new ArrayList<Integer>();
		consPos = new ArrayList<Integer>();
		whats = new ArrayList<TNode>();
		
	}
	public Ruler(String x)
	{
		positions = new ArrayList<Integer>();
		consPos = new ArrayList<Integer>();
		whats = new ArrayList<TNode>();
		this.initConstantPosition();
		vec = new ArrayList<TNode>();
		Org = x;
		tokenize();
	}
	public void initConstantPosition()
	{
		String target = ",";
		for(int i =0; i<vec.size();i++)
		{
			if(vec.get(i).text.compareTo(target)==0)
			{
				this.consPos.add(i);
			}
		}
		
	}
	public void setNewInput(String x)
	{
		this.Org = x;
		this.Trgt = "";
		this.vec = new ArrayList<TNode>();
		this.curPos = 0;
		this.tokenize();
		this.initConstantPosition();
	}
	public void setNewInput(List<TNode> x)
	{
		this.Org = x.toString();
		this.Trgt = "";
		this.vec = new ArrayList<TNode>();
		this.curPos = 0;
		this.vec = x;
		this.initConstantPosition();
	}
	public List<TNode> parsewhat(String input)
	{
//		String sep1 = "||";
		String sep2 = " ";
		StringTokenizer st1 = new StringTokenizer(input,sep2);
		List<TNode> vecx = new ArrayList<TNode>();
		while(st1.hasMoreTokens())
		{
			String tks = st1.nextToken();
			String[] t = tks.split("\\|\\|");
			int type = -1;
			if(t[0].compareTo("ANYTYP")==0)
			{
				type = TNode.ANYTYP;
			}
			else if(t[0].compareTo("NUMTYP")==0)
			{
				type = TNode.NUMTYP;
			}
			else if(t[0].compareTo("Word")==0)
			{
				type = TNode.WRDTYP;
			}
			else if(t[0].compareTo("Symbol")==0)
			{
				type = TNode.SYBSTYP;
			}
			else if(t[0].compareTo("Blank")==0)
			{
				type = TNode.BNKTYP;
			}
			String cnt = "";
			if(t[1].compareTo("ANYTOK")==0)
			{
				cnt = TNode.ANYTOK;
			}
			else
			{
				cnt = t[1].substring(1,t[1].length()-1);
			}
			vecx.add(new TNode(type,cnt));
		}
		return vecx;
	}
	//in current data,search the position of the tvec
	public static int search(List<TNode> xvec,List<TNode> tvec,int bpos)
	{
		boolean isFind = false;
		int p1 = -1;
		for(int t = bpos;t<xvec.size()-tvec.size()+1;t++)
		{
			p1 = t;
			for(int x = 0; x<tvec.size();x++)
			{
				int p2 = x;
				if(xvec.get(p1).sameNode(tvec.get(p2)))
				{
					p1++;
				}
				else
				{
					isFind = false;
					break;
				}
				isFind = true;
			}
			if(isFind)
			{
				return t;
			}
		}
		return -1;
	}
	//evalPos()
	public int evalPos(String input,List<TNode> t, String option)
	{
		boolean incld = false;
		if(input.contains("first"))
		{
			if(!input.contains("incld"))
			{
				incld = false;
			}
			else
			{
				incld = true;
			}
			//int pos1 = this.Search(this.vec,t,0);
			if(option.compareTo("from_beginning")==0)
			{
				int pos = search(vec,t, 0);
				if(pos == -1)
					return -1;
				if(incld)
				{
					return pos;
				}
				else
				{
					if(pos<vec.size())
						if(pos>0)
						{
							return pos-1;
						}
						else {
							return 0;
						}
						
					else
						return vec.size()-1;
				}
				
			}
			else
			{
				List<TNode> tmpvec = new ArrayList<TNode>(this.vec);
				Collections.reverse(tmpvec);
				int pos = search(tmpvec,t, 0);
				if(pos == -1)
					return -1;
				if(incld)
				{
					if(this.vec.size()- pos-1>=0 && this.vec.size()- pos-1 <= vec.size())
						return this.vec.size()- pos-1;
					else
						return 0;
				}
				else
				{
					if(this.vec.size()- pos>=0 && this.vec.size()- pos <= vec.size())
						return this.vec.size()- pos;
					else
						return 0;
				}
			}
		}
		/*LSA to do*/
		else
		{
			if(option.compareTo("from_beginning")==0)
			{
				return Integer.parseInt(input)-1;
			}
			else
			{
				return this.vec.size()-Integer.parseInt(input);
			}
		}
	}
	public int parseStart(String input)
	{
		boolean incld = false;
		if(input.contains("FST"))
		{
			if(!input.contains("incld"))
			{
				input = input.substring(9);
				incld = false;
			}
			else
			{
				input = input.substring(10);
				incld = true;
			}
			List<TNode> t = this.parsewhat(input);
			int pos1 = search(this.vec,t,0);
			if(incld)
			{
				return pos1;
			}
			else
			{
				return pos1+1;
			}
		}
		/*else if(input.contains("LST"))
		{
			input = input.substring(4);
			List<TNode> x = this.parsewhat(input);
			int pos1 = this.Search(x,0);
			return pos1;
		}*/
		else
		{
			return Integer.parseInt(input);
		}
	}
	public int parseEnd(String input)
	{
		boolean incld = false;
		if(input.contains("FST"))
		{
			if(!input.contains("incld"))
			{
				input = input.substring(9);
				incld = false;
			}
			else
			{
				incld = true;
				input = input.substring(10);
			}
			List<TNode> t = this.parsewhat(input);
			List<TNode> tmpvec = new ArrayList<TNode>(this.vec);
			Collections.reverse(tmpvec);
			int pos = search(tmpvec,t, 0);
			if(pos == -1)
				return 0;
			if(incld)
			{
				return this.vec.size()- pos;
			}
			else
			{
				return this.vec.size()- pos-1;
			}
		}
		/*else if(input.contains("LST"))
		{
			
		}*/
		else
		{
			return this.vec.size() - Integer.parseInt(input);
		}
	}
	/*public int parseQuantifier(String input)
	{
		int quan = Integer.parseInt(input);
		if(quan == -1)
		{
			return TNode.ANYNUM;
		}
		else
		{
			return quan;
		}
	}
	public void ParseParameters(HashMap<String,String> hm)
	{
		String oper = "";
		int quan = 0;
		int startpos = -1;
		int endpos = 1000;
		List<TNode> pat = new ArrayList<TNode>();
		if(hm.containsKey("what"))
		{
			pat = this.parsewhat(hm.get("what"));
		}
		if(hm.containsKey("operator"))
		{
			oper = hm.get("operator");
		}
		if(hm.containsKey("qnum"))
		{
			quan = this.parseQuantifier(hm.get("qnum"));
		}
		if(hm.containsKey("start"))
		{
			startpos = this.parseStart(hm.get("start"));
		}
		if(hm.containsKey("end"))
		{
			endpos = this.parseEnd(hm.get("end"));
		}
		if(oper.compareTo("del")==0)
		{
			this.det(quan, pat, startpos, endpos);
		}
	}*/
	public void addOperators(Object[][] opers)
	{
		for(int j = 0; j<opers.length;j++)
		{
			operators.add(opers[j]);
		}
	}
	//seperate by , and " "
	//
	public void tokenize()
	{
		CharStream cs =  new ANTLRStringStream(Org);
		Tokenizer tk = new Tokenizer(cs);
		Token t;
		t = tk.nextToken();
		while(t.getType()!=-1)
		{
			int mytype = -1;
			String txt = "";
			if(t.getType()==Tokenizer.WORD)
			{
				mytype = TNode.WRDTYP;
				txt = t.getText();
			}
			else if(t.getType() == Tokenizer.BLANK)
			{
				mytype = TNode.BNKTYP;
				txt = t.getText();
			}
			else if(t.getType() == Tokenizer.NUMBER)
			{
				mytype 	= TNode.NUMTYP;
				txt = t.getText();
			}
			else if(t.getType() == Tokenizer.SYBS)
			{
				mytype = TNode.SYBSTYP;
				txt = t.getText();
			}
			else if(t.getType() == Tokenizer.START)
			{
				mytype = TNode.STARTTYP;
				txt = "";
			}
			else if(t.getType() == Tokenizer.END)
			{
				mytype = TNode.ENDTYP;
				txt = "";
			}
			TNode tx = new TNode(mytype,txt);
			vec.add(tx);
			//System.out.println("cnt: "+t.getText()+" type:"+t.getType());
			t = tk.nextToken();
		}
	}
	public void applyrule()
	{
		/*for(int i = 0; i< operators.size(); i++)
		{
			Object[] oper = operators.get(i);
			if(Integer.parseInt((String)oper[2])<0)
			{
				move(Integer.parseInt((String)oper[0]), (TNode)oper[1], Integer.parseInt((String)oper[2]));
			}
			else if(Integer.parseInt((String)oper[2])>=0)
			{
				det(Integer.parseInt((String)oper[0]), (TNode)oper[1], Integer.parseInt((String)oper[2]));
			}
		}*/
	}
	
	public static void main(String[] args)
	{
		Object[][] opers = {{"0",new TNode(4,","),"0"}};
		//generate the ruler
		
		String fpath = "/Users/bowu/Research/dataclean/data/d1.txt";
		String fpath1 = "/Users/bowu/Research/dataclean/data/td1.txt";
		String fpath2 = "/Users/bowu/Research/dataclean/data/d1r.txt";
		BufferedReader br = null;
		BufferedWriter bw1 = null;
		BufferedWriter bw2 = null;
		try
		{
			br = new BufferedReader(new FileReader(fpath));
			bw1= new BufferedWriter(new FileWriter(fpath1));
			bw2 = new BufferedWriter(new FileWriter(fpath2));
			String line = "";
			while((line=br.readLine())!=null)
			{
				if(line.compareTo("")==0)
					break;
				Ruler r = new Ruler(line);
				bw1.write(r.print());
				bw1.write("\n");
				r.addOperators(opers);
				r.applyrule();
				System.out.println(""+r.print());
				bw2.write(r.print());
				bw2.write("\n");
			}
			bw1.flush();
			bw2.flush();
		}
		catch(Exception ex)
		{
			System.out.println(""+ex.toString());
		}
		finally {
			if (br !=null) {
				try {
					br.close();
				} catch (IOException e) {
					// ignore error in error handler
				}
			}
			if (bw1 !=null) {
				try {
					bw1.close();
				} catch (IOException e) {
					// ignore error in error handler
				}
			}
			if (bw2 !=null) {
				try {
					bw2.close();
				} catch (IOException e) {
					// ignore error in error handler
				}
			}
		}
	}
	
	//move a position complied with condition
	// move to n -1
	// move up to tok -2
	public void move(int n, TNode tok,int opt)
	{
		if(opt == -1)
		{
			this.curPos = n;
		}
		else if(opt == -2)
		{
			Iterator<TNode> iter = vec.iterator();
			while(iter.hasNext())
			{
				if(iter.next().sameText(tok))
				{
					this.curPos ++ ;
				}
			}
		}
	}
	public String print()
	{
		String res = "";
		for(int i =0;i<vec.size();i++)
		{
			String type = "";
			if(vec.get(i).type==TNode.WRDTYP)
				type = "WRD";
			else if(vec.get(i).type==TNode.SYBSTYP)
				type = "SYB";
			else if(vec.get(i).type==TNode.NUMTYP)
				type = "NUM";
			else if(vec.get(i).type==TNode.BNKTYP)
				type = "BNK";
			res += vec.get(i).text+"<"+type+">";
		}
		return res;
	}
	public String toString()
	{
		String res = "";
		for(int i=0;i<vec.size();i++)
		{
			res += vec.get(i).text;
		}
		return res;
	}
	public void doOperation(String oper,String num,List<TNode> x,int spos,int epos)
	{
		int quan = 0 ;
		if(num==null||num.compareTo("anynumber")==0)
		{
			quan = Integer.MAX_VALUE;
		}
		else
		{
			quan = Integer.parseInt(num);
		}
		if(oper.compareTo("del")==0)
		{
			if(spos < 0)
			{
				return;// not applicable
			}
			if(epos < 0)
			{
				return; // not applicable
			}
			this.det(quan,x, spos, epos);
		}
		if(oper.compareTo("mov")==0)
		{
			if(spos > epos)
			{
				this.vec = null;
				return;
			}
			this.mov(x, Integer.parseInt(num), spos, epos);
		}
		if(oper.compareTo("ins")==0)
		{
			this.ins(x, spos);
		}
	}
	public void collectPoss(int x)
	{
		// the consPos show be sorted from small to high
		for(int i = 0;i<this.consPos.size();i++)
		{
			if(x<consPos.get(i))
			{
				this.positions.add(i);
			}
		}
		this.positions.add(consPos.size());
	}
	//toks is the token sequence that needed to be inserted into original token sequence
	//dpos is the position start of the insertion
	public void ins(List<TNode> toks,int dpos)
	{
		if(dpos<vec.size())
		{
			vec.addAll(dpos, toks);
		}
		else
		{
			vec.addAll(toks);
		}
	}
	//dpos is the destination position
	//toks specify the tokens need to be moved
	//spos is the start position of the segment
	//epos is the end position of the segment
	public void mov(List<TNode> toks, int dpos, int spos,int epos)
	{
		int pos = 0;
		int size = 0;
		if(toks!=null)
		{
//			pos = this.search(this.vec,toks, spos);
			if(pos+toks.size()>epos+1 || pos == -1)
			{
				return;
			}
			size = toks.size();
		}
		else
		{
			pos = spos;
			if(epos == vec.size())
			{
				size = vec.size()-spos;
			}
			else{
				size = epos - spos+1;
			}
		}
		//update the end position and do the del
		ListIterator<TNode> l = this.vec.listIterator(pos);
		//ListIterator<TNode> dl = this.vec.listIterator(dpos);
		int c = 0;
		List<TNode> x = new ArrayList<TNode>();
		for(c = 0;c<size;c++)
		{
			//this.collectPoss(pos);
			TNode tn = l.next();
			tn.setColor(TNode.MOVCOLOR); // set color for moving.
			x.add(tn);
			//this.whats.add(tn);
			l.remove();
		}
		if(dpos <= spos)
		{
			if(dpos==vec.size())
			{
				this.vec.addAll(x);
				return;
			}
			this.vec.addAll(dpos, x);
			
		}
		if(dpos>=epos)
		{
			dpos = dpos-size;
			if(dpos==vec.size())
			{
				this.vec.addAll(x);
				return;
			}
			this.vec.addAll(dpos, x);
		}
	}
	public void det(int n,List<TNode> toks, int start, int end)
	{
		int cnt = 0;
		int pos = 0;
		int deleng = 0;
		while(cnt < n)
		{
			if(toks == null) // don't specify all particular token sequence
			{
				pos = start;
				n = 0;
				deleng = end-start+1;
			}
			else
			{
//				pos = this.search(this.vec,toks,start);
				deleng = toks.size();
			}
			if(pos+deleng>end+1 || pos == -1)
			{
				break;
			}
			//update the end position and do the del
			ListIterator<TNode> l = this.vec.listIterator(pos);
			int c = 0;
			for(c = 0;c<deleng;c++)
			{
				//this.collectPoss(pos);
				TNode tn = l.next();
				this.whats.add(tn);
				l.remove();
				pos ++;
				
			}
			end = end - deleng;
			cnt ++;
		}
	}
}
