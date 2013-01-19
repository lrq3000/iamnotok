package com.google.iamnotok.utils;

import java.util.List;

import android.location.Address;

import com.google.iamnotok.LocationAddress;

public class FormatUtils {
	public String formatRecipients(List<String> to) {
		if (to == null || to.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(to.get(0));
		for (int i = 1; i < to.size(); i++) {
			sb.append(",").append(to.get(i));
		}
		return sb.toString();
	}

	public String formatMessage(
			LocationAddress locationAddress, String customMessage) {
		String message = "I am not OK!";
		if (customMessage != null && !"".equals(message)) {
			message += " " + customMessage;
		}
		if (locationAddress == null) {
			message += " No location information available!";
		} else {
			String address;
			if (locationAddress.address != null) {
				address = ": '" + formatAddress(locationAddress.address) + "'";
			} else {
				address = " Unknown";
			}
			String location;
			if (locationAddress.location != null) {
				location = " (" + "latitude: " + locationAddress.location.getLatitude() + ", longitude: " + locationAddress.location.getLongitude() + ")";
			} else {
				location = "";
			}
			message += " My current location is" + address + location;
		}
		return message;
	}

	public String formatSubject(String name, String lineNumber) {
		return "Emergency message from " + name + " (" + lineNumber + ")";
	}

	private String formatAddress(Address address) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
			if (i > 0)
				result.append(" ");
			result.append(address.getAddressLine(i));
		}
		return result.toString();
	}
}
