/**
 * License: src/main/resources/license/escidoc.license
 */

package de.mpg.jena.export.format;

import java.util.HashMap;

import de.mpg.jena.ImejiJena;

/**
 * Export in a pretty RDF (without technical triples) of collections
 * 
 * @author Friederike Kleinfercher
 *
 */
public class RDFCollectionExport extends RDFExport
{
	private String[] filteredTriples = 
	{
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
			"http://purl.org/escidoc/metadata/profiles/0.1/pos",
			"http://imeji.mpdl.mpg.de/id",
			"http://imeji.mpdl.mpg.de/metadata/id"
	};
	
	@Override
	public void init() 
	{
		model = ImejiJena.collectionModel;
		super.filteredTriples = this.filteredTriples;
	}	

	@Override
	protected void initNamespaces()
	{
		super.namespaces = new HashMap<String, String>();
		super.namespaces.put("http://imeji.mpdl.mpg.de/", "imeji");
		super.namespaces.put("http://imeji.mpdl.mpg.de/container/", "imeji-metadata");
		super.namespaces.put("http://purl.org/escidoc/metadata/terms/0.1/", "eterms");
		super.namespaces.put("http://purl.org/dc/elements/1.1/", "dcterms");
		super.namespaces.put("http://purl.org/escidoc/metadata/profiles/0.1/", "eprofiles");
	}

	@Override
	protected String openTagResource(String uri) 
	{
		return "<imeji:collection rdf:about=\"" + uri +"\">";
	}
	
	@Override
	protected String closeTagResource() 
	{
		return "</imeji:collection>";
	}

}
