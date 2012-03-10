 package app.combined;

import java.io.IOException;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.text.format.Time;
import android.util.Log;

public class Sender3GListener implements MessageListener{
	private SenderActivity sender;
	private Integer currentPacket;
	//private long t;
	
	public Sender3GListener (SenderActivity sender) {
		this.sender = sender;
	}
	
	public void waiting (int n){
        long t0, t1;
        t0 =  System.currentTimeMillis();
        Log.i("INSIDE WAITING", Integer.toString(n));
        do{
            t1 = System.currentTimeMillis();
        }
        while ((t1 - t0) < (n * 1000));
    }
	
	public void processMessage(Chat chat, Message message) {
		Log.e("XMPPSender:Receiving", "Received text [" + message.getBody() + "]");
		Time time = new Time();
		
		if (message.getBody().equals("%&proceed")) {
			time.setToNow();
			//sender.setInitial(time.toMillis(true));
			setCurrentPacket(sender.getTracker());
			for (int i = 0; i < 10; i ++) {
				sendPackets(chat, time);
				waiting(2);
			}	
			
		}else if (message.getBody().equals("%&DONE")) {
			sender.setTracker(getCurrentPacket());
			Log.e("XMPPSender:Sending", "Sending file SUCCESSFUL");
			//setCurrentPacket(0);
			
			/*try {
				sender.getWriter().write((t - sender.getT1()) + " : total time\n");
				sender.getWriter().write(sender.getT2() - sender.getT1() + " : processing time\n");
				sender.getWriter().write((t - sender.getInitial()) + " : sending time\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				sender.getWriter().close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			sender.finish();
			
		}else if (message.getBody().equals("%&CONTINUE")) {
			sender.setTracker(getCurrentPacket());
			for (int i = 0; i < 10; i ++) {
				sendPackets(chat, time);
				waiting(2);
			}	
		}
		
	}
	
	private void sendPackets (Chat chat, Time time) {
		try {
			int counter = 0;
			String line;
			String packet = "";
			while (counter < 40) {
				
				//if (getCurrentPacket() == sender.getPacketList().size()) {
				if (getCurrentPacket() == sender.getPacketList().size()) {
					time.setToNow();
					//t = time.toMillis(true);
					break;
				}
				
				line = sender.getPacketList().get(getCurrentPacket());
				packet = packet + "%&" + getCurrentPacket() + " " + line;
				//setCurrentPacket(getCurrentPacket() + 1);
				setCurrentPacket(getCurrentPacket() + 1);
				counter++;
			}
			
			if (!packet.equals("")) {
				Message reply = new Message();
				reply.setType(Message.Type.chat);
				reply.setBody(packet);
				
				/*try {
					time.setToNow();
					sender.getWriter().write(time.toString() + " : Packet " + getCurrentPacket() +  "Sending\n");
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				
				chat.sendMessage(reply);
				
				try {
					time.setToNow();
					sender.getWriter().write(time.toString() + " : Packet " + getCurrentPacket() +  "Sent Via 3G\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				Log.e("XMPPSender:Sending", "Sending text [" + reply.getBody() + "] SUCCESS");
			}
		} catch (XMPPException e) {
			Log.e("XMPPSender:Sending", "Sending text packet FAILED");
		}
	}

	public Integer getCurrentPacket() {
		return currentPacket;
	}

	public void setCurrentPacket(Integer currentPacket) {
		this.currentPacket = currentPacket;
	}

	/*public Integer getCurrentPacket() {
		return currentPacket;
	}

	public void setCurrentPacket(Integer currentPacket) {
		this.currentPacket = currentPacket;
	}*/
	
}
