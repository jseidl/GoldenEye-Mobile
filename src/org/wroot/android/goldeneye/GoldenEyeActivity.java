package org.wroot.android.goldeneye;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wroot.android.goldeneye.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GoldenEyeActivity extends Activity {
	
	static final int GET_CODE = 0;
	
	private OnClickListener smashClickListener = new OnClickListener() {
		public void onClick(View v) {
			attack();
		}//end :: onClick
	};// end :: OnClickListener
	
	
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Bind SMASH Button Activity
        Button smashButton = (Button) findViewById(R.id.attack_victim_btn);
        smashButton.setOnClickListener(smashClickListener);
        
    }
    
    /** Main Attack Routine **/
    public void attack() {
    	
    	Context context = getApplicationContext();
    	
    	Toast.makeText(this, R.string.toast_locating_target, Toast.LENGTH_SHORT).show();
    	
    	// If host and nr threads is ok
    	if (this.validateInputFields()) {
    		
    		// Check if host is up
    		if (!this.pingVictimUrl()) {
    			
    			int textResource = R.string.errmsg_cannot_ping_victim;
            	int duration = Toast.LENGTH_SHORT;

            	Toast toast = Toast.makeText(context, textResource, duration);
            	toast.show();
    			
    		} else {
    			
    			Toast.makeText(this, R.string.toast_target_acquired, Toast.LENGTH_SHORT).show();
    			
    			Intent intent = new Intent();
    		    intent.setClass(this,AttackActivity.class);    		    
    		    
    	    	EditText txtVictimHost = (EditText)findViewById(R.id.victim_hostname);
    	    	EditText txtNrThreads = (EditText)findViewById(R.id.attack_threads_nr);
    	    	EditText txtNrConn = (EditText)findViewById(R.id.attack_conn_nr);
    	    	EditText txtNrSock = (EditText)findViewById(R.id.attack_sockets_nr);
    	    	
    	    	String victimHost = txtVictimHost.getText().toString();
    	    	int nrThreads = Integer.parseInt(txtNrThreads.getText().toString());
    	    	int nrConn = Integer.parseInt(txtNrConn.getText().toString());
    	    	int nrSock = Integer.parseInt(txtNrSock.getText().toString());
    	    	
    		    intent.putExtra("victimHost", victimHost);
    		    intent.putExtra("nrThreads", nrThreads);
    		    intent.putExtra("nrConn", nrConn);
    		    intent.putExtra("nrSock", nrSock);
    		    
    		    startActivityForResult(intent,GET_CODE);
    		    
    		}//end :: if
        	
    	}//end :: if
    	
    }//end :: attack

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	  super.onActivityResult(requestCode, resultCode, data);

    	  if (requestCode == GET_CODE){
    	   if (resultCode == RESULT_OK) {
    		   Toast.makeText(this, R.string.toast_attack_finished, Toast.LENGTH_SHORT).show();
    	   }
    	   else{
    		   Toast.makeText(this, R.string.toast_attack_cancelled, Toast.LENGTH_SHORT).show();
    	   }
    	  }
    	}
    
    private boolean validateInputFields() {
    	EditText txtVictimHost = (EditText)findViewById(R.id.victim_hostname);
    	EditText txtNrThreads = (EditText)findViewById(R.id.attack_threads_nr);
    	
    	String victimHost = txtVictimHost.getText().toString();
    	int nrThreads = Integer.parseInt(txtNrThreads.getText().toString());
    	
    	// Validate URL
    	if (!URLUtil.isValidUrl(victimHost)) {
    	  Toast.makeText(this, R.string.errmsg_invalid_host, Toast.LENGTH_SHORT).show();
    	  return false;
    	} 
    	
    	if (nrThreads < 0 || nrThreads > 128) {
    		Toast.makeText(this, R.string.errmsg_invalid_nr_threads, Toast.LENGTH_SHORT).show();
    		return false;
    	}
    	
    	return true;
    	
    }//end validateInputFields
    
    private boolean pingVictimUrl() {
    	
    	EditText txtVictimHost = (EditText)findViewById(R.id.victim_hostname);
    	String victimHost = txtVictimHost.getText().toString();
    	
    	try {
    		HttpHead httpHead = new HttpHead(victimHost);
    		DefaultHttpClient httpclient = new DefaultHttpClient();
    	
    		// Execute HTTP Get Request
    		HttpResponse response = httpclient.execute(httpHead);
    		int status = response.getStatusLine().getStatusCode();
    		
    		// If page accepts HEAD request
    		if (status == 200) {
    			return true;
    		} else {
    			return false;
    		}//end :: if
    		
    	} catch (Exception e) {
    		return false;
    	}//end :: try/catch
    	
    }//end :: pingVictimUrl
}