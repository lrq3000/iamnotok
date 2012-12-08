package com.google.iamnotok;

import java.util.List;


public class Contact {

	private final String id;
	private final String name;
	private final List<String> phones;
	private final List<String> emails;

	public Contact(String id, String name, List<String> phones, List<String> emails) {
		this.id = id;
		this.name = name;
		this.phones = phones;
		this.emails = emails;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSelectedPhone() {
		if (phones.size() > 0)
			return phones.get(0);
		return null;
	}

	public String getSelectedEmail() {
		if (emails.size() > 0)
			return emails.get(0);
		return null;
	}

	@Override
	public String toString() {
		return id + ": " + name + " (" + getSelectedPhone() + ") <" + getSelectedEmail() + ">";
	}
}