#!/bin/bash

namespace="http://fusepool.info/";
repoMeta="/home/fusepool/patents-reengineering/data/meta/";
pubmed="/data-new/pubmed/";
xslPubMed="/var/www/publications/xsl/pubmed.xsl";
tdbAssembler="/usr/lib/fuseki/tdb.pubmed.ttl";
JVM_ARGS="-Xmx32000M";
db="/data/tdb/fusepool/";
javatdbloader="java $JVM_ARGS tdb.tdbloader --desc=$tdbAssembler";
