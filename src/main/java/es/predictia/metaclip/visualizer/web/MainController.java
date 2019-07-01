package es.predictia.metaclip.visualizer.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping; 


@Controller
public class MainController{

	@RequestMapping(value={"/","/interpreter"})
	public String main(){
		return "pages/index";
	}
	
	@RequestMapping("/about")
	public String about(){
		return "pages/about";
	}
	
	@RequestMapping("/contributors")
	public String contributors(){
		return "pages/contributors";
	}

}
