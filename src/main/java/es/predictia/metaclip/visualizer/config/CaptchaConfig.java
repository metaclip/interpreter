package es.predictia.metaclip.visualizer.config;

import java.awt.Color;
import java.awt.Font;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.octo.captcha.CaptchaFactory;
import com.octo.captcha.component.image.backgroundgenerator.UniColorBackgroundGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.SimpleTextPaster;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.word.FileDictionary;
import com.octo.captcha.component.word.wordgenerator.DictionaryWordGenerator;
import com.octo.captcha.engine.GenericCaptchaEngine;
import com.octo.captcha.image.gimpy.GimpyFactory;
import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.service.multitype.GenericManageableCaptchaService;

@Configuration
@ComponentScan("com.octo.captcha")
public class CaptchaConfig {

	private static final Integer BACKGROUND_WIDTH = 220;
	private static final Integer BACKGROUND_HEIGHT = 60;
	private static final String FONT = "Arial";
	private static final Integer MIN_FONT_SIZE = 40;
	private static final Integer MAX_FONT_SIZE = 50;
	private static final Integer MIN_ACCEPTED_WORD_LENGTH = 4;
	private static final Integer MAX_ACCEPTED_WORD_LENGTH = 5;
	private static final Integer MIN_STORAGE_DELAY = 180;
	private static final Integer MAX_STORAGE_DELAY = 180000;

	@Bean
	public ImageCaptchaService captchaService() {
		return new GenericManageableCaptchaService(imageEngine(), MIN_STORAGE_DELAY, MAX_STORAGE_DELAY);
	}

	@Bean
	public GenericCaptchaEngine imageEngine() {
		return new GenericCaptchaEngine(new CaptchaFactory[] { captchaFactory() });
	}

	@Bean
	public GimpyFactory captchaFactory() {
		return new GimpyFactory(wordgen(), wordtoimage());
	}

	@Bean
	public DictionaryWordGenerator wordgen() {
		return new DictionaryWordGenerator(filedict());
	}

	@Bean
	public FileDictionary filedict() {
		return new FileDictionary("toddlist");
	}

	@Bean
	public ComposedWordToImage wordtoimage() {
		return new ComposedWordToImage(fontGenRandom(), backGenUni(), simpleWhitePaster());
	}

	@Bean
	public RandomFontGenerator fontGenRandom() {
		return new RandomFontGenerator(MIN_FONT_SIZE, MAX_FONT_SIZE, new Font[] { fontArial() });
	}

	@Bean
	public Font fontArial() {
		return new Font(FONT, 0, 10);
	}

	@Bean
	UniColorBackgroundGenerator backGenUni() {
		return new UniColorBackgroundGenerator(BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
	}

	@Bean
	SimpleTextPaster simpleWhitePaster() {
		return new SimpleTextPaster(MIN_ACCEPTED_WORD_LENGTH, MAX_ACCEPTED_WORD_LENGTH, colorPurple());
	}

	@Bean
	Color colorPurple() {
		return new Color(86, 61, 124);
	}

}