/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.DocumentDAO;
import org.ala.dao.FulltextSearchDao;
import org.ala.dao.IndexedTypes;
import org.ala.dao.InfoSourceDAO;
import org.ala.dao.TaxonConceptDao;
import org.ala.dao.VocabularyDAO;
import org.ala.dto.ExtendedTaxonConceptDTO;
import org.ala.dto.SearchDTO;
import org.ala.dto.SearchResultsDTO;
import org.ala.dto.SearchTaxonConceptDTO;
import org.ala.lucene.Autocompleter;
import org.ala.model.CommonName;
import org.ala.model.Document;
import org.ala.model.InfoSource;
import org.ala.model.SimpleProperty;
import org.ala.repository.Predicates;
import org.ala.util.ImageUtils;
import org.ala.util.MimeType;
import org.ala.util.RepositoryFileUtils;
import org.ala.util.StatusType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Main controller for the BIE site
 *
 * TODO: If this class gets too big or complex then split into multiple Controllers.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Controller("speciesController")
public class SpeciesController {

	/** Logger initialisation */
	private final static Logger logger = Logger.getLogger(SpeciesController.class);
	/** DAO bean for access to taxon concepts */
	@Inject
	private TaxonConceptDao taxonConceptDao;
	/** DAO bean for access to repository document table */
	@Inject
	private DocumentDAO documentDAO;
	/** DAO bean for SOLR search queries */
	@Inject
	private FulltextSearchDao searchDao;
	/** DAO bean for access to info sources */
	@Inject
	private InfoSourceDAO infoSourceDAO;
	/** DAO bean for vocabularies */
	@Inject
	private VocabularyDAO vocabularyDAO;
	/** Name of view for site home page */
	private String HOME_PAGE = "homePage";
//	/** Name of view for an empty search page */
//	private final String SPECIES_SEARCH = "species/search";
//	/** Name of view for list of taxa */
//	private final String SPECIES_LIST = "species/list";
	/** Name of view for a single taxon */
	private final String SPECIES_SHOW = "species/show";
    /** Name of view for a taxon error page */
	private final String SPECIES_ERROR = "species/error";
	/** Name of view for list of pest/conservation status */
	private final String STATUS_LIST = "species/statusList";
	/** Name of view for list of datasets */
	private final String DATASET_LIST = "species/datasetList";
	/** Name of view for list of vocabularies */
	private final String VOCABULARIES_LIST = "species/vocabularies";
	
	protected String repositoryPath = "/data/bie/";
	
	protected String repositoryUrl = "http://alaslvweb2-cbr.vm.csiro.au/repository/";
	
	/**
	 * Custom handler for the welcome view.
	 * <p>
	 * Note that this handler relies on the RequestToViewNameTranslator to
	 * determine the logical view name based on the request URL: "/welcome.do"
	 * -&gt; "welcome".
	 *
	 * @return viewname to render
	 */
	@RequestMapping("/")
	public String homePageHandler() {
		return HOME_PAGE;
	}

	/**
	 * Map to a /{guid} URI.
	 * E.g. /species/urn:lsid:biodiversity.org.au:afd.taxon:a402d4c8-db51-4ad9-a72a-0e912ae7bc9a
	 * 
	 * @param guid
	 * @param model
	 * @return view name
	 * @throws Exception
	 */ 
	@RequestMapping(value = "/species/{guid}", method = RequestMethod.GET)
	public String showSpecies(
            @PathVariable("guid") String guid,
            @RequestParam(value="conceptName", defaultValue ="", required=false) String conceptName,
            Model model) throws Exception {
		
        ExtendedTaxonConceptDTO etc = taxonConceptDao.getExtendedTaxonConceptByGuid(guid);

        if (etc.getTaxonConcept() == null || etc.getTaxonConcept().getGuid() == null) {
            model.addAttribute("errorMessage", "The requested taxon was not found: "+conceptName+" ("+ guid+")");
            return SPECIES_ERROR;
        }

        model.addAttribute("extendedTaxonConcept", etc);
		model.addAttribute("commonNames", getCommonNamesString(etc));
		model.addAttribute("textProperties", filterSimpleProperties(etc));
		return SPECIES_SHOW;
	}

	/**
	 * Map to a /{guid} URI.
	 * E.g. /species/urn:lsid:biodiversity.org.au:afd.taxon:a402d4c8-db51-4ad9-a72a-0e912ae7bc9a
	 * 
	 * @param guid
	 * @param model
	 * @return view name
	 * @throws Exception
	 */ 
	@RequestMapping(value = "/species/info/{guid}.json", method = RequestMethod.GET)
	public String showBriefSpecies(
            @PathVariable("guid") String guid,
            @RequestParam(value="conceptName", defaultValue ="", required=false) String conceptName,
            Model model) throws Exception {
		
		SearchResultsDTO<SearchDTO> stcs = searchDao.findByName(IndexedTypes.TAXON, guid, null, 0, 1, "score", "asc");
        if(stcs.getTotalRecords()>0){
        	SearchTaxonConceptDTO st = (SearchTaxonConceptDTO) stcs.getResults().get(0);
        	model.addAttribute("taxonConcept", fixRepoUrls(st));
        }
		return SPECIES_SHOW;
	}
	
