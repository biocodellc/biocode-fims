package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Class to send email notifications via Gmail.  If this appears to be a useful class later we can break it down
 * and allow for different email applications.
 */
public class sendEmail extends Thread {

    Properties props;
    String username;
    String password;
    String from;
    String to;
    String subject;
    String text;

    private static Logger logger = LoggerFactory.getLogger(sendEmail.class);

    public static void main(String[] args) {
        SettingsManager sm;

        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        String subject = "From a thread...";
        String text = "How are you?";

      /*  sendEmail sendEmail = new sendEmail(sm.retrieveValue("mailUser"),
                sm.retrieveValue("mailPassword"),
                sm.retrieveValue("mailFrom"),
                sm.retrieveValue("mailTo"),
                subject,
                text);
        sendEmail.start();*/
    }

    /**
     * Send an email message, initializing the various properties
     *
     * @param username
     * @param password
     * @param from
     * @param to
     * @param subject
     * @param text
     * @throws MessagingException
     */
    public  sendEmail(String username, String password, String from, String to, String subject, String text) {
        // A properties to store mail server smtp information such
        // as the host name and the port number. With this properties
        // we create a Session object from which we'll create the
        // Message object.
        //
        props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        this.username = username;
        this.password = password;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.text = text;
    }

    /**
     * run() starts the thread, this is the slow part...
     */
    public void run() {
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        //
        // Message is a mail message to be send through the
        // Transport object. In the Message object we set the
        // sender address and the recipient address. Both of
        // this address is a type of InternetAddress. For the
        // recipient address we can also set the type of
        // recipient, the value can be TO, CC or BCC. In the next
        // two lines we set the email subject and the content text.
        //

        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(from));

            message.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            message.setSubject("[Biocode-Fims Application]" + subject);
            message.setText(text);

            //
            // Send the message to the recipient.
            //
            Transport.send(message);
        } catch (MessagingException e) {
            logger.warn("MessagingException thrown", e);
        }
    }
}

