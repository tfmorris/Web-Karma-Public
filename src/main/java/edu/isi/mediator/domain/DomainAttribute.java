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

package edu.isi.mediator.domain;

import edu.isi.mediator.gav.util.MediatorConstants;

/**
 * Representation of a DomainAttribute (name:type)
 * @author mariam
 *
 */
public class DomainAttribute{
	
	/**
	 * Supported Attribute Types:
	 * STRING, NUMBER, OTHER
	 */
	public enum AttrType {
	    STRING, NUMBER, OTHER
	};

	/**
	 * attribute name
	 */
	protected String name;
	/**
	 * attribute type
	 */
	protected AttrType type;
	/**
	 * true if the name has illegal chars that need to be escaped in the SQL query
	 */
	protected boolean hasIllegalChars = false;
	
	/** 
	 * Constructs a DomainAttribute with name and type
	 * @param name
	 * @param type
	 * 			can be 'STRING' | 'NUMBER'
	 */
	public DomainAttribute(String name, String type){
		this.name = name;
		/*
		if(name.contains(MediatorInstance.ILLEGAR_CHARS))
			hasIllegalChars=true;
*/
		if(type.equals("STRING")){
			this.type=AttrType.STRING;
		}else if(type.equals("NUMBER")){
			this.type=AttrType.NUMBER;
		}else{
			this.type=AttrType.OTHER;
		}
	}

	/**
	 * @return name of attribute
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Returns name with illegal characters escaped.
	 * @return name with illegal characters escaped
	 */
	public String getSQLName(){
		if(hasIllegalChars){
			return "`" + name.replaceAll(MediatorConstants.ILLEGAR_CHARS," ") + "`";
		}else{
			return name;
		}
	}

	/**
	 * Returns true if the type is a number.
	 * @return true if this attribute is a number
	 * 			false otherwise
	 */
	public boolean isNumber(){
		if(type.equals(AttrType.NUMBER)){
			return true;
		}else{
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		String s = "";
		s += name + ":" + type;
		return s;
	}

}

