package es.predictia.metaclip.visualizer.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.io.CharStreams;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import es.predictia.metaclip.visualizer.config.MetaclipConfig;
import es.predictia.metaclip.visualizer.service.GraphService;
import es.predictia.metaclip.visualizer.utils.OntologyReader;

@Controller
@CrossOrigin
public class VocabularyController {

	@RequestMapping(value={"/datasource/datasource.owl","/datasource/{version}/datasource.owl","/datasource.owl"})
	@ResponseBody
	public ResponseEntity<String> datasource(@PathVariable(required=false) String version){
		return get("datasource",version);
	}
	
	@RequestMapping(value={"/calibration/calibration.owl","/calibration/{version}/calibration.owl","/calibration.owl"})
	@ResponseBody
	public ResponseEntity<String> calibration(@PathVariable(required=false) String version){
		return get("calibration",version);
	}
	
	@RequestMapping(value={"/graphical_output/graphical_output.owl","/graphical_output/{version}/graphical_output.owl","/graphical_output.owl"})
	@ResponseBody
	public ResponseEntity<String> graphicalOutput(@PathVariable(required=false) String version){
		return get("graphical_output",version);
	}
	
	@RequestMapping(value={"/verification/verification.owl","/verification/{version}/verification.owl","/verification.owl"})
	@ResponseBody
	public ResponseEntity<String> verification(@PathVariable(required=false) String version){
		return get("verification",version);
	}
	
	@RequestMapping(value={"/ipcc_terms/ipcc_terms.owl","/ipcc_terms/{version}/ipcc_terms.owl","/ipcc_terms.owl"})
	@ResponseBody
	public ResponseEntity<String> ipccTerms(@PathVariable(required=false) String version){
		return get("ipcc_terms",version);
	}
	
	@RequestMapping("/individuals")
	@ResponseBody
	public ResponseEntity<Collection<String>> individuals(
			@RequestParam(name="vocab") String vocab,
			@RequestParam(name="class") String className,
			@RequestParam(name="version",defaultValue="devel") String version){

		Optional<OntModel> model = OntologyReader.model(url(vocab,version));
		Collection<String> result = new ArrayList<String>();
		if(model.isPresent()){
			OntClass clazz = model.get().getOntClass(metaclipUrl+vocab+"/"+vocab+".owl#"+className);
			if(clazz != null){
				ExtendedIterator<Individual> iterator = model.get().listIndividuals(clazz);
				while(iterator.hasNext()){
					Individual individual = iterator.next();
					result.add(individual.getLocalName());
				}
			}
		}
		return new ResponseEntity<Collection<String>>(result,HttpStatus.OK);
	}
	
	@RequestMapping("/individual")
	@ResponseBody
	public ResponseEntity<Collection<String>> individual(
			@RequestParam(name="id") String id,
			@RequestParam(name="vocab") String vocab,
			@RequestParam(name="version",defaultValue="devel") String version){
		Set<String> classes = new LinkedHashSet<>();
		
		Optional<OntModel> model = OntologyReader.model(url(vocab,version));
		
		if(model.isPresent()){
			Individual individual = model.get().getIndividual(metaclipUrl+vocab+"/"+vocab+".owl#"+id);
			if(individual!=null){
				Node_URI node = OntologyReader.clazz(metaclipConfig,individual);
				Optional<OntClass> mainClazz = OntologyReader.clazz(metaclipConfig,node);
				if(mainClazz.isPresent()){
					classes.add(mainClazz.get().toString());
					OntologyReader.parentClasses(metaclipConfig,node, classes);			
				}
			}
		}
		return new ResponseEntity<Collection<String>>(classes,HttpStatus.OK);
	}
	
	@RequestMapping("/classes")
	@ResponseBody
	public ResponseEntity<Collection<String>> classes(@RequestParam(name="version",defaultValue="devel") String version){
		Collection<String> result = new ArrayList<String>();
		// excluded those vocabularies which do not include class definition (e.g. term-only vocabularies)
		for(String vocab : new String[]{"datasource","calibration","verification","graphical_product"}){
			Optional<OntModel> model = OntologyReader.model(url(vocab,version));
			if(model.isPresent()){
				ExtendedIterator<OntClass> iterator = model.get().listClasses();
				while(iterator.hasNext()){
					OntClass clazz = iterator.next();
					if(clazz.getNameSpace() == null) continue;
					if(!clazz.getNameSpace().startsWith(graphService.config().getUri())) continue;				
					if(!clazz.getNameSpace().startsWith(metaclipUrl+vocab)) continue;
					result.add(metaclipUrl+vocab+":"+clazz.getLocalName());
				}
			}
		}
		return new ResponseEntity<Collection<String>>(result,HttpStatus.OK);
	}
	
	private ResponseEntity<String> get(String vocabulary,String version){
		String url = url(vocabulary,version);
		try{
			UrlResource urlResource = new UrlResource(url);
			Reader reader = new InputStreamReader(urlResource.getInputStream());
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_XML);
			return ResponseEntity.ok().headers(headers).body(CharStreams.toString(reader));
		}catch(IOException e){
			return ResponseEntity.badRequest().body("Vocabulary not found "+url);
		}
	}
	
	private String url(String vocab,String version) throws IllegalArgumentException{
		if("devel".equals(version)){
			return githubDevelRepo+vocab+"/"+vocab+".owl";
		}else{
			String versionParam = "";
			Boolean hasVersion = false;
			if(!StringUtils.isEmpty(version)){
				if(version.matches(VERSION_REGEXP)){
					versionParam = version+"/";
					hasVersion = true;
				}else{
					throw new IllegalArgumentException("Invalid version "+version+". Must follow the syntax X.X");
				}
			}
			return githubRepo+(hasVersion?"archive/":"")+vocab+"/"+versionParam+vocab+".owl";
		}
	}

	@Autowired private GraphService graphService;
	@Autowired private MetaclipConfig metaclipConfig;
	
	@Value("${metaclip.uri}") private String metaclipUrl;
	@Value("${github-repo}") private String githubRepo;
	@Value("${github-repo-devel}") private String githubDevelRepo;	
	
	private static final String VERSION_REGEXP = "\\d\\.\\d";
}
