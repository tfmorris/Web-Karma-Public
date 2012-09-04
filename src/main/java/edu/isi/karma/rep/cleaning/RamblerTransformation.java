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
package edu.isi.karma.rep.cleaning;

import java.util.ArrayList;
import java.util.List;

import edu.isi.karma.cleaning.RuleUtil;


public class RamblerTransformation implements Transformation {

	private List<String> rules = new ArrayList<String>();
	public String signature = "";
	public RamblerTransformation(List<String> rules)
	{
		this.setTransformationRules(rules);
	}
	public void setTransformationRules(List<String> rules)
	{
		this.rules = rules;
		for(int i = 0; i< rules.size(); i++)
		{
			signature += rules.get(i)+"\n";
		}
	}
	@Override
	public String transform(String value) {
		if(this.rules.size() == 0)
		{
			return value; // if no rule exists, return the original string
		}
		String s = RuleUtil.applyRule(this.rules, value);
		return s;
	}	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return this.signature;
	}
	
	@Override
	public int hashCode()
	{
		return this.signature.hashCode();
	}
	@Override
	public boolean equals(Object other) 
	{
	    return other != null && other instanceof RamblerTransformation 
	            && this.signature.equals(((RamblerTransformation)other).signature);
	}

}
