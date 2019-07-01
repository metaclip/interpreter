package es.predictia.metaclip.visualizer.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

import es.predictia.metaclip.visualizer.config.MetaclipConfig;
import es.predictia.metaclip.visualizer.model.MetaclipGraph;
import es.predictia.metaclip.visualizer.model.MetaclipNode;
import es.predictia.metaclip.visualizer.model.MetaclipRelationship;
import es.predictia.metaclip.visualizer.model.NodePosition;
import es.predictia.metaclip.visualizer.utils.OntologyReader;

@Service
public class GraphServiceImpl implements GraphService{

	@Override
	@Cacheable("graphs")
	public MetaclipGraph graph(String metaclip) throws Exception {
		Dataset dataset = DatasetFactory.create();
		RDFDataMgr.read(dataset,IOUtils.toInputStream(metaclip,"UTF-8"),BASE_URI,RDFLanguages.JSONLD);
		Graph graph = dataset.asDatasetGraph().getDefaultGraph();
		
		MetaclipGraph metaclipGraph = new MetaclipGraph();
		
		List<Node> subjects = GraphUtil.listSubjects(graph, Node.ANY, Node.ANY).toList();
		for(Node node : subjects){
			if(!(node instanceof Node_URI)){
				// subjects are always Node_URI
				LOGGER.warn("Non URI nodes are not supported: "+node.toString());
				continue;
			}
			Node_URI subjectNode = (Node_URI)node;
			Optional<MetaclipNode> metaclipNodeOpt = OntologyReader.find(metaclipConfig,metaclipGraph,graph,subjectNode);
			if(!metaclipNodeOpt.isPresent()){
				MetaclipNode metaclipNode = OntologyReader.create(metaclipConfig,graph,subjectNode);
				OntologyReader.properties(metaclipConfig, metaclipNode, metaclipGraph, subjectNode, graph);
				metaclipGraph.addNode(metaclipNode);
			}
		}
		
		// add annotations
		OntologyReader.annotations(metaclipConfig,metaclipGraph);
		OntologyReader.prefixes(metaclipConfig,metaclipGraph);
		virtualNodes(metaclipGraph,10);
		separateDuplicateLinks(metaclipGraph);

		return metaclipGraph;
	}

	// sets link number
	private static void separateDuplicateLinks(MetaclipGraph metaclipGraph){
		List<MetaclipRelationship> toAdd = new ArrayList<>();
		metaclipGraph.getRelationships().forEach(d -> {
			Optional<MetaclipRelationship> brothers = metaclipGraph.getRelationships().stream().filter(e -> {
				// same source/target
				return e.getSource().equals(d.getSource()) && e.getTarget().equals(d.getTarget()) && !e.equals(d);
			}).findFirst();
			if(brothers.isPresent()){
				Optional<MetaclipNode> node = metaclipGraph.getNode(d.getTarget());
				if(node.isPresent()){
					String newId = node.get().getId()+"_bis";
					Optional<MetaclipNode> newNodeOpt = metaclipGraph.getNode(newId);
					if(!newNodeOpt.isPresent()){
						// duplicate node
						MetaclipNode newNode = new MetaclipNode();
						newNode.setId(node.get().getId()+"_bis");
						newNode.setLabel(node.get().getLabel());
						newNode.setClasses(node.get().getClasses());
						newNode.setGroup(node.get().getGroup());
						newNode.setProperties(node.get().getProperties());
						metaclipGraph.addNode(newNode);
						d.setTarget(newNode.getId());
						MetaclipRelationship brotherLink = new MetaclipRelationship();
						brotherLink.setSource(node.get().getId());
						brotherLink.setTarget(newId);
						brotherLink.setType("(duplicated-node)");
						toAdd.add(brotherLink);
						
						// inverse brother link
						MetaclipRelationship inverseBrotherLink = new MetaclipRelationship();
						inverseBrotherLink.setTarget(node.get().getId());
						inverseBrotherLink.setSource(newId);
						inverseBrotherLink.setType("");
						toAdd.add(inverseBrotherLink);
					}
				}
			}
		});
		for(MetaclipRelationship link : toAdd){
			metaclipGraph.addRelationship(link);
		}
	}
	
