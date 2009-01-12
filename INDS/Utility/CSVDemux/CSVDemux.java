    
    /* CSVDemux - demux csv data
     *
     * 08/08/07  JPW  Initially developed to parse Panther data strings captured from the WB57 during TC4.  Developed for Global Scan
     * 02/13/08  JPW  Made program more general by not hardwiring the expected number of data values in the CSV string.
     *                Added the "-d" flag to specify date format.
     *                Added the output "_CSV" channel, to contain the CSV string.
     * 03/03/08  JPW  Adjust to use XMLParser class which was also being used in IWG1Caster
     *		      Add support for skipping fields (see xmlSkipList).
     * 03/07/08  JPW  Don't include fields that were skipped in the output "_CSV" string.
     *                Add "-p" option: check embedded timestamp in received data; if it matches the previously received timestamp, don't demux/output the data.
     *
     */
    
    import java.io.*;
    import java.lang.String;
    import java.lang.StringBuffer;
    import java.text.ParsePosition;
    import java.text.SimpleDateFormat;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.TimeZone;
    
    import com.rbnb.sapi.ChannelMap;
    import com.rbnb.sapi.Sink;
    import com.rbnb.sapi.Source;
    import com.rbnb.utility.ArgHandler;
    import com.rbnb.utility.RBNBProcess;
    
    public class CSVDemux{
	
	private String serverAddress="localhost:3333";
	private String serverAddressOut="localhost:3333";
	private int archive=0;  // default archive is off
	private int cache=1000;  // was 100, larger helps build larger framesets = more efficient
	private double start = 0.;
	private String reference = null;
	
	// Specify which embedded channel is the time channel
	private int tstampField=1;
	
	// JPW 03/07/2008: Check embedded timestamp in received data; if it
	//                 matches the previously received timestamp, should
	//                 the data be demuxed/output?  Using the "-p" command-
	//                 line option will set this boolean to false.
	private boolean bOutputDuplicateTimestamp = true;
	// The previously received embedded timestamp
	private String prevEmbeddedTimeStr = "";
	
	// Use the RBNB arrival time as the output timestamp?
	// NOTE: This is actually the UDP packet's arrival time at UDPCapture
	// To use the embedded time in the UDP packet as the output time: use
	// "-t" on the command line; that will set this flag false
	private boolean bUseArrivalTime = true;
	
	private String mode=new String("none");
	private String In=null;
	private String out=null;
	private boolean multichan= true;  // multiple channels, i.e. one RBO per chan
	private long duration = 600000;  //write to disk every x, default 10 minutes
	
	// Use the "id" attribute as the channel name?
	// When this is false, the channel name is taken from the content of the "label" field.
	// This is set to true by using the "-I" flag
	private boolean bUseIDForChanName = false;
	
	private Sink snk=null;
	private Source src=null;
	private ChannelMap cmin=new ChannelMap(); //channels received
	private ChannelMap csub=new ChannelMap(); //channel to subscribe to
	private ChannelMap regmap = null;
	
	XMLParser xml;
	private String XMLFile = null;
	
	private boolean bSilent = false;  //silent, default is debugging on
	private static final int printPeriod = 1000;
	// num packets read; print debug once every printPeriod number of packets
	private int count = 0;
	
	// Recover mode; a single request is made for data from recoverStartTime to recoverStopTime
	private boolean bRecoverMode = false;
	private double recoverStartTime = 0;
	private double recoverStopTime = 0;
	
	private String markerStr = null;
	
	private SimpleDateFormat sdf = null;
	private final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	
	// The list of parameters from the XML file
	private String[] xmlFieldList = null;
	
	// JPW 03/03/2008: Specifies what fields to skip (don't send to the RBNB)
	private boolean[] xmlSkipList = null;
	
	//-------------------------------------------------------------------------
	//Main
	public static void main(String[] arg) throws Exception {
	    new CSVDemux(arg).start();
	}
	
	//-------------------------------------------------------------------------
	//Constructor
	public CSVDemux(String[] arg) throws Exception {
	    
	    try {
		ArgHandler ah=new ArgHandler(arg);
		if (ah.checkFlag('h')) {
		    System.err.println("CSVDemux");
		    System.err.println(" -a <server address> : address of RBNB server to read data from");
		    System.err.println("             default : localhost:3333");
		    System.err.println(" -A <server address> : address of RBNB server to write data to");
		    System.err.println("             default : localhost:3333");
		    System.err.println(" -c <num>            : cache frames");
		    System.err.println(" -d <date format>    : date format (as specified by Java's SimpleDateFormat class)");
		    System.err.println("             default : yyyy-MM-dd'T'HH:mm:ss.SSS");
		    System.err.println(" -h                  : print this usage info");
		    System.err.println(" -i <name>           : name of input");
		    System.err.println("             no default; required option");
		    System.err.println(" -k <num>            : archive frames (append)");
		    System.err.println(" -K <num>            : archive frames (create)");
		    System.err.println("             default : 0 (no archiving)");
		    // Multichan (where we flush each channel individually) is the only mode CSVDemux currently runs in
		    // System.err.println(" -m                  : turn on multiple channels (default on)");
		    // System.err.println(" -M                  : turn OFF multiple channels");
		    System.err.println(" -o <name>           : name of output Source");
		    System.err.println("             default : use the marker string");
		    System.err.println(" -p                  : Check embedded timestamp in received data; if it matches the previously");
		    System.err.println("                       received timestamp, don't demux/output the data.");
		    System.err.println(" -r                  : reference (default newest)");
		    System.err.println(" -s                  : start (default 0)");
		    System.err.println(" -S                  : silent mode");
		    System.err.println(" -t                  : Use the embedded timestamp param (rather than arrival time)");
		    System.err.println(" -T                  : embedded timestamp param position (NOTE: must be greater than 0)");
		    System.err.println("             default : 1");
		    System.err.println(" -x <name>           : name of XML file");
		    System.err.println("             no default; required option");
		    System.err.println(" -z <start>,<stop>   : Recover mode; read from input channel from time <start> to time <stop>");
		    System.err.println("                     : <start> and <stop> must be in seconds since epoch");
		    RBNBProcess.exit(0);
		}
		
		// server to read data from
		if (ah.checkFlag('a')) {
		    String serverAddressL=ah.getOption('a');
		    if (serverAddressL != null) serverAddress=serverAddressL;
		}
		
		// server to put data to
		if (ah.checkFlag('A')) {
		    String serverAddressL=ah.getOption('A');
		    if (serverAddressL != null) serverAddressOut=serverAddressL;
		} else {
		    serverAddressOut = serverAddress;
		}
		
		// cache size
		if (ah.checkFlag('c')) {
		    String tc = ah.getOption('c');
		    cache = Integer.parseInt(tc);
		}
		
		// JPW 02/13/2008: Add "-d" flag: date format
		if (ah.checkFlag('d')) {
		    // Get date format from the supplied argument
		    String formatStr = ah.getOption('d');
		    if ( (formatStr != null) && (!formatStr.trim().equals("")) ) {
			sdf = new SimpleDateFormat(formatStr);
		    }
		}
		
		// input channel name
		if (ah.checkFlag('i')) {
		    In = ah.getOption('i');
		} else {
		    System.err.println("Input name must be specified with the -i option.");
		    System.exit(0);
		}
		
		//archive
		if (ah.checkFlag('k')) {
		    String naf=ah.getOption('k');
		    if (naf!=null) archive=Integer.parseInt(naf);
		    if (archive>0) {
			mode=new String("append");  // was create
			if (archive<cache) cache=archive;
		    }
		}
		
		//archive
		if (ah.checkFlag('K')) {
		    String naf=ah.getOption('K');
		    if (naf!=null) archive=Integer.parseInt(naf);
		    if (archive>0) {
			mode=new String("create");  // was create
			if (archive<cache) cache=archive;
		    }
		}
		
		// single/multiple channels 
		if (ah.checkFlag('m')){
		    multichan = true;
		}
		
		// single/multiple channels 
		if (ah.checkFlag('M')){
		    multichan = false;
		}
		
		//output
		if (ah.checkFlag('o')) {
		    out = ah.getOption('o');
		}
		
		// Don't demux/output data with duplicate embedded timestamp
		if (ah.checkFlag('p')) {
		    bOutputDuplicateTimestamp = false;
		}
		
		if(ah.checkFlag('r')) {
		    reference = ah.getOption('r');
		}
		
		if(ah.checkFlag('s')) {
		    String strt = ah.getOption('s');
		    if(strt != null) start = Double.parseDouble(strt);
		}
		
		//silent
		if (ah.checkFlag('S')){
		    bSilent = true;
		} else bSilent = false;
		
		// Use embedded timestamp param as the output timestamp
		if (ah.checkFlag('t')) {
		    bUseArrivalTime = false;
		}
		
		// timestamp parameter field
		// NOTE: This software is currently developed assuming that
		//       tstampField = 1.  If this is not the case, the software
		//       will need to be reworked
		if (ah.checkFlag('T')){
		    String tsf = ah.getOption('T');
		    tstampField = Integer.parseInt(tsf);
		    // JPW 07/24/2006: Must be greater than 0
		    if (tstampField < 1) {
			tstampField = 1;
		    }
		    if (tstampField != 1) {
			System.err.println(
			    "\n\n\nNOTE: XMLDemux currently requires the\n" +
			    "timestamp parameter to be the first\n" +
			    "parameter in the UDP packet.\n\n" +
			    "You have specified that the timestamp\n" +
			    "parameter is number " +
			    tstampField +
			    ".\n\n" +
			    "XMLDemux will not work correctly.\n\n");
			try { Thread.sleep(10000); } catch (Exception e) {}
		    }
		} else tstampField = 1;
		
		//XML file
		if (ah.checkFlag('x')){
		    XMLFile = ah.getOption('x');
		} else {
		    System.err.println("XML file must be specified with the -x option");
		    System.exit(0);
		}
		
		// JPW 12/14/2006: Recover mode
		if (ah.checkFlag('z')){
		    bRecoverMode = true;
		    // The argument must be <start time>,<stop time>
		    String timesStr = ah.getOption('z');
		    if ( (timesStr == null) || (timesStr.trim().equals("")) ) {
			System.err.println("Missing arguments to the \"-z\" flag.  Must be \"-z <start time>,<stop time>\".");
			System.exit(0);
		    }
		    String[] timesStrArray = timesStr.split(",");
		    if ( (timesStrArray == null)              ||
			 (timesStrArray.length != 2)          ||
	             	 (timesStrArray[0] == null)           ||
			 (timesStrArray[0].trim().equals("")) ||
			 (timesStrArray[1] == null)           ||
			 (timesStrArray[1].trim().equals("")) )
		    {
			System.err.println("Missing or illegal arguments to the \"-z\" flag.  Must be \"-z <start time>,<stop time>\".");
			System.exit(0);
		    }
		    try {
			recoverStartTime = Double.parseDouble(timesStrArray[0]);
			recoverStopTime = Double.parseDouble(timesStrArray[1]);
			if (recoverStopTime < recoverStartTime) {
			    System.err.println("Illegal argument to the \"-z\" flag: Stop time must be greater than or equal to start time.");
			    System.exit(0);
			}
		    } catch (NumberFormatException nfe) {
			System.err.println("Missing or illegal arguments to the \"-z\" flag.  Must be \"-z <start time>,<stop time>\".");
			System.exit(0);
		    }
		}
	    }
	    catch (Exception e) {
		System.err.println("Exception parsing arguments");
		e.printStackTrace();
		RBNBProcess.exit(0);
	    }
	    
	    // Use the default date format if none has been assigned
	    if (sdf == null) {
		sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	    }
	    sdf.setTimeZone(calendar.getTimeZone());
	    
	    // Parse XML file
	    if(!(new File(XMLFile)).exists())
	    {
		System.err.println("!!! Could not open XML file: "+XMLFile);
		System.exit(0);
	    }
	    xml = new XMLParser(XMLFile);
	    // Get the number of parameters
	    int numParameters = xml.getNParam();
	    // Get the marker
	    markerStr = xml.getMarker();
	    // Get the list of parameters
	    xmlFieldList = xml.getID();
	    xmlSkipList = xml.getSkip();
	    // Get info (used as registration user info)
	    String[] info = xml.getInfo();
	    for (int i = 0; i < numParameters; ++i) {
		System.err.println(
		    "\t" +
		    i +
		    ": id = " +
		    xmlFieldList[i] +
		    ", info (used in chan registration) = " +
		    info[i] +
		    ", skip = " +
		    xmlSkipList[i]);
	    }
	    
	    // Channel map for subscription
	    csub.Add(In);
	    
	    System.err.println("marker: \"" + markerStr + "\"");
	    if(out == null) {
		out = markerStr;
	    }
	    
	    // registration channel map
	    String cname = null;
	    regmap = new ChannelMap();
	    int reg_idx = 0;
	    // NOTE: start at i=1 (skip over the Marker field)
	    for (int i = 1; i < numParameters; ++i) {
		cname = xmlFieldList[i];
		if (xmlSkipList[i]) {
		    System.out.println("Skipping Channel \"" + cname + "\"");
		} else {
		    System.out.println("Adding Channel \"" + cname + "\"");
		    reg_idx = regmap.Add(cname);
		    if(info[i] != null) {
			regmap.PutUserInfo(reg_idx,info[i]);
		    } else {
			regmap.PutUserInfo(reg_idx,"<null userinfo>");
		    }
		}
	    }
	    
	    // add latency channel
	    cname = "_Latency";
	    System.out.println("Adding Channel \"" + cname + "\"");
	    reg_idx = regmap.Add(cname);
	    regmap.PutUserInfo(reg_idx,"recieved_time - embedded_time");
	    
	    // JPW 02/13/2008: add CSV string as an output channel
	    cname = "_CSV";
	    System.out.println("Adding Channel \"" + cname + "\"");
	    reg_idx = regmap.Add(cname);
	    regmap.PutUserInfo(reg_idx,"CSV string");
	    
	}
       
       private void makeSource() {
	   do {
	       try {
		   if (src!=null) { //a restart; do not overwrite earlier data
		       if (!mode.equals("none")) mode="append";
		       System.err.println("restarting source connection...");
		       if (src.VerifyConnection()) src.CloseRBNBConnection();
		   }
		   src=new Source();
		   src.SetRingBuffer(cache,mode,archive);
		   System.err.println("Output to: " + out);
		   //System.err.println("Connecting to source: "+serverAddressOut);
		   src.OpenRBNBConnection(serverAddressOut,out);
		   System.err.println("Connected to Source: "+serverAddressOut);
		   // If it takes a while for the Source to come up,
		   // this Register() call can throw an exception.
		   // Therefore, try it in a loop.
		   boolean bRegistered = false;
		   while (!bRegistered) {
			   try {
				   src.Register(regmap);
				   bRegistered = true;
			   } catch (Exception ex) {
				   System.err.println("Can't yet register chans...try again...");
			   }
		   }
		   break;
	       } catch (Exception e) {
		   System.err.println("exception creating source; will try again after a 100 sec sleep");
		   e.printStackTrace();
		   // JPW/MJM 08/01/2007: Sleep longer in case archive recovery is happening
		   try {Thread.currentThread().sleep(100000);} catch (Exception e2) {}
	       }
	   } while (true);
       } //end method makeSource

	       
       private void makeSink() {
	   do {
	       try {
		   //open RBNB connections
		   if (snk!=null) { //a restart; must be from newest
		       if (reference!=null && reference.equals("oldest")) reference="newest";
		       System.err.println("restarting sink connection...");
		       if (snk.VerifyConnection()) snk.CloseRBNBConnection();
		   }
		   snk=new Sink();
		   snk.OpenRBNBConnection(serverAddress,"DemuxSink");
		   System.err.println("Connected to Sink: "+serverAddress);
		   // JPW 12/14/2006: Add recover mode; we'll make one request
		   //                 for all the data
		   if (bRecoverMode) {
		       System.err.println(
		           "\nRecover mode: make one request for data from time " +
			   recoverStartTime +
			   " to time " +
			   recoverStopTime);
		       double duration = recoverStopTime - recoverStartTime;
		       snk.Request(csub,recoverStartTime,duration,"absolute");
		   } else {
		       if(reference == null || reference.equals("")) 	{
		           System.err.println("Running subscribe-by-frame newest mode");
			   snk.Subscribe(csub);
		       } else {
		           System.err.println("Running subscribe-by-time mode: "+start+", "+reference);
			   snk.Subscribe(csub,start,0,reference);
		       }
		   }
		   break;
	       } catch (Exception e) {
		   System.err.println("exception creating sink; will try again");
		   e.printStackTrace();
		   try {Thread.currentThread().sleep(10000);} catch (Exception e2) {}
	       }
	   } while (true);
       } //end method makeSink
       
   	//-------------------------------------------------------------------------
   	//loop listening for incoming data
       public void start() throws Exception {
	 
	 int index=0;
         count = 0;
	 
	 //create RBNB connections
	 makeSource();
	 makeSink();
	 
	 // JPW 12/14/2006: Add recovery mode
	 if (bRecoverMode) {
	     processRecoverRequest();
	     return;
	 }
	 
         while (true)
         {
	    //print debug when count == printPeriod
            count++;
            
            do {
		try {
		    cmin=snk.Fetch(600000);
		    break;
		} catch (Exception e) {
		    System.err.println("exception fetching from sink, restarting");
		    e.printStackTrace();
		    makeSink();
		}
	    } while (true);
	    
	    // MJM 07/14/2005: Add check on number of channels
            if ((cmin.NumberOfChannels() > 0) && !cmin.GetIfFetchTimedOut()) 
            {
               index++;
               if (!bSilent) System.out.println("\n\n" + index+" -Packet Recieved-, size: "+cmin.GetData(0).length);          	
               process(cmin);
	       if (bSilent) System.out.print(".");
            } else {
		System.err.print("x");
	    }
	    
	    // Reset count if needed
	    if (count == printPeriod) {
               System.err.println();
               count = 0;
            }
	    
         }
      }
      
	//---------------------------------------------------------------------
	// processRecoverRequest
	//
	// JPW 12/14/2006
	//
	// Make one request for all data from recoverStartTime to
	// recoverStopTime.  Then loop through the fetched data and process it.
	// Detach from the Source when complete.
	private void processRecoverRequest() {
	    
	    try {
		System.err.println("Fetching data to recover...");
		cmin=snk.Fetch(-1);
		System.err.println("Got the data.");
	    } catch (Exception e) {
		System.err.println("Exception fetching from sink; closing connections");
		e.printStackTrace();
		snk.CloseRBNBConnection();
		src.CloseRBNBConnection();
		return;
	    }
	    
	    if ( (cmin.NumberOfChannels() != 1) || (cmin.GetIfFetchTimedOut()) ) {
		System.err.println("Didn't fetch any data; closing connections");
		snk.CloseRBNBConnection();
		src.CloseRBNBConnection();
		return;
	    }
	    
	    if (cmin.GetIndex(In)!=0) {
		System.err.println("Wrong name for input channel; closing connections");
		snk.CloseRBNBConnection();
		src.CloseRBNBConnection();
		return;
	    }
	    
	    try {
		process(cmin);
	    } catch (Exception e) {
		System.err.println("Exception processing data; closing connections");
		e.printStackTrace();
		snk.CloseRBNBConnection();
		src.CloseRBNBConnection();
		return;
	    }
	    
	    snk.CloseRBNBConnection();
	    src.Detach();
	    System.err.println("Recovered data.  Source detached.");
	    
	}
      
      
      //-------------------------------------------------------------------------
      // process data channels
      //
      public void process(ChannelMap in) throws Exception {
	
	ChannelMap out = new ChannelMap();
	
	try {
	    if (in.GetIndex(In)!=0) return;
	    
            // String[] data = in.GetDataAsString(0);
	    byte[][] byteData=in.GetDataAsByteArray(0);
	    
	    if ( (byteData == null) || (byteData.length == 0) ) {
	        return;
	    }
	    
	    String[] data = new String[ byteData.length ];
	    for (int k = 0; k < byteData.length; ++k) {
		data[k] = new String(byteData[k]);
	    }
	    
	    if (bRecoverMode) {
		System.err.println("Recover " + data.length + " packets");
	    }
            
            for (int i=0; i<data.length; ++i) {
		
		if ( (bRecoverMode) && ((i % 10) == 0) ) {
		    System.err.println("Packet " + i + " out of " + data.length);
		}
		
		String nextCSVStr = data[i];
		StringBuffer outputCSVStr = new StringBuffer("");
		
		if ( (nextCSVStr == null) || (!nextCSVStr.trim().startsWith(markerStr)) ) {
		    System.err.println("String didn't start with correct marker");
		    continue;
		}
		
		String[] strArray = nextCSVStr.split(",");
		
		// JPW 02/13/2008: Don't hardwire that we need 14 tokens (this
		//                 was specifically for the Panther data).
		//                 Rather, we need to match what was in the
		//                 XML file
		int requiredNumComponents = xml.getNParam();
		if (strArray.length != requiredNumComponents) {
		    System.err.println("ERROR: CSV string has " + strArray.length + " components, requires " + requiredNumComponents);
		    continue;
		}
		
		double[] dataArray = new double[1];
		double dataVal = 0.0;
		
		if (!bSilent) System.err.println("Parse string:\n\t" + nextCSVStr);
		
		// Marker
		String marker = strArray[0].trim();
		if (!marker.equals(markerStr)) {
		    System.err.println("Marker mismatch: expecing \"" + markerStr + "\", got \"" + marker + "\"");
		    continue;
		}
		
		// Embedded time
		String cname = "TimeStamp";
		// Need to set time zone
		// Timestamp embedded in the UDP packet
		String embeddedTimeStr = strArray[1].trim();
		// JPW 03/07/08: Add check on duplicate embedded timestamp
		if ( (!bOutputDuplicateTimestamp) && (embeddedTimeStr.equals(prevEmbeddedTimeStr)) ) {
		    if (!bSilent) System.err.println("Duplicate embedded timestamp (" + embeddedTimeStr + "); don't output data.");
		    continue;
		}
		prevEmbeddedTimeStr = embeddedTimeStr;
		Date date = sdf.parse(embeddedTimeStr, new ParsePosition(0));
		// Convert time to number of seconds since epoch
		double embeddedTime = ((double)date.getTime())/1000.0;
		// Time the UDP packet arrived at UDPCapture
		double arrivalTime = in.GetTimes(0)[i];
		// Timestamp put on the output data
		// This can either be arrivalTime or embeddedTime;
		// initialize it to arrivalTime for now
		double rbnbOutTime = arrivalTime;
		if (!bUseArrivalTime) {
		    rbnbOutTime = embeddedTime;
		}
		out.Clear();
		int idx = out.Add(cname);
		dataArray[0] = embeddedTime;
		out.PutTime(rbnbOutTime,0);
		out.PutDataAsFloat64(idx,dataArray);
		do {
		    try {
			src.Flush(out);
		        break;
		    } catch (Exception e) {
			System.err.println("exception flushing to source, restarting");
			e.printStackTrace();
			makeSource();
		    }
		} while (true);
		if (!bSilent) System.err.println("Put TimeStamp value " + dataArray[0]);
		
		// JPW 03/07/08: Build up the CSV string to output (don't
		//               include skipped fields)
		outputCSVStr.append(marker);
		outputCSVStr.append(",");
		outputCSVStr.append(embeddedTimeStr);
		
		// Start at j=2 (skip "Marker" and "TimeStamp" fields)
		for (int j = 2; j < xml.getNParam(); ++j) {
		    // JPW 03/03/08: Add support for skipping fields
		    if (xmlSkipList[j]) {
			continue;
		    }
		    cname = xmlFieldList[j];
		    try {
			dataVal = Double.parseDouble(strArray[j].trim());
		    } catch (NumberFormatException nfe) {
			System.err.println("\nCaught NumberFormatException parsing data value; set value to NaN:\n" + nfe);
			dataVal = Double.NaN;
		    }
		    // JPW 03/10/08: Append this field to the CSV string
		    outputCSVStr.append(",");
		    outputCSVStr.append(strArray[j].trim());
		    out.Clear();
		    idx = out.Add(cname);
		    dataArray[0] = dataVal;
		    out.PutTime(rbnbOutTime,0);
		    out.PutDataAsFloat64(idx,dataArray);
		    do {
			try {
			    src.Flush(out);
			    break;
			} catch (Exception e) {
			    System.err.println("exception flushing to source, restarting");
			    e.printStackTrace();
			    makeSource();
			}
		    } while (true);
		    if (!bSilent) System.err.println("Put " + cname + " value: " + dataArray[0]);
		}
		
		// Calculate latency and send to the _Latency channel
		out.Clear();
		idx = out.Add("_Latency");
		double lat[] = new double[1];
		lat[0] = arrivalTime - embeddedTime;
		out.PutTime(rbnbOutTime,0);
		out.PutDataAsFloat64(idx,lat);
		do {
		    try {
			src.Flush(out);
			break;
		    } catch (Exception e) {
			System.err.println("exception flushing to source, restarting");
			e.printStackTrace();
			makeSource();
		    }
		} while (true);
		if (!bSilent) System.err.println("Put _Latency value: " + lat[0]);
		
		// JPW 02/13/2008: send the CSV string to the _CSV channel
		out.Clear();
		idx = out.Add("_CSV");
		out.PutTime(rbnbOutTime,0);
		// JPW 03/10/08: Output outputCSVStr, which only contains those
		//               components not skipped
		// out.PutDataAsString(idx,nextCSVStr);
		// Append a final "\n" to the CSV string
		outputCSVStr.append("\n");
		String outStr = outputCSVStr.toString();
		out.PutDataAsString(idx,outStr);
		do {
		    try {
			src.Flush(out);
			break;
		    } catch (Exception e) {
			System.err.println("exception flushing to source, restarting");
			e.printStackTrace();
			makeSource();
		    }
		} while (true);
		if (!bSilent) System.err.println("Put _CSV value: " + outStr);
            }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	
      } //end method process
      
   } //end class CSVDemux
   