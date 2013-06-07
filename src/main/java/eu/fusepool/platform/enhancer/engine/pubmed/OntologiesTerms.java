package eu.fusepool.platform.enhancer.engine.pubmed;

import org.apache.clerezza.rdf.core.UriRef;

/*
 * This class contains terms from ontologies that are not yet available
 * in org.apache.clerezza.rdf.ontologies.*
 */
public class OntologiesTerms {
	
	// FISE
	final static String FISE_NS = "http://fise.iks-project.eu/ontology/";
	final static UriRef fiseEntityReference = new UriRef(FISE_NS + "entity-reference");
	
	// BIBO
	final static String BIBO_NA = "http://purl.org/ontology/bibo/";
	final static UriRef biboDocument = new UriRef(BIBO_NA + "Document");

}