	// adds virtual nodes where there exist a high number of children
	private static void virtualNodes(MetaclipGraph metaclipGraph,Integer collapse){
		Collection<MetaclipNode> nodesAdded = new ArrayList<>();
		for(MetaclipNode node : metaclipGraph.getNodes()){
			// link type, node class, count 
			Map<String,Map<String,Integer>> count = new HashMap<>();			
			for(MetaclipRelationship link : metaclipGraph.getRelationships()){
				if(!link.getSource().equals(node.getId())) continue;
				if(!count.containsKey(link.getType())){
					count.put(link.getType(),new HashMap<>());
				}
				Map<String,Integer> subCount = count.get(link.getType());
				Optional<MetaclipNode> rNode = metaclipGraph.getNode(link.getTarget());
				if(!rNode.isPresent()) continue;
				Collection<String> classes = rNode.get().getClasses();
				if(CollectionUtils.isEmpty(classes)) continue;
				String clazz = classes.iterator().next();
				if(!subCount.containsKey(clazz)){
					subCount.put(clazz,0);
				}
				int n = subCount.get(clazz);
				subCount.put(clazz,n+1);
			}
			for(Map.Entry<String,Map<String,Integer>> link : count.entrySet()){
				for(Map.Entry<String,Integer> child : link.getValue().entrySet()){
					if(child.getValue()>collapse){
						// collapse
						MetaclipNode cluster = new MetaclipNode();
						cluster.setId(node.getId()+"-"+link.getKey()+"-cluster");
						cluster.setGroup("cluster");
						cluster.setLabel("cluster");
						cluster.setClasses(Arrays.asList("cluster"));
						MetaclipRelationship relationship = new MetaclipRelationship();
						relationship.setSource(node.getId());
						relationship.setTarget(cluster.getId());
						relationship.setType(link.getKey());
						nodesAdded.add(cluster);
						metaclipGraph.addRelationship(relationship);
						
						List<MetaclipRelationship> toBeDeleted = new ArrayList<>();
						List<MetaclipRelationship> toBeAdded = new ArrayList<>();
						for(MetaclipRelationship relation : metaclipGraph.getRelationships()){
							if(!relation.getSource().equals(node.getId())) continue;
							if(!relation.getType().equals(link.getKey())) continue;
							Optional<MetaclipNode> rNode = metaclipGraph.getNode(relation.getTarget());
							if(!rNode.isPresent()) continue;
							Collection<String> classes = rNode.get().getClasses();
							if(CollectionUtils.isEmpty(classes)) continue;
							String clazz = classes.iterator().next();
							if(clazz.equals(child.getKey())){
								toBeDeleted.add(relation);
								MetaclipRelationship newRelation = new MetaclipRelationship();
								newRelation.setSource(cluster.getId());
								newRelation.setTarget(relation.getTarget());
								newRelation.setType(link.getKey());
								toBeAdded.add(newRelation);
							}
						}
						metaclipGraph.setRelationships(metaclipGraph.getRelationships().stream().filter(d -> toBeDeleted.indexOf(d)<0).collect(Collectors.toList()));
						for(MetaclipRelationship relation : toBeAdded){
							metaclipGraph.addRelationship(relation);
						}
					}
				}
			}
		}
		for(MetaclipNode node : nodesAdded){
			metaclipGraph.addNode(node);
		}
	}

	public Map<String,NodePosition> positions(String input){
		if(StringUtils.isEmpty(input)) return new HashMap<>();
		try{
			final ObjectMapper mapper = new ObjectMapper();
			final MapType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, NodePosition.class);
			return mapper.readValue(input, type);
		}catch(Exception e){
			LOGGER.error("Error parsing positions: "+e.getMessage());
		}
		return new HashMap<>();
	}
	
	@Override
	public MetaclipConfig config() {
		return metaclipConfig;
	}
	
	@Autowired private MetaclipConfig metaclipConfig;
	
	private static final String BASE_URI = "http://localhost/";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphServiceImpl.class);

}
