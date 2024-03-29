/*
	BuiltInSchemaResolver.java
	
	Copyright 2008 Creare Inc.
	
	Licensed under the Apache License, Version 2.0 (the "License"); 
	you may not use this file except in compliance with the License. 
	You may obtain a copy of the License at 
	
	http://www.apache.org/licenses/LICENSE-2.0 
	
	Unless required by applicable law or agreed to in writing, software 
	distributed under the License is distributed on an "AS IS" BASIS, 
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
	See the License for the specific language governing permissions and 
	limitations under the License.
	
	---  History  ---
	2008/12/02  WHF  Created.
*/

package com.rbnb.inds.exec;


import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;


/**
  * Resolves our INDS execution schema URL against the one stored in the 
  *    file.
  */
class BuiltInSchemaResolver implements EntityResolver
{
	public InputSource resolveEntity(String publicId, String systemId)
	{
		if (systemId.contains("inds_startup.xsd")) {
			java.io.InputStream schemaStream = getClass().getClassLoader()
					.getResourceAsStream("com/rbnb/schema/inds_startup.xsd");
					
			if (schemaStream != null) {
				return new InputSource(schemaStream);
			}
		}
		
		return null; // delegate to parser to find entity
	}
}
