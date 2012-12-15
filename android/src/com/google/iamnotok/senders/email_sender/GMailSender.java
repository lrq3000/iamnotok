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
import javax.mail.internet.AddressException;
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

        new EmailSenderTask(from, subject, body, sender, recipients).execute();
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
    
    /**
     * Solving NetworkOnMainThreadException in JellyBean. 
     */
    private class EmailSenderTask extends AsyncTask<Void, Void, Void> {
    	public final int REPLY_TO_ADDRESSES_AMOUNT = 1;
    	
    	private String subject;
    	private String recipients;
    	private InternetAddress from;
    	private InternetAddress[] replyToAddresses;
    	private MimeMessage message;
        private DataHandler handler;
    	
    	public EmailSenderTask(final String from, String subject, String body, String sender,
                                      String recipients) {
    		this.subject = subject;
    		this.recipients = recipients;
    		
    		try {
				this.from = new InternetAddress(from.contains("@") ? from : sender);
			} catch (AddressException e) {
				Log.e("sendMail", e.getMessage(), e);
			}
    		
    		this.replyToAddresses = new InternetAddress[REPLY_TO_ADDRESSES_AMOUNT];
    		this.replyToAddresses[0] = this.from;
    		
    		this.message = new MimeMessage(session);
    		this.handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		try {
	    		// TODO: gmail seems to override this... need to find out how to set the sender
	            // COMMENT: Added reply to address so that the user would be able to reply directly to the sender.
	            this.message.setSender(this.from);
	            this.message.setFrom(this.from);
	            this.message.setReplyTo(this.replyToAddresses);
	            this.message.setSubject(this.subject);
	            this.message.setDataHandler(this.handler);
	
	            if (this.recipients.indexOf(',') > 0) {
	            	this.message.setRecipients(Message.RecipientType.TO,
	                        InternetAddress.parse(this.recipients));
	            } else {
	            	this.message.setRecipient(Message.RecipientType.TO, new InternetAddress(
	            			this.recipients));
	            }
    		} catch(Exception e) {
    			Log.e("sendMail", e.getMessage(), e);
    		}
    	}
    	
    	@Override
        protected Void doInBackground(Void... params) {
            try {
                Transport.send(this.message);
            } catch (Exception e) {
                Log.e("sendMail", e.getMessage(), e);
            }

            return null;
        }
    }
}