package com.google.iamnotok;

import java.util.*;

import android.util.Log;

import com.google.iamnotok.EmergencyNotificationService.VigilanceState;
import com.google.iamnotok.LocationTracker.LocationAddress;
import com.google.iamnotok.utils.AccountUtils;
import com.google.iamnotok.utils.FormatUtils;

public class EmailNotificationSender implements NotificationSender {
	private static final String LOG_TAG = "IAMNOTOK - emailer";

	private final FormatUtils formatUtils;
	private final AccountUtils accountUtils;

	public EmailNotificationSender(FormatUtils formatUtils, AccountUtils accountUtils) {
		this.formatUtils = formatUtils;
		this.accountUtils = accountUtils;
	}

	@Override
	public boolean sendNotifications(
			Collection<Contact> contacts, LocationAddress locationAddress, VigilanceState state) {
		List<String> emailList = getAllContactEmails(contacts);
		if (emailList.size() > 0) {
			sendEmailMessage(emailList, locationAddress, state);
		}

		return true;
	}

	private List<String> getAllContactEmails(Collection<Contact> contacts) {
		List<String> emails = new ArrayList<String>();
		for (Contact contact : contacts) {
			if (contact.getEmail() != null) {
				emails.add(contact.getEmail());
			}
		}
		return emails;
	}

	/**
	 * Sends an email
	 */
	private void sendEmailMessage(
			List<String> to, LocationAddress locationAddress, VigilanceState state) {
	  String recipients = formatUtils.formatRecipients(to);
	  Log.d(LOG_TAG, "Sending email to: " + to);
	  String subject = formatUtils.formatSubject(accountUtils.getAccountName(), accountUtils.getPhoneNumber());
	  String message = "";
	  if (state == VigilanceState.NORMAL_STATE) {
	    message = "I am OK now";
	    Log.d(LOG_TAG, "Sending the email " + message);
	  } else {
	    Log.d(LOG_TAG, "Getting location");
	    message = formatUtils.formatMessage(locationAddress, accountUtils.getCustomMessage());
	    if (locationAddress.location != null) {
	      message += " " + getMapUrl(locationAddress);
	    }
	  }

	  try {
	    GMailSender sender = new GMailSender("imnotokandroidapplication@gmail.com", "googlezurich");
	    String mailAddress = accountUtils.getMailAddress();
	    sender.sendMail(mailAddress, subject, message, "imnotokapplication@gmail.com", recipients);
	  } catch (Exception e) {
	    Log.e("SendMail", e.getMessage(), e);
	  }
	}

	private String getMapUrl(LocationTracker.LocationAddress locAddr) {
		String template = "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q=%f,%f&sll=%f,%f&sspn=0.005055,0.009645&ie=UTF8&z=16";
		return String.format(template, locAddr.location.getLatitude(), locAddr.location.getLongitude(),
				locAddr.location.getLatitude(), locAddr.location.getLongitude());
	}
}
