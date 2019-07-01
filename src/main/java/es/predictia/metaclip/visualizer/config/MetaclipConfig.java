package es.predictia.metaclip.visualizer.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
@EnableCaching
@ConfigurationProperties(ignoreInvalidFields = false, ignoreUnknownFields = false, prefix = "metaclip")
public class MetaclipConfig {

	private String uri;
	
	private Map<String, String> prefixes = new HashMap<>();
	
	private Map<String, String> groups = new HashMap<>();
	
	public void setUri(String uri){
		this.uri = uri;;
	}
	
	public String getUri(){
		return uri;
	}
	
	public Map<String, String> getPrefixes(Boolean meatclipOnly){
		if(meatclipOnly){
			HashMap<String, String> filteredPrices = new HashMap<String, String>();
			for(String key : prefixes.keySet()) {
				if(key.startsWith(uri)){
					filteredPrices.put(key, prefixes.get(key));
				}
			}
		}
		return prefixes;
	}
	
	public Function<String,String> compressClassname(){
		return s -> {
			for(Map.Entry<String,String> entry : prefixes.entrySet()){
				String uri = entry.getKey();
				if(s.contains("#")){
					if(!uri.endsWith("#")) uri = uri+"#";
				}else{
					if(uri.endsWith("#")) uri = uri.substring(0,uri.length()-1);
				}
				if(s.indexOf(uri)==0){
					return s.replace(uri,entry.getValue()+":");
				}
			}
			return s;
		};
	};

	public Function<String,String> decompressClassname(){
		return s -> {
			for(Map.Entry<String,String> entry : prefixes.entrySet()){
				String prefix = entry.getValue();
				if(!prefix.endsWith(":")) prefix += ":";
				if(s.indexOf(prefix)==0){
					return s.replace(entry.getValue()+":",entry.getKey());
				}
			}
			return s;
		};
	}
	
}
