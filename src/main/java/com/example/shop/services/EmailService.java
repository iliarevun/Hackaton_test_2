package com.example.shop.services;


import com.example.shop.email.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@AllArgsConstructor
public class EmailService implements EmailSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    @Async
    @Override
    public void send(String emailTo, String email){
        try{
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage,
                    "utf-8");
            mimeMessageHelper.setText(email, true);
            mimeMessageHelper.setTo(emailTo);
            mimeMessageHelper.setSubject("Thank you for registering at SecureMind Hub");
            mimeMessageHelper.setFrom("SecureMindHub1995@gmail.com", "SecureMind Hub");
            javaMailSender.send(mimeMessage);

        }
        catch(MessagingException e){
            LOGGER.error("Failed to send email");
            throw new IllegalStateException("Failed to send email");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }
}
