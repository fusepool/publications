/**
 * 
 */
package eu.fusepool.platform.enhancer.engine.pubmed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.fusepool.platform.enhancer.engine.pubmed.PubMedEnhancementEngine;
import eu.fusepool.platform.enhancer.engine.pubmed.testutil.MockComponentContext;

/**
 * @author Giorgio Costa
 * @author Luigi Selmi
 *
 */
public class PubMedEnhancementEngineTest {


	static PubMedEnhancementEngine engine ;
	static MockComponentContext ctx ;
	
	private static ContentItem ci = null ;

	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	// The file used for these tests must not be changed. Results, such as number of entities and enhancements, depend on this file.
	// If another file is used the following values must be updated accordingly
	private static final String TEST_FOLDER = "/test/data/";
	//private static final String TEST_FILE = "505-520.xml";
	private static final String TEST_FILE = "AAPS_J_2008_Apr_2_10(1)_193-199.nxml";
	private final int PERSONS_NUMBER = 3; // number of entities of type foaf:Person extracted from the test file.



	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		//properties.put("CLEAN_ON_STARTUP", false) ;
		properties.put(EnhancementEngine.PROPERTY_NAME, "PubMedEngine") ;
		ctx = new MockComponentContext(properties) ;

		engine = new PubMedEnhancementEngine() ;
		engine.parser = Parser.getInstance() ;
		//Set<String> supportedFormats = engine.parser.getSupportedFormats() ;
		engine.activate(ctx) ;
		
		// creates a content item from the document and compute the enhancements
		createContentItemFromFile(TEST_FILE);
	}

	@AfterClass 
	public static void clean() {
		engine.deactivate(ctx) ;
		File test_data_folder = new File(MockComponentContext.BUNDLE_TEST_DATA_FOLDER) ;
		if(test_data_folder.exists() && test_data_folder.isDirectory())
			test_data_folder.delete() ;
	}
	
	/*
	 * Test if subjects of type person are found after the transformation and prints them
	 */
	
	@Test
	public void testTransformXML() {
		
		MGraph graph = null;
		
		try {
			
			graph = engine.transformXML(ci);
			
		} catch (EngineException e) {
			 
			System.out.println("Error while transforming the XML file into RDF");
		}
		
		int personsNumber = 0;
		
		if (! graph.isEmpty()) {
			
			// Filter triples for persons
			
			//Iterator<Triple> ipersons = graph.filter(null, RDF.type, FOAF.Person) ;
			Iterator<Triple> ipersons = graph.filter(null, RDF.type, null) ;
			
			while (ipersons.hasNext()){
				personsNumber += 1;
				Triple triple = ipersons.next();
				System.out.println(triple.toString());
				//String subjectUri = triple.getSubject().toString();
				//System.out.println("Filtered subject of type foaf:Person = " + subjectUri);
				
			}
			
			
		}
		else {
			System.out.println("Enhancement graph empty !");
		}
		
		//assertTrue("Subjects of type foaf:Person found in the document " + personsNumber, personsNumber == PERSONS_NUMBER);
		
	}



	private static void createContentItemFromFile(String fileName) {
		
		String filePath = TEST_FOLDER + fileName;
		try {
			InputStream in = PubMedEnhancementEngineTest.class.getResourceAsStream(filePath) ;
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			String content = writer.toString();
			//System.out.println(theString);
			ci = ciFactory.createContentItem(new UriRef("urn:test:content-item:") + fileName, new StringSource(content)) ;
			
		}
		catch (IOException e) {
			System.out.println("Error while creating content item from file " + filePath);
		} 
		
	}

}
