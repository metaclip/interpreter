package es.predictia.metaclip.visualizer.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import es.predictia.metaclip.visualizer.config.MetaclipConfig;
import es.predictia.metaclip.visualizer.model.MetaclipClass;
import es.predictia.metaclip.visualizer.model.MetaclipGraph;
import es.predictia.metaclip.visualizer.model.MetaclipNode;
import es.predictia.metaclip.visualizer.model.MetaclipRelationship;

public class OntologyReader {

	private static final Map<String, OntModel> MODELS = new HashMap<>();
	
	public static Optional<MetaclipNode> find(MetaclipConfig config,MetaclipGraph metaclipGraph, Graph graph, Node_URI node){
		Optional<MetaclipNode> metaclipNode = metaclipGraph.getNodes().stream().filter(s ->{
			return s.getId().equals(node.getLocalName());
		}).findAny();
		if(metaclipNode.isPresent()){
			return metaclipNode;
		}else{
			// looking for the node as an individual in the corresponding model
			Optional<OntModel> model = model(config,node.getNameSpace());
			if(model.isPresent()){
				Individual individual = model.get().getIndividual(node.getURI());
				if(individual != null){
					MetaclipNode newMetaclipNode = create(config,graph,individual);
					properties(config,newMetaclipNode,metaclipGraph,individual,graph);
					metaclipGraph.addNode(newMetaclipNode);
					return Optional.of(newMetaclipNode);
				}
			}
		}
		return Optional.empty();
	}
	
	public static Optional<Node_URI> type(Graph graph,Node_URI node){
		List<Node> properties = GraphUtil.listPredicates(graph, node, Node.ANY).toList();
		for(Node property : properties){
			if(!(property instanceof Node_URI)){
				continue;
			}
			List<Node> propertieValues = GraphUtil.listObjects(graph, node, property).toList();
			for(Node propertyValue : propertieValues){
				if(!RDF.type.asNode().equals(property)) continue;
				if(propertyValue instanceof Node_Literal){
					return Optional.of((Node_URI)NodeFactory.createURI(propertyValue.getLiteralValue().toString()));
				}else if(propertyValue instanceof Node_URI){
					return Optional.of((Node_URI)NodeFactory.createURI(propertyValue.getURI()));
				}
			}
		}
		return Optional.empty();
	}
	
	public static MetaclipNode create(MetaclipConfig config,Graph graph, Node_URI node){
		MetaclipNode newNode = new MetaclipNode();
		newNode.setId(node.getLocalName());

		// guess if it is not an individual
		if(!model(config,node.getNameSpace()).isPresent()){
			// add parent classes
			Optional<Node_URI> typeNode = type(graph, node);
			if(typeNode.isPresent()){
				LinkedHashSet<String> parents = new LinkedHashSet<>();
				parents.add(typeNode.get().getURI());
				OntologyReader.parentClasses(config,(Node_URI)NodeFactory.createURI(typeNode.get().getURI()), parents);
				if(!parents.isEmpty()){
					newNode.setClasses(parents.stream().map(config.compressClassname()).collect(Collectors.toCollection(LinkedHashSet::new)));
				}
				String group = config.compressClassname().apply(typeNode.get().getURI());
				if(config.getGroups().containsKey(group)){
					group = config.getGroups().get(group);
				}else{
					group = "datasource";
				}
				newNode.setGroup(group);
			}
		}
		return newNode;
	}
	
