package es.predictia.metaclip.visualizer.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.octo.captcha.service.image.ImageCaptchaService;

import es.predictia.metaclip.visualizer.model.Contact;
import es.predictia.metaclip.visualizer.service.EmailSender;

@Controller
public class ContactController {

	@RequestMapping("/contact")
	public ModelAndView contact(@RequestParam(required = false) final String success) {
		ModelAndView mav = new ModelAndView("pages/contact").addObject("contact", new Contact());
		return (success != null) ? mav.addObject("success", success) : mav;
	}
	
	@RequestMapping(value = "/contact", method = RequestMethod.POST)
	public ModelAndView contact(@Valid @ModelAttribute("contact") Contact contact, BindingResult result, HttpServletRequest request){
		String sessionId = request.getSession().getId();

		Boolean isResponseCorrect = captchaService.validateResponseForID(sessionId, contact.getCaptcha());
		if(isResponseCorrect == false){
			result.addError(new FieldError("contact", "captcha", "Captcha not valid"));
		}
		if(result.hasErrors()){
			return contact(null).addAllObjects(result.getModel());
		}else{
			CompletableFuture.runAsync(() -> {
				try{
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					String date = formatter.format(LocalDateTime.now());
					String ip = request.getRemoteAddr();
					String subject = "Contact from metaclip";
					StringBuffer content = new StringBuffer();
					content.append("Date: "+date+"<br/>");
					content.append("IP: "+ip+"<br/>");
					content.append("Name: "+contact.getName()+"<br/>");
					content.append("Email: "+contact.getEmail()+"<br/>");
					content.append("Message: "+contact.getMessage()+"<br/>");
					emailSender.sendHtmlEmail(Arrays.asList(destination.split(",")), subject, content.toString());
				}catch(Exception e){
					LOGGER.error("Error sending email: "+e.getMessage());
				}
			}, executorService);
			return new ModelAndView("redirect:/contact?success=1");
		}
	}
	
	@Autowired private transient ExecutorService executorService;
	@Autowired private transient EmailSender emailSender;
	
	@Value("${mail.to}") private String destination;
	@Resource(name = "captchaService") private transient ImageCaptchaService captchaService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactController.class);

}