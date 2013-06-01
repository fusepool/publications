package eu.fusepool.platform.enhancer.engine.pubmed;


import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.platform.enhancer.engine.pubmed.xslt.CatalogBuilder;
import eu.fusepool.platform.enhancer.engine.pubmed.xslt.XMLProcessor;
import eu.fusepool.platform.enhancer.engine.pubmed.xslt.impl.PubMedXMLProcessor;



@Component(immediate = true, metatype = true)
@Service
@Properties(value={
		@Property(name=EnhancementEngine.PROPERTY_NAME, value="PubMedEngine"),
		@Property(name=Constants.SERVICE_RANKING,intValue=PubMedEnhancementEngine.DEFAULT_SERVICE_RANKING)
})
public class PubMedEnhancementEngine 
extends AbstractEnhancementEngine<IOException,RuntimeException> 
implements EnhancementEngine, ServiceProperties {

	public static final String DEFAULT_ENGINE_NAME = "PubMedEngine";
	/**
	 * Default value for the {@link Constants#SERVICE_RANKING} used by this engine.
	 * This is a negative value to allow easy replacement by this engine depending
	 * to a remote service with one that does not have this requirement
	 */
	public static final int DEFAULT_SERVICE_RANKING = 101;
	/**
	 * The default value for the Execution of this Engine. Currently set to
	 * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}
	 */
	public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;


	// MIME TYPE of the PubMed document
	public static final String MIME_TYPE_XML = "application/xml";
	
	final Logger logger = LoggerFactory.getLogger(this.getClass()) ;

	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	/**
	 * The literal factory
	 */
	//private final LiteralFactory literalFactory = LiteralFactory.getInstance();

	public static final Set<String> supportedMediaTypes;
	static {
		Set<String> types = new HashSet<String>();
		//ensure everything is lower case
//		types.add(SupportedFormat.N3.toLowerCase());
//		types.add(SupportedFormat.N_TRIPLE.toLowerCase());
//		types.add(SupportedFormat.RDF_JSON.toLowerCase());
		types.add(SupportedFormat.RDF_XML.toLowerCase());
		types.add(SupportedFormat.TURTLE.toLowerCase());
//		types.add(SupportedFormat.X_TURTLE.toLowerCase());
		supportedMediaTypes = Collections.unmodifiableSet(types);
	}

	protected ComponentContext componentContext ;

	protected CatalogBuilder catalogBuilder ;
	
	
	@Reference
	protected Parser parser ;
	

	//@SuppressWarnings("unchecked")
	protected void activate(ComponentContext ce) throws IOException, ConfigurationException {
		super.activate(ce);
		this.componentContext = ce ;
		
		catalogBuilder = new CatalogBuilder(ce.getBundleContext()) ;
		try {
			catalogBuilder.build() ;
		} catch (Exception e1) {
			logger.error("Error building dtd catalog", e1) ;
		}


		logger.info("activating "+this.getClass().getName());

	}

	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
		catalogBuilder.cleanupFiles() ;
	}

	/*
	 * Check if content is present and mime type is correct (application/xml).
	 * 
	 */
	public int canEnhance(ContentItem ci) throws EngineException {
		 
        try {
            if ((ci.getBlob() == null)
                    || (ci.getBlob().getStream().read() == -1)) {
                return CANNOT_ENHANCE;
            }
            
            // MIME Type of the input patent document must be application/xml
            if (! ci.getMimeType().equals(this.MIME_TYPE_XML)) {
            	return CANNOT_ENHANCE;
            }
            
            
        } catch (IOException e) {
        	logger.warn("Failed to get the text for "
                    + "enhancement of content: " + ci.getUri() + " or wrong MIME TYPE (must be application/xml)");
            throw new InvalidContentException(this, ci, e);
        }
        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
        
        // or there are some reasons ?
		//return ENHANCE_SYNCHRONOUS;
	}

	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {

		UriRef contentItemId = ci.getUri();
		logger.info("UriRef: "+contentItemId.getUnicodeString()) ;
				
		try {
			
			//ci.getLock().writeLock().lock();
			
			// Transform the patent XML file into RDF
			MGraph xml2rdf = transformXML(ci);
						
			// Add a part to the content item as a text/plain representation of the XML document 
			addPartToContentItem(ci);
			
			// Create enhancements to each entity extracted from the XML
			MGraph enhancements = addEnhancements(ci, xml2rdf);
			
			// Add all the RDF triples to the content item metadata
			ci.getMetadata().addAll(xml2rdf);
			ci.getMetadata().addAll(enhancements);
			
			
		} catch (Exception e) {
			logger.error( "", e) ;			
		}
		/*
		finally {
			ci.getLock().writeLock().unlock();
		}
		*/
	}

	/*
	 *  Add a part to the content item as a text/plain representation of the XML document to be
	 *  used by the ECS for indexing.
	 */
	public void addPartToContentItem(ContentItem ci) {
		
		logger.debug("Start adding plain text representation");
		
		try {
			
			UriRef partUri = new UriRef("urn:fusepool-pubmed-engine:part-01:" + randomUUID()); // part uri with index 1 (part with index 0 is reserved to the input data)
			// Add the same content of the document as text/plain. This part can contain some
			// text extracted from the full content for indexing as title and abstract 
			byte [] content = IOUtils.toByteArray(ci.getBlob().getStream());
			
			// Add some content to the new part as plain text 
			ContentSource source = new ByteArraySource(content, "text/plain");
			ci.addPart(ci.getUri(), source);
			
			// Add metadata about the new part of the content item
			ci.getMetadata().add(new TripleImpl(ci.getUri(), DCTERMS.hasPart, partUri));
			
		}
		catch (IOException e) {
			
			logger.error("Error adding text/plain part", e) ;
			
		}
		
		
		logger.debug("Finished adding plain text representation");
	}
	
	/*
	 * Transform patent XML documents into RDF using an XSLT transformation and add the graph to the content item metadata.
	 */
	public MGraph transformXML(ContentItem ci) throws EngineException {
		
		MGraph xml2rdf = null;
		
		XMLProcessor processor = new PubMedXMLProcessor() ;
		InputStream rdfIs = null ; 
	
		logger.debug("Starting transformation from XML to RDF");
		
		try {
			
			xml2rdf = new IndexedMGraph();
			rdfIs = processor.processXML(ci.getStream()) ;
			parser.parse(xml2rdf, rdfIs, SupportedFormat.RDF_XML) ;
			rdfIs.close() ;
			
			
		} catch (Exception e) {
			logger.error("Wrong data format for the " + this.getName() + " enhancer.", e) ;			
		}
		
		logger.debug("Finished transformation from XML to RDF");
		
		return xml2rdf;
	}
	
	/*
	 * Create an entity annotation for each entity found by the transformation of the XML document. 
	 * Each annotation is referred to its entity.
	 */
	public MGraph addEnhancements(ContentItem ci, MGraph xml2rdf) {
		
		MGraph enhancements = new IndexedMGraph();
		
		if (! xml2rdf.isEmpty()) {
			
			Iterator<Triple> ipersons = xml2rdf.filter(null, RDF.type, FOAF.Person) ;
			
			
			while (ipersons.hasNext()) {
				// create an entity annotation
				UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this) ;
				
				Triple person = ipersons.next();
				NonLiteral subPerson = person.getSubject(); 
				
				// add a triple to link the enhancement to the entity
				Triple entityReference = new TripleImpl(entityAnnotation, TechnicalClasses.ENHANCER_ENHANCEMENT, subPerson);
				enhancements.add( entityReference);
				
				// add a confidence value
				Triple confidence = new TripleImpl(entityAnnotation, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0"));
				enhancements.add(confidence);			
			}
		}
		
		return enhancements;		
	}	
	
	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}


	//@Activate
	public void registered(ServiceReference ref) {
		logger.error(this.getClass().getName()
									+" registered") ;
	}

	//@Deactivate
	public void unregistered(ServiceReference ref) {
		logger.info(this.getClass().getName()+" unregistered") ;
	}



	/**
	 * Converts the type and the subtype of the parsed media type to the
	 * string representation as stored in {@link #supportedMediaTypes} and then
	 * checks if the parsed media type is contained in this list.
	 * @param mediaType the MediaType instance to check
	 * @return <code>true</code> if the parsed media type is not 
	 * <code>null</code> and supported. 
	 */
	@SuppressWarnings("unused")
	private boolean isSupported(String mediaType){
		return mediaType == null ? false : supportedMediaTypes.contains(
				mediaType.toLowerCase());
	}


}
