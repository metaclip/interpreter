package es.predictia.metaclip.visualizer.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SampleImage {

	RPSS_MAP("/static/img/samples/rpss_map.png"),
	ETCCDI_MAP("/static/img/samples/etccdi_map.png"),
	ENSO_PLUME("/static/img/samples/enso_plume.png"),
	EUROCORDEX_ENSEMBLE_BIAS("/static/img/samples/eurocordex_ensemble_bias.png"),
	EUROCORDEX_INDEX_BIAS("/static/img/samples/eurocordex_index_bias.png");
	
	private final String resource;
	
	public String resource(){
		return resource;
	}

}
