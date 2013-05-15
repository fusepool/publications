#!/bin/bash

. ./pubmed.config.sh

JVM_ARGS="-Xmx5000M";

java "$JVM_ARGS" -classpath "$saxonb:$xmlresolver" -Dxml.catalog.files="$pathToCatalog" -Dxml.catalog.verbosity=2 net.sf.saxon.Transform -r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader -ext:on -t -tree:linked -s ../data/pmc/AAPS_J/AAPS_J_2008_Apr_2_10\(1\)_193-199.nxml -xsl "$xslPubMed" > ../data/pmc/AAPS_J/AAPS_J_2008_Apr_2_10\(1\)_193-199.rdf

java "$JVM_ARGS" -classpath "$saxonb:$xmlresolver" -Dxml.catalog.files="$pathToCatalog" -Dxml.catalog.verbosity=2 net.sf.saxon.Transform -r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader -ext:on -t -tree:linked -s ../data/pmc/Brain/Brain_2008_Aug_20_131\(8\)_2181-2191.nxml -xsl "$xslPubMed" > ../data/pmc/Brain/Brain_2008_Aug_20_131\(8\)_2181-2191.rdf

java "$JVM_ARGS" -classpath "$saxonb:$xmlresolver" -Dxml.catalog.files="$pathToCatalog" -Dxml.catalog.verbosity=2 net.sf.saxon.Transform -r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader -ext:on -t -tree:linked -s ../data/pmc/Brain/Brain_2013_May_4_136\(5\)_1462-1475.nxml -xsl "$xslPubMed" > ../data/pmc/Brain/Brain_2013_May_4_136\(5\)_1462-1475.rdf

