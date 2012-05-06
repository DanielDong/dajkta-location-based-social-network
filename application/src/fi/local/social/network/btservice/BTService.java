package fi.local.social.network.btservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;




import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class BTService extends Service{


	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SEND_EVENT = 3;
	public static final int MSG_REC_EVENT = 4;
	public static final int MSG_NEW_ADDR = 5;
	public static final int MSG_START_DISCOVERY = 6;
	public static final int MSG_REC_MESSAGE = 7; 
	public static final int MSG_START_CONNCETION = 8;
	public static final int MSG_REGISTERED_CLIENT = 9;
	public static final int MSG_PING = 10;
	public static final int MSG_CHAT_MESSAGE = 11;
	public static final int LEAVE_CHATACTIVITY = 12;
	public static final int CONNECTION_LOST = 13;
	public static final int START_CHAT_AVTIVITY = 14;
	public static final int CONNECTION_FAILED = 15;

	private BluetoothAdapter mBluetoothAdapter = null;


	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	/** Keeps track of all current registered clients. */
	public static ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	private String TAG = "btservice";
	private IntentFilter intFilter;

	private static final String NAME = "MobileNeighbour";

	// TODO: Change this
	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");//fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	public static int mState = -1;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
//	private CheckVisablityThread checkVisablityThread;
	private static boolean isRunning;

	private static final boolean D = true;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {


		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				sendRegisteredClient();
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SEND_EVENT:
				// TODO send to device
				break;
			case MSG_START_DISCOVERY:
				doDiscovery();
				break;
			case MSG_START_CONNCETION:
				Bundle data = msg.getData();
				String address = data.getString("address");
				System.err.println("starting connection to address:  " + address);
				BluetoothDevice b = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
				connect(b);
				break;
			case MSG_CHAT_MESSAGE:
				Bundle chatMessage = msg.getData();
				String message = chatMessage.getString("chatMessage");
				byte[] bytes = null;
				try {
					bytes = message.getBytes("UTF-16LE");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				write(bytes);
				break;
			case LEAVE_CHATACTIVITY:
				// check if we are still in the connected thread
				if(mState == 3)
				{
					start();
				}


				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void ensureDiscoverable() {
		if (D) Log.d(TAG, "Inside method: ensureDiscoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
			discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(discoverableIntent);
		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		if (D) Log.d(TAG, "Inside method: onCreate");
		super.onCreate();

		
//		this.stopSelf();
//		ActivityManager systemService = (ActivityManager)getApplicationContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
//		List<RunningServiceInfo> runningServices = systemService.getRunningServices(Integer.MAX_VALUE);
//		for (RunningServiceInfo runningServiceInfo : runningServices) {
//			if("fi.local.social.network.btservice.BtService".equals(runningServiceInfo.service.getClassName().toString()))
//			{
//				//runningServiceInfo.
//				System.err.println("service is still running when we start our service");
//			}
//		}
//		checkVisablityThread = new CheckVisablityThread();
//		checkVisablityThread.start();

		// is the bluetooth turned on?
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// turn bt on if it not turned on
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();


		


		// define filter for broadcast
		intFilter = new IntentFilter();
		intFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		intFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);





		// Register for broadcasts when a device is discovered
		this.registerReceiver(broadCastReceiver, intFilter);
		start();
		ensureDiscoverable();
		isRunning = true;
	}


	public static boolean isRunning() {
		return isRunning;
	}


	@Override
	public void onDestroy() {
		if (D) Log.d(TAG, "Inside method: onDestroy");
		super.onDestroy();
		if (D) Log.d(TAG, "Cancelling discovery");
		mBluetoothAdapter.cancelDiscovery();
		isRunning = false;
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void doDiscovery() {
		if (D) Log.d(TAG, "Inside method: doDiscovery");



		// If we're already discovering, stop it
		if (mBluetoothAdapter.isDiscovering())
		{
			if (D) Log.d(TAG, "Cancelling discovery");
			mBluetoothAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		if (D) Log.d(TAG, "Starting discovery");
		mBluetoothAdapter.startDiscovery();
	}

	// search for devices binded and not binded
	private static final BroadcastReceiver broadCastReceiver = new BroadCastReceiverDevices();

	// send the founded devices to the peopleactivity
	public static void sendRegisteredClient() {
		System.err.println("new registered client");
		for(int i = 0; i < mClients.size() ; i++)
		{
			Messenger client = mClients.get(i);

			Message msg = Message.obtain(null, MSG_REGISTERED_CLIENT);
			try {
				client.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}




	// send the founded devices to the peopleactivity
	public static void sendAddrToPeopleActivity(String addr, String deviceName) {
		System.err.println("addres: " + addr);
		for(int i = 0; i < mClients.size() ; i++)
		{
			Messenger client = mClients.get(i);

			Bundle b = new Bundle();
			b.putString("address", addr);
			b.putString("deviceName", deviceName);
			Message msg = Message.obtain(null, MSG_NEW_ADDR);
			msg.setData(b);
			try {
				client.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}


	public static void sendMessageToUI(String key, String data,  int MSG_TYPE) {
		System.err.println("Inside method: sendMessageToUI");
		for(int i = 0; i < mClients.size() ; i++)
		{
			Messenger client = mClients.get(i);

			Bundle b = new Bundle();
			b.putString(key, data);
			Message msg = Message.obtain(null, MSG_TYPE);
			msg.setData(b);
			try {
				client.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

	// Cool stuff

	public synchronized void start() {
		if (D) Log.d(TAG, "Inside method: start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null)
		{
			if (D) Log.d(TAG, "Starting a new AcceptThread");
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		else
			if (D) Log.d(TAG, "Did not start a new AcceptThread!");
		setState(STATE_LISTEN);
	}


	private void connectionLost() {
		if (D) Log.d(TAG, "Inside method: connectionLost");
		setState(STATE_LISTEN);
		sendMessageToUI("connectionLost", "", CONNECTION_LOST);
		if (D) Log.d(TAG, "Starting method stop");
		stop();
		start();
		sendMessageToUI("lostConnection", "", BTService.LEAVE_CHATACTIVITY);
		// Send a failure message back to the Activity
		// TODO send to activity
	}

	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		// TODO send to activity
	}

	private void connectionFailed() {
		if (D) Log.d(TAG, "Inside method: connectionFailed");
		setState(STATE_LISTEN);
		sendMessageToUI("connectionFailed", "", CONNECTION_FAILED);

		// Send a failure message back to the Activity
		// TODO send to activity
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */


	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D) Log.d(TAG, "Inside method: connected");

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Cancel the accept thread because we only want to connect to one device
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		// TODO send to activity

		setState(STATE_CONNECTED);
	}

	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.d(TAG, "Inside method: connect");
		if (D) Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void stop() {
		if (D) Log.d(TAG, "Inside method: stop");
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		if (D) Log.d(TAG, "Inside method: write");
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED) return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}
	
	private class ConnectThread extends Thread	{
		
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		public ConnectThread(BluetoothDevice device)
		{
			if (D) Log.d(TAG, "Inside constructor of ConnectThread");
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				if (D) Log.d(TAG, "Trying to create mmSocket in ConnectThread");
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "mmSocket creating failed in ConnectThread!", e);
			}
			mmSocket = tmp;
		}

		public void run()
		{
			Log.i(TAG, "BEGIN mConnectThread (run)");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				if (D) Log.d(TAG, "Trying to connect mmSocket in ConnectThread.run()");
				mmSocket.connect();

			} catch (IOException e) {
				if (D) Log.d(TAG, "Failed to connect in ConnectThread.run()");
				connectionFailed();
				// Close the socket
				try {
					if (D) Log.d(TAG, "Trying to close mmSocket in ConnectThread.run()");
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "Unable to close() socket in ConnectThread.run()", e2);
				}
				// Start the service over to restart listening mode
				if (D) Log.d(TAG, "Calling BTService.this.start from ConnectThread.run");
				BTService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BTService.this) {
				if (D) Log.d(TAG, "Setting mConnectThread to null");
				mConnectThread = null;
			}

			// Start the connected thread
			if (D) Log.d(TAG, "Calling connected-method from ConnectThread.run");
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				if (D) Log.d(TAG, "Trying to close mmSocket in ConnectThread.cancel");
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted
	 * (or until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				if (D) Log.d(TAG, "Trying to create mmServerSocket in AcceptThread");
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "mmSocket creating failed in ConnectThread!", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D) Log.d(TAG, "BEGIN mAcceptThread (run)" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;
//			if(!checkVisablityThread.isAlive())
//				checkVisablityThread.start();

			// Listen to the server socket if we're not connectedeplyTo
			while (mState != STATE_CONNECTED) {
				try {
					if (D) Log.d(TAG, "Trying to listen for a connection in AcceptThread.run (accept)");
					if (D) Log.d(TAG, "Trying to connect socket from mmServerSocket");
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();

				} catch (IOException e) {
					Log.e(TAG, "mmServerSocket listening failed, socket not created in AcceptThread.run", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					if (D) Log.d(TAG, "Socket connection successfull in AcceptThread.run");
					synchronized (BTService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							if (D) Log.d(TAG, "Calling connected from AcceptThread.run");
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate new socket.
							try {
								if (D) Log.d(TAG, "We are already connected? Trying to close socket in AcceptThread.run");
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Failed to close the socket!", e);
							}
							break;
						}
					}
				}
			}
			if (D) Log.i(TAG, "END mAcceptThread");
		}

		public void cancel() {
			if (D) Log.d(TAG, "Inside method cancel in AcceptThread");
			try {
				if (D) Log.d(TAG, "Trying to close mmServerSocket in AcceptThread.cancel");
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Failed to close mmServerSocket in AcceptThread.cancel", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "In the constructor of ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				if (D) Log.d(TAG, "Trying to get inputStream and outputStream from socket in ConnectedThread");
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Failed to get inputStream or outputStream from socket in ConnectedThread", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread (run)");
			byte[] buffer = new byte[1024];
			
			sendMessageToUI("startChatActivity", "", START_CHAT_AVTIVITY);
			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					Log.i(TAG, "Trying to read from mmInStream in ConnectedThread.run");
					mmInStream.read(buffer);
					String receivedMessage = new String(buffer,"UTF-16LE");

					// Send the obtained bytes to the UI Activity
					buffer = null;
					buffer = new byte[1024];

					sendMessageToUI("chatMessage",receivedMessage,MSG_REC_MESSAGE);

					Log.i(TAG, receivedMessage);
				} catch (Exception e) {
					Log.e(TAG, "Encountered an exception (while reading inputstream?) in ConnectedThread.run", e);
					try {
						if (D) Log.d(TAG, "Trying to close streams in ConnectedThread.run");
						mmInStream.close();
						mmOutStream.close();
					} catch (IOException e1) {
						if (D) Log.e(TAG, "Closing streams in ConnectedThread.run failed!", e1);
						e1.printStackTrace();
					}
					
					connectionLost();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				if (D) Log.d(TAG, "In ConnectedThread.write, trying to write the buffer");
				mmOutStream.write(buffer);
				if (D) Log.d(TAG, "In ConnectedThread.write, buffer written!");
				
				// TODO: Share the sent message back to the UI Activity
				Log.i(TAG, buffer.toString());
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}


		public void cancel() {
			try {
				if (D) Log.d(TAG, "In ConnectedThread.cancel, trying to close mmSocket");
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "In ConnectedThread.cancel, closing of mmSocket failed!", e);
			}
		}
	}
}
