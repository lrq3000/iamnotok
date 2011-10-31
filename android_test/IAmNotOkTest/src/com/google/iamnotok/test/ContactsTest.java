package com.google.iamnotok.test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;

import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.*;

import android.content.*;
import android.content.SharedPreferences.Editor;

import com.google.iamnotok.*;


public class ContactsTest {

	private IMocksControl ctrl;

	@Before
	public void setup() {
		ctrl = createControl();
	}
	
	@After
	public void cleanUp() {
		ctrl.reset();
	}
	
	@Test
	public void testLoadEmptyContacts() {
		// mock the context behavior
		Context mockContext = ctrl.createMock(Context.class);
		SharedPreferences mockPrefs = ctrl.createMock(SharedPreferences.class);
		
		expect(mockContext.getSharedPreferences("MyPrefsFile", 0)).andReturn(mockPrefs).anyTimes();
		expect(mockPrefs.getString("contact_ids", "")).andReturn("");
		
		ctrl.replay();
		
		// create the object for test
		EmergencyContactsHelper helper = new EmergencyContactsHelper(mockContext, new ContactLookupUtil());
		
		// check that initially, there are no contacts
		assertTrue(helper.getAllContacts().isEmpty());

		ctrl.verify();
	}
	
	@Test
	public void testLoadSingleContact() {
		// mock the context behavior
		Context mockContext = ctrl.createMock(Context.class);
		SharedPreferences mockPrefs = ctrl.createMock(SharedPreferences.class);
		ContactLookup mockLookup = ctrl.createMock(ContactLookup.class);
		
		expect(mockContext.getSharedPreferences("MyPrefsFile", 0)).andReturn(mockPrefs).anyTimes();
		expect(mockPrefs.getString("contact_ids", "")).andReturn("5");
		
		Contact expectedContact = new Contact("5", "Igal Kreichman", "1-800-111-222", "iamnotok@gmail.com");
		expect(mockLookup.lookup(mockContext, "5")).andReturn(expectedContact);
		
		ctrl.replay();
		
		// create the object for test
		EmergencyContactsHelper helper = new EmergencyContactsHelper(mockContext, mockLookup);
		
		// check that initially, there are no contacts
		Collection<Contact> contacts = helper.getAllContacts();
		assertEquals(1, contacts.size());
		assertEquals(expectedContact, contacts.iterator().next());

		ctrl.verify();
	}

	@Test
	public void testLoadSeveralContact() {
		// mock the context behavior
		Context mockContext = ctrl.createMock(Context.class);
		SharedPreferences mockPrefs = ctrl.createMock(SharedPreferences.class);
		ContactLookup mockLookup = ctrl.createMock(ContactLookup.class);
		
		expect(mockContext.getSharedPreferences("MyPrefsFile", 0)).andReturn(mockPrefs).anyTimes();
		expect(mockPrefs.getString("contact_ids", "")).andReturn("5,1,2");
		
		Contact expectedContact5 = new Contact("5", "Igal Kreichman", "1-800-111-222", "iamnotok@gmail.com");
		Contact expectedContact1 = new Contact("1", "Nadir Izrael", null, "iamnotok@gmail.com");
		Contact expectedContact2 = new Contact("2", "Misha Seltzer", "1-800-111-222", null);
		expect(mockLookup.lookup(mockContext, "5")).andReturn(expectedContact5);
		expect(mockLookup.lookup(mockContext, "1")).andReturn(expectedContact1);
		expect(mockLookup.lookup(mockContext, "2")).andReturn(expectedContact2);
		
		ctrl.replay();
		
		// create the object for test
		EmergencyContactsHelper helper = new EmergencyContactsHelper(mockContext, mockLookup);
		
		// check that initially, there are no contacts
		Collection<Contact> contacts = helper.getAllContacts();
		assertEquals(3, contacts.size());
		Iterator<Contact> iterator = contacts.iterator();
		assertEquals(expectedContact5, iterator.next());
		assertEquals(expectedContact1, iterator.next());
		assertEquals(expectedContact2, iterator.next());
		
		ctrl.verify();
	}
	
	
	static String currContactIds;
	
	@Test
	public void testAddRemoveContact() {
		// mock the context behavior
		Context mockContext = ctrl.createMock("mockContext", Context.class);
		SharedPreferences mockPrefs = ctrl.createMock(SharedPreferences.class);
		ContactLookup mockLookup = ctrl.createMock(ContactLookup.class);
		final Editor mockEditor = ctrl.createMock(Editor.class);
		
		expect(mockContext.getSharedPreferences("MyPrefsFile", 0)).andReturn(mockPrefs).anyTimes();
		expect(mockPrefs.getString("contact_ids", "")).andAnswer(new IAnswer<String>() {
			@Override
			public String answer() throws Throwable {
				return currContactIds;
			}
		}).anyTimes();
		expect(mockPrefs.edit()).andReturn(mockEditor).anyTimes();
		expect(mockEditor.putString(anyObject(String.class), anyObject(String.class))).andAnswer(new IAnswer<Editor>() {
			@Override
			public Editor answer() throws Throwable {
				Object[] currArgs = getCurrentArguments();
				currContactIds = (String) currArgs[1];
				return mockEditor;
			}
		}).anyTimes();
		expect(mockEditor.commit()).andReturn(true).anyTimes();
		
		Contact expectedContact5 = new Contact("5", "Igal Kreichman", "1-800-111-222", "iamnotok@gmail.com");
		Contact expectedContact1 = new Contact("1", "Nadir Izrael", null, "iamnotok@gmail.com");
		Contact expectedContact2 = new Contact("2", "Misha Seltzer", "1-800-111-222", null);
		expect(mockLookup.lookup(mockContext, "1")).andReturn(expectedContact1).anyTimes();
		expect(mockLookup.lookup(mockContext, "2")).andReturn(expectedContact2).anyTimes();
		expect(mockLookup.lookup(mockContext, "5")).andReturn(expectedContact5).anyTimes();
		
		ctrl.replay();
		
		// create the object for test
		EmergencyContactsHelper helper = new EmergencyContactsHelper(mockContext, mockLookup);

		currContactIds = "5";
		
		// check that initially, there are no contacts
		Collection<Contact> contacts = helper.getAllContacts();
		assertEquals(1, contacts.size());
		Iterator<Contact> iterator = contacts.iterator();
		assertEquals(expectedContact5, iterator.next());

		// now add a contact
		assertTrue(helper.addContact("1"));
		contacts = helper.getAllContacts();
		assertEquals(2, contacts.size());
		iterator = contacts.iterator();
		assertEquals(expectedContact5, iterator.next());
		assertEquals(expectedContact1, iterator.next());
		
		// now remove a contact
		assertTrue(helper.deleteContact("5"));
		contacts = helper.getAllContacts();
		assertEquals(1, contacts.size());
		iterator = contacts.iterator();
		assertEquals(expectedContact1, iterator.next());
		
		// check that removing a user which is no longer in the list fails
		assertFalse(helper.deleteContact("5"));
		
		
		// check that adding an existing user fails
		assertFalse(helper.addContact("1"));
		
		// now add back a contact
		assertTrue(helper.addContact("2"));
		assertTrue(helper.addContact("5"));
		contacts = helper.getAllContacts();
		assertEquals(3, contacts.size());
		iterator = contacts.iterator();
		assertEquals(expectedContact1, iterator.next());
		assertEquals(expectedContact2, iterator.next());
		assertEquals(expectedContact5, iterator.next());
		
		ctrl.verify();
	}
	
}
