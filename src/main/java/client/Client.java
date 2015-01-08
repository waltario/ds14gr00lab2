package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

import controller.runnables.ClientListener;
import util.Config;
import util.Keys;
import util.SecurityUtils;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {


	private static Log log = LogFactory.getLog(Client.class);

	
	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private Shell shell = null;
	private ExecutorService executor = null;
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintStream writer = null;
	
	private String isNotAuthenticated = "You are not authenticated";
	private String alreadyAuthenticated = "You are already authenticated";
	
	//crypto data
	private SecretKey aesSecretKEy = null;
	private byte[] ivParameter = null;
	private Cipher cipherAESencode = null;
	private Cipher cipherAESdecode = null;
	
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		initRunnables();
		SecurityUtils.registerBouncyCastle();
	}

	private void initRunnables() {

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		executor = Executors.newFixedThreadPool(1);
	}

	@Override
	public void run() {

		try {

			socket = new Socket(config.getString("controller.host"),
					config.getInt("controller.tcp.port"));
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			writer = new PrintStream(socket.getOutputStream(), true);
			executor.execute(shell);

		} catch (IOException e) {

			System.err.println("Can not connect to server at "
					+ config.getString("controller.host") + ":"
					+ config.getInt("controller.tcp.port"));
		}
	}

	private boolean isAuthenticated(){
		if(this.cipherAESencode != null){
			return true;
		}
		else{
			return false;
		}
	}
	
	@Override
	@Command
	public String login(String username, String password) throws IOException {

		writer.println("!login " + username + " " + password);
		return reader.readLine();
	}
	
	private String encodeAES(String command){

		 byte[] retMessageEncyrypted = null;
		 try {
			 
			 retMessageEncyrypted = cipherAESencode.doFinal(command.getBytes());
					
		} catch (IllegalBlockSizeException | BadPaddingException e) {	
				e.printStackTrace();
		}
				
		byte[] final3rdMessageEncryptedBase64 = Base64.encode(retMessageEncyrypted);
		String retString = null;
		try {
				retString = new String(final3rdMessageEncryptedBase64,"UTF-8");
				
		} catch (UnsupportedEncodingException e) {	
				e.printStackTrace();
		}
		//teest	
			
		return retString;
			
	}
	

	private String decodeAES(String command){
	
		 byte[] retMessageEncyrypted = null;
		 try {
			
			 retMessageEncyrypted = cipherAESdecode.doFinal(Base64.decode(command.getBytes()));
					
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				
				e.printStackTrace();
			}

	   
		String retString = null;
		
		try {
				retString = new String(retMessageEncyrypted,"UTF-8");
		} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
		}
		return retString;
	}

	@Override
	@Command
	public String logout() throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
		
		  String sendCommand = "!logout";
		
		  /********************
		   * Send Command !logout
		   *******************/

		  String test = this.encodeAES(sendCommand);  
		  writer.println(test);
		  
		  /********************
		   * Wait for Answer
		   *******************/
		 
		String read = reader.readLine();		
		String ret = decodeAES(read);
		 
		//clears all keys at client
		this.aesSecretKEy = null;
		this.aesSecretKEy = null;
		ivParameter = null;
		cipherAESencode = null;
		cipherAESdecode = null;
	
		return ret;
		
		
	}

	@Override
	@Command
	public String credits() throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
		
		String sendCommand = "!credits";
		
		writer.println(this.encodeAES(sendCommand));
		return decodeAES(reader.readLine());
	}

	@Override
	@Command
	public String buy(long credits) throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
			
		String sendCommand = "!buy " + credits;
		
		writer.println(this.encodeAES(sendCommand));
		return decodeAES(reader.readLine());
	}

	@Override
	@Command
	public String list() throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
		
		String sendCommand = "!list";
		
		writer.println(this.encodeAES(sendCommand));
		return decodeAES(reader.readLine());
	}

	@Override
	@Command
	public String compute(String term) throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
		
		String sendCommand = "!compute " + term;
		
		writer.println(this.encodeAES(sendCommand));
		return decodeAES(reader.readLine());
	}

	@Override
	@Command
	public String exit() throws IOException {
		
		//writer.println("!exit");
		///String ret = reader.readLine();
		if(isAuthenticated())
			this.logout();
		
		close();
	
		this.aesSecretKEy = null;
		this.aesSecretKEy = null;
		ivParameter = null;
		cipherAESencode = null;
		cipherAESdecode = null;
		
		
		return "Exitting client!";
	}

	private void close() {

		boolean isShutDown = false;

		executor.shutdown();

		try {

			isShutDown = executor.awaitTermination(1, TimeUnit.MILLISECONDS);

		} catch (InterruptedException e) {

		} finally {

			if (!isShutDown)
				executor.shutdownNow();
		}

		if (writer != null)
			writer.close();

		if (reader != null)
			try {
				reader.close();
			} catch (IOException e) {

			}

		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {

			}

		shell.close();
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {

		new Client(args[0], new Config("client"), System.in, System.out).run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---


	@Command
	public String auth(String username) throws IOException {
		
		//if client is already authenticated return
		if(isAuthenticated())
			return this.alreadyAuthenticated;
		

		PrivateKey privateKeyUser = null;
		PublicKey publicKeyController = null;
		byte[] finalByteMessageEncryptedBase64 = null;
		
		byte[] base64Message =null;
		
		File file = new File(config.getString("keys.dir")+"/"+username+".pem");
		
		//check if file exists - private user key - only proceed if exists
		if(file.exists()){
			
			privateKeyUser = Keys.readPrivatePEM(new File(config.getString("keys.dir")+"/"+username+".pem"));
			publicKeyController = Keys.readPublicPEM(new File(config.getString("controller.key")));
	
			
			// generates a 32 byte secure random number
			SecureRandom secureRandom = new SecureRandom();
			byte[] number = new byte[32];
			secureRandom.nextBytes(number);
			
			// encode client challenge into Base64 format
			base64Message = Base64.encode(number);
		
			//strings or bytes
			String authandUsername = "!authenticate " + username + " ";
					
			byte[] authandUsernameBytes = authandUsername.getBytes();
			
			//concat 2 byte arrays
			byte[] finalByteMessage = new byte[authandUsernameBytes.length + base64Message.length];
			System.arraycopy(authandUsernameBytes, 0, finalByteMessage, 0, authandUsernameBytes.length);
			System.arraycopy(base64Message, 0, finalByteMessage, authandUsernameBytes.length, base64Message.length);
			
			
			// prepare cipher RSA
			Cipher cipher = null;
			byte[] finalByteMessageEncrypted = null;
			
			try {
				cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.ENCRYPT_MODE,publicKeyController);
				finalByteMessageEncrypted = cipher.doFinal(finalByteMessage);
			} catch (NoSuchAlgorithmException e) {
				
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				
				e.printStackTrace();
			} catch (BadPaddingException e) {
				
				e.printStackTrace();
			}
			
			// encode final encrypted message into Base64 format
			finalByteMessageEncryptedBase64 = Base64.encode(finalByteMessageEncrypted);
		
		}
		else{
			return "Error | No Key Available for User " + username;
		}
		
		
		//send to Controller as a String 1st
		writer.println(new String(finalByteMessageEncryptedBase64,"UTF-8"));
		

		//wait for answer controller 2nd
		String  message2returned = reader.readLine();
		
		//System.out.println("message 2 received");
		
		//2nd message received -> read parameters and send 3rd message back
		byte[] message2returnedBytesBase64 = message2returned.getBytes();
		
		//base64 -> encrypted message
		byte[] byteReceivedInputEncrypted = Base64.decode(message2returnedBytesBase64);

		//System.out.println("returned message "+ byteReceivedInputEncrypted);
		
		// prepare cipher RSA
		Cipher cipher2 = null;
		byte[] finalByteMessageDecrypted = null;
		
		//encrypt the 2nd message
		 try {
			cipher2 = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher2.init(Cipher.DECRYPT_MODE, privateKeyUser);
			finalByteMessageDecrypted = cipher2.doFinal(byteReceivedInputEncrypted);
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			
			e.printStackTrace();
		} catch (BadPaddingException e) {
			
			e.printStackTrace();
		}
		
		 //split command !ok and all the others
		 String[] message2ndparameter = new String(finalByteMessageDecrypted).split(" ");
		 	 
		
		 if(Arrays.equals(message2ndparameter[1].getBytes(),base64Message)){
				System.out.println("Client Challenge Accepted");
			}
			 
		 //controller challenge 
		 String controllerChallange = message2ndparameter[2];	
		 
		 //IV Parameter
		 this.ivParameter = Base64.decode(message2ndparameter[4].getBytes());
		 
		 //AES Key
		 byte[] keyAESBytes = message2ndparameter[3].getBytes();
		 this.aesSecretKEy = new SecretKeySpec(Base64.decode(keyAESBytes), 0, Base64.decode(keyAESBytes).length, "AES");
		 
		 //send 3rd message
		byte[] final3rdMessageEncrypted = null;
		
		try {
			this.cipherAESencode = Cipher.getInstance("AES/CTR/NoPadding");
			this.cipherAESdecode = cipherAESencode;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			
			e1.printStackTrace();
		}
	
		 // MODE is the encryption/decryption mode
		 // KEY is either a private, public or secret key
		 // IV is an init vector, needed for AES
		 IvParameterSpec ivspec = new IvParameterSpec(this.ivParameter);
		
		 try {
			 
			 cipherAESencode.init(Cipher.ENCRYPT_MODE,this.aesSecretKEy,ivspec);
			 cipherAESdecode.init(Cipher.DECRYPT_MODE,this.aesSecretKEy,ivspec);
			 final3rdMessageEncrypted = cipherAESencode.doFinal(controllerChallange.getBytes());
					
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			
			e.printStackTrace();
		}
		
			
		 byte[] final3rdMessageEncryptedBase64 = Base64.encode(final3rdMessageEncrypted);
				
		//send to Controller as a String 1st
		writer.println(new String(final3rdMessageEncryptedBase64,"UTF-8")); 
		
		return "Authentication Successful";	
		
	}

	@Override
	@Command
	public String authenticate(String username) throws IOException {

		//if client is already authenticated return
		if(isAuthenticated())
			return this.alreadyAuthenticated;
		

		PrivateKey privateKeyUser = null;
		PublicKey publicKeyController = null;
		byte[] finalByteMessageEncryptedBase64 = null;
		
		byte[] base64Message =null;
		
		File file = new File(config.getString("keys.dir")+"/"+username+".pem");
		
		//check if file exists - private user key - only proceed if exists
		if(file.exists()){
			
			privateKeyUser = Keys.readPrivatePEM(new File(config.getString("keys.dir")+"/"+username+".pem"));
			publicKeyController = Keys.readPublicPEM(new File(config.getString("controller.key")));
	
			
			// generates a 32 byte secure random number
			SecureRandom secureRandom = new SecureRandom();
			byte[] number = new byte[32];
			secureRandom.nextBytes(number);
			
			// encode client challenge into Base64 format
			base64Message = Base64.encode(number);
		
			//strings or bytes
			String authandUsername = "!authenticate " + username + " ";
					
			byte[] authandUsernameBytes = authandUsername.getBytes();
			
			//concat 2 byte arrays
			byte[] finalByteMessage = new byte[authandUsernameBytes.length + base64Message.length];
			System.arraycopy(authandUsernameBytes, 0, finalByteMessage, 0, authandUsernameBytes.length);
			System.arraycopy(base64Message, 0, finalByteMessage, authandUsernameBytes.length, base64Message.length);
			
			
			// prepare cipher RSA
			Cipher cipher = null;
			byte[] finalByteMessageEncrypted = null;
			
			try {
				cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.ENCRYPT_MODE,publicKeyController);
				finalByteMessageEncrypted = cipher.doFinal(finalByteMessage);
			} catch (NoSuchAlgorithmException e) {
				
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				
				e.printStackTrace();
			} catch (BadPaddingException e) {
				
				e.printStackTrace();
			}
			
			// encode final encrypted message into Base64 format
			finalByteMessageEncryptedBase64 = Base64.encode(finalByteMessageEncrypted);
		
		}
		else{
			return "Error | No Key Available for User " + username;
		}
		
		
		//send to Controller as a String 1st
		writer.println(new String(finalByteMessageEncryptedBase64,"UTF-8"));
		

		//wait for answer controller 2nd
		String  message2returned = reader.readLine();
		
		//System.out.println("message 2 received");
		
		//2nd message received -> read parameters and send 3rd message back
		byte[] message2returnedBytesBase64 = message2returned.getBytes();
		
		//base64 -> encrypted message
		byte[] byteReceivedInputEncrypted = Base64.decode(message2returnedBytesBase64);

		//System.out.println("returned message "+ byteReceivedInputEncrypted);
		
		// prepare cipher RSA
		Cipher cipher2 = null;
		byte[] finalByteMessageDecrypted = null;
		
		//encrypt the 2nd message
		 try {
			cipher2 = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher2.init(Cipher.DECRYPT_MODE, privateKeyUser);
			finalByteMessageDecrypted = cipher2.doFinal(byteReceivedInputEncrypted);
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			
			e.printStackTrace();
		} catch (BadPaddingException e) {
			
			e.printStackTrace();
		}
		
		 //split command !ok and all the others
		 String[] message2ndparameter = new String(finalByteMessageDecrypted).split(" ");
		 	 
		
		 if(Arrays.equals(message2ndparameter[1].getBytes(),base64Message)){
				System.out.println("Client Challenge Accepted");
			}
			 
		 //controller challenge 
		 String controllerChallange = message2ndparameter[2];	
		 
		 //IV Parameter
		 this.ivParameter = Base64.decode(message2ndparameter[4].getBytes());
		 
		 //AES Key
		 byte[] keyAESBytes = message2ndparameter[3].getBytes();
		 this.aesSecretKEy = new SecretKeySpec(Base64.decode(keyAESBytes), 0, Base64.decode(keyAESBytes).length, "AES");
		 
		 //send 3rd message
		byte[] final3rdMessageEncrypted = null;
		
		try {
			this.cipherAESencode = Cipher.getInstance("AES/CTR/NoPadding");
			this.cipherAESdecode = cipherAESencode;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			
			e1.printStackTrace();
		}
	
		 // MODE is the encryption/decryption mode
		 // KEY is either a private, public or secret key
		 // IV is an init vector, needed for AES
		 IvParameterSpec ivspec = new IvParameterSpec(this.ivParameter);
		
		 try {
			 
			 cipherAESencode.init(Cipher.ENCRYPT_MODE,this.aesSecretKEy,ivspec);
			 cipherAESdecode.init(Cipher.DECRYPT_MODE,this.aesSecretKEy,ivspec);
			 final3rdMessageEncrypted = cipherAESencode.doFinal(controllerChallange.getBytes());
					
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			
			e.printStackTrace();
		}
		
			
		 byte[] final3rdMessageEncryptedBase64 = Base64.encode(final3rdMessageEncrypted);
				
		//send to Controller as a String 1st
		writer.println(new String(final3rdMessageEncryptedBase64,"UTF-8")); 
		
		return "Authentication Successful";
	}

}
