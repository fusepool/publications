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
    xmlns:scoro="http://purl.org/spar/scoro/"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:mml="http://www.w3.org/1998/Math/MathML"

    xmlns:uuid="java:java.util.UUID"

    exclude-result-prefixes="xsl fn uuid xlink"
    >
    <xsl:import href="common.xsl"/>

    <xsl:output encoding="utf-8" indent="yes" method="xml" omit-xml-declaration="no"/>

    <xsl:param name="pathToProvDocument"/>

    <xsl:strip-space elements="*"/>

    <xsl:variable name="xslDocument" select="'https://github.com/fusepool/publications/xsl/pubmed.xsl'"/>

    <xsl:key name="pub-id-type" match="/article/front/article-meta/article-id" use="@pub-id-type"/>
    <xsl:key name="aff" match="//aff" use="@id"/>

    <xsl:template match="/article">
        <rdf:RDF>
            <xsl:apply-templates select="front"/>
            <xsl:apply-templates select="body"/>
            <xsl:apply-templates select="back"/>
        </rdf:RDF>
    </xsl:template>


    <xsl:template match="front">
        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>

        <xsl:variable name="affiliations">
            <rdf:RDF>
                <xsl:for-each select="//aff">
                    <rdf:Description rdf:about="{concat($entityID, uuid:randomUUID())}">
                        <rdf:type rdf:resource="{$foaf}Organization"/>
                        <rdfs:value><xsl:value-of select="@id"/></rdfs:value>

                        <xsl:choose>
                            <xsl:when test="institution or addr-line or country or fax or phone or email or uri">
                                <xsl:apply-templates select="institution"/>
                                <xsl:apply-templates select="email"/>
                                <xsl:if test="uri">
                                    <foaf:homepage rdf:resource="normalize-space(uri)"/>
                                </xsl:if>

                                <xsl:if test="addr-line or country or fax or phone">
                                    <schema:address>
                                        <rdf:Description rdf:about="{concat($entityID, uuid:randomUUID())}">
                                            <rdf:type rdf:resource="{$schema}PostalAddress"/>

                                            <xsl:apply-templates select="addr-line"/>
                                            <xsl:apply-templates select="fax"/>
                                            <xsl:apply-templates select="phone"/>
                                        </rdf:Description>
                                    </schema:address>
                                </xsl:if>
                            </xsl:when>

                            <xsl:otherwise>
                                <foaf:name><xsl:value-of select="."/></foaf:name>
                                <rdfs:label><xsl:value-of select="."/></rdfs:label>
                            </xsl:otherwise>
                        </xsl:choose>
                    </rdf:Description>
                </xsl:for-each>
            </rdf:RDF>
        </xsl:variable>

        <rdf:Description rdf:about="{concat($pmc, $pmcid)}">
            <rdf:type rdf:resource="{$bibo}Document"/>

            <xsl:apply-templates select="journal-meta"/>
            <xsl:call-template name="article-meta">
                <xsl:with-param name="affiliations" select="$affiliations" tunnel="yes"/>
            </xsl:call-template>
        </rdf:Description>
        <xsl:copy-of select="$affiliations/rdf:RDF/*"/>
    </xsl:template>

    <xsl:template match="journal-meta">
        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>

        <xsl:variable name="issn">
            <xsl:variable name="i" select="normalize-space(issn[@pub-type='epub'])"/>
            <xsl:choose>
                <xsl:when test="$i != ''">
                    <xsl:value-of select="$i"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="normalize-space(issn[@pub-type='ppub'])"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="issue" select="normalize-space(../article-meta/issue)"/>

        <xsl:variable name="issuePath">
            <xsl:if test="$issue != ''">
                <xsl:value-of select="replace(concat($uriThingSeparator, $issue), ' ', '-')"/>
            </xsl:if>
        </xsl:variable>

        <xsl:variable name="journalURL" select="concat($journal, $issn)"/>
        <xsl:variable name="issueURL" select="concat($journalURL, $issuePath)"/>

        <dcterms:isPartOf>
            <rdf:Description rdf:about="{$issueURL}">
                <rdf:type rdf:resource="{$bibo}Issue"/>
                <dcterms:hasPart rdf:resource="{concat($pmc, $pmcid)}"/>
                <bibo:issue><xsl:value-of select="$issue"/></bibo:issue>

                <xsl:if test="$issue != ''">
                    <dcterms:isPartOf>
                        <rdf:Description rdf:about="{$journalURL}">
                            <rdf:type rdf:resource="{$bibo}Journal"/>
                            <dcterms:hasPart rdf:resource="{$issueURL}"/>
                            <dcterms:identifier rdf:resource="urn:issn:{$issn}"/>
                            <dcterms:title><xsl:value-of select="journal-title-group/journal-title"/></dcterms:title>
                            <bibo:issn><xsl:value-of select="$issn"/></bibo:issn>
                            <dcterms:publisher>
                                <rdf:Description rdf:about="{concat($entityID, uuid:randomUUID())}">
                                    <rdf:type rdf:resource="{$foaf}Agent"/>
                                    <foaf:name><xsl:value-of select="publisher/publisher-name"/></foaf:name>
                                </rdf:Description>
                            </dcterms:publisher>
                        </rdf:Description>
                    </dcterms:isPartOf>
                </xsl:if>
            </rdf:Description>
        </dcterms:isPartOf>
    </xsl:template>

    <xsl:template name="article-meta">
        <xsl:param name="affiliations" tunnel="yes"/>

        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>

        <xsl:for-each select="article-meta">
            <xsl:for-each select="article-id">
                <xsl:variable name="pub-id-type" select="@pub-id-type"/>
                <xsl:variable name="value" select="normalize-space(.)"/>

                <dcterms:identifier><xsl:value-of select="concat($pub-id-type, ':', $value)"/></dcterms:identifier>

                <xsl:choose>
                    <xsl:when test="$pub-id-type = 'doi'">
                        <bibo:doi><xsl:value-of select="$value"/></bibo:doi>
                        <xsl:variable name="valueIRISafe" select="replace(replace($value, '&lt;', '%3C'), '&gt;', '%3E')"/>
                        <owl:sameAs rdf:resource="http://dx.doi.org/{$valueIRISafe}"/>
                    </xsl:when>
                    <xsl:when test="$pub-id-type = 'pmc'">
                        <owl:sameAs rdf:resource="http://biotea.idiginfo.org/pubmedOpenAccess/rdf/PMC{$value}"/>
                        <dcterms:isFormatOf rdf:resource="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC{$value}"/>
                    </xsl:when>
                    <xsl:when test="$pub-id-type = 'pmid'">
                        <bibo:pmid><xsl:value-of select="$value"/></bibo:pmid>
                        <owl:sameAs>
                            <rdf:Description rdf:about="{concat($pubmed, $value)}">
                                <owl:sameAs rdf:resource="{concat($pmc, $pmcid)}"/>
                            </rdf:Description>
                        </owl:sameAs>
                        <owl:sameAs rdf:resource="http://linkedlifedata.com/resource/pubmed/id/{$value}"/>
                        <owl:sameAs rdf:resource="http://bio2rdf.org/pubmed:{$value}"/>
                        <rdfs:seeAlso rdf:resource="http://www.ncbi.nlm.nih.gov/pubmed/{$value}"/>
                        <rdfs:seeAlso rdf:resource="http://identifiers.org/pubmed/{$value}"/>
                        <rdfs:seeAlso rdf:resource="http://europepmc.org/abstract/MED/{$value}"/>
                        <rdfs:seeAlso rdf:resource="http://www.hubmed.org/display.cgi?uids={$value}"/>
                    </xsl:when>
                    <xsl:otherwise>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

            <xsl:apply-templates select="title-group/article-title | article-title"/>
            <xsl:apply-templates select="abstract"/>
            <xsl:apply-templates select="kwd-group/kwd"/>

            <xsl:apply-templates select="volume"/>
            <xsl:apply-templates select="fpage"/>
            <xsl:apply-templates select="lpage"/>
            <xsl:apply-templates select="counts/page-count/@count"/>
            <xsl:apply-templates select="permissions/license/@xlink:href"/>

            <bibo:contributorList>
                <rdf:Description rdf:about="{concat($pmc, $pmcid, '/contributors')}">
                    <rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq"/>

                    <xsl:for-each select="contrib-group/contrib">
                        <rdfs:member>
                            <xsl:call-template name="contributor"/>
                        </rdfs:member>
                    </xsl:for-each>
                </rdf:Description>
            </bibo:contributorList>

            <xsl:apply-templates select="author-notes"/>
        </xsl:for-each>
    </xsl:template>


    <xsl:template name="contributor">
        <xsl:param name="affiliations" tunnel="yes"/>

        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>
        <xsl:variable name="corresp" select="@corresp"/>
        <xsl:variable name="corresp-rid" select="xref[@ref-type = 'corresp']/@rid"/>

        <xsl:variable name="contributor" select="concat($entityID, uuid:randomUUID())"/>
            <rdf:Description rdf:about="{$contributor}">
                <rdf:type rdf:resource="{$foaf}Person"/>

                <xsl:choose>
                    <xsl:when test="$corresp or $corresp-rid">
                        <foaf:publications>
                            <rdf:Description rdf:about="{concat($pmc, $pmcid)}">
                                <scoro:corresponding-author rdf:resource="{$contributor}"/>
                            </rdf:Description>
                        </foaf:publications>
                    </xsl:when>
                    <xsl:otherwise>
                        <foaf:publications rdf:resource="{concat($pmc, $pmcid)}"/>
                    </xsl:otherwise>
                </xsl:choose>

                <xsl:for-each select="name">
                    <xsl:if test="given-names and surname">
                        <rdfs:label><xsl:value-of select="concat(given-names, ' ', surname)"/></rdfs:label>
                    </xsl:if>

                    <xsl:apply-templates select="given-names"/>
                    <xsl:apply-templates select="surname"/>
                    <xsl:apply-templates select="prefix"/>
                    <xsl:apply-templates select="suffix"/>
                </xsl:for-each>

                <xsl:for-each select="address">
                    <xsl:apply-templates select="phone"/>
                    <xsl:apply-templates select="fax"/>
                    <xsl:apply-templates select="email"/>
                </xsl:for-each>

                <xsl:for-each select="xref[@ref-type='aff']">
                    <xsl:variable name="rid" select="@rid"/>
                    <schema:affiliation rdf:resource="{$affiliations/rdf:RDF/rdf:Description[rdfs:value=$rid]/@rdf:about}"/>
                </xsl:for-each>
            </rdf:Description>
    </xsl:template>


    <xsl:template match="back">
        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>

        <xsl:for-each select="ref-list/ref/*[local-name() = 'element-citation' or local-name() = 'mixed-citation' or local-name() = 'citation']">
            <xsl:variable name="pmcid-cites" select="pub-id[@pub-id-type = 'pmc'][1]"/>
            <xsl:variable name="pmid-cites" select="pub-id[@pub-id-type = 'pmid'][1]"/>

            <xsl:variable name="p-cites">
                <xsl:choose>
                    <xsl:when test="$pmcid-cites != ''">
                        <xsl:value-of select="concat($pmc, $pmcid-cites)"/>
                    </xsl:when>
                    <xsl:when test="$pmid-cites != ''">
                        <xsl:value-of select="concat($pubmed, $pmid-cites)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat($doc, uuid:randomUUID())"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <rdf:Description rdf:about="{concat($pmc, $pmcid)}">
                <bibo:cites>
                    <rdf:Description rdf:about="{$p-cites}">
                        <rdf:type rdf:resource="{$bibo}Document"/>
                        <bibo:citedBy rdf:resource="{concat($pmc, $pmcid)}"/>

                        <xsl:if test="$pmid-cites != ''">
                            <rdfs:seeAlso rdf:resource="http://www.ncbi.nlm.nih.gov/pubmed/{$pmid-cites}"/>
                        </xsl:if>

<!--                        <xsl:for-each select="person-group/name">-->
<!--                            <dcterms:contributor>-->
<!--                                <xsl:variable name="contributor" select="concat($entityID, uuid:randomUUID())"/>-->
<!--                                <rdf:Description rdf:about="{$contributor}">-->
<!--                                    <rdf:type rdf:resource="{$foaf}Person"/>-->

<!--                                    <foaf:publications rdf:resource="{$pmid-cites}"/>-->

<!--                                    <xsl:apply-templates select="given-names"/>-->
<!--                                    <xsl:apply-templates select="surname"/>-->
<!--                                    <xsl:apply-templates select="prefix"/>-->
<!--                                    <xsl:apply-templates select="suffix"/>-->
<!--                                </rdf:Description>-->
<!--                            </dcterms:contributor>-->
<!--                        </xsl:for-each>-->
                    </rdf:Description>
                </bibo:cites>
            </rdf:Description>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="title-group/article-title | article-title">
        <dcterms:title><xsl:value-of select="."/></dcterms:title>
    </xsl:template>

    <xsl:template match="volume">
        <bibo:volume><xsl:value-of select="."/></bibo:volume>
    </xsl:template>

    <xsl:template match="fpage">
        <bibo:pageStart><xsl:value-of select="."/></bibo:pageStart>
    </xsl:template>
    <xsl:template match="lpage">
        <bibo:pageEnd><xsl:value-of select="."/></bibo:pageEnd>
    </xsl:template>
    <xsl:template match="counts/page-count/@count">
        <bibo:numPages><xsl:value-of select="."/></bibo:numPages>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{local-name()}" namespace="http://www.w3.org/1999/xhtml">
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="abstract">
        <dcterms:abstract rdf:parseType="Literal">
            <div xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
               <xsl:apply-templates/>
            </div>
        </dcterms:abstract>
    </xsl:template>

    <xsl:template match="body">
        <xsl:variable name="pmcid" select="key('pub-id-type', 'pmc')"/>

        <rdf:Description rdf:about="{concat($pmc, $pmcid)}">
            <dcterms:description rdf:parseType="Literal">
                <div xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
                   <xsl:apply-templates/>
                </div>
            </dcterms:description>
        </rdf:Description>
    </xsl:template>

    <xsl:template match="permissions/license/@xlink:href">
        <dcterms:license rdf:resource="{normalize-space(.)}"/>
    </xsl:template>

    <xsl:template match="kwd-group/kwd">
        <xsl:variable name="uri" select="concat($conceptSchemePubMed, $uriThingSeparator, uuid:randomUUID())"/>

        <dcterms:subject>
            <rdf:Description rdf:about="{$uri}">
                <rdf:type rdf:resource="{$skos}Concept"/>
                <skos:inScheme rdf:resource="{$conceptSchemePubMed}"/>
                <skos:topConceptOf>
                    <rdf:Description rdf:about="{$conceptSchemePubMed}">
                        <skos:hasTopConcept rdf:resource="{$uri}"/>
                    </rdf:Description>
                </skos:topConceptOf>
                <skos:prefLabel><xsl:value-of select="."/></skos:prefLabel>
            </rdf:Description>
        </dcterms:subject>
    </xsl:template>

    <xsl:template match="author-notes">
        <skos:note rdf:parseType="Literal">
            <div xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
               <xsl:apply-templates/>
            </div>
        </skos:note>
    </xsl:template>

    <xsl:template match="institution">
        <foaf:name><xsl:value-of select="."/></foaf:name>
        <rdfs:label><xsl:value-of select="."/></rdfs:label>
    </xsl:template>
    <xsl:template match="given-names">
        <foaf:firstName><xsl:value-of select="."/></foaf:firstName>
    </xsl:template>
    <xsl:template match="surname">
        <foaf:lastName><xsl:value-of select="."/></foaf:lastName>
    </xsl:template>
    <xsl:template match="prefix">
        <foaf:honorificPrefix><xsl:value-of select="."/></foaf:honorificPrefix>
    </xsl:template>
    <xsl:template match="suffix">
        <foaf:honorificSuffix><xsl:value-of select="."/></foaf:honorificSuffix>
    </xsl:template>
    <xsl:template match="phone">
        <foaf:phone rdf:resource="tel:{replace(normalize-space(.), ' ', '-')}"/>
    </xsl:template>
    <xsl:template match="fax">
        <schema:faxNumber><xsl:value-of select="normalize-space(.)"/></schema:faxNumber>
    </xsl:template>
    <xsl:template match="email">
        <foaf:mbox rdf:resource="mailto:{normalize-space(.)}"/>
    </xsl:template>
    <xsl:template match="addr-line">
        <schema:streetAddress><xsl:value-of select="."/></schema:streetAddress>
    </xsl:template>
</xsl:stylesheet>
