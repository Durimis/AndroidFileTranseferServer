package com.example.androidsocketfiletransferserver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import Funskionet.Deflate;
import Funskionet.EnkriptimiAES;
import model.Message;
import model.MessagesListAdapter;

public class MainActivity extends Activity {

	TextView infoIp, infoPort;

	static final int SocketServerPORT = 8080;
	ServerSocket serverSocket;
	ServerSocketThread serverSocketThread;
	ListView listViewMesazhet;
	private MessagesListAdapter adapter;
	private List<Message> listMessages;
	private Button button_hape;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		infoIp = (TextView) findViewById(R.id.infoip);
		infoPort = (TextView) findViewById(R.id.infoport);
		button_hape = (Button) findViewById(R.id.button_lokacioni);
		infoIp.setText(getIpAddress());
		
		serverSocketThread = new ServerSocketThread();
		serverSocketThread.start();
		listViewMesazhet = (ListView) findViewById(R.id.listViewMesazhet);
		listMessages = new ArrayList<Message>();

		adapter = new MessagesListAdapter(this, listMessages);
		listViewMesazhet.setAdapter(adapter);
		button_hape.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				hapeFolderinPranimit();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
					.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces
						.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface
						.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip += "Ip adresa lokale: "
								+ inetAddress.getHostAddress() + "\n";
					}

				}

			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		}

		return ip;
	}
	
	public class ServerSocketThread extends Thread {

		@Override
		public void run() {
			Socket socket = null;
			DataInputStream dataInputStream = null;
			DataOutputStream dataOutputStream = null;
			
			try {
				serverSocket = new ServerSocket(SocketServerPORT);
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						infoPort.setText("Duke pritur lidhjen: "
								+ serverSocket.getLocalPort());
					}});
				
				while (true) {
					socket = serverSocket.accept();

					dataInputStream = new DataInputStream(
							socket.getInputStream());
					dataOutputStream = new DataOutputStream(
							socket.getOutputStream());

					ObjectInputStream ois = new ObjectInputStream(dataInputStream);
					ObjectOutputStream oos = new ObjectOutputStream(dataOutputStream);
					byte[] bytes;
					FileOutputStream fos = null;
					String emriFajllit = "";
					String mesazhiPranuar = "";
					try {
						bytes = (byte[])ois.readObject();

						try {
							String mesazhiDekriptuar = merrTeDekriptuar(ois.readUTF());
							JSONObject mesazhiPranuarJSON = new JSONObject(mesazhiDekriptuar);
							emriFajllit = mesazhiPranuarJSON .getString("emriFajllit");
							mesazhiPranuar = mesazhiPranuarJSON.getString("mesazhi") + "\n" + "Fajlli :" + emriFajllit ;
							Message m = new Message("Klienti" + socket.getInetAddress(),mesazhiPranuar , false);
							appendMessage(m);


						} catch (JSONException e) {
							e.printStackTrace();
						}
						Deflate deflate = new Deflate();

						ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

						File output_file = new File(
								//"mnt/shared/s4/pranuar",
								"/storage/emulated/0/serveri",emriFajllit);

						FileUtils.copyInputStreamToFile(deflate.getUncompressedDataFromInputStream(bis), output_file);

					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						if (fos != null) {
							fos.close();
						}
					}
					String mesazhiEnkriptuar = merrTeEnkriptuar("Fajlli: '" + emriFajllit + "' u pranua.");
					oos.writeUTF(mesazhiEnkriptuar);
					Message m = new Message("Serveri","Fajlli: '" + emriFajllit + "' u pranua." , true);
					appendMessage(m);
					oos.flush();
					socket.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

	}

	private void appendMessage(final Message m) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				listMessages.add(m);

				adapter.notifyDataSetChanged();

				// Playing device's notification
				//playBeep();
			}
		});
	}

	public String merrTeDekriptuar(String perDekriptim){

		String seedValue = "kodi Sekret Test";
		String mesazhiDekriptuar = "";
		try {



			mesazhiDekriptuar  = EnkriptimiAES.decrypt(seedValue, perDekriptim);



		} catch (Exception e) {

			// TODO Auto-generated catch block

			e.printStackTrace();

		}

		return  mesazhiDekriptuar;

	}

	public String merrTeEnkriptuar(String perEnkriptim){

		String seedValue = "kodi Sekret Test";
		String mesazhiEnkriptuar = "";
		try {

			mesazhiEnkriptuar = EnkriptimiAES.encrypt(seedValue, perEnkriptim);



		} catch (Exception e) {

			// TODO Auto-generated catch block

			e.printStackTrace();

		}

		return  mesazhiEnkriptuar;

	}

	public void hapeFolderinPranimit()
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		Uri uri = Uri.parse("/storage/emulated/0/serveri");
		intent.setDataAndType(uri, "*/*");
		startActivity(Intent.createChooser(intent, "Hape folderi"));
	}


}
