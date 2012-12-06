package com.google.iamnotok.senders.email_sender;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Email sender provider
 */

public class GMailSender extends javax.mail.Authenticator {
    private String mailhost = "smtp.gmail.com";
    private String user;
    private String password;
    private Session session;

    static {
        Security.addProvider(new JSSEProvider());
    }

    public GMailSender(String user, String password) {
        this.user = user;
        this.password = password;

        Properties props = new Properties();

        props.put("mail.smtp.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        
        session = Session.getDefaultInstance(props, this);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    public synchronized void sendMail(final String from, String subject, String body, String sender,
                                      String recipients) {

        new EmailSenderTask().execute(from, subject, body, sender, recipients);
    }

    private class EmailSenderTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            String from = params[0];
            String subject = params[1];
            String body = params[2];
            String sender = params[3];
            String recipients = params[4];
            InternetAddress[] replyToAddresses = new InternetAddress[1];

            try {
                MimeMessage message = new MimeMessage(session);
                DataHandler handler = new DataHandler(new ByteArrayDataSource(
                        body.getBytes(), "text/plain"));

                InternetAddress fromAddress = new InternetAddress(from.contains("@") ? from : sender);
                replyToAddresses[0] = fromAddress;

                // TODO: gmail seems to override this... need to find out how to set the sender
                // COMMENT: Added reply to address so that the user would be able to reply directly to the sender.
                message.setSender(fromAddress);
                message.setFrom(fromAddress);
                message.setReplyTo(replyToAddresses);
                message.setSubject(subject);
                message.setDataHandler(handler);

                if (recipients.indexOf(',') > 0) {
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(recipients));
                } else {
                    message.setRecipient(Message.RecipientType.TO, new InternetAddress(
                            recipients));
                }

                Transport.send(message);
            } catch (Exception e) {
                Log.e("sendMail", e.getMessage(), e);
            }

            return null;
        }
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String getContentType() {
            if (type == null)
                return "application/octet-stream";
            else
                return type;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String getName() {
            return "ByteArrayDataSource";
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }
}