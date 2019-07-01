package es.predictia.metaclip.visualizer.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.jena.ext.com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Lists;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

@Service
public class EmailSender {

	@Autowired 
	private transient ExecutorService executor;
	
	public Future<?> sendHtmlEmail(final String email, final String subject, final String html){
		return executor.submit(new Runnable() {
			@Override
			public void run() {
				doSendHtmlEmails(Lists.newArrayList(email), subject, html);
			}
		});
	}
	
	public Future<?> sendHtmlEmail(final List<String> emails, final String subject, final String html){
		return executor.submit(new Runnable() {
			@Override
			public void run() {
				doSendHtmlEmails(emails, subject, html);	
			}
		});
	}
	
	private void doSendHtmlEmails(Collection<String> emails, String subject, String html){
		if(emails.isEmpty()){
			return;
		}
		LOGGER.info("Sending emails");
		for(final String email : emails){
			try {
			    EMAIL_RETRYER.call(new Callable<Boolean>() {
				    public Boolean call() throws MailException {
				    	EMAIL_RATE_LIMITER.acquire();
				    	LOGGER.warn("Sending email to {}", email);
				    	sendMail(email,subject,html);
				        return true;
				    }
				});
			} catch (RetryException | ExecutionException e) {
				LOGGER.error("Unable to send mail to {}: {}", email, e.getMessage());
			}
		}
		LOGGER.info("Sent {} mails", emails.size());
	}

	private Response sendMail(String email, String subject, String html){
		try{
			Email sgFrom = new Email(fromEmail);
		    Email sgTo = new Email(email.trim());
		    Content sgContent = new Content("text/html",html);
		    Mail mail = new Mail(sgFrom, subject, sgTo, sgContent);
		    SendGrid sg = new SendGrid(sendGridApi);
		    Request request = new Request();
		    request.setMethod(Method.POST);
		    request.setEndpoint("mail/send");
		    request.setBody(mail.build());
		    return sg.api(request);
		}catch(IOException e){
			LOGGER.error("Error sending email "+e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static final int MAX_EMAILS_PER_MINUTE = 60; // el limite global de 1and1 es 60 por minuto
	
    private static final RateLimiter EMAIL_RATE_LIMITER = RateLimiter.create((double) MAX_EMAILS_PER_MINUTE / 60);
	
	private static final Retryer<Boolean> EMAIL_RETRYER = RetryerBuilder.<Boolean> newBuilder()
			.retryIfExceptionOfType(MailException.class)
			.withWaitStrategy(WaitStrategies.fixedWait(30, TimeUnit.SECONDS))
			.withStopStrategy(StopStrategies.stopAfterAttempt(1)).build();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);
	
	@Value("${mail.from}") private String fromEmail;
	@Value("${mail.sendgrid.api}") private String sendGridApi;


}
