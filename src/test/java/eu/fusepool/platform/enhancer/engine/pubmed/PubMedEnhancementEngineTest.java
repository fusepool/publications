/**
 * 
 */
package eu.fusepool.platform.enhancer.engine.pubmed;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
	//private static final String TEST_FILE = "Arthritis_2010_Dec_20_2010_924518.nxml";
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
		
		
		
		if (! graph.isEmpty()) {
			
			// Filter triples for persons
			Iterator<Triple> ipersons = graph.filter(null, RDF.type, FOAF.Person) ;
			
			int personsNumber = 0;
			while (ipersons.hasNext()){
				personsNumber += 1;
				Triple triple = ipersons.next();
				System.out.println(triple.toString());
			}
			
			System.out.println("Total number of entity of type foaf:Person: " + personsNumber);
			
			// Filter triples for documents
			Iterator<Triple> idocuments = graph.filter(null, RDF.type, OntologiesTerms.biboDocument) ;
			
			int documentsNumber = 0;
			while (idocuments.hasNext()){
				documentsNumber += 1;
				Triple triple = idocuments.next();
				System.out.println(triple.toString());
			}
			
			System.out.println("Total number of entity of type bibo:Document: " + documentsNumber);

			
		}
		else {
			System.out.println("Enhancement graph empty !");
		}
		
		//assertTrue("Subjects of type foaf:Person found in the document " + personsNumber, personsNumber == PERSONS_NUMBER);
		
	}
	
	
	/*
	 * Test if entity-reference annotations have been added for each entity found after the transformation of the input XML file into RDF.
	 */
	
	@Test
	public void testAddEnhancements() {
		
		MGraph xml2rdf = null;
		
		try {
			
			xml2rdf = engine.transformXML(ci);
			
		} catch (EngineException e) {
			 
			System.out.println("Error while transforming the XML file into RDF");
		}
		
		MGraph enhancementGraph = engine.addEnhancements(ci, xml2rdf);
		
		
		
		if (! enhancementGraph.isEmpty()) {
			
			int entityReferences = 0;
			// Filter triples for entities references
			Iterator<Triple> ireferences = enhancementGraph.filter(null, OntologiesTerms.fiseEntityReference, null);
			while (ireferences.hasNext()) {
				entityReferences += 1;
				Triple reference = ireferences.next();
				//Triple enhancement-reference: <enhancement> <fise:entity-reference> <entity>
				UriRef enhancement = (UriRef) reference.getSubject();
				UriRef entity = (UriRef) reference.getObject();
				// entity type
				Iterator<Triple> itypes = xml2rdf.filter(entity, RDF.type, null);
				String entityType = "";
				while(itypes.hasNext()) {
					entityType = itypes.next().getObject().toString();
				}
				System.out.println("Entity references " + entityReferences + ") "  + enhancement + ", entity reference: " + entity + ", type: " + entityType);
			}
			
			System.out.println("Total number of entity references: " + entityReferences);
		}
		else {
			System.out.println("Enhancement graph empty !");
		}
		
		//assertTrue("Entities found in the document " + entityReferences, entityReferences == ENTITIES_REFERENCED);
	
	
	}
	
	
	/*
	 * Test whether a plain text representation part of document has been added to the content item. 
	 */
	@Test
	public void testAddPartToContentItem() {
		
		String plainTextPart = "";
		
		engine.addPartToContentItem(ci);
		
		// Reading part with index 1 of the content item that contain an object of type ContentSource
		ByteArraySource source = ci.getPart(1, ByteArraySource.class);
		
		// Get the MIME TYPE of the part
		String partMimeType = source.getMediaType();
		
		
		try {
			
			plainTextPart = IOUtils.toString(source.getStream());
			
			//System.out.println(plainTextPart);
		}
		catch(IOException e) {
			System.out.println("Error while reading plain text part");
		}
		
		Iterator<Triple> iparts = ci.getMetadata().filter(ci.getUri(), DCTERMS.hasPart, null);
		while(iparts.hasNext()) {
			System.out.println("Content Item parts: " + iparts.next().getObject().toString());
		}
		
		assertTrue("text/plain".equals(partMimeType));
		
		
	}
	
	@Test
	public void testConstructText() {
		
		MGraph xml2rdf = null;
		
		try {
			
			xml2rdf = engine.transformXML(ci);
			
		} catch (EngineException e) {
			 
			System.out.println("Error while transforming the XML file into RDF");
		}
		
		String text = engine.constructText(xml2rdf);
		
		System.out.println("TEXT FOR INDEXING: " + text);
		
	}
	
	@Test
	public void testGetDocumentUri() {
		
		MGraph xml2rdf = null;
		
		try {
			
			xml2rdf = engine.transformXML(ci);
			
		} catch (EngineException e) {
			 
			System.out.println("Error while transforming the XML file into RDF");
		}
		
		UriRef documentUri = engine.getDocumentUri( xml2rdf );
		
		System.out.println("Document URI: " + documentUri);
		
		Iterator<Triple> idocumentWithTitle = xml2rdf.filter(documentUri, DCTERMS.title, null);
		
		while(idocumentWithTitle.hasNext()){
			
				System.out.println("Title: " + idocumentWithTitle.next().getObject().toString());
			
		}
		
		//assertTrue(hasSameTitle);
	}
	
	@Test
	public void testGetContributors() {
		
		MGraph xml2rdf = null;
		
		try {
			
			xml2rdf = engine.transformXML(ci);
			
		} catch (EngineException e) {
			 
			System.out.println("Error while transforming the XML file into RDF");
		}
		
		UriRef documentUri = engine.getDocumentUri( xml2rdf );
		
		Iterator<UriRef> icontributors = engine.getContributors(documentUri, xml2rdf).iterator();
		
		while(icontributors.hasNext()) {
			UriRef contributor = icontributors.next();
			Iterator<Triple> itriples = xml2rdf.filter(contributor, FOAF.lastName, null);
			while(itriples.hasNext()) {
				String lastname = itriples.next().getObject().toString();
				System.out.println("Contributor's lastname: " + lastname);
			}
		}
		
		
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
