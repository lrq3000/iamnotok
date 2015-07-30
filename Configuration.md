# Introduction #

This page describe configuration process of a future version of the "I'm Not OK" application.

# Mandatory configuration #

These options are required to make the system operational.

  * Contacts - one or more contacts records, containing one or more phone number for sending SMS notifications, and/or one or more email address for sending email notifications.

# Optional configuration #

These options let the user customize the application for her needs:

  * Call Phone Number - If a phone number is set, the system will make a voice call and open the speaker when entering emergency state. The default phone number is empty, skipping the voice call.

  * Account - when sending Email notification, the user name and email address are used for creating the mail subject and body. If not configured, the default user account is used. If no account is configured on the phone, a default "Unknown User" name is used.

  * Quite Mode - when enabled, pressing the on/off button 6 times within 5 seconds cause the application to start the emergency procedure, as if the "SOS" button was clicked within the application or the widget. This option is enabled by default.

  * Custom Message - message to use when sending SMS or email notifications. If not configured, the application uses a default message.

  * Notification Interval - when in emergency state, every Notification Interval seconds the system will send notifications to all contacts.

  * Cancelation Delay - after triggering an emergency, the system waits Cancelation Delay seconds before entering emergency state. If the user cancel the emergency within Cancelation Delay seconds, no notifications are sent.

# Contact Information #

For displaying configuration and sending notification, the system needs this info for each contact:

  * id - used to find contacts in the database and keep contact insertion order.
  * system\_id - contact id in the system contacts database
  * name - contact name for displaying in the contact list.
  * phones - optional phone numbers for sending SMS notifications
  * emails - optional email addresses for sending email notifications

## Adding contact notifications ##

Here is the steps needed to add a contact:

  1. Tap "Add contact" in the main activity
  1. Find and tap contact name in the system contacts activity
  1. Display ContactNotifications activity, selecting the first mobile phone and email address, assuming that they are the most important ones.
```
Foo Bar notifications:
                                                                                                                                                                            
SMS notifications:
 [x] 0545001234         MOBILE
 [ ] 0545003456         MOBILE
 [ ] 0545006789         MOBILE

Email notifictions:
 [x] foo@bar.com        WORK
 [ ] foo123@gmail.com   OTHER

[Cancel] [OK]
```

Notes: should we allow adding a contact without any notifications? For example, we can enable the OK button if at least one item is selected.

The same activity can be used to edit contact details later, and it is mostly implemented today (see ContactsDetailsActivity).

## Managing contact information ##

Keep the selected values when adding a contacts, and validate them with the system contacts database when using the contact information. The system will validate and update the emergency contacts database when displaying contacts information or when sending notifications.

If the selected value (e.g. phone number) does not exists in the system contacts database, replace it with the default value in the system contacts database. If no value exists in the system database, keep the current value.

In this case a wrong value in the emergency contacts database will be will be automatically replaced by correct value from the system contacts database, assuming that the system contacts database is more correct then the emergency database.

The schema for this solution:
```
create table contact (
    id integer primary key not null autoincrement,
    system_id text unique not null, /* Id used by the system contacts database */
    name text not null,
);

create table notification (
    id integer primary key not null autoincrement, /* For keeping insertion order */ 
    contact_id text not null references contact (id),
    type text not null, /* "EMAIL" or "SMS" */
    target text not null /* "0541231234" or "user@host.com" */
);

create unique index id_target on notifications (contact_id, type, target); /* Only one notification per same phone/email */
```

## Selecting mobile phone number ##

When selecting mobile phone number, choose the first number using the label "mobile".

If no number is labeled "mobile", select the first number in the list. It is likely that a user added another phone with the default label (e.g. work) instead of selecting mobile.

When displaying list of phones, show the mobile numbers, then other numbers.

To make the selection more clear, include the label in the item text:
```
Select mobile phone number:
-------------------------
| (054) 1234567  MOBILE |
| (052) 1237890  MOBILE |
| (034) 456789   HOME   |
-------------------------
```

A simpler solution is to choose and show only numbers labeled "mobile". The user can fix the label in the contacts application. If no mobile number defined, display "No mobile phone number".

See also http://code.google.com/p/iamnotok/issues/detail?id=7

Testing contacts application in version 4.2, if the user does not select a label, the first number is MOBILE, the second is HOME, third is WORK, and all other numbers are OTHER. So we may show only numbers labeled as HOME, WORK or OTHER, assuming that the user did not chose these labels. Other labels (e.g. FAX or PAGER) are likely to be chosen and are not mobile. Of course this may break easily if these labels are localized :-)