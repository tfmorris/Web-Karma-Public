package edu.isi.karma.cleaning;

import java.util.ArrayList;
import java.util.List;
//a Class aims to store the parameter for edit operations
public class EditOper{
	public String oper="";
	public int starPos=-1;
	public int endPos=-1;
	public int dest = -1;
	public List<TNode> tar = new ArrayList<TNode>();
	public List<TNode> before = new ArrayList<TNode>();
	public List<TNode> after = new ArrayList<TNode>();
	public EditOper()
	{
		
	}
	public String toString()
	{
		return oper+": "+starPos+","+endPos+","+dest+tar.toString();
	}
}
