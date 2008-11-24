
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;  
import java.io.File;
import java.io.IOException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.*;

/* 
 * Parse an XML configuration file used to describe NASA IWG1 streams. This
 * file contains one or more <parameter>...</parameter> elements.  Each of
 * these parameter structures contains other fields.  XMLParser provides
 * access to the list of the following elements are subfields within the
 * parameter fields:
 *
 * <type>, <label>, <info>, and (optionally) <skip>
 *
 * XMLParser was originally developed by Jon Paul Barker on 5-17-05 for use
 * with XMLDemux.
 *
 */

/*
 *
 * 03/03/2008  JPW	Add support for a new field, <skip>. Provide access
 *			to this array using getSkip()
 *
 * 10/02/2007  JPW	Rename nParam as paramIndex
 *			Initialize paramIndex to -1 (it gets incremented upon
 *			first use in printNode()); also, DON'T decrement
 *			paramIndex when dealing with the Marker - that is,
 *			we now store Marker ID and Type in the respective
 *			arrays.
 *			getNParam() returns the total number of parameters,
 *			including the Marker parameter.
 *
 * 08/09/2007  JPW	Changes in printNode():
 *			Add bIsMarker; only want to pull out the marker	from
 *			the "format" node when the parameter == "Marker".
 *			For requesting the "id" attribute of the "parameter"
 *			element, if "id" doesn't match anything, request
 *			"xml:id"
 */

public class XMLParser {
    static boolean DEBUG = false;
    
    static String type[];
    static String label[];
    static String info[];
    static String id[];
    // JPW 03/03/08: Add bSkip array; specifies whether this parameter is to be skipped in the processing
    static boolean bSkip[];
    static String marker;
    static int size;
    // JPW 10/02/2007: Rename nParam as paramIndex;
    //                 initialize to -1 instead of 0
    static int paramIndex = -1;
    static int nMarker = 0;

    // An array of names for DOM node-types
    // (Array indexes = nodeType() values.)
    static final String[] typeName = {
	"none",
	"Element",
	"Attr",
	"Text",
	"CDATA",
	"EntityRef",
	"Entity",
	"ProcInstr",
	"Comment",
	"Document",
	"DocType",
	"DocFragment",
	"Notation",
	"parameter id",
    };
    
    //Constructor
    public XMLParser(org.w3c.dom.Node nodeI) {
	// JPW 03/03/08: Change the array size from 16384 to 2048
	// TO-DO: Make the array size dynamic, based on the XML config file
	int arraySize = 2048;
	type  = new String[arraySize];
	label = new String[arraySize];
	info  = new String[arraySize];
	id    = new String[arraySize];
	// JPW 03/03/08: Add bSkip array
	bSkip = new boolean[arraySize];
	// Initialize the bSkip array to all false (that is, by default, we
	// will use all parameters specified in the XML file)
	for (int i = 0; i < arraySize; ++i) {
	    bSkip[i] = false;
	}
	
	printNode(nodeI,"",false,"",false);
    }
    
    public XMLParser(String file) {
	marker = "";
	nMarker=0;
	// JPW 10/02/2007: Rename nParam as paramIndex;
	//                 initialize to -1 instead of 0
	paramIndex = -1;
	size = 0;
	
	DocumentBuilderFactory factory =
	    DocumentBuilderFactory.newInstance();
	
	try {
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document document = builder.parse( new File(file) );
	    
	    // setLength(document);
	    new XMLParser(document);
	} 
	catch (SAXParseException spe) {
	    // Error generated by the parser
	    System.out.println("\n** Parsing error"
			       + ", line " + spe.getLineNumber()
			       + ", uri " + spe.getSystemId());
	    System.out.println("   " + spe.getMessage() );
	    
	    // Use the contained exception, if any
	    Exception  x = spe;
	    if (spe.getException() != null)
		x = spe.getException();
	    x.printStackTrace();
	} 
	catch (SAXException sxe) {
	    // Error generated during parsing)
	    Exception  x = sxe;
	    if (sxe.getException() != null)
		x = sxe.getException();
	    x.printStackTrace();	    
	} 
	catch (ParserConfigurationException pce) {
	    // Parser with specified options can't be built
	    pce.printStackTrace();
	    
	} 
	catch (IOException ioe) {
	    // I/O error
	    ioe.printStackTrace();
	}
    } // end Constructor
    