	/**
	 * JSON output for TC guid
	 *
	 * @param guid
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/species/{guid}.json", method = RequestMethod.GET)
	public ExtendedTaxonConceptDTO showSpeciesJson(@PathVariable("guid") String guid) throws Exception {
		logger.info("Retrieving concept with guid: "+guid);
		return taxonConceptDao.getExtendedTaxonConceptByGuid(guid);
	}

	/**
	 * JSON web service (AJAX) to return details for a repository document
	 *
	 * @param documentId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/species/document/{documentId}.json", method = RequestMethod.GET)
	public Document getDocumentDetails(@PathVariable("documentId") int documentId) throws Exception {
		Document doc = documentDAO.getById(documentId);

		if (doc != null) {
			// augment data with title from reading dc file
			String fileName = doc.getFilePath()+"/dc";
			RepositoryFileUtils repoUtils = new RepositoryFileUtils();
			List<String[]> lines = repoUtils.readRepositoryFile(fileName);
			//System.err.println("docId:"+documentId+"|filename:"+fileName);
			for (String[] line : lines) {
				// get the dc.title value
				if (line[0].endsWith(Predicates.DC_TITLE.getLocalPart())) {
					doc.setTitle(line[1]);
				} else if (line[0].endsWith(Predicates.DC_IDENTIFIER.getLocalPart())) {
					doc.setIdentifier(line[1]);
				}
			}
		}
		return doc;
	}

	/**
	 *
	 * @param documentId
	 * @param scale
	 * @param square
	 * @param outputStream
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping(value="/species/images/{documentId}.jpg", method = RequestMethod.GET)
	public void thumbnailHandler(@PathVariable("documentId") int documentId, 
			@RequestParam(value="scale", required=false, defaultValue ="100") Integer scale,
			@RequestParam(value="square", required=false, defaultValue ="true") Boolean square,
			OutputStream outputStream,
			HttpServletResponse response) throws IOException {
		Document doc = documentDAO.getById(documentId);

		if (doc != null) {
			// augment data with title from reading dc file
			MimeType mt = MimeType.getForMimeType(doc.getMimeType());
			String fileName = doc.getFilePath()+"/raw"+mt.getFileExtension();

            ImageUtils iu = new ImageUtils();
            iu.load(fileName); // problem with Jetty 7.0.1

			if (square) {
				iu.square();
			}

			iu.smoothThumbnail(scale);
			response.setContentType(mt.getMimeType());
			ImageIO.write(iu.getModifiedImage(), mt.name(), outputStream);
		}
	}

	/**
	 * Pest / Conservation status list
	 *
	 * @param statusStr
	 * @param filterQuery 
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/species/status/{status}", method = RequestMethod.GET)
	public String listStatus(
			@PathVariable("status") String statusStr,
			@RequestParam(value="fq", required=false) String filterQuery,
			Model model) throws Exception {
		StatusType statusType = StatusType.getForStatusType(statusStr);
		if (statusType==null) {
			return "redirect:/error.jsp";
		}
		model.addAttribute("statusType", statusType);
		model.addAttribute("filterQuery", filterQuery);
		SearchResultsDTO searchResults = searchDao.findAllByStatus(statusType, filterQuery,  0, 10, "score", "asc");// findByScientificName(query, startIndex, pageSize, sortField, sortDirection);
		model.addAttribute("searchResults", searchResults);
		return STATUS_LIST;
	}

	/**
	 * Pest / Conservation status JSON (for yui datatable)
	 *
	 * @param statusStr
	 * @param filterQuery 
	 * @param startIndex
	 * @param pageSize
	 * @param sortField
	 * @param sortDirection
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/species/status/{status}.json", method = RequestMethod.GET)
	public SearchResultsDTO listStatusJson(@PathVariable("status") String statusStr,
			@RequestParam(value="fq", required=false) String filterQuery,
			@RequestParam(value="startIndex", required=false, defaultValue="0") Integer startIndex,
			@RequestParam(value="results", required=false, defaultValue ="10") Integer pageSize,
			@RequestParam(value="sort", required=false, defaultValue="score") String sortField,
			@RequestParam(value="dir", required=false, defaultValue ="asc") String sortDirection,
			Model model) throws Exception {

		StatusType statusType = StatusType.getForStatusType(statusStr);
		SearchResultsDTO searchResults = null;

		if (statusType!=null) {
			searchResults = searchDao.findAllByStatus(statusType, filterQuery, startIndex, pageSize, sortField, sortDirection);// findByScientificName(query, startIndex, pageSize, sortField, sortDirection);
		}

		return searchResults;
	}

	/**
	 * List of data sets.
	 * 
	 *
	 * @param model
	 * @return view name
	 */
	@RequestMapping(value = "/species/contributors", method = RequestMethod.GET)
	public String listDatasets (Model model) throws Exception  {
		List<InfoSource> infoSources = infoSourceDAO.getAllByDatasetType();
		List<Integer> infoSourceIDWithVocabulariesMapList = new ArrayList<Integer>();		
		for (InfoSource infoSource : infoSources) {
			List<Map<String,Object>> vocabulariesMap = vocabularyDAO.getTermsByInfosourceId(infoSource.getId());
			
			if (vocabulariesMap != null && vocabulariesMap.size() != 0) {
				infoSourceIDWithVocabulariesMapList.add(infoSource.getId());
			}
		}
		
		model.addAttribute("infoSources", infoSources);
		model.addAttribute("infoSourceIDWithVocabulariesMapList", infoSourceIDWithVocabulariesMapList);
		Map<String, Long> countsMap = searchDao.getAllDatasetCounts();
		model.addAttribute("countsMap", countsMap);

		return DATASET_LIST;
	}

	/**
	 * List of vocabularies for a given Info Source Id
	 *
	 * @param model
	 * @return view name
	 */
	@RequestMapping(value = "/species/vocabularies/{infosourceId}", method = RequestMethod.GET)
	public String listVocabularies (@PathVariable("infosourceId") String infoSourceId, Model model) throws Exception {
		model.addAttribute("infoSource", infoSourceId);
		
        int infoId = Integer.parseInt(infoSourceId);

        logger.debug(vocabularyDAO.getPreferredTermsFor(infoId, "" ,  ""));

        List<Map<String,Object>> vocabulariesMap = vocabularyDAO.getTermsByInfosourceId(infoId);

        model.addAttribute("vocabulariesMap", vocabulariesMap);

        InfoSource infoSource = infoSourceDAO.getById(infoId);
        String infoSourceName = infoSource.getName();
        logger.debug("Infosource Name:" + infoSourceName);
        model.addAttribute("infoName", infoSourceName);

		return VOCABULARIES_LIST;
	}

	/**
	 * Autocomplete AJAX service for JQuery-autocomplete
	 *
	 * @param query
	 * @param response
	 */
	@RequestMapping(value = "/species/terms", method = RequestMethod.GET)
	public void listTerms(
			@RequestParam(value="term", required=false) String query,
			HttpServletResponse response) {

		try {
			OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream());
			Autocompleter ac = new Autocompleter();
			List<String> terms = new ArrayList<String>();
			terms = ac.suggestTermsFor(query.toLowerCase().trim(), 10);
			// create JSON string using Jackson
			ObjectMapper o = new ObjectMapper();
			String json = o.writeValueAsString(terms);
			response.setContentType("application/json");
			os.write(json);
			os.close();
		} catch (IOException ex) {
			logger.error("Problem running Autocompleter: "+ex.getMessage(), ex);
		}

		return;
	}

	/**
	 * Utility to pull out common names and remove duplicates, returning a string
	 *
	 * @param etc
	 * @return
	 */
	private String getCommonNamesString(ExtendedTaxonConceptDTO etc) {
		HashMap<String, String> cnMap = new HashMap<String, String>();

		for (CommonName cn : etc.getCommonNames()) {
			String lcName = cn.getNameString().toLowerCase().trim();

			if (!cnMap.containsKey(lcName)) {
				cnMap.put(lcName, cn.getNameString());
			}
		}

		return StringUtils.join(cnMap.values(), ", ");
	}

	/**
	 * Filter a list of SimpleProperty objects so that the resulting list only
	 * contains objects with a name ending in "Text". E.g. "hasDescriptionText".
	 *
	 * @param etc
	 * @return
	 */
	private List<SimpleProperty> filterSimpleProperties(ExtendedTaxonConceptDTO etc) {
		List<SimpleProperty> simpleProperties = etc.getSimpleProperties();
		List<SimpleProperty> textProperties = new ArrayList<SimpleProperty>();

		for (SimpleProperty sp : simpleProperties) {
			if (sp.getName().endsWith("Text") || sp.getName().endsWith("hasPopulateEstimate")) {
				textProperties.add(sp);
			}
		}

		return textProperties;
	}

	/**
	 * Fix the repository URLs
	 * 
	 * @param searchConceptDTO
	 * @return
	 */
	public SearchTaxonConceptDTO fixRepoUrls(SearchTaxonConceptDTO searchConceptDTO){
		
		String thumbnail = searchConceptDTO.getThumbnail();
		if(thumbnail!=null && thumbnail.contains(repositoryPath)){
			searchConceptDTO.setThumbnail(thumbnail.replace(repositoryPath, repositoryUrl));
		}
		String image = searchConceptDTO.getImage();
		if(image!=null && image.contains(repositoryPath)){
			searchConceptDTO.setImage(image.replace(repositoryPath, repositoryUrl));
		}
		return searchConceptDTO;
	}
	
	
	/**
	 * @param taxonConceptDao the taxonConceptDao to set
	 */
	public void setTaxonConceptDao(TaxonConceptDao taxonConceptDao) {
		this.taxonConceptDao = taxonConceptDao;
	}
}