package es.predictia.metaclip.visualizer.web;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import es.predictia.metaclip.visualizer.model.MetaclipGraph;
import es.predictia.metaclip.visualizer.model.MetaclipNode;
import es.predictia.metaclip.visualizer.model.NodePosition;
import es.predictia.metaclip.visualizer.model.SampleImage;
import es.predictia.metaclip.visualizer.service.GraphService;
import es.predictia.metaclip.visualizer.utils.MetaclipExtractor;
import es.predictia.metaclip.visualizer.utils.MetaclipExtractor.MetaclipExtraction;
import es.predictia.metaclip.visualizer.utils.OntologyReader; 


@Controller
@CrossOrigin
public class FileUploadController{

	@RequestMapping("/sample-image")
	@ResponseBody
	public ResponseEntity<?> sample(@RequestParam SampleImage sample){
		try{
			InputStream stream = getClass().getResourceAsStream(sample.resource());
			MetaclipExtraction metaclip = MetaclipExtractor.extract(stream);
			stream.close();
			if(!metaclip.getMetaclip().isPresent()){
				return new ResponseEntity<String>("{'error':'No metaclip info found'}",HttpStatus.BAD_REQUEST);
			}
			if(LOGGER.isDebugEnabled()){
				LOGGER.debug(metaclip.getMetaclip().get());
			}
			MetaclipGraph graph = graphService.graph(metaclip.getMetaclip().get());
			if(metaclip.getLayout().isPresent()){
				positions(graph,metaclip.getLayout().get());
			}
			return new ResponseEntity<MetaclipGraph>(graph,HttpStatus.OK);
		}catch(Exception e){
			LOGGER.warn("Problem extracting metaclip info: "+e.getMessage());e.printStackTrace();
			return new ResponseEntity<String>("{'error':'No valid metaclip info found: "+e.getMessage()+"'}",HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping("/image")
	@ResponseBody
	public ResponseEntity<?> upload(@RequestParam(name="file",required=false) MultipartFile input){
		try{
			MetaclipExtraction metaclip = new MetaclipExtraction();
			if(input.getOriginalFilename().toLowerCase().endsWith(".json")){
				String result = new BufferedReader(new InputStreamReader(input.getInputStream())).lines().parallel().collect(Collectors.joining());
				metaclip.setMetaclip(Optional.of(result));
			}else{
				metaclip = MetaclipExtractor.extract(input.getInputStream());
			}
			if(!metaclip.getMetaclip().isPresent()){
				return new ResponseEntity<String>("{'error':'No metaclip info found'}",HttpStatus.BAD_REQUEST);
			}
			if(LOGGER.isDebugEnabled()){
				// FileUtils.copyInputStreamToFile(IOUtils.toInputStream(metaclip.get(),"UTF-8"), new File("/tmp/juaco.txt"));
				LOGGER.debug(metaclip.getMetaclip().get());
			}
			MetaclipGraph graph = graphService.graph(metaclip.getMetaclip().get());
			if(metaclip.getLayout().isPresent()){
				positions(graph,metaclip.getLayout().get());
			}
			return new ResponseEntity<MetaclipGraph>(graph,HttpStatus.OK);
		}catch(Exception e){
			LOGGER.warn("Problem extracting metaclip info: "+e.getMessage());e.printStackTrace();
			return new ResponseEntity<String>("{'error':'No valid metaclip info found: "+e.getMessage()+"'}",HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping("/export")
	public ResponseEntity<byte[]> export(
			@RequestParam(required=false) String content,
			@RequestParam(required=false,name="sample") String sampleSrc,
			@RequestParam(required=false) String position) throws Exception{
		
		File file = File.createTempFile("metaclip-img-",".png");
		if(!StringUtils.isEmpty(sampleSrc)){
			for(SampleImage sample : SampleImage.values()){
				if(!sample.resource().endsWith(sampleSrc)) continue;
				InputStream stream = getClass().getResourceAsStream(sample.resource());
				FileUtils.copyInputStreamToFile(stream,file);
				stream.close();
				break;
			}
		}else{
			FileOutputStream outputStream = new FileOutputStream(file);
			byte[] bytes = Base64.getDecoder().decode(content);
			outputStream.write(bytes);
			outputStream.close();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);
		headers.setCacheControl(CacheControl.noCache().getHeaderValue());
	    headers.set(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=metaclip-image.png");
 
		byte[] bytes = null;
		if(!StringUtils.isEmpty(position)){
			MetaclipExtraction metaclip = MetaclipExtractor.extract(file);
			BufferedImage image = ImageIO.read(file);
			bytes = MetaclipExtractor.writePositions(image,"png",position,metaclip.getMetaclip().get());
			return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
		}else{
			InputStream stream = FileUtils.openInputStream(file);
			bytes = IOUtils.toByteArray(stream);
		}
		headers.setContentLength(bytes.length);
		file.delete();
		return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
	}
	
	private void positions(final MetaclipGraph graph,final String positionDefinition){
		Map<String,NodePosition> positions = graphService.positions(positionDefinition);
		for(Entry<String,NodePosition> entry : positions.entrySet()){
			Optional<MetaclipNode> node = graph.getNodes().stream().filter(s -> s.getId().equals(entry.getKey())).findFirst();
			if(node.isPresent()){
				graph.addPosition(node.get(),entry.getValue());
			}else{
				LOGGER.warn("Position set for unknown node "+entry.getKey());
			}
		}
	}
	
	@RequestMapping("/restart")
	@ResponseStatus(value = HttpStatus.OK)
	public void restart(){
		cacheManager.getCacheNames().parallelStream().forEach(name -> cacheManager.getCache(name).clear());
		OntologyReader.reset();
	}

	@Autowired private CacheManager cacheManager;
	@Autowired private GraphService graphService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadController.class);

}
