package eu.fusepool.platform.enhancer.engine.pubmed;


import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
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

import eu.fusepool.datalifecycle.Rdfizer;
import eu.fusepool.platform.enhancer.engine.pubmed.OntologiesTerms;


@Component(immediate = true, metatype = true)
@Service
@Properties(value={
		@Property(name=EnhancementEngine.PROPERTY_NAME, value=PubMedEnhancementEngine.DEFAULT_ENGINE_NAME),
		@Property(name=Constants.SERVICE_RANKING,intValue=PubMedEnhancementEngine.DEFAULT_SERVICE_RANKING),
		@Property(name="CLEAN_ON_STARTUP", boolValue=false)
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

//	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	/**
	 * The literal factory
	 */
	//private final LiteralFactory literalFactory = LiteralFactory.getInstance();

	public static final Set<String> supportedMediaTypes;
	static {
		Set<String> types = new HashSet<String>();
		//ensure everything is lower case
		types.add(SupportedFormat.N3.toLowerCase());
		types.add(SupportedFormat.N_TRIPLE.toLowerCase());
		types.add(SupportedFormat.RDF_JSON.toLowerCase());
		types.add(SupportedFormat.RDF_XML.toLowerCase());
		types.add(SupportedFormat.TURTLE.toLowerCase());
		types.add(SupportedFormat.X_TURTLE.toLowerCase());
		supportedMediaTypes = Collections.unmodifiableSet(types);
	}

	protected ComponentContext componentContext ;
	
	
	@Reference
	protected Parser parser ;
	
	// Binding to pubmed rdfizer
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            referenceInterface=eu.fusepool.datalifecycle.Rdfizer.class)
    private Rdfizer rdfizer;
    public static final String RDFIZER_NAME = "pubmed";
	

	//@SuppressWarnings("unchecked")
	protected void activate(ComponentContext ce) throws IOException, ConfigurationException {
		super.activate(ce);
		this.componentContext = ce ;

		logger.info("PubMedEngine is being activated.");

	}

	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
		logger.info("PubMedEngine is being deactivated.");
	}
	
	/**
     * Bind pubmed rdfizer
     */
    protected void bindRdfizer(Rdfizer rdfizer) {
        
        if( RDFIZER_NAME.equals( rdfizer.getName() ) ) {
            this.rdfizer = rdfizer; 
            logger.info("Rdfizer " + rdfizer.getName() + " bound");            
        }
        
    }
    
    /**
     * Unbind pubmed rdfizer
     */
    protected void unbindRdfizer(Rdfizer rdfizer) {
        
        if( RDFIZER_NAME.equals( rdfizer.getName() ) ) {
            this.rdfizer = null;
            logger.info("Rdfizer " + rdfizer.getName() + " unbound");
            
        }
        
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
            if (! ci.getMimeType().equals(MIME_TYPE_XML)) {
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
		logger.info("UriRef: " + contentItemId.getUnicodeString()) ;
				
		try {
			
			// Transform the patent XML file into RDF
			MGraph xml2rdf = transformXML(ci);
			
			// Create enhancements to each entity extracted from the XML
			MGraph enhancements = addEnhancements(ci, xml2rdf);
			
			// Add all the RDF triples to the content item metadata
			ci.getMetadata().addAll(xml2rdf);
			ci.getMetadata().addAll(enhancements);
			
			// Add a part to the content item as a text/plain representation of the XML document for indexing. 
			addPartToContentItem(ci);
			
			
		} catch (Exception e) {
			logger.error( "Error while computing the enhancements.", e) ;			
		}
		/*
		finally {
			ci.getLock().writeLock().unlock();
		}
		*/
	}

	/*
	 *  Add a part to the content item as a text/plain representation of the XML document to be
	 *  used by the ECS for indexing. The part text is constructed from triples properties values so 
	 *  this method must be called after the xml to rdf transformation and after the rdf triple have 
	 *  been added to the content item metadata.
	 */
	public void addPartToContentItem(ContentItem ci) {
		
		logger.debug("Start adding plain text representation");
		
		try {
			
			UriRef partUri = new UriRef("urn:fusepool-pubmed-engine:part-01:" + randomUUID()); // part uri with index 1 (part with index 0 is reserved to the input data)
			// Add the same content of the document as text/plain. This part can contain some
			// text extracted from the full content for indexing as title and abstract 
			
			// full document
			//byte [] content = IOUtils.toByteArray(ci.getBlob().getStream());
			
			// construct the text for the part from triples properties values
			@SuppressWarnings("deprecation")
			byte [] content = IOUtils.toByteArray(constructText(ci.getMetadata()));
			
			// Add some content to the new part as plain text 
			ContentSource source = new ByteArraySource(content, "text/plain");
			ci.addPart(ci.getUri(), source);
			
			// Add metadata about the new part of the content item
			ci.getMetadata().add(new TripleImpl(ci.getUri(), DCTERMS.hasPart, partUri));
			
		}
		catch (IOException e) {
			
			logger.error("Error adding text/plain part", e) ;
			
		}
		
		
		logger.debug("Finished adding plain text part representation");
	}
	
	/*
	 * Transform patent XML documents into RDF using an XSLT transformation and add the graph to the content item metadata.
	 */
	private MGraph transformXML(ContentItem ci) throws EngineException { 
	
		logger.debug("Starting transformation from XML to RDF");
		
		MGraph documentGraph = rdfizer.transform(ci.getStream());
		
		logger.debug("Finished transformation from XML to RDF");
		
		return documentGraph;
	}
	
	/*
	 * Create an entity annotation for each entity found by the transformation of the XML document. 
	 * Each annotation is referred to its entity. The types of entities are: 
	 * foaf:Person
	 * foaf:Organization
	 * bibo:Document
	 * pmo:PatentPublication, schema:PostalAddress
	 */
	public MGraph addEnhancements(ContentItem ci, MGraph xml2rdf) {
		
		MGraph enhancements = new IndexedMGraph();
		
		UriRef documentUri = getDocumentUri(xml2rdf); //uri of the document in the content item (not of other document mentioned in it)
		
		if (! xml2rdf.isEmpty()) {
			
			// Create an enhancement for each entity of type foaf:Person that are contributors of the document
			List<UriRef> contributorsList = getContributors(documentUri,xml2rdf);
			Iterator<UriRef> icontributors = contributorsList.listIterator();			
			while (icontributors.hasNext()) {
				// create an entity annotation
				UriRef personEnhancement = EnhancementEngineHelper.createEntityEnhancement(ci, this) ;
				
				UriRef contributor = icontributors.next(); 
				
				// add a triple to link the enhancement to the entity referenced
				//Triple entityReference = new TripleImpl(personEnhancement, OntologiesTerms.fiseEntityReference, contributor);
				enhancements.add( new TripleImpl(personEnhancement, OntologiesTerms.fiseEntityReference, contributor) );
				
				// add a confidence value
				//Triple confidence = new TripleImpl(personEnhancement, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0"));
				enhancements.add(new TripleImpl(personEnhancement, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0")) );			
			}
			
			// Create an enhancement for each entity of type foaf:Organizazion that are contributors' affiliatons 
			Iterator<Triple> iorganizations = xml2rdf.filter(null, RDF.type, FOAF.Organization) ;
			while (iorganizations.hasNext()) {
				// create an entity annotation
				UriRef organizationEnhancement = EnhancementEngineHelper.createEntityEnhancement(ci, this) ;
				
				UriRef organization = (UriRef) iorganizations.next().getSubject(); 
				
				// add a triple to link the enhancement to the entity referenced
				//Triple entityReference = new TripleImpl(organizationEnhancement, OntologiesTerms.fiseEntityReference, organization);
				enhancements.add( new TripleImpl( organizationEnhancement, OntologiesTerms.fiseEntityReference, organization) );
				
				// add a confidence value
				//Triple confidence = new TripleImpl(organizationEnhancement, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0"));
				enhancements.add(new TripleImpl( organizationEnhancement, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0")) );			
			}
			
			// Create one enhancement for one entity of type bibo:Document that is directly related to the input XML document.
			if( documentUri != null) {
				UriRef documentEnhancement = EnhancementEngineHelper.createEntityEnhancement(ci, this) ;
				// add a triple to link the enhancement to the entity			
				enhancements.add( new TripleImpl(documentEnhancement, OntologiesTerms.fiseEntityReference, documentUri) );
				// add the confidence level
				enhancements.add( new TripleImpl(documentEnhancement, TechnicalClasses.FNHANCER_CONFIDENCE_LEVEL, new PlainLiteralImpl("1.0")) );
			}
		}
		
		return enhancements;		
	}	
	
	/*
	 * Creates a string filled with values from properties:
	 * dcterms:title of the publication
	 * dcterms:abstract of the publication
	 * foaf:firstName and foaf:lastName of contributors
	 * The text is used for indexing. The graph passed as argument must contain the RDF triples created after the transformation.
	 */
	public String constructText(MGraph xml2rdf) {
		
		String text = "";
		
		UriRef documentUri = getDocumentUri(xml2rdf);
		
		// Get the title
		Iterator<Triple> ititles = xml2rdf.filter(documentUri, DCTERMS.title, null);
		String title = "";
		while(ititles.hasNext()) {
			title = unquote( ititles.next().getObject().toString() ) + " ";
			text += title;
		}
		
		
		// Get the abstract
		Iterator<Triple> iabstracts = xml2rdf.filter(documentUri, DCTERMS.abstract_, null);
		String abstract_ = " ";
		while(iabstracts.hasNext()) {
			abstract_ = unquote( iabstracts.next().getObject().toString() ) + " ";
			text += abstract_;
		}
		
		// Get all the foaf:firstName and foaf:lastName of entities of type foaf:Person (contributors).
		Iterator<UriRef> ipersonNames = getContributors(documentUri, xml2rdf).iterator();
		
		while(ipersonNames.hasNext()) {
			
			String personName = "";
			
			UriRef contributor = ipersonNames.next();
			
			Iterator<Triple> inames = xml2rdf.filter(contributor, FOAF.firstName, null);
			while(inames.hasNext()) {
				personName += unquote( inames.next().getObject().toString() ) + " ";
			}
			Iterator<Triple> ilastnames = xml2rdf.filter(contributor, FOAF.lastName, null);
			while(ilastnames.hasNext()) {
				personName += unquote( ilastnames.next().getObject().toString() ) + " ";
			}
			
			text += personName;
		}
		
		// Get the organization of affiliation
		Iterator<Triple> iorganizations = xml2rdf.filter(null, RDF.type, FOAF.Organization);
		String organizationName = "";
		while(iorganizations.hasNext()) {
			UriRef organization = (UriRef) iorganizations.next().getSubject();
			Iterator<Triple> inames = xml2rdf.filter(organization, FOAF.name, null);
			while(inames.hasNext()) {
				organizationName += unquote( inames.next().getObject().toString() );
			}
			
			text += organizationName;
			
		}
		logger.info("Text to be indexed" + text);
		
		return text;
	}
	
	/*
	 * Retrieves the uri that refers to the original document. As this publication can refer to other publication the first one must be
	 * selected using properties that are filled that are not for the mentioned document as title and abstract. These properties can also be used for the plain text
	 * representation of the document to be indexed instead of the full XML document. 
	 */
	public UriRef getDocumentUri(MGraph xml2rdf) {
		
		UriRef documentUri = null;
		
		Iterator<Triple> idocuments = xml2rdf.filter(null, RDF.type, OntologiesTerms.biboDocument) ;
		
		while(idocuments.hasNext()) {
			
			UriRef documentUriTemp = (UriRef) idocuments.next().getSubject();
			// Filter triple with a bibo:Document as subject and with dcterms:title property filled. There exist only one such triple in each 
			// document.
			Iterator<Triple> idocumentWithTitle = xml2rdf.filter(documentUriTemp, DCTERMS.title, null);
			while(idocumentWithTitle.hasNext()) {
				documentUri = (UriRef) idocumentWithTitle.next().getSubject(); 
			}
			
		}
				
		return documentUri;
	
	}
	
	/*
	 * Gets a list of all contributors of the document. It returns a list of contributors' URIs 
	 */
	public List<UriRef> getContributors(UriRef documentUri, MGraph xml2rdf) {
		
		List<UriRef> contributors = new ArrayList<UriRef>();
		
		Iterator<Triple> icontributors = xml2rdf.filter(documentUri, DCTERMS.contributor, null);
		
		while(icontributors.hasNext()) {
			
			contributors.add((UriRef) icontributors.next().getObject());
			
		}
		
		return contributors;
	}
	
	/*
	 * Removes quotations from properties values
	 * 
	 */
	public String unquote(String string) {
		String result = "";
		
		if(string.length() > 3) {
		
			result = string.replace('"', ' ');
		
		}
		
		return result;
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
