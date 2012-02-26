package app.combined;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SenderInitial extends Activity {
	private String phoneNo = new String();
	private Button btnSendSMS;
	private EditText txtPhoneNo;
	private EditText txtMessage;
	private EditText txtFileName;
	private File selectedFile;
	int packetCount; // total number of packets
	private Time time = new Time();
	private String sub;
	private ArrayList<String> packetList = new ArrayList<String>();
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final int FILE_EXPLORE_RESULT = 1002;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Debug.startMethodTracing("sender",32000000);
		//LOG FILES
		Intent intent = getIntent();
		intent.getStringExtra("start?");
		setContentView(R.layout.sender);
		btnSendSMS = (Button) findViewById(R.id.btnSendSMS);
		txtMessage = (EditText) findViewById(R.id.txtMessage);
		txtPhoneNo = (EditText) findViewById(R.id.phoneNumberText);
		txtFileName = (EditText) findViewById(R.id.fileNameText);

		InputFilter[] FilterArray = new InputFilter[1];
		FilterArray[0] = new InputFilter.LengthFilter(160);
		txtMessage.setFilters(FilterArray);

		btnSendSMS.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				phoneNo = txtPhoneNo.getText().toString();
				String message = txtMessage.getText().toString();
				if (phoneNo.length() > 0 && message.length() > 0) {
					//sendSMS(phoneNo, message);
					//sendSMS(phoneNo, "%& sendFile " + packetCount + " " + sub);
					Intent intent = new Intent(SenderInitial.this,
							SenderActivity.class);
					intent.putExtra("phoneNum", phoneNo);
					intent.putStringArrayListExtra("arraylist", packetList);
					intent.putExtra("packetCount", packetCount);
					intent.putExtra("start?", "fromInitial");
					startActivity(intent);
					
				} else
					Toast.makeText(getBaseContext(),
							"Please enter both phone number and message.",
							Toast.LENGTH_SHORT).show();

			}
		});

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Cursor cursor = null;
				String number = "";
				try {
					Uri result = data.getData();
					Log.v("Contact Picker",
							"Got a contact result: " + result.toString());

					// get the contact id from the Uri
					String id = result.getLastPathSegment();

					cursor = getContentResolver().query(Phone.CONTENT_URI,
							null, Phone.CONTACT_ID + "=?", new String[] { id },
							null);

					int numberIdx = cursor.getColumnIndex(Phone.DATA);

					// let's just get the first contact
					if (cursor.moveToFirst()) {
						number = cursor.getString(numberIdx);
						Log.v("Contact Picker", "Got mobile number: " + number);
					} else {
						Log.w("Contact Picker", "No results");
					}
				} catch (Exception e) {
					Log.e("Contact Picker", "Failed to get contact number", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					EditText numberEntry = (EditText) findViewById(R.id.phoneNumberText);
					numberEntry.setText(number);
					if (number.length() == 0) {
						Toast.makeText(this,
								"No mobile number found for contact.",
								Toast.LENGTH_LONG).show();
					}
				}
				break;
			case FILE_EXPLORE_RESULT:
				txtFileName.setText(data.getExtras().getString("fileName"));
				File file = new File(data.getExtras().getString("filePath")
						+ "/" + data.getExtras().getString("fileName"));
				selectedFile = file;
				int j;
				for (j = selectedFile.getName().length() - 1; selectedFile
						.getName().charAt(j) != '.'; j--)
					;

				sub = selectedFile.getName().substring(j + 1);

				txtFileName.setText(file.getName() + " "
						+ Long.toString((file.length())));
				
				
				compression.compressGzip(
						data.getExtras().getString("filePath"), data
								.getExtras().getString("fileName"));
				time.setToNow();
		
				Log.i("Base 64", "Before Base 64");
				try {
					Log.i("FILE", data.getExtras().getString("filePath") + "/"
							+ data.getExtras().getString("fileName") + ".gz");
					packetList = Base64FileEncoder.encodeFile(data.getExtras()
							.getString("filePath")
							+ "/"
							+ data.getExtras().getString("fileName") + ".gz",
							data.getExtras().getString("filePath") + "/"
									+ "encodedFile.txt");
					Log.i("Base 64", "After Base 64");
					

					packetCount = packetList.size();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			}

		} else {
			Log.w("OnActivityResult", "Warning: activity result not ok");
		}
	}

	
	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	public void exploreFiles(View view) {
		Log.i("INSIDE EXPLORE FILES", "CLICKED FILE");
		Intent fileExploreIntent = new Intent(SenderInitial.this,
				FileExplore.class);
		startActivityForResult(fileExploreIntent, FILE_EXPLORE_RESULT);
	}

	// ---sends an SMS message to another device---
	private void sendSMS(String phoneNumber, String message) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
				SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	}

	

	
}

