<!--
    Author: Sarven Capadisli <info@csarven.ca>
    Author URI: http://csarven.ca/#i

    Description: XSLT for PubMed data
-->
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:owl="http://www.w3.org/2002/07/owl#"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
    xmlns:fn="http://270a.info/xpath-function/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:foaf="http://xmlns.com/foaf/0.1/"
    xmlns:prov="http://www.w3.org/ns/prov#"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    xmlns:xkos="http://purl.org/linked-data/xkos#"
    xmlns:property="http://fusepool.info/property/"
    xmlns:schema="http://schema.org/"
    xmlns:bibo="http://purl.org/ontology/bibo/"
    xmlns:uuid="java:java.util.UUID"

    exclude-result-prefixes="xsl fn uuid"
    >
    <xsl:import href="common.xsl"/>

    <xsl:output encoding="utf-8" indent="yes" method="xml" omit-xml-declaration="no"/>

    <xsl:param name="pathToProvDocument"/>

    <xsl:strip-space elements="*"/>

    <xsl:variable name="xslDocument" select="'https://github.com/fusepool/publications/xsl/pubmed.xsl'"/>

    <xsl:template match="/">
        <rdf:RDF>
            <xsl:apply-templates select="front"/>

<!--            <xsl:apply-templates select="front"/>-->

            <xsl:variable name="pmid" select="front/article-meta/article-id[@pub-id-type='pmid']"/>

            <rdf:Description rdf:about="{concat($pubmed, $pmid)}">
                <rdf:type rdf:resource="{$bibo}Document"/>
            </rdf:Description>
        </rdf:RDF>
    </xsl:template>


    <xsl:template name="front">
<!--        <xsl:apply-templates name="journal-meta"/>-->
        <xsl:apply-templates select="article-meta"/>
    </xsl:template>


    <xsl:template name="article-meta">

    </xsl:template>


    <xsl:template name="back">
    </xsl:template>

</xsl:stylesheet>
