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


import java.util.ArrayList;

import edu.isi.mediator.gav.main.MediatorException;
import edu.isi.mediator.gav.util.MediatorUtil;

/**
 * A Predicate can be a "normal" relation, a built-in predicate, like equality, lessThan, like, etc., or a function. 
 * <br>For a "normal" relation, the "name" is the name of the relation. 
 * <br>{@link edu.isi.mediator.gav.rule.RelationPredicate}
 * <br>For built-in predicates like equality, lessThan, the "name" will be the relation type
 * <br>{@link edu.isi.mediator.gav.rule.BuiltInPredicate}
 * 
 * @author mariam
 *
 */
public abstract class Predicate implements Cloneable {
	
	protected String name;
	protected ArrayList<Term> terms = new ArrayList<Term>();
		
	/**
	 * true if domain predicate
	 * <br>false if source predicate
	 */
	private boolean isDomainPredicate = false;
	
	public void copy(Predicate source) {
	    this.name = source.name;
	    this.terms = new ArrayList<Term>(source.terms);
	    this.isDomainPredicate = source.isDomainPredicate;
	}
	public abstract Predicate clone();
	public abstract String toString();

	//for UAC
	private boolean isUACPredicate = false;
	public boolean isUACPredicate(){return isUACPredicate;}
	public void isUACPredicate(boolean b){isUACPredicate=b;}
	///////////////////////////////////////
	
	/**
	 * @return
	 * true if domain predicate
	 * <br> false if source predicate
	 */
	public boolean isDomainPredicate(){
		return isDomainPredicate;
	}
	
	/**
	 * Adds a term to the predicate.
	 * @param var
	 * 		variable name for {@link edu.isi.mediator.gav.domain.VarTerm}
	 * 		<br>constant value for {@link edu.isi.mediator.gav.domain.ConstTerm}
	 */
	public void addTerm(String var){
		Term t;
		if(!MediatorUtil.isVar(var)){
			t=new ConstTerm(var);
		}else{
			t = new VarTerm(var);
		}
		terms.add(t);
	}
	
	/**
	 * Adds a term to the predicate if the term is not a duplicate of an existing term.
	 * @param var
	 * 		variable name for {@link edu.isi.mediator.gav.domain.VarTerm}
	 * 		<br>constant value for {@link edu.isi.mediator.gav.domain.ConstTerm}
	 */
	public void addTermIfUnique(String var){
		Term t;
		if(!MediatorUtil.isVar(var)){
			t=new ConstTerm(var);
		}else{
			t = new VarTerm(var);
		}
		if(!containsTerm(t)){
			terms.add(t);
		}
	}

	/**
	 * Adds a term to the predicate.
	 * @param t the term
	 */
	public void addTerm(Term t){
		terms.add(t);
	}
	
	/**
	 * Returns all terms
	 * @return all terms
	 */
	public ArrayList<Term> getTerms(){
		return terms;
	}
	
