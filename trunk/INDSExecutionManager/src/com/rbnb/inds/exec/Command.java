/*
	Command.java
	
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
	2008/12/23  WHF  Added name field, and ByteArrayOutputStreams.
	2009/01/13  WHF  Changed calls to String.isEmpty() to String.length() == 0.	
*/

package com.rbnb.inds.exec;


import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import java.util.ArrayList;

import org.xml.sax.Attributes;

/**
  * Base type for all execution commands.
  */
public abstract class Command
{
	public Command(Attributes attr)
	{
		String temp;

		temp = attr.getValue("initialDirectory");
		if (temp == null || temp.length() == 0)
			initialDirectory = ".";
		else initialDirectory = temp;
		logFile = attr.getValue("logFile");

		tag = (temp = attr.getValue("tag")) == null ? "" : temp;
		
		id = getClass().getSimpleName() + '_' + (++commandCount);
	}
	
	/**
	  * Starts execution of a command.
	  *
	  * @return true if the command has completed synchronously; false if the
	  *  program continues to run.
	  */
	public final void startExecution() throws java.io.IOException
	{
		if (logFile != null) {
			logStream = new FileOutputStream(initialDirectory+"/"+logFile);
		}
		
		executionComplete = doExecute();
	}
	
	/**
	  * If the command has already stopped this fucntion does nothing.
	  */
	public final void stopExecution()
	{
		if (executionComplete) return;
		doKill();
		executionComplete = true;
	}
	
	public final void waitFor() throws InterruptedException
	{
		doWaitFor();
		executionComplete = true;
	}
	
	public final void addInput(Port p)
	{
		inputs.add(p);
	}
	
	public final void addOutput(Port p)
	{
		outputs.add(p);
	}
	
//*************************  Accessors (final)  *****************************//
	/**
	  * Returns an unmodifiable view of the input connections.
	  */
	public final java.util.List<Port> getInputs()
	{
		return java.util.Collections.unmodifiableList(inputs);
	}

	/**
	  * Returns an unmodifiable view of the input connections.
	  */
	public final java.util.List<Port> getOutputs()
	{
		return java.util.Collections.unmodifiableList(outputs);
	}	
	
	public final ByteArrayOutputStream getLocalStdOutStream() 
	{ return localStdOutStream; }
	public final ByteArrayOutputStream getLocalStdErrStream()
	{ return localStdErrStream; }

	public final String getInitialDirectory() { return initialDirectory; }
	public final String getLogfile() { return logFile; }
	public final String getId() { return id; }
	public final String getTag() { return tag; }
	public final boolean isExecutionComplete() { return executionComplete; }
	
	final java.io.OutputStream getLogStream() { return logStream; } 
	
	public final String getXmlSnippet() { return xmlSnippet; }
	final void setXmlSnippet(String xmlSnippet) 
	{ this.xmlSnippet = xmlSnippet; }

	/**
	  * Obtain the map of keys to values for the specified Command subclass.
	  */
	protected final java.util.Map<String, String> getCommandProperties()
	{
		return commandProperties.get().get(getClass());
	}
	

//***************************  Overrideables  *******************************//
	protected abstract boolean doExecute() throws java.io.IOException;
	// TODO: get thread of non-process commands, and interrupt/terminate
	protected void doKill() {} //  throws java.io.IOException;
	protected void doWaitFor() throws InterruptedException
	{}	
	
	public InputStream getStdOut() { return null; }
	public InputStream getStdErr() { return null; }
	
	public abstract String getPrettyName();
	public String getChildConfiguration() { return ""; }
	
	public String toString()
	{
		return xmlSnippet;
	}

//**************************  Private Member Data  **************************//	
	private final String initialDirectory, logFile, id, tag;
	private String xmlSnippet;
	private java.io.OutputStream logStream;
	private final ArrayList<Port> 
		inputs = new ArrayList<Port>(),
		outputs = new ArrayList<Port>();
	private final ByteArrayOutputStream 
		localStdOutStream = new ByteArrayOutputStream(), 
		localStdErrStream = new ByteArrayOutputStream();

	private boolean executionComplete = false;
	
//****************************  Static Methods  *****************************//
	public static void putCommandProperties(Class<? extends Command> c, 
			java.util.Map<String, String> props)
	{
		commandProperties.get().put(c, props);
	}
	
	protected static String file2string(java.io.File file) 
	{		
		java.io.StringWriter sw = new java.io.StringWriter();
		char buff[] = new char[1024];
		
		try {
			java.io.FileReader fr = new java.io.FileReader(file);
			int nRead;
			while ((nRead = fr.read(buff)) > 0)
				sw.write(buff, 0, nRead);
			fr.close();
		} catch (java.io.IOException ioe) {
			return "CONFIG FILE READ FAILED!!!";
		}
		return sw.toString();
	}		
	
//****************************  Static Data  ********************************//
	/**
	  * Incremented with each new command to generate unique names.
	  */
	private static int commandCount = 0;
	
	/**
	  * Messy, isn't it?  This is why C++ has typedefs.
	  */	  
	private static final ThreadLocal< java.util.Map<Class<? extends Command>,
			java.util.Map<String, String> > > commandProperties = new
			ThreadLocal< java.util.Map<Class<? extends Command>,
			java.util.Map<String, String> > > () {
				protected java.util.Map<Class<? extends Command>,
			java.util.Map<String, String> > initialValue() { return new
				java.util.HashMap<Class<? extends Command>,
					java.util.Map<String, String> > ();
			}
	};
}

