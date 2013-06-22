#!/bin/bash

namespace="http://fusepool.info/";
repoMeta="/home/fusepool/patents-reengineering/data/meta/";
pubmed="/data-new/pmc/";
xslPubMed="../src/main/resources/xsl/pubmed.xsl";
tdbAssembler="/usr/lib/fuseki/tdb.fusepool.ttl";
JVM_ARGS="-Xmx32000M";
db="/data/tdb/fusepool/";
javatdbloader="java $JVM_ARGS tdb.tdbloader --desc=$tdbAssembler";

saxonb="/usr/share/java/saxonb.jar";
xmlresolver="/usr/share/java/xml-resolver.jar";
pathToCatalog="../src/main/resources/catalog/catalog.xml";
xmlcatalog="-Dxml.catalog.files=$pathToCatalog -Dxml.catalog.verbosity=2";
rxy="-r:org.apache.xml.resolver.tools.CatalogResolver -x org.apache.xml.resolver.tools.ResolvingXMLReader -y org.apache.xml.resolver.tools.ResolvingXMLReader";
