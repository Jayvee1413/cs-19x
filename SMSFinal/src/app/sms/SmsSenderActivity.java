package app.sms;

import java.io.*;
import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
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

public class SmsSenderActivity extends Activity {
	File logfile, logfile1;
	FileWriter fw = null, fw1 = null;
	BufferedWriter bw = null, bw1 = null;
	String phoneNo = new String();
	Button btnSendSMS;
	EditText txtPhoneNo;
	EditText txtMessage;
	EditText txtFileName;
	EditText rcvdMessage;
	SmsReceiver rcvd;
	File selectedFile;
	int packetCount; // total number of packets
	int tracker = 0;
	int sent = 0;
	int indexLimit;
	int totalresends = 0;
	Boolean initialR=false;
	Boolean check10Received;
	Boolean done;
	Time time = new Time();
	long t1, t2, initial;
	TelephonyManager Tel;
	MyPhoneStateListener MyListener;
	String sub;
	String sen = "";
	ProgressDialog dialog;
	ArrayList<String> packetList = new ArrayList<String>();
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final int FILE_EXPLORE_RESULT = 1002;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Debug.startMethodTracing("sender",32000000);
		logfile = new File("/sdcard/smsSenderLog.txt");
		logfile1 = new File("/sdcard/smsSenderSignal.txt");
		done = false;
		try {
			fw = new FileWriter(logfile);
			bw = new BufferedWriter(fw);
			fw1 = new FileWriter(logfile1);
			bw1 = new BufferedWriter(fw1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MyListener = new MyPhoneStateListener();
		Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		// Toast.makeText(getApplicationContext(),
		// android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS,
		// Toast.LENGTH_LONG).show();
		dialog = new ProgressDialog(SmsSenderActivity.this);
		Intent intent = getIntent();
		intent.getStringExtra("start?");
		setContentView(R.layout.main);
		rcvd = new SmsReceiver();
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
					sendSMS(phoneNo, message);
					sendSMS(phoneNo, "%& sendFile " + packetCount + " " + sub);
					btnSendSMS.setClickable(false);
					btnSendSMS.setVisibility(Button.INVISIBLE);
					
				} else
					Toast.makeText(getBaseContext(),
							"Please enter both phone number and message.",
							Toast.LENGTH_SHORT).show();

			}
		});

	}
	public void sms(String phoneNum, int startIndex, int endIndex) throws IOException{
		tracker = startIndex;
		indexLimit = endIndex;
		send10(phoneNum);
	}
	public void onNewIntent(Intent intent){
		if ((intent.getStringExtra("start?").toString()).equals("start sending")) {

			try {
				sms(intent.getStringExtra("phoneNum").toString(), 0, packetCount);	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if ((intent.getStringExtra("start?").toString())
				.equals("done receiving")) {
			done = true;
			Toast.makeText(getBaseContext(), "Done Sending", Toast.LENGTH_SHORT);
			
			time.setToNow();
			try {
				bw.write(time.toString() + " Total Resends " + totalresends);
				bw.write(t1- time.toMillis(true) + " : total time\n");
				bw.write(t1-t2 + " : processing time\n");
				bw.write(initial-t2 + " : sending time\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				bw.close();
				fw.close();
				bw1.close();
				fw1.close();
				Debug.stopMethodTracing();
				
				this.finish();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if ((intent.getStringExtra("start?").toString()).equals("sendAgain")) {
			check10Received= true;
			Log.i("sendAgain", "inside sendAgain");
			String resend = intent.getStringExtra("resendPackets");
			if (resend.equals("none")) {
				// do nothing
			} else {
				String[] num;
				num = resend.split(" ");

				for (int i = 0; i < num.length; i++) {

					if (!num[i].equals(" ") || !num[i].equals("") || !num[i].equals("\n")) {
						Log.e("-----NUM[i]-----", num[i]);
						int j = Integer.parseInt(num[i]);
						time.setToNow();
						try {
							bw.write(time.toString() + " : Resend " + j);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Log.e("RESEND LIST", num[i]);
						sendSMS(phoneNo, "&% " + j + " " + packetList.get(j));
						Log.i("RESENT", packetList.get(j));
						totalresends++;
					}

				}

			}

			try {
				Log.i("send10", "Before send10");
				send10(phoneNo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
				
				time.setToNow();
				t1 = time.toMillis(true);
				try {
					bw.write(time.toString() + "before compression\n");
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				
				compression.compressGzip(
						data.getExtras().getString("filePath"), data
								.getExtras().getString("fileName"));
				time.setToNow();
				try {
					bw.write(time.toString() + "after compression and before b64\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
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
					time.setToNow();
					t2= time.toMillis(true);
					bw.write(time.toString() + "after b64\n");
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

	private void send10(String phoneNumber) throws IOException {
		if(!initialR){
			initial= time.toMillis(true);
			initialR= true;
		}
		String submessage = new String();
		String headerBegin = new String();
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage("Sending SMS...");
		dialog.setCancelable(false);
		dialog.setProgress(0);
		dialog.setMax(indexLimit);
		Log.i("send10", "I AM AT send10");
		dialog.show();

		// dialog.show(SmsMessagingActivity.this, "Sending SMS", "Please Wait");
		sen = "sending";
		for (int counter = 0; counter < 10 && tracker < indexLimit; counter++) {
			Log.i("send10", "inside send10 for loop");
			headerBegin = "&% " + tracker + " ";
			submessage = headerBegin + packetList.get(tracker);
			tracker++;
			Log.i("SUBMESSAGE", submessage);
			Log.i("PHONE NUMBER", phoneNumber);
			sendSMS(phoneNumber, submessage);
			waiting(3);

		}
		check10Received= false;
		sendSMS(phoneNumber, "%&check10 " + tracker);
		Log.i("After send tracker", "tracker" + tracker);
	
		Thread thread = new waitThread();
		thread.start();
		dialog.cancel();

	}
	class waitThread extends Thread {
	    // This method is called when the thread runs
	    public void run() {
	    	long t0, t1;
			t0 = System.currentTimeMillis();
			do {
				t1 = System.currentTimeMillis();
			} while ((t1 - t0) < (90 * 1000) && check10Received==false && done==false); //wait for 90seconds
			if(check10Received||done==true){
				//do nothing
			}else{
				Log.i("resend check10", "tracker" + tracker);
				sendSMS(phoneNo, "%&check10 " + tracker);
			
				//resend check10
			}
			
	    }
	    
	}

	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	public void exploreFiles(View view) {
		Log.i("INSIDE EXPLORE FILES", "CLICKED FILE");
		Intent fileExploreIntent = new Intent(SmsSenderActivity.this,
				FileExplore.class);
		startActivityForResult(fileExploreIntent, FILE_EXPLORE_RESULT);
	}

	public static void waiting(int n) {
		long t0, t1;
		t0 = System.currentTimeMillis();
		Log.i("INSIDE WAITING", Integer.toString(n));
		do {
			t1 = System.currentTimeMillis();
		} while ((t1 - t0) < (n * 1000));
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
		try {
			time.setToNow();
			bw.write(time.toString() + " : Message Sending\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
		try {
			time.setToNow();
			bw.write(time.toString() + " : Message Sent\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
	}

	/* Called when the application resumes */
	@Override
	protected void onResume() {
		super.onResume();
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}

	/* ���������� */
	/* Start the PhoneState listener */
	/* ���������� */
	private class MyPhoneStateListener extends PhoneStateListener {
		/*
		 * Get the Signal strength from the provider, each tiome there is an
		 * update
		 */
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {

			super.onSignalStrengthsChanged(signalStrength);
			time.setToNow();
			try {
				bw1.write(time.toString() + ": "
						+ String.valueOf(signalStrength.getGsmSignalStrength())
						+ "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	};/* End of private Class */

	protected void onDestroy() {
		try {
			bw.close();
			bw1.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onDestroy();
	}
	
	
}

