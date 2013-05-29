package com.bt.android.arduinobluetooth;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BluetoothCom extends Activity implements OnClickListener {

	private static final String TAG = "AndroidBluetooth";
	private static final boolean DEBUG = true;
	
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
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
 // Layout Views
    private ListView mComHistoryView;
    private EditText mOutgoingMsg;
    private Button mBtnSend;
    private Button btn_A0, btn_A1, btn_A2, btn_A3, btn_A4, btn_A5; 
    private ToggleButton tbtn_D00, tbtn_D01, tbtn_D02, tbtn_D03, tbtn_D04, tbtn_D05, tbtn_D06, tbtn_D07, tbtn_D08, tbtn_D09, tbtn_D10, tbtn_D11, tbtn_D12, tbtn_D13;
    public ToggleButton[] dPinList = {tbtn_D00, tbtn_D01, tbtn_D02, tbtn_D03, tbtn_D04, tbtn_D05, tbtn_D06, tbtn_D07, tbtn_D08, tbtn_D09, tbtn_D10, tbtn_D11, tbtn_D12, tbtn_D13};
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothComService mComService = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.board_view);
		if(DEBUG) Log.d(TAG, "Our App Successfully Loaded!");
		initBoardButtons();
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        } 
	}
	
	@Override
    public void onStart() {
        super.onStart();
        if(DEBUG) Log.d(TAG, "onStart Called");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mComService == null) setupCom();
        }
    }
	
	@Override
    public synchronized void onResume() {
        super.onResume();
        if(DEBUG) Log.d(TAG, "onResume Called");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mComService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mComService.getState() == BluetoothComService.STATE_NONE) {
              // Start the Bluetooth chat services
              mComService.start();
            }
        }
	}
	
	@Override
    public synchronized void onPause() {
        super.onPause();
        if(DEBUG) Log.d(TAG, "onPaused Called");
    }

	@Override
	public void onStop() {
        super.onStop();
        if(DEBUG) Log.d(TAG, "onStop Called");
	}
	 
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    // Stop the Bluetooth chat services
        if (mComService != null) mComService.stop();
        if(DEBUG) Log.d(TAG, "onDestroyed Called");
        
	    if (mBluetoothAdapter != null) {
	    	mBluetoothAdapter.cancelDiscovery();
        }
	    
	} 
	 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        Intent serverIntent = null;
	        switch (item.getItemId()) {
	        case R.id.secure_connect_scan:
	            // Launch the DeviceListActivity to see devices and do scan
	            serverIntent = new Intent(this, DeviceListActivity.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
	            return true;
	        case R.id.insecure_connect_scan:
	            // Launch the DeviceListActivity to see devices and do scan
	            serverIntent = new Intent(this, DeviceListActivity.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
	            return true;
	        case R.id.discoverable:
	            // Ensure this device is discoverable by others
	            ensureDiscoverable();
	            return true;
	        }
	        return false;
	    }
	
	private void setupCom() {
        if(DEBUG) Log.d(TAG, "setupCom Called");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mComHistoryView = (ListView) findViewById(R.id.lv_msgHistory);
        mComHistoryView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutgoingMsg = (EditText) findViewById(R.id.etxt_outgoing);

        // Initialize the send button with a listener that for click events
        mBtnSend = (Button) findViewById(R.id.btn_sendMsg);
        mBtnSend.setOnClickListener(this);
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mComService = new BluetoothComService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
	
	private void ensureDiscoverable() {
        if(DEBUG) Log.d(TAG, "ensureDiscoverable Called");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
	
	/**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mComService.getState() != BluetoothComService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.notConnectedString, Toast.LENGTH_LONG).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mComService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutgoingMsg.setText(mOutStringBuffer);
        }
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.btn_sendMsg:
		        if(DEBUG) Log.d(TAG, "btn_sendMsg Clicked");
				String message = mOutgoingMsg.getText().toString();
                if(DEBUG) Log.d(TAG, "With msg:"+message);
                sendMessage(message);
				break;
			default:
				if(String.valueOf(v.getClass()).contains("ToggleButton")) {
					ToggleButton tempBtn = (ToggleButton)findViewById(v.getId());
					sendMessage("pRQST"+String.valueOf(tempBtn.getTag())+String.valueOf(tempBtn.isChecked()));
				} else if(String.valueOf(v.getTag()).contains("A0")) {
					sendMessage("pRQST"+String.valueOf(v.getTag())+"false");
				}
		}
	}
	
	public String myMessage = "";
	// The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Log.d(TAG, "handleMessageCalled");
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothComService.STATE_CONNECTED:
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothComService.STATE_CONNECTING:
                    break;
                case BluetoothComService.STATE_LISTEN:
                case BluetoothComService.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                if(!writeMessage.contains("pRQST")) {
                	mConversationArrayAdapter.add("Me:  " + writeMessage);
                } else {
                	mConversationArrayAdapter.add("Me:  request sent");
                }
                break;
            case MESSAGE_READ:
            	String readMessage = (String)msg.obj;
            	Log.d(TAG, "readMessage:"+readMessage);
                if(!myMessage.contains(readMessage)) myMessage = myMessage + readMessage;
                Log.d(TAG, myMessage);
                if(myMessage.endsWith("~") || myMessage.endsWith(System.getProperty("line.separator"))) {
                	myMessage = myMessage.replaceAll("~", "");
//                	myMessage = myMessage.replaceAll("\r", "");
                	if(myMessage.contains("blpins:")) {
                		handleBlackListedPins(myMessage);
                	} else {
                		mConversationArrayAdapter.add(mConnectedDeviceName+":  " + myMessage);
                	}
                	myMessage = "";
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a com session
                setupCom();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bluetoothDenied, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mComService.connect(device, secure);
    }
    
    private void handleBlackListedPins(String msg) {
    	Log.d(TAG, "handleBlackListedPins Called");
    	msg = msg.replace("blpins:D", "");
    	String temp[] = msg.split("&D");
    	for(int i=0; i < temp.length; i++) {
    		int tInt = Integer.parseInt(temp[i]);
    		if(tInt >= 0 && tInt < dPinList.length) {
    			Log.d(TAG, String.valueOf(tInt));
    			dPinList[tInt].setEnabled(false);
    		}
    		
    	}
    }
    
    private void initBoardButtons() {
    	btn_A0 = (Button)findViewById(R.id.btn_A0);
    	btn_A1 = (Button)findViewById(R.id.btn_A1);
    	btn_A2 = (Button)findViewById(R.id.btn_A2);
    	btn_A3 = (Button)findViewById(R.id.btn_A3);
    	btn_A4 = (Button)findViewById(R.id.btn_A4);
    	btn_A5 = (Button)findViewById(R.id.btn_A5);
    	
    	tbtn_D00 = (ToggleButton)findViewById(R.id.tbtn_D00);
    	tbtn_D01 = (ToggleButton)findViewById(R.id.tbtn_D01);
    	tbtn_D02 = (ToggleButton)findViewById(R.id.tbtn_D02);
    	tbtn_D03 = (ToggleButton)findViewById(R.id.tbtn_D03);
    	tbtn_D04 = (ToggleButton)findViewById(R.id.tbtn_D04);
    	tbtn_D05 = (ToggleButton)findViewById(R.id.tbtn_D05);
    	tbtn_D06 = (ToggleButton)findViewById(R.id.tbtn_D06);
    	tbtn_D07 = (ToggleButton)findViewById(R.id.tbtn_D07);
    	tbtn_D08 = (ToggleButton)findViewById(R.id.tbtn_D08);
    	tbtn_D09 = (ToggleButton)findViewById(R.id.tbtn_D09);
    	tbtn_D10 = (ToggleButton)findViewById(R.id.tbtn_D10);
    	tbtn_D11 = (ToggleButton)findViewById(R.id.tbtn_D11);
    	tbtn_D12 = (ToggleButton)findViewById(R.id.tbtn_D12);
    	tbtn_D13 = (ToggleButton)findViewById(R.id.tbtn_D13);
    	
    	dPinList[0] = tbtn_D00;
    	dPinList[1] = tbtn_D01;
    	dPinList[2] = tbtn_D02;
    	dPinList[3] = tbtn_D03;
    	dPinList[4] = tbtn_D04;
    	dPinList[5] = tbtn_D05;
    	dPinList[6] = tbtn_D06;
    	dPinList[7] = tbtn_D07;
    	dPinList[8] = tbtn_D08;
    	dPinList[9] = tbtn_D09;
    	dPinList[10] = tbtn_D10;
    	dPinList[11] = tbtn_D11;
    	dPinList[12] = tbtn_D12;
    	dPinList[13] = tbtn_D13;
    	
    	btn_A0.setOnClickListener(this);
    	btn_A1.setOnClickListener(this);
    	btn_A2.setOnClickListener(this);
    	btn_A3.setOnClickListener(this);
    	btn_A4.setOnClickListener(this);
    	btn_A5.setOnClickListener(this);
    	
    	for(int i=0; i < dPinList.length; i++) {
    		dPinList[i].setOnClickListener(this);
    	}
    	
    }
	
}








