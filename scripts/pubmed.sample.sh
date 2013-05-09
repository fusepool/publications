#!/bin/bash

. ./pubmed.config.sh

JVM_ARGS="-Xmx5000M";

saxonb="/usr/share/java/saxonb.jar";
xmlresolver="/usr/share/java/xml-resolver.jar";
xmlcatalog="-Dxml.catalog.files=../lib/catalog.xml -Dxml.catalog.verbosity=2"
rxy="-r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader"

java "$JVM_ARGS" -classpath "$saxonb:$xmlresolver" -Dxml.catalog.files=/var/www/publications/lib/catalog.xml -Dxml.catalog.verbosity=2 net.sf.saxon.Transform -r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader -t -tree:linked -s ../data/pmc/Brain/Brain_2008_Aug_20_131\(8\)_2181-2191.nxml -xsl "$xslPubMed" > ../data/pmc/Brain/Brain_2008_Aug_20_131\(8\)_2181-2191.rdf

java "$JVM_ARGS" -classpath "$saxonb:$xmlresolver" -Dxml.catalog.files=/var/www/publications/lib/catalog.xml -Dxml.catalog.verbosity=2 net.sf.saxon.Transform -r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader -t -tree:linked -s ../data/pmc/Brain/Brain_2013_May_4_136\(5\)_1462-1475.nxml -xsl "$xslPubMed" > ../data/pmc/Brain/Brain_2013_May_4_136\(5\)_1462-1475.rdf

