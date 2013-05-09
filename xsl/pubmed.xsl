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
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:mml="http://www.w3.org/1998/Math/MathML"

    xmlns:uuid="java:java.util.UUID"

    exclude-result-prefixes="xsl fn uuid"
    >
    <xsl:import href="common.xsl"/>

    <xsl:output encoding="utf-8" indent="yes" method="xml" omit-xml-declaration="no"/>

    <xsl:param name="pathToProvDocument"/>

    <xsl:strip-space elements="*"/>

    <xsl:variable name="xslDocument" select="'https://github.com/fusepool/publications/xsl/pubmed.xsl'"/>

    <xsl:template match="/article">
        <rdf:RDF>
            <xsl:variable name="pmid" select="front/article-meta/article-id[@pub-id-type='pmid']"/>

            <xsl:call-template name="front">
                <xsl:with-param name="pmid" select="$pmid" tunnel="yes"/>
            </xsl:call-template>
        </rdf:RDF>
    </xsl:template>


    <xsl:template name="front">
        <xsl:param name="pmid" tunnel="yes"/>

        <rdf:Description rdf:about="{concat($pubmed, $pmid)}">
            <rdf:type rdf:resource="{$bibo}Document"/>

<!--        <xsl:apply-templates name="journal-meta"/>-->
            <xsl:apply-templates select="front/article-meta"/>
        </rdf:Description>
    </xsl:template>

    <xsl:template match="front/journal-meta">
        <xsl:for-each select="issn">
            <bibo:issn><xsl:value-of select="@epub"/></bibo:issn>            
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="front/article-meta">
        <xsl:for-each select="article-id">
            <xsl:variable name="pub-id-type" select="@pub-id-type"/>
            <xsl:variable name="value" select="normalize-space(.)"/>

            <dcterms:identifier><xsl:value-of select="concat($pub-id-type, ':', $value)"/></dcterms:identifier>

            <xsl:choose>
                <xsl:when test="$pub-id-type = 'doi'">
                    <bibo:doi><xsl:value-of select="$value"/></bibo:doi>
                    <owl:sameAs rdf:resource="http://dx.doi.org/{$value}"/>
                </xsl:when>
                <xsl:when test="$pub-id-type = 'pmc'">
                    <dcterms:source rdf:resource="http://www.ncbi.nlm.nih.gov/pmc/articles/{$value}"/>
                    <owl:sameAs rdf:resource="http://biotea.idiginfo.org/pubmedOpenAccess/rdf/PMC{$value}"/>
                    <owl:sameAs rdf:resource="http://www.ncbi.nlm.nih.gov/pmc/articles/{$value}"/>
                    <dcterms:isFormatOf rdf:resource="http://www.ncbi.nlm.nih.gov/pmc/articles/{$value}"/>
                </xsl:when>
                <xsl:when test="$pub-id-type = 'pmid'">
                    <owl:sameAs rdf:resource="http://www.ncbi.nlm.nih.gov/pubmed/{$value}"/>
                    <owl:sameAs rdf:resource="http://linkedlifedata.com/resource/pubmed/id/{$value}"/>
                    <rdfs:seeAlso rdf:resource="http://identifiers.org/pubmed/{$value}"/>
                    <rdfs:seeAlso rdf:resource="http://bio2rdf.org/pubmed:{$value}"/>
                    <rdfs:seeAlso rdf:resource="http://europepmc.org/abstract/MED/{$value}"/>
                    <rdfs:seeAlso rdf:resource="http://www.hubmed.org/display.cgi?uids={$value}"/>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

        <xsl:apply-templates select="title-group/article-title"/>
        <xsl:apply-templates select="abstract"/>
        <xsl:apply-templates select="kwd-group/kwd"/>

        <xsl:apply-templates select="volume"/>
        <xsl:apply-templates select="issue"/>
        <xsl:apply-templates select="fpage"/>
        <xsl:apply-templates select="lpage"/>

        <xsl:apply-templates select="permissions/license/@xlink:href"/>
    </xsl:template>

    <xsl:template match="title-group/article-title">
        <dcterms:title><xsl:value-of select="."/></dcterms:title>
    </xsl:template>

    <xsl:template match="volume">
        <bibo:volume><xsl:value-of select="."/></bibo:volume>
    </xsl:template>
    <xsl:template match="issue">
        <bibo:issue><xsl:value-of select="."/></bibo:issue>
    </xsl:template>
    <xsl:template match="fpage">
        <bibo:pageStart><xsl:value-of select="."/></bibo:pageStart>
    </xsl:template>
    <xsl:template match="lpage">
        <bibo:pageEnd><xsl:value-of select="."/></bibo:pageEnd>
    </xsl:template>


    <xsl:template match="abstract">
        <dcterms:abstract><xsl:value-of select="*/text()"/></dcterms:abstract>
    </xsl:template>
    <xsl:template match="permissions/license/@xlink:href">
        <dcterms:license rdf:resource="{normalize-space(.)}"/>
    </xsl:template>

    <xsl:template match="kwd-group/kwd">
        <xsl:variable name="uri" select="concat('urn:uuid:', uuid:randomUUID())"/>

        <dcterms:subject>
            <rdf:Description rdf:about="{$uri}">
                <rdf:type rdf:resource="{$skos}Concept"/>
                <skos:inScheme rdf:resource="{$conceptPubMed}"/>
                <skos:topConceptOf>
                    <rdf:Description rdf:about="{$conceptPubMed}">
                        <skos:hasTopConcept rdf:resource="{$uri}"/>
                    </rdf:Description>
                </skos:topConceptOf>
                <skos:prefLabel><xsl:value-of select="."/></skos:prefLabel>
            </rdf:Description>
        </dcterms:subject>
    </xsl:template>

    <xsl:template name="back">
    </xsl:template>

</xsl:stylesheet>