	public static void annotations(MetaclipConfig config, MetaclipGraph metaclipGraph){
		Set<String> usedClasses = new HashSet<String>();
		for(MetaclipNode node : metaclipGraph.getNodes()){
			for(String clazz : node.getClasses()){
				usedClasses.add(clazz);
			}
		}
		for(String usedClass : usedClasses){
			Optional<OntClass> ontClass = clazz(config,(Node_URI)NodeFactory.createURI(usedClass));
			MetaclipClass metaclipClass = new MetaclipClass();
			metaclipClass.setId(usedClass);
			if(ontClass.isPresent()){
				StmtIterator iterator = ontClass.get().listProperties();
				while(iterator.hasNext()){
					Statement statement = iterator.next();
					Triple triple = statement.asTriple();
					if(RDF.type.asNode().equals(triple.getPredicate())) continue;
					if(!showProperty(triple.getPredicate().toString())) continue;
					if(triple.getPredicate().getLocalName().equals("subClassOf")) continue;
					if(triple.getObject() instanceof Node_URI){
						metaclipClass.addAnnotation(triple.getPredicate().toString(),triple.getObject().getNameSpace());
					}else if(triple.getObject() instanceof Node_Literal){
						metaclipClass.addAnnotation(triple.getPredicate().toString(),triple.getObject().getLiteralValue().toString());
					}
				}
			}
			String prefix = usedClass;
			if(config.getPrefixes(false).containsKey(usedClass)){
				prefix = config.getPrefixes(false).get(usedClass);
			}
			metaclipGraph.addClass(prefix,metaclipClass);
		}
		Set<String> usedProperties = new HashSet<String>();
		for(MetaclipRelationship link : metaclipGraph.getRelationships()){
			usedProperties.add(link.getType());
		}
		for(MetaclipNode node : metaclipGraph.getNodes()){
			for(Entry<String,String> entry : node.getProperties().entrySet()){
				usedProperties.add(entry.getKey());
			}
		}
		for(String usedProperty : usedProperties){
			Optional<Property> property = property(config,(Node_URI)NodeFactory.createURI(usedProperty));
			MetaclipClass metaclipClass = new MetaclipClass();
			metaclipClass.setId(usedProperty);
			if(property.isPresent()){
				StmtIterator iterator = property.get().listProperties();
				while(iterator.hasNext()){
					Statement statement = iterator.next();
					Triple triple = statement.asTriple();
					if(RDF.type.asNode().equals(triple.getPredicate())) continue;
					if(!showProperty(triple.getPredicate().toString())) continue;
					if(triple.getPredicate().getLocalName().equals("subClassOf")) continue;
					if(triple.getObject() instanceof Node_URI){
						metaclipClass.addAnnotation(triple.getPredicate().toString(),triple.getObject().getNameSpace());
					}else if(triple.getObject() instanceof Node_Literal){
						metaclipClass.addAnnotation(triple.getPredicate().toString(),triple.getObject().getLiteralValue().toString());
					}
				}
			}
			String prefix = usedProperty;
			if(config.getPrefixes(false).containsKey(usedProperty)){
				prefix = config.getPrefixes(false).get(usedProperty);
			}
			if(!metaclipClass.getAnnotations().isEmpty()){
				metaclipGraph.addClass(prefix,metaclipClass);
			}
		}
	}
	
	public static void prefixes(MetaclipConfig config,MetaclipGraph metaclipGraph){
		for(Map.Entry<String,String> entry : config.getPrefixes(false).entrySet()){
			metaclipGraph.addPrefixes(entry.getValue(),entry.getKey());
		}		
	}
	
	public static MetaclipNode create(MetaclipConfig config, Graph graph, Individual individual){
		MetaclipNode newNode = create(config, graph,(Node_URI)individual.asNode());
		if(CollectionUtils.isEmpty(newNode.getClasses())){
			// add parent classes
			Node_URI typeNode = clazz(config,individual);
			Set<String> parents = new LinkedHashSet<>();
			parents.add(typeNode.getURI());
			OntologyReader.parentClasses(config,(Node_URI)NodeFactory.createURI(typeNode.getURI()), parents);
			if(!parents.isEmpty()){
				newNode.setClasses(parents.stream().map(config.compressClassname()).collect(Collectors.toCollection(LinkedHashSet::new)));
			}
			newNode.setGroup(typeNode.getLocalName());
		}
		return newNode;
	}
	
	public static void properties(MetaclipConfig config,MetaclipNode metaclipNode,MetaclipGraph metaclipGraph,Individual individual,Graph graph){
		StmtIterator iterator = individual.listProperties();
		while(iterator.hasNext()){
			Statement statement = iterator.next();
			Triple property = statement.asTriple();
			if(RDF.type.asNode().equals(property.getPredicate())) continue;
			if(!showProperty(property.getPredicate().toString())) continue;
			if(property.getObject() instanceof Node_Literal){
				if(RDF_LABEL.equals(property.getPredicate().toString())){
					metaclipNode.setLabel(property.getObject().getLiteralValue().toString());
				}else if(GEO_HAS_SERIALIZATION_LABEL.equals(property.getPredicate().toString()) || GEO_AS_WKT_LABEL.equals(property.getPredicate().toString())){
					metaclipNode.addProperty(config.compressClassname().apply(property.getPredicate().getURI()),property.getObject().getLiteralLexicalForm());
				}else{
					metaclipNode.addProperty(config.compressClassname().apply(property.getPredicate().getURI()),property.getObject().getLiteralValue().toString());
				}
			}
		}
		properties(config,metaclipNode,metaclipGraph,(Node_URI)individual.asNode(),graph);
	}

	private static Boolean showProperty(String property){
		if("http://purl.org/dc/elements/1.1/creator".equals(property)) return false;
		if("http://purl.org/dc/elements/1.1/date".equals(property)) return false;
		if("http://purl.org/dc/terms/creator".equals(property)) return false;
		if("http://purl.org/dc/terms/date".equals(property)) return false;
		if("dc:date".equals(property)) return false;
		if("dc:creator".equals(property)) return false;
		return true;
	}
	
