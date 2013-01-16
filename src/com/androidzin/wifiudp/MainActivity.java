package com.androidzin.wifiudp;

import java.net.DatagramSocket;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.androidzin.wifiudp.NetworkObserver.Listener;

public class MainActivity extends Activity {

	private TextView text, messageWall, deviceAddress;
	private Button helloNetworkButton, sendButton;
	private DatagramSocket socket;
	private NetworkObserver networkObserver;
	private String address;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text);
		deviceAddress = (TextView) findViewById(R.id.deviceAddress);
		messageWall = (TextView) findViewById(R.id.messageWall);
		helloNetworkButton = (Button) findViewById(R.id.button);
		sendButton = (Button) findViewById(R.id.btnAnswer);
		networkObserver = new NetworkObserver(getApplicationContext(), new Listener() {
			
			@Override
			public void onHelloAnswered(final String deviceAddress) {
				final String received = deviceAddress + "\n";
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						messageWall.append("device discovered: "+ received);
					}
				});
			}

			@Override
			public void onLocalIpGet(final String ipAddress) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						deviceAddress.setText(ipAddress);
					}
				});
			}

			@Override
			public void onMesssageReceived(final String deviceAddres,final  String message) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						messageWall.append(deviceAddres + ": " + message + "\n");
					}
				});
			}
			
		});
		networkObserver.startObserver();

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//networkObserver.sendUDPUnicastMessage(address, "new message");
				networkObserver.sendBroadcastData("new message");
			}
		});
		helloNetworkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				networkObserver.helloNetwork();
			}
		});

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		networkObserver.stopObserver();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
