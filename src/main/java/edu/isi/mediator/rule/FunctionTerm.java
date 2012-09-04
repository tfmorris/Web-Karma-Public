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

import edu.isi.mediator.gav.main.MediatorException;

/**
 * Defines a function.
 * @author mariam
 *
 */
public class FunctionTerm extends Term{
	
	/**
	 * the function as {@link edu.isi.mediator.gav.domain.FunctionPredicate}
	 */
	private FunctionPredicate function;
	
	/**
	 * Constructs an empty FunctionTerm
	 */
	public FunctionTerm(){}
	/**
	 * Constructs a FunctionTerm with the variable name "var"
	 * @param var
	 */
	public FunctionTerm(String var){this.var=var;}
	/**
	 * Constructs a FunctionTerm with the variable name "var" and query name "qName"
	 * @param var
	 * @param queryName
	 */
	public FunctionTerm(String var, String queryName){
		this.var=var;
		this.queryName=queryName;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#clone()
	 */
	public FunctionTerm clone(){
		FunctionTerm t = new FunctionTerm();
		t.copy(this);
		t.function=function.clone();
		return t;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getFreeVar()
	 * Always returns null.
	 */
	public String getFreeVar(){
		return null;
	}

	/**
	 * Sets the function.
	 * @param p
	 * 		the FunctionPredicate
	 * @throws MediatorException
	 */
	public void setFunction(FunctionPredicate p) throws MediatorException{
		if(function==null)
			function=p;
		else{
			//a variable can be assigned only to one function
			throw new MediatorException("A variable can be assigned only to one function var=" + var);
		}
	}
	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#getFunction()
	 * Returns the function.
	 */
	public FunctionPredicate getFunction(){
		return function;
	}

	/**
	 * Returns the SQL string equivalent for this function. 
	 * @return
	 * @throws MediatorException
	 */
	public String getFunctionForSQL() throws MediatorException{
		return function.getFunctionForSQL();
	}
	
	/**
	 * Returns true if the output of this function is a number.
	 * @return
	 * 		true if the output of this function is a number, false otherwise.
	 */
	public boolean outputIsNumber(){
		return function.outputIsNumber();
	}
	
	/**
	 * Returns true if the functions in both terms are equal.
	 * @param t
	 * 		comparison term
	 * @return
	 * 		true if the functions in both terms are equal, false otherwise.
	 */
	public boolean equals(Term t){
		//System.out.println("Equal term : " + this + " and " + t);
	    return t instanceof FunctionTerm && function.equals(((FunctionTerm)t).function);
	}
	
	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#needsBinding(boolean)
	 * Returns true if "b" is true, otherwise 
	 * returns true if the {@link edu.isi.mediator.gav.domain.FunctionPredicate} needs binding and false otherwise. 
	 */
	public boolean needsBinding(boolean b){
		if(b==true) return true;
		else{
			//if this function needs bindings => return true, else false
			return function.needsBinding();
		}
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#unify(edu.isi.mediator.gav.domain.Binding)
	 * Also unify the vars within the FunctionPredicate,
	 */
	public Term unify(Binding binding){
		//unify here; there might be vars inside the function that need to be unified
		function.unify(binding);
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.isi.mediator.gav.domain.Term#toString()
	 */
	public String toString(){
		String s = "";
		if(var!=null)
			s += var + ":";
		if(queryName!=null)
			s += queryName + ":";
		s+=function.toString();
		return s;
	}

}

