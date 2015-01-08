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
		log.info("start encoding with AES");

		 byte[] retMessageEncyrypted = null;
		 try {
			 
			 log.info("doFinal - encrypt String: " + command);
			 retMessageEncyrypted = cipherAESencode.doFinal(command.getBytes());
			 log.info( "after encoding aes doFinal" + retMessageEncyrypted);	
				
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			 log.info("base encode encoded message");	
			 byte[] final3rdMessageEncryptedBase64 = Base64.encode(retMessageEncyrypted);
			 log.info("base encoded" + final3rdMessageEncryptedBase64);
				
			log.info("create string");
			String retString = null;
			try {
				retString = new String(final3rdMessageEncryptedBase64,"UTF-8");
			} catch (UnsupportedEncodingException e) {	
				e.printStackTrace();
			}
			log.info("created string: " + retString);
			
			return retString;
			
	}
	

	
	private String decodeAES(String command){
		log.info("start deoding with AES");

		 byte[] retMessageEncyrypted = null;
		 try {
			 log.info("doFinal - encrypt String: " + command);
			 retMessageEncyrypted = cipherAESdecode.doFinal(Base64.decode(command.getBytes()));
			  log.info( "after decoding aes doFinal " + retMessageEncyrypted);	
				
				
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
				
			// byte[] final3rdMessageEncryptedBase64 = Base64.encode(retMessageEncyrypted);
				//log.info( "before send 3");
				

		    log.info("create string");
			String retString = null;
			try {
				retString = new String(retMessageEncyrypted,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				
				e.printStackTrace();
			}
			log.info("created string: " + retString);
			
			return retString;
	}

	@Override
	@Command
	public String logout() throws IOException {
		
		if(!isAuthenticated())
			return this.isNotAuthenticated;
		
		  String logout = "!logout";
		
		  /********************
		   * Send Command !logout
		   *******************/

		  String test = this.encodeAES(logout);
		  log.info("#################### Encoded" + test);
		  
		
		  
		 String test2 =  this.decodeAES(test);
		 log.info("Test sended String: Decoded" + test2);
		  
		 writer.println(test);
		  

		  /********************
		   * Wait for Answer
		   *******************/
		 
		 log.info("wait for reading Line");
		String read = reader.readLine();
		 log.info("## recveived String: ## " + read) ;
		String ret = decodeAES(read);
		 
		log.info("returned value to print decoded: " +ret);
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

	@Override
	@Command
	public String auth(String username) throws IOException {
		
		//check if clientis already authenticated 
		if(isAuthenticated())
			return this.alreadyAuthenticated;
		

		PrivateKey privateKeyUser = null;
		PublicKey publicKeyController = null;
		byte[] finalByteMessageEncryptedBase64 = null;
		
		byte[] base64Message =null;
		
		File file = new File(config.getString("keys.dir")+"/"+username+".pem");
		
		//check if file exists - private user key
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// encode final encrypted message into Base64 format
			finalByteMessageEncryptedBase64 = Base64.encode(finalByteMessageEncrypted);
		
		}
		else{
			return "Error | No Key Available for User " + username;
		}
		
		System.out.println("send message1");
		//send to Controller as a String 1st
		writer.println(new String(finalByteMessageEncryptedBase64,"UTF-8"));
		
		
		
		//wait for answer controller 2nd
		String  message2returned = reader.readLine();
		
		System.out.println("message 2 received");
		
		//2nd message received -> read parameters and send 3rd message back
		byte[] message2returnedBytesBase64 = message2returned.getBytes();
		System.out.println("returned message "+ message2returnedBytesBase64);
		//base64 -> encrypted message
		byte[] byteReceivedInputEncrypted = Base64.decode(message2returnedBytesBase64);

		System.out.println("returned message "+ byteReceivedInputEncrypted);
		
		// prepare cipher RSA
		Cipher cipher2 = null;
		byte[] finalByteMessageDecrypted = null;
		
		//encrypt the 2nd message
		 try {
			cipher2 = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher2.init(Cipher.DECRYPT_MODE, privateKeyUser);
			System.out.println("try to decode");
			finalByteMessageDecrypted = cipher2.doFinal(byteReceivedInputEncrypted);
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 System.out.println("try to split");
		 //split command !ok and all the others
		 String[] message2ndparameter = new String(finalByteMessageDecrypted).split(" ");
		 
		 log.info("how many message2ndparameter " + message2ndparameter.length);
		 log.info("1 mess "+message2ndparameter[0] + " 2 challange client "+ message2ndparameter[1]  + " 3 contr "+
		                   message2ndparameter[2] + " 4 aes " + message2ndparameter[3] + " 5 iv "+ message2ndparameter[4] );
		 
		 //client challenge string to base = equals
		 log.info("try to check client challenge");
		
		 if(Arrays.equals(message2ndparameter[1].getBytes(),base64Message)){
				log.info("Client Challenge received and Accepted - Securtiy Channel establisehd");
			}
			
			 
		 
		 //controller challenge 
		 String controllerChallange = message2ndparameter[2];	
		 

		 //IV Parameter
		 log.info("get iv to bytes");
		 this.ivParameter = Base64.decode(message2ndparameter[4].getBytes());
		 
		 //AES Key
		 log.info("keyAes byte");
		 byte[] keyAESBytes = message2ndparameter[3].getBytes();
		 log.info("make secret key");
		 this.aesSecretKEy = new SecretKeySpec(Base64.decode(keyAESBytes), 0, Base64.decode(keyAESBytes).length, "AES");
		 
		 
		 //send 3rd message
		
		byte[] final3rdMessageEncrypted = null;
		
		log.info("try to make 3rd message enxy");
		try {
			this.cipherAESencode = Cipher.getInstance("AES/CTR/NoPadding");
			this.cipherAESdecode = cipherAESencode;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			//
			e1.printStackTrace();
		}
		log.info("after get instance");
		 // MODE is the encryption/decryption mode
		 // KEY is either a private, public or secret key
		 // IV is an init vector, needed for AES
		
		log.info("controller challlang before encry base64"+ controllerChallange.getBytes() + "  "+  controllerChallange);
		 IvParameterSpec ivspec = new IvParameterSpec(this.ivParameter);
		 log.info( "try to use iv");
		 try {
			 
			 cipherAESencode.init(Cipher.ENCRYPT_MODE,this.aesSecretKEy,ivspec);
			 cipherAESdecode.init(Cipher.DECRYPT_MODE,this.aesSecretKEy,ivspec);
			log.info( "after init aes cipher");
			final3rdMessageEncrypted = cipherAESencode.doFinal(controllerChallange.getBytes());
			log.info( "after init aes doFinal");	
			
			
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			log.info("before base54");
			
		 byte[] final3rdMessageEncryptedBase64 = Base64.encode(final3rdMessageEncrypted);
			log.info( "before send 3");
			
		//send to Controller as a String 1st
			log.info("sending message3 bytee" + final3rdMessageEncrypted);
				
			log.info("sending message3 base" + final3rdMessageEncryptedBase64);
		writer.println(new String(final3rdMessageEncryptedBase64,"UTF-8")); 
		
		//return reader.readLine();
		log.info("return auth");
		return "Authentication Successfull";	
		
	}

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