	/**
	 * Return the position of the first term,that is equal to t: starting at "index"
	 * @param t the term
	 * @param index	position where the search starts
	 * @return position of term that is equals to t
	 */
	public int findTerm(Term t, int index){
		//System.out.println("Find term " + t + " in " + terms + " starting at " + index);
		for(int i=index; i<terms.size(); i++){
			Term t1 = terms.get(i);
			if(t1.equals(t)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns true if predicate contains the specified term
	 * @param t1 a term
	 * @return
	 * 		true if the predicate contains a term equal to the specified term
	 * 		false otherwise
	 */
	public boolean containsTerm(Term t1){
		for(int i=0; i<terms.size(); i++){
			Term t2 = terms.get(i);
			if(t1.equals(t2)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes the term at specified position
	 * @param i the position of the term to be removed
	 */
	public void removeTerm(int i){
		terms.remove(i);
	}
	
	/**
	 * Sets a term at a specified position 
	 * @param t	the term
	 * @param index	the position
	 */
	public void setTerm(Term t, int index){
		terms.set(index, t);
	}
	
	/**
	 * Returns predicate name
	 * @return	predicate name
	 */
	public String getName(){
		return name;
	}
	/**
	 * Sets the predicate name
	 * @param name
	 */
	public void setName(String name){
		this.name=name;
	}

	/** Returns names of variables that are NOT attached to a constant
	 * @return names of variables that are NOT attached to a constant
	 */
	public ArrayList<String> getFreeVars(){
		ArrayList<String> vars = new ArrayList<String>();
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			String var = t.getFreeVar();
			if(var!=null){
				vars.add(var);
			}
		}
		return vars;
	}

	/**
	 * Returns names of ALL variables
	 * @return names of ALL variables
	 */
	public ArrayList<String> getVars(){
		ArrayList<String> vars = new ArrayList<String>();
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			String var = t.getVar();
			if(var!=null){
				vars.add(var);
			}
		}
		return vars;
	}

	/**
	 * Returns all values.
	 * @return all values
	 * 		<br>for a {@link edu.isi.mediator.gav.domain.VarTerm} the value is the variable name(var)
	 * 		<br>for a {@link edu.isi.mediator.gav.domain.ConstTerm} the value is the constant(val)
	 */
	public ArrayList<String> getValues(){
		ArrayList<String> vars = new ArrayList<String>();
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			String var = t.getTermValue();
			if(var!=null){
				vars.add(var);
			}
		}
		return vars;
	}

	/**
	 * Returns the name of ALL variables
	 * @return the name of ALL variables
	 * 		<br>for the {@link edu.isi.mediator.gav.domain.ConstTerm} that do not have a given variable name
	 * 		a name is generated
	 */
	public ArrayList<String> getVarsAndConst(){
		ArrayList<String> vars = new ArrayList<String>();
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			String var = t.getVar();
			if(var!=null){
				vars.add(var);
			}
			else{
				vars.add("Const." + name + i);
			}
		}
		return vars;
	}
	
	/**
	 * Returns the index of the specified variable
	 * @param var
	 * 			the variable name
	 * @return the index of the specified variable
	 */
	public int getVarIndex(String var){
		ArrayList<String> vars = getVarsAndConst();
		return vars.indexOf(var);
	}
	
	/**
	 * Returns true if the predicates are equals
	 * @param p a predicate
	 * @return 	true if the predicates are equals
	 * 			<br>false otherwise
	 * 			<br>Two predicates are equal if the name is equals and all terms are equals
	 */
	@Override
	public boolean equals(Object other){
	    if (other == null || !(other instanceof Predicate)) {
	        return false;
	    }
	    Predicate p = (Predicate) other;
		//System.out.println("Compare " + this + " and " + p1);
		if(!p.getName().equals(name)){
			return false;
		}
		//if different number of terms, can't be a UNION
		if(terms.size()!=p.terms.size()){
			return false;
		}
		//compare the terms
		for(int i=0; i<terms.size(); i++){
			Term t1 = terms.get(i);
			Term t2 = p.getTerms().get(i);
			//System.out.println("Compare " + t1 + " and " + t2);
			if(!t1.equals(t2)){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
	    return this.getName().hashCode() + this.terms.size();
	}
	
	/**
	 * Generates unique variable names given an index.
	 * @param index
	 */
	public void setUniqueVarNames(int index){
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			t.changeVarName(index);
		}
	}
	
	
	//change the predicate vars based on unification
	//it returns false if this predicate is something like "6"="7"
	//we should never get to this case; the unification algorithm
	//shouldn't let us get this far ... to cases like 6=7
	/**	Unifies the terms of the predicate given a specific Binding
	 * @param binding	the binding
	 */
	public void unify(Binding binding){
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			t=t.unify(binding);
			terms.set(i,t);
		}
	}
	
	
	//outVar is the outVar of func
	//if this pred contains outVar, replace it with func
	/**	Normalize a predicate with a given function.
	 *  <br>If the predicate contains "outVar" replace that Term with a FunctionTerm whose function is set to "func"
	 * @param func the function	
	 * @param outVar	the variable name to be replaced
	 * @throws MediatorException
	 */
	public void normalizeWithFunction(FunctionPredicate func, String outVar) throws MediatorException{
		//System.out.println("Normalize " + this + " with " + func + " outVar=" + outVar);
		for(int i=0; i<terms.size(); i++){
			Term t = terms.get(i);
			String var = t.getVar();
			if(var!=null && var.equals(outVar)){
				FunctionTerm ft=new FunctionTerm(var, t.queryName);
				ft.setFunction(func);
				terms.set(i, ft);
			}
		}
	}
	
}
