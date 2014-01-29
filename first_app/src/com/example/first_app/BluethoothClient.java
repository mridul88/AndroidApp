package com.example.first_app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class BluethoothClient extends Activity {
	// Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
	
	private ParcelFileDescriptor mInputPFD;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothService mBluetoothService;
	private InputStream file_in;
    
	
	 // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluethooth_connectivity);
		
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		
		
		if(Intent.ACTION_SEND.equals(action) && type !=null)
		{
			if(type.startsWith("image"))
			{
				handleSendImage(intent);
			}
		}
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluethooth_connectivity, menu);
		return true;
	}
	
	 @Override
	    public void onDestroy() {
	        super.onDestroy();
	        // Stop the Bluetooth chat services
	        if (mBluetoothService != null) mBluetoothService.stop();
	        if(D) Log.e(TAG, "--- ON DESTROY ---");
	    }
	
	private int handleSendImage(Intent intent)
	{
		
		 	Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		    //Uri imageUri = intent.getData();
		 	System.out.println(imageUri.getLastPathSegment());
		 	//String mimeType = getContentResolver().getType(imageUri);
		    System.out.println("uri -> "+imageUri);
		 	
			/*File file = new File(dir.getPath());
			long size = file.length();
			String name = file.getName();
			
			System.out.println("File name: " + name + "\n Mime type = "+ mimeType +"\nsize =" + size);*/
			/* Display the file name, size and type*/
		    
			try {
                
                 /* Get the content resolver instance for this context, and use it
                  to get a ParcelFileDescriptor for the file.*/
                mInputPFD = getContentResolver().openFileDescriptor(imageUri, "r");
                FileDescriptor fd = mInputPFD.getFileDescriptor();
                file_in = new BufferedInputStream(new FileInputStream(fd));
                System.out.println("available "+file_in.available());
				 
            } 
			catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MainActivity", "File not found.");
                return -1;
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
            startBluetooth();

		    return 0;

	}
	
	private void startBluetooth()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			System.out.println("Device does not support Bluetooth");
			return;
		}
		
		if (!mBluetoothAdapter.isEnabled())
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			discoverDevices();
		}
		
	}
	
	private void discoverDevices()
	{
		mBluetoothService = new BluetoothService(this, mHandler);
		//start the accept thread to listen for the connection
		mBluetoothService.start();
		
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		
		 ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
	     pairedListView.setAdapter(mPairedDevicesArrayAdapter);
	     pairedListView.setOnItemClickListener(mDeviceClickListener);
		
		// If there are paired devices
		System.out.println("No of pair = " + pairedDevices.size());
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
			
		    for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to an array adapter to show in a ListVi
		        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		        System.out.println(device.getName() + "\n" + device.getAddress());
		    }
		    System.out.println("search complete");
		}
		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	
            	System.out.println("Bluetooth Enabled.");
  
            	discoverDevices();          	 
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "bt_not_enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


	 // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            
            connectDevice(address, true);
      
        }
    };
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    //setStatus(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    //setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                //mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                //Toast.makeText(getApplicationContext(), "Connected to "
                 //              + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    private void  connectDevice(String address, boolean security)
    {
    	 BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    	 mBluetoothService.connect(device, file_in);
    	 // After Connecting send the file selected
    	 //sendMessage();
    	 return;
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage() {
    	byte[] send = new byte[1024];
        // Check that we're actually connected before trying anything
       // while (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
         //   System.out.println("Not Connected -- Error in send message");
        	//return;
        //}

            // Get the message bytes and tell the BluetoothChatService to write
        	try 
        	{
				while( -1 != file_in.read(send, 0, 1024))
				{
					System.out.println("data ->"+send.toString());
					mBluetoothService.write(send);
				}
			} 
        	catch (IOException e) 
        	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }


}/*End of Main Blurtooth Connectivity class*/
