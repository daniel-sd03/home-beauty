package sodresoftwares.homebeauty.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationCode(String to, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(to);
            message.setSubject("Seu código de ativação - Home Beauty");

            String text = String.format(
                    "Olá %s!\n\n" +
                            "Bem-vindo(a) ao Home Beauty.\n" +
                            "Seu código de ativação de 6 dígitos é: %s\n\n" +
                            "Este código expira em 10 minutos.\n\n" +
                            "Equipe Home Beauty",
                    name, code
            );

            message.setText(text);
            mailSender.send(message);

            log.info("Verification email successfully sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
}