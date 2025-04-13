import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sosgps.wayrecall.utils.MailSender;

import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

// Отправка простого сообщения с типом контента "text/plain"
public class TestEmail {

    public static void main(String[] args) {

        final MailSender mailSender = new MailSender();
        mailSender.setEmail("info@ksb-stels.ru");
        mailSender.setHost("mail.ksb-stels.ru");
        mailSender.setLogin("info");
        mailSender.setPassword("Asdqwe13a");

        mailSender.sendEmail("xiexed@gmail.com","тема","текст");

    }

}