    /*
     *  Main for testing purposes
     */
    public static void main(String argv[]) {
	XMLParser c = new XMLParser(argv[0]);
	
	for (int i = 0; i < c.getNParam(); ++i) {
	    System.out.println(type[i]+"\t"+label[i]+"\t"+info[i]+"\t"+bSkip[i]);
	}
	
	System.out.println("Channels: " + c.getNParam());
	System.out.println("Marker: " + marker);
	System.out.println("Size: " + size);
	System.out.println("Label.length()" + label.length);
    } // end main
	
   	
	//*************************************  	
	/*
	 * Recursively check all nodes.  When the parameter tag is encountered, 
	 * pull out <type>, <label>, <info>, and (optional) <skip> tags.
	 *
	 * 03/03/2008  JPW	Add support for <skip>
	 *
	 * 10/02/2007  JPW	Rename nParam as paramIndex
	 *			Don't decrement paramIndex when we are dealing
	 *			with the Marker; store the Marker ID and Type
	 *			just like any other parameter.
	 * 08/09/2007  JPW	Add bIsMarker; only want to pull out the marker
	 *			from the "format" node when the
	 *			parameter == "Marker"
	 *			For requesting the "id" attribute of the
	 *			"parameter" element, if "id" doesn't match
	 *			anything, request "xml:id"
	 */
	private void printNode(org.w3c.dom.Node nodeI, String prefix, boolean b, String str, boolean bIsMarker) {
	    String temp = nodeI.getNodeName();      
	    
	    if(temp.equals("parameter")) {
		// JPW 10/02/2007: Rename nParam as paramIndex
		paramIndex++;
		
		org.w3c.dom.NamedNodeMap na=nodeI.getAttributes();
		org.w3c.dom.Node nid = na.getNamedItem("id");
		// JPW 08/09/2007: if "id" doesn't turn up anything, try "xml:id"
		if (nid == null) {
		    nid = na.getNamedItem("xml:id");
		}
		id[paramIndex] = nid.getNodeValue();
		// System.err.println("paramIndex: "+paramIndex+", id: "+id[paramIndex]);
		
		org.w3c.dom.NodeList nodeList = nodeI.getChildNodes();
		for (int i=0; i<nodeList.getLength(); i++) {
		    org.w3c.dom.Node subnode = nodeList.item(i);
		    boolean b_is_marker = false;
		    if ( (id[paramIndex] != null) && (id[paramIndex].equals("Marker")) ) {
			b_is_marker = true;
		    }
		    printNode(subnode, prefix + "...", true,"", b_is_marker);
		}
	    }
	    
	    if(b) {
		if(nodeI.getNodeName().equals("#text")) {
		    if (str.equals("type")) {
			type[paramIndex] = nodeI.getNodeValue().trim();
			if(nodeI.getNodeValue().trim().equals("double"))
			    size+=8;
			if(nodeI.getNodeValue().trim().equals("float"))
			    size+=4;
		    }
		    
		    if(str.equals("label")) {
			label[paramIndex] = nodeI.getNodeValue().trim();
			//System.err.println("label["+paramIndex+"]: "+label[paramIndex]);
		    }
		    
		    if(str.equals("info")) {
			info[paramIndex] = nodeI.getNodeValue().trim();
		    }
		    
		    // JPW 03/03/08: Add support for <skip>; if the content is
		    //               "1", then mark skip as true; otherwise,
		    //               mark skip as false.
		    if(str.equals("skip")) {
			String skipStr = nodeI.getNodeValue().trim();
			if (skipStr.equals("1")) {
			    bSkip[paramIndex] = true;
			} else {
			    bSkip[paramIndex] = false;
			}
		    }
		    
		    if ( (str.equals("format")) && (bIsMarker) ) {  // this is a marker
			marker = marker+nodeI.getNodeValue().trim();	// concatenates markers
			size+=nodeI.getNodeValue().trim().length();
			// JPW 10/02/2007: Don't decrement paramIndex when we
			//                 are dealing with the Marker; store
			//                 the Marker ID and Type just like
			//                 any other parameter.
			// paramIndex--;
                        nMarker++;
		    }
		}  
         	
		//process all nodes
		if (nodeI.getNodeType() == Node.ELEMENT_NODE) {
		    //System.err.println( prefix + "node attributes: length = " + nodeI.getAttributes().getLength());
		    for (int i=0; i<nodeI.getAttributes().getLength(); ++i) {
			//System.err.println( prefix + "Item " + i + ":\n" + prefix + "<<<<<");
			printNode(nodeI.getAttributes().item(i), prefix + "---",false, nodeI.getNodeName(),false);
			//System.err.println(prefix + ">>>>>");
		    }
		}
	    }
	    
	    //process all nodes
	    org.w3c.dom.NodeList nodeList = nodeI.getChildNodes();
	    for (int i=0; i<nodeList.getLength(); i++) {
		org.w3c.dom.Node subnode = nodeList.item(i);
		printNode(subnode, prefix + "...",b,nodeI.getNodeName(),bIsMarker);
	    }
	    
	}
	
   	//--------------------------------------------------------------------
   	//get Methods
	public String[] getType(){
	    return type;}
	public String[] getInfo(){
	    return info;}
	// JPW 03/03/08: Make the skip array available
	public boolean[] getSkip(){
	    return bSkip;}
	public String[] getLabel(){
	    return label;}
	public int getSize(){
	    return size;}
	public String getMarker(){
	    return marker;}
	public String[] getID() {
	    return id; }
	public int getNParam() {
	    // JPW 10/02/2007: Return the total number of parameters,
	    //                 including the Marker
	    return paramIndex + 1; }
	
    }


