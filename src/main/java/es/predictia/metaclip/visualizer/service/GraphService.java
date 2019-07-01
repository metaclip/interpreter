package es.predictia.metaclip.visualizer.service;

import java.util.Map;

import es.predictia.metaclip.visualizer.config.MetaclipConfig;
import es.predictia.metaclip.visualizer.model.MetaclipGraph;
import es.predictia.metaclip.visualizer.model.NodePosition;

public interface GraphService {

	public MetaclipGraph graph(String input) throws Exception;
	
	public MetaclipConfig config();
	
	public Map<String,NodePosition> positions(String input);
}