	public static void properties(MetaclipConfig config,MetaclipNode metaclipNode,MetaclipGraph metaclipGraph,Node_URI node,Graph graph){
		// properties pointing to this subject
		List<Node> properties = GraphUtil.listPredicates(graph, node, Node.ANY).toList();
		for(Node property : properties){
			if(!(property instanceof Node_URI)){
				LOGGER.warn("Literal properties are not supported: "+property.toString());
				continue;
			}
			// values for this property
			List<Node> propertieValues = GraphUtil.listObjects(graph, node, property).toList();
			if(!showProperty(property.toString())) continue;
			for(Node propertyValue : propertieValues){
				if(RDF.type.asNode().equals(property)) continue;
				if(propertyValue instanceof Node_Literal){
					if(RDF_LABEL.equals(property.getURI())){
						metaclipNode.setLabel(propertyValue.getLiteralValue().toString());
					}else if(GEO_HAS_SERIALIZATION_LABEL.equals(property.getURI()) || GEO_AS_WKT_LABEL.equals(property.getURI())){
						metaclipNode.addProperty(config.compressClassname().apply(property.getURI()),propertyValue.getLiteralLexicalForm());
					}else{
						metaclipNode.addProperty(config.compressClassname().apply(property.getURI()),propertyValue.getLiteralValue().toString());
					}
				}else if(propertyValue instanceof Node_URI){
					MetaclipRelationship relationship = new MetaclipRelationship();
					relationship.setType(config.compressClassname().apply(property.getURI()));
					Optional<MetaclipNode> objectNodeOpt = OntologyReader.find(config,metaclipGraph,graph,(Node_URI)propertyValue);
					MetaclipNode objectNode = null;
					if(!objectNodeOpt.isPresent()){
						objectNode = OntologyReader.create(config,graph,(Node_URI)propertyValue);
					}else{
						objectNode = objectNodeOpt.get();
					}
					relationship.setSource(metaclipNode.getId());
					relationship.setTarget(objectNode.getId());
					metaclipGraph.addRelationship(relationship);
				}
			}
		}
	}

	public static void parentClasses(MetaclipConfig config, Node_URI node, Collection<String> parents){
		Optional<OntClass> clazzOpt = clazz(config,node);
		if(clazzOpt.isPresent()){
			OntClass clazz = clazzOpt.get();
			OntClass parent = clazz.getSuperClass();
			if(parent == null) return;
			if(!parent.equals(clazz)){
				parents.add(parent.getURI());
				parentClasses(config,(Node_URI)parent.asNode(), parents);
			}
		}		
	}
	
	public static Optional<OntClass> clazz(MetaclipConfig config, Node_URI node){
		String uri = config.decompressClassname().apply(node.getNameSpace());
		Optional<OntModel> model = model(config,uri);
		if(model.isPresent()){
			ExtendedIterator<OntClass> extendedIterator = model.get().listClasses();
			while(extendedIterator.hasNext()){
				OntClass clazz = extendedIterator.next();
				if(node.getLocalName().equals(clazz.getLocalName())){
					return Optional.of(clazz);
				}
			}
		}
		return Optional.empty();
	}
	
	public static Node_URI clazz(MetaclipConfig config, Individual individual){
		Node_URI node = (Node_URI)individual.getRDFType(false).asNode();
		if("NamedIndividual".equals(node.getLocalName())){
			// In some cases both owl:NamedIndividual and rdf:type generates
			// rdf:type triplets. The order of these triplets seems to be arbitrary
			// resolving a NamedIndividual class or the one specified in the
			// rdf:type clause. The following code prioritizes the rdf:type clause
			Graph myGraph = OntologyReader.model(config,individual.getNameSpace()).get().getGraph();
			ExtendedIterator<Triple> iterator = myGraph.find(individual.asNode(), RDF.type.asNode(), Node.ANY);
			while(iterator.hasNext()){
				Triple triple = iterator.next();
				if(!"NamedIndividual".equals(triple.getObject().getLocalName())){
					node = (Node_URI)NodeFactory.createURI(triple.getObject().getURI());
					break;
				}
			}
		}
		return node;
	}
	
	public static Optional<Property> property(MetaclipConfig config, Node_URI name){
		String uri = config.decompressClassname().apply(name.getNameSpace());
		Optional<OntModel> model = model(config,uri);
		if(model.isPresent()){
			return Optional.ofNullable(model.get().getProperty(uri+name.getLocalName()));
		}
		return Optional.empty();
	}
	
	public static void reset(){
		MODELS.clear();
	}
	
	public static Optional<OntModel> model(MetaclipConfig config, String uri){
		if(MODELS.isEmpty()){
			for(Map.Entry<String,String> entry : config.getPrefixes(true).entrySet()){
				OntModel ontModel = ModelFactory.createOntologyModel();
				ontModel.read(entry.getKey());
				if(!ontModel.isEmpty()){
					MODELS.put(entry.getKey(), ontModel);
				}
			}
		}
		return Optional.ofNullable(MODELS.get(uri));
	}

	public static Optional<OntModel> model(String uri){
		try{
			OntModel ontModel = ModelFactory.createOntologyModel();
			ontModel.read(uri);
			return Optional.ofNullable(ontModel);
		}catch(Exception e){
			return Optional.empty();
		}
	}

	private static final String RDF_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
	private static final String GEO_AS_WKT_LABEL = "http://www.opengis.net/ont/geosparql#asWKT";
	private static final String GEO_HAS_SERIALIZATION_LABEL = "http://www.opengis.net/ont/geosparql#hasSerialization";	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyReader.class);
}
