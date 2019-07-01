package es.predictia.metaclip.visualizer.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.ImageCaptchaService;

@Controller
@RequestMapping("/captcha")
public class CaptchaController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaController.class);

	@Resource(name = "captchaService")
	private ImageCaptchaService captchaService;

	@RequestMapping(value = "/image", method = RequestMethod.GET)
	public @ResponseBody
	Model image(HttpServletRequest request,HttpServletResponse response, Model model) throws Exception {
		
		LOGGER.debug("Received a request to show captcha image");

		// the output stream to render the captcha image as jpeg into
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			// get the session id that will identify the generated captcha.
			// the same id must be used to validate the response, the session id
			// is a good candidate!

			String captchaId = request.getSession().getId();

			LOGGER.debug("Captcha ID which gave the image::" + captchaId);

			// call the ImageCaptchaService getChallenge method
			BufferedImage challenge = captchaService.getImageChallengeForID(captchaId, request.getLocale());

			// a jpeg encoder
			ImageIO.write(challenge, "jpeg", outputStream);
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		} catch (CaptchaServiceException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}

		// flush it in the response
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/jpeg");
		ServletOutputStream responseOutputStream = response.getOutputStream();
		responseOutputStream.write(outputStream.toByteArray());
		outputStream.close();
		responseOutputStream.close();

		return null;
	}

}