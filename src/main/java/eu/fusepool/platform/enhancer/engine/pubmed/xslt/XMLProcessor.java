package eu.fusepool.platform.enhancer.engine.pubmed.xslt;

import java.io.InputStream;

public interface XMLProcessor {

	public abstract InputStream processXML(InputStream is)
			throws Exception;

}