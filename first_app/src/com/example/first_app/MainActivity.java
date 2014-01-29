package com.example.first_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

	public final static String EXTRA_MSG = "com.example.first_app.url_message";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /* On click of button_send*/
    public void sendMessage (View view)
    {
    	Intent intent = new Intent(this, DisplayMsgActivity.class);
    	EditText edittext = (EditText) findViewById(R.id.enter_url);
    	String url = edittext.getText().toString();
    	intent.putExtra(EXTRA_MSG, url);
    	startActivity(intent);
    }
    
}
