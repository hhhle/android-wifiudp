package com.androidzin.wifiudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkObserver extends Thread {

	private static final int DISCOVERY_PORT = 9000;
	private String TAG = NetworkObserver.class.getCanonicalName();
	private DatagramSocket datagramSocket;
	private Context mContext;
	private Listener mListener;

	/**
	 * @author sauloaguiar
	 *
	 */
	public interface Listener {
		void onHelloAnswered(String deviceAddress);
		void onLocalIpGet(String ipAddress); 
		void onMesssageReceived(String deviceAddres, String message);
	}
	
	public NetworkObserver(Context context, Listener listener) {
		try {
			mContext = context;
			mListener = listener;
			datagramSocket = new DatagramSocket(DISCOVERY_PORT);
			datagramSocket.setBroadcast(true);
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void startObserver() {
		start();
		try {
			sendDiscoveryRequest();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true && !isInterrupted()) {
			try {
				listenForResponses();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void helloNetwork() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sendDiscoveryRequest();
				} catch (IOException e) {
					Log.e(TAG, "Could not send discovery request", e);
				}
			}
		});
		t.start();
	}

	/**
	 * Send a broadcast UDP packet containing a request for boxee services to
	 * announce themselves.
	 * 
	 * @throws IOException
	 */
	private void sendDiscoveryRequest() throws IOException {
		final String data = "hello: hello network";
		Log.d(TAG, "Sending data " + data);

		(new Thread(new Runnable() {
			@Override
			public void run() {
					sendBroadcastMessage(data);
			}
		})).start();

	}
	
	/**
	 * @param address 
	 * @param message
	 */
	public void sendUDPUnicastMessage(final String address, final String message) {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String numericAddress = address.substring(1);
					DatagramPacket	packet = new DatagramPacket(message.getBytes(), message.length(),InetAddress.getByName(numericAddress), DISCOVERY_PORT);
					datagramSocket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		})).start();
	}

	
	public void sendBroadcastData(final String message) {
		final String msg = "message: " + message;
		sendBroadcastMessage(msg);
	}
	/**
	 * @param message
	 */
	private void sendBroadcastMessage(final String message) {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(),getBroadcastAddress(), DISCOVERY_PORT);
					datagramSocket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		})).start();
	}
	
	/**
	 * Listen on socket for responses, timing out after TIMEOUT_MS
	 * 
	 * @param socket
	 *            socket on which the announcement request was sent
	 * @throws IOException
	 */
	private void listenForResponses() throws IOException {
		byte[] buf = new byte[1024];
		try {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			datagramSocket.receive(packet);
			String localIP = "/" + getLocalIp();
			if (localIP.equalsIgnoreCase(packet.getAddress().toString())) {
				// own message - do nothing
			} else {
				String received = new String(packet.getData(), 0,packet.getLength());
				handleIncomingData(received, packet.getAddress().toString());				
			}
		} catch (SocketTimeoutException e) {
			Log.d(TAG, "Receive timed out");
		}
	}

	private void handleIncomingData(String received, String deviceAddress) {
		if ( received.startsWith("hello:") ){ 
			sendBroacastIP();
		}
		if ( received.startsWith("device:") ){ 
			mListener.onHelloAnswered(deviceAddress);
		}
		if ( received.startsWith("message:")) {
			String message = received.substring(8);
			mListener.onMesssageReceived(deviceAddress, message);
		}
		//mListener.onHelloAnswered(received);
	}

	
	private void sendBroacastIP(){
		String message = "device:" + getLocalIp();
		sendBroadcastMessage(message);
	}
	/**
	 * @return
	 */
	private String getLocalIp() {
		WifiManager wifi = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifi.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String address = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
				(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
				(ipAddress >> 24 & 0xff));
		mListener.onLocalIpGet(address);
		return address;

	}

	/**
	 * @return
	 * @throws IOException
	 */
	private InetAddress getBroadcastAddress() throws IOException {
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}

	

	/**
	 * 
	 */
	public void stopObserver() {
		this.interrupt();
		mListener = null;
		if ( datagramSocket != null) {
			datagramSocket.close();
		}
	}
}
