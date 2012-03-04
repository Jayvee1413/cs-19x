package app.combined;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LogInSettings extends Dialog implements android.view.View.OnClickListener{
	private ReceiverActivity receiver;
	private SenderActivity sender;
	private boolean isSender;
	
	public LogInSettings (ReceiverActivity receiver) {
		super(receiver);
		this.receiver = receiver;
		this.isSender = false;
	}
	
	public LogInSettings (SenderActivity sender) {
		super(sender);
		this.sender = sender;
		this.isSender = true;
	}
	
	protected void onStart() {
        super.onStart();
        setContentView(R.layout.login);
        getWindow().setFlags(4, 4);
        setTitle("Please Log-in");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }
	
	public void onClick(View v) {
		 String username = getText(R.id.userid);
	     String password = getText(R.id.password);
	     
	     /*SASLAuthentication.supportSASLMechanism("PLAIN");
	     ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
	     XMPPConnection connection = new XMPPConnection(connConfig);
	     try {
	            connection.connect();
	            Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
	        	connection.login(username, password);
	            Log.i("XMPPClient", "Logged in as " + connection.getUser());
	            
	            Presence presence = new Presence(Presence.Type.available, "", 24, Presence.Mode.chat);
	            connection.sendPacket(presence);
	            if (isSender) {
	            	sender.setConnection(connection);
	            }else {
	            	receiver.setConnection(connection);
	            }
	            //xmppClient.setConnection(connection);
	            
	     } catch (XMPPException ex) {
	            Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
	            if (isSender) {
	            	sender.setConnection(null);
	            }else {
	            	receiver.setConnection(null);
	            }
	     }*/
	     if (isSender) {
	    	 sender.setUsername(username);
	    	 sender.setPassword(password);
	    	 sender.establishConnection(username, password);
	     }else {
	    	 receiver.setUsername(username);
	    	 receiver.setPassword(password);
	    	 receiver.establishConnection(username, password);
	     }
	     dismiss();
	}

   private String getText(int id) {
       EditText widget = (EditText) this.findViewById(id);
       return widget.getText().toString();
   }
}
