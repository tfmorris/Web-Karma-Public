/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *  
 *    This code was developed by the Information Integration Group as part 
 *    of the Karma project at the Information Sciences Institute of the 
 *    University of Southern California.  For more information, publications, 
 *    and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.mediator.rule;

import java.math.BigDecimal;

import edu.isi.mediator.gav.main.MediatorException;
import edu.isi.mediator.gav.util.MediatorConstants;

/**
 * Defines a constant term.
 * @author mariam
 *
 */
public class ConstTerm extends Term{

	/**
	 * the constant
	 */
	private String val;

	/**
	 * Constructs an empty ConstTerm.
	 */
	public ConstTerm(){}

	/**
	 * Constructs a ConstTerm with the value "val"
	 * @param val
	 * 		constant value
	 */
	public ConstTerm(String val){
		this.val = normalizeVal(val);
	}
	/**
	 * Constructs a ConstTerm with the variable "var" and value "val".
	 * @param var
	 * 		variable name
	 * @param val
	 * 		constant value
	 */
	public ConstTerm(String var, String val){
		this.var=var;
		this.val = normalizeVal(val);
	}

	/**
	 * Constructs a ConstTerm with the variable "var" and value "val".
	 * Associates this term with the SQL query name "queryName" (equivalent to the "as" name ... select a as x)
	 * @param var
	 * 		variable name
	 * @param val
	 * 		constant value
	 * @param queryName
	 * 		sql query variable name (used only with SQL interface; (equivalent to the "as" name ... select a as x)
	 */
	public ConstTerm(String var, String val, String queryName){
		this.var=var;
		this.val = normalizeVal(val);
		this.queryName = queryName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ConstTerm clone(){
		ConstTerm t = new ConstTerm();
		t.copy(this);
		t.val=val;
		return t;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getFreeVar()
	 * return null
	 */
	@Override
	public String getFreeVar(){
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getTermValue()
	 * return val
	 */
	@Override
	public String getTermValue(){
		return val;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getVal()
	 */
	@Override
	public String getVal(){
		return val;
	}

	/**
	 * Sets the constant vale;
	 * @param v
	 * 		the constant value
	 */
	public void setVal(String v){
		//System.out.println("Set Value " + v);
		val=normalizeVal(v);
	}	

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#equals(edu.isi.mediator.gav.domain.Term)
	 */
	@Override
	public boolean equals(Term t){
		//System.out.println("Equal term : " + this + " and " + t);
		return t instanceof ConstTerm && ((t.var == null && this.var == null) || this.var.equals(t.var));
	}

	private String unquote(String s) {
		if(s != null && (s.startsWith("\"") || s.startsWith("'"))) {
			return s.substring(1, s.length()-1);
		} else {
			return s;
		}
	}
	/**
	 * Checks equality of VALUE.
	 * @param t
	 * @return
	 * 		true if the VALUE of the two terms is equal
	 * 		false otherwise
	 */
	public boolean equalsValue(ConstTerm t){

		return t != null 
				&& ((t.val == null && this.val == null) 
						|| unquote(t.val).equals(unquote(this.val)));
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#needsBinding(boolean)
	 * Always returns false; A ConstTerm is already bound.
	 */
	@Override
	public boolean needsBinding(boolean b){
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#unify(edu.isi.mediator.gav.domain.Binding)
	 */
	@Override
	public Term unify(Binding binding){
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getSqlVal(boolean)
	 */
	@Override
	public String getSqlVal(boolean isNumber) throws MediatorException{
		if(isNumber){
			return getNumberVal();
		}else{
			return getStringVal();
		}
	}

	//I don't know what it is, so I return t as is
	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getSqlValNoType()
	 */
	@Override
	public String getSqlValNoType() throws MediatorException{
		String newV = val;
		if(newV.equals(MediatorConstants.NULL_VALUE)){
			return MediatorConstants.NULL_VALUE;
		}
		if(newV.startsWith("\"") || newV.startsWith("'")){
			newV = val.substring(1, val.length()-1);
			if(newV.equals(MediatorConstants.NULL_VALUE)){
				return MediatorConstants.NULL_VALUE;
			}else{
				return "'" + newV + "'";
			}
		}
		else{
			return newV;
		}
	}

	/**
	 * Returns the value as a number.
	 * @return
	 * 		the value as a number (not enclosed between single quotes)
	 * @throws MediatorException
	 */
	private String getNumberVal() throws MediatorException{
		//val could start with " or '
		//if it does remove them
		String newV = val;
		if(newV.equals(MediatorConstants.NULL_VALUE)){
			return MediatorConstants.NULL_VALUE;
		}
		try{
			if(newV.startsWith("\"") || newV.startsWith("'")){
				newV = val.substring(1, val.length()-1);
			}
			//a number can be between "'" in a sql query
			/*
			else if(newV.startsWith("'")){
				throw new MediatorException(newV + " should be a Number! Do not enclose it between \"'\"");
			}
			 */
			//make sure that newV is a number
			if(newV.equals(MediatorConstants.NULL_VALUE)){
				return MediatorConstants.NULL_VALUE;
			}
			else{
				new BigDecimal(newV);
			}
		}catch(NumberFormatException ne){
			throw new MediatorException(newV + " should be a number!" + ne.getMessage());			
		}catch(Exception e){
			throw new MediatorException(e.getMessage());
		}

		return newV;
	}

	/**
	 * Returns the value as a string.
	 * @return
	 * 		the value as a string (enclosed between single quotes)
	 * @throws MediatorException
	 */
	public String getStringVal() throws MediatorException{
		//if val should be a string it has to start with " or '
		//if it doesn't it's not a string

		String newV = val;
		if(newV.equals(MediatorConstants.NULL_VALUE)){
			return MediatorConstants.NULL_VALUE;
		}
		if(newV.startsWith("\"") || newV.startsWith("'")){
			newV = val.substring(1, val.length()-1);
			if(newV.equals(MediatorConstants.NULL_VALUE)){
				return MediatorConstants.NULL_VALUE;
			}else{
				return "'" + newV + "'";
			}
		}
		else{
			throw new MediatorException(newV + " should be a String! Enclose it between \"'\"");
		}
	}

	/**
	 * Returns NULL for an input of null (always uppercase)
	 * @param val
	 * 		input value
	 * @return
	 * 		NULL for an input of null (always uppercase)
	 * 		input value otherwise
	 */
	private String normalizeVal(String val){
		if(val==null) {
			return MediatorConstants.NULL_VALUE;
		} else if(val.toUpperCase().equals(MediatorConstants.NULL_VALUE)) {
			return MediatorConstants.NULL_VALUE;
		} else {
			return val;
		}
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#toString()
	 */
	@Override
	public String toString(){
		String s = val;
		if(var!=null){
			s = var + ":" + s;
		}
		if(queryName!=null){
			s += ":" + queryName;
		}
		return s;
	}

}

