package eu.fusepool.enhancer.pubmed;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fusepool.enhancer.pubmed.xslt.CatalogBuilder;
import eu.fusepool.enhancer.pubmed.xslt.XMLProcessor;
import eu.fusepool.enhancer.pubmed.xslt.impl.PubMedXMLProcessor;



@Component(immediate = true, metatype = true)
@Service
@Properties(value={
		@Property(name=EnhancementEngine.PROPERTY_NAME, value="PubMedEngine"),
		@Property(name=Constants.SERVICE_RANKING,intValue=PubMedLifterEnhancementEngine.DEFAULT_SERVICE_RANKING)
})
public class PubMedLifterEnhancementEngine 
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



	
	final Logger logger = LoggerFactory.getLogger(this.getClass()) ;

	
	
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
//		types.add(SupportedFormat.TURTLE.toLowerCase());
//		types.add(SupportedFormat.X_TURTLE.toLowerCase());
		supportedMediaTypes = Collections.unmodifiableSet(types);
	}

	protected ComponentContext componentContext ;

	protected CatalogBuilder catalogBuilder ;
	
	


	@Reference
	protected Parser parser ;

	//protected TripleCollection tripleCollection ;
	

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

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		return ENHANCE_SYNCHRONOUS;
	}

	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		UriRef contentItemId = ci.getUri();
		logger.info("UriRef: "+contentItemId.getUnicodeString()) ;

		

		

		//TODO: check if mimetype is supported
//		String mimeType = ci.getMimeType() ;
//		if(!isSupported(mimeType)) {
//			throw new EngineException("Cannot enhance mimetype: "+mimeType) ;
//		}
		
		// Load rdf graph 
		MGraph rdfGraph = new IndexedMGraph();
		
		try {
//			File fTrans = componentContext.getBundleContext().getDataFile("dump.xml") ;
//			FileOutputStream file = new FileOutputStream(fTrans) ;
//			IOUtils.copy(ci.getStream(), file) ;	
			ci.getLock().writeLock().lock();
			XMLProcessor processor = new PubMedXMLProcessor() ;
			InputStream rdfIs = null ;
			try {
				rdfIs = processor.processXML(ci.getStream()) ;
//				fTrans = componentContext.getBundleContext().getDataFile("transformed.xml") ;
//				FileOutputStream file2 = new FileOutputStream(fTrans) ;
//				IOUtils.copy(rdfIs, file2) ;
				parser.parse(rdfGraph, rdfIs, SupportedFormat.RDF_XML) ;
//				file.close() ;
				
				
				rdfIs.close() ;
			} catch (Exception e) {
				logger.error("Wrong data format for the "+this.getName()+" enhancer", e) ;
				return ;
			}
		
		  
	
		ci.getMetadata().addAll(rdfGraph) ;
		
			
		} catch (Exception e) {
			logger.error("", e) ;
			
		} finally {
			ci.getLock().writeLock().unlock();
		}
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
