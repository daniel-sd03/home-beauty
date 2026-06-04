package sodresoftwares.homebeauty.services;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;


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

            log.info("Verification email successfully sent to: {}", maskEmail(to));

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", maskEmail(to), e.getMessage(), e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username.charAt(0) + "***@" + domain;
        }
        return username.substring(0, 2) + "***@" + domain;
    }
}