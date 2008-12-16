/*
	Port.java
	
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
	2008/12/15  WHF  Created.
*/

package com.rbnb.inds.exec;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
  * Defines network connections.  They are created from XML attributes using
  *  {@link #createPort(Attributes) }.
  */
public abstract class Port
{
	private Port(Attributes attr)
	{
		type = attr.getValue("type");
		port = attr.getValue("port");
	}
	
	public final String getType() { return type; }
	public final String getPort() { return port; }

	private final String type, port;
	
	/**
	  * Factory method.
	  */
	public static Port createPort(Attributes attr)
		throws SAXException
	{
		String type = attr.getValue("type");	
		
		if ("rbnb".equals(type)) return new RbnbPort(attr);
		else if ("tcp".equals(type)) return new TcpPort(attr);
		else if ("udp".equals(type)) return new UdpPort(attr);
		
		throw new SAXException("Unrecognized connection type \""+type+"\"/.");
	}
	
	public static class RbnbPort extends Port
	{
		private RbnbPort(Attributes attr)
		{
			super(attr);
			
			name = attr.getValue("name");
			channel = attr.getValue("channel");
			
			String temp;
			temp = attr.getValue("cacheFrames");
			cacheFrames = temp != null && !temp.isEmpty() 
					? Integer.parseInt(temp) : 0;
			temp = attr.getValue("archiveFrames");		
			archiveFrames = temp != null && !temp.isEmpty() 
					? Integer.parseInt(temp) : 0;
			archiveMode = "true".equals(attr.getValue("overwriteArchive"))
					? "create" : "append";			
		}
		
		public final int getCacheFrames() { return cacheFrames; }
		public final int getArchiveFrames() { return archiveFrames; }
		public final String getName() { return name; }
		public final String getChannel() { return channel; }	
		public final String getArchiveMode() { return archiveMode; }
	
		private final int cacheFrames, archiveFrames;
		private final String name, channel, archiveMode;
	}
	
	public static class TcpPort extends Port
	{
		private TcpPort(Attributes attr) { super(attr); }
	}

	public static class UdpPort extends Port
	{
		private UdpPort(Attributes attr) { super(attr); }
	}
}



