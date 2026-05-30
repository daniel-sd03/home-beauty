package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for EmailService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private final String testEmail = "client@test.com";
    private final String testName = "John Doe";
    private final String testCode = "123456";

    @BeforeEach
    void setUp() {
        // Injects a mock value into the @Value annotated field bypasssing Spring's context
        ReflectionTestUtils.setField(emailService, "remetente", "noreply@homebeauty.com");
    }

    @Test
    @DisplayName("Should build and send verification email successfully")
    void testSendVerificationCode_Success() {
        // Act
        emailService.sendVerificationCode(testEmail, testName, testCode);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getFrom()).isEqualTo("noreply@homebeauty.com");
        assertThat(sentMessage.getTo()).containsExactly(testEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Seu código de ativação - Home Beauty");

        // Assert that the text contains the critical dynamic variables
        assertThat(sentMessage.getText()).contains(testName);
        assertThat(sentMessage.getText()).contains(testCode);
    }

    @Test
    @DisplayName("Should catch exception and not crash when JavaMailSender fails")
    void testSendVerificationCode_Failure() {
        // Arrange
        // Simulates the mail server being down or throwing an exception
        doThrow(new MailSendException("Mail server connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        // We use assertThatCode(...).doesNotThrowAnyException() to explicitly verify
        // that the catch block works and the application doesn't crash.
        assertThatCode(() -> emailService.sendVerificationCode(testEmail, testName, testCode))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}