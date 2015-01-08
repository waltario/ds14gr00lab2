package controller.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

import client.Client;
import util.Keys;
import controller.helpers.NodeHelper;
import controller.helpers.StatsCollector;

public class ClientListener implements Runnable, Closeable {

	private static Log log = LogFactory.getLog(ClientListener.class);
	
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintStream writer = null;
	private boolean stopped;
	private StatsCollector collector = null;
	private String uname = null;
	
	//public secret crypto data
	private PublicKey publicKeyClient = null;
	private byte[] initV =null;
	private SecretKey secretKeyAES = null;
	private Cipher cipherAESencode = null;
	private Cipher cipherAESdecode = null;
	
	public ClientListener(Socket socket) throws IOException {

		
		this.socket = socket;
		this.stopped = false;
		collector = StatsCollector.getInstance();

		initIO();
	}

	private void initIO() throws IOException {

		reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		writer = new PrintStream(socket.getOutputStream(), true);
	}

	@Override
	public void run() {

		String input = null;
		int exitCount = 0;

		while (!stopped) {

			try {

				input = reader.readLine();

				if (input == null)
					break;
				
				String toSend = executeCommand(input);
				if(!toSend.equals("Client Accepted"))
					writer.println(toSend);
				else{
					System.out.println(toSend + this.uname);
				}
				
			} catch (IOException e) {

				System.err.println("Error while reading from InputStream!");
				exitCount++;
				if (exitCount == 3)
					stopped = true;
			}
		}
	}

	@Override
	public void close() {

		stopped = true;

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
	}

	private String executeCommand(String input) {

		
		
	if(this.uname == null){
		//user not logged in -> check only !authenticate and !login
		//use RSA 
		
	
		byte[] inputBytesBase64 = input.getBytes();
		
		
		//base64 -> encrypted message
		byte[] byteReceivedInputEncrypted = Base64.decode(inputBytesBase64);
		
		//get controller private key
		PrivateKey privateKeyController =collector.getPrivateKey();
				
				
		// prepare cipher RSA
		Cipher cipher = null;
		byte[] finalByteMessageDecrypted = null;
				
		
		try {
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, privateKeyController);
			
			finalByteMessageDecrypted = cipher.doFinal(byteReceivedInputEncrypted);
								
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
					
				e.printStackTrace();
		} catch (InvalidKeyException e) {
					
				e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
					
				e.printStackTrace();
		} catch (BadPaddingException e) {
					
			e.printStackTrace();
		}
				
		
				 //split command
		String[] command = null;
		
		try {
				//split incoming command 
				command = new String(finalByteMessageDecrypted,"UTF-8").split(" ");
					
		} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
		}
		
		
		switch (command[0]) {

		case "!login":
			return loginCommand(command[1], command[2]);
			
		case "!credits":
			return "You have " + collector.getCredits(uname) + " credits left.";
			
		case "!buy":
			return buyCommand(command[1]);
			
		case "!list":
			return encodeAES(collector.listOperators());
			
		case "!compute":
			return computeCommand(command);
			
		case "!logout":
			collector.logout(uname);
			this.uname = null;
			return encodeAES("Succesfully logged out!");
			
		case "!authenticate":	
			return authCommand(command[1],command[2].getBytes()); 
		}

		return "CLIENT SAID: " + input;
	}	
	else{
		//user  logged in -> check everything apart !authenticate
		//use AES
			
		String decodedRequest = decodeAES(input);
		String[] command = null;		
				
		command = decodedRequest.split(" ");

		switch (command[0]) {

		case "!login":
			return loginCommand(command[1], command[2]);
			
		case "!credits":
			return encodeAES("You have " + collector.getCredits(uname) + " credits left.");
			
		case "!buy":
			return encodeAES(buyCommand(command[1]));
			
		case "!list":
			return encodeAES(collector.listOperators());
			
		case "!compute":
			return encodeAES(computeCommand(command));
			
		case "!logout":
			collector.logout(uname);
			this.uname = null;
			return encodeAES("Succesfully logged out");
			
		case "!authenticate":	
			return authCommand(command[1],command[2].getBytes()); 
		}
		
		return "CLIENT SAID: " + input;	
	}
	
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
	
	
	//receive name to auth and clientchallange base64 encrypted
	private String authCommand(String clientName, byte[] clientChallenge){

	boolean loggedIn = collector.isLoggedIn(clientName);
	
	/*
	if (!loggedIn) {
		log.info("WE ARE NOT LOGG IN GO ON !!!!" + loggedIn);
	}*/
	
		//read public key of client with name 	
		try {
			this.publicKeyClient = Keys.readPublicPEM(new File(collector.getPublicKeyPathClient()+"/"+clientName+".pub.pem"));
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
	
		String authReturnMessage = "!ok ";
		String whiteSpace =" ";
		byte[] whiteSpaceByte = whiteSpace.getBytes();
		
		//concat return Message and Client Challange	
		//authReturnMessage = authReturnMessage + clientChallenge + " ";	
		byte[] authReturnMessageBytes = authReturnMessage.getBytes();
		
		
		byte[] finalByteMessage0 = new byte[authReturnMessageBytes.length + clientChallenge.length];
		//					byte 1, start byte 1  ,deszination, start dest, end byte 1
		System.arraycopy(authReturnMessageBytes, 0, finalByteMessage0, 0, authReturnMessageBytes.length);
		System.arraycopy(clientChallenge, 0, finalByteMessage0, authReturnMessageBytes.length, clientChallenge.length);
		
		//concat 2 byte arrays finalByteMessagee + whiteSpace
				byte[] finalByteMessage01 = new byte[finalByteMessage0.length + whiteSpaceByte.length];
				System.arraycopy(finalByteMessage0, 0, finalByteMessage01, 0, finalByteMessage0.length);
				System.arraycopy(whiteSpaceByte, 0, finalByteMessage01, finalByteMessage0.length, whiteSpaceByte.length);
				
		
		
		//generates a 32 byte secure random number controllerChallange
		SecureRandom secureRandom = new SecureRandom();
		byte[] controllerChallange = new byte[32];
		secureRandom.nextBytes(controllerChallange);
		
		
		
		// encode client challenge into Base64 format
		byte[] controllerChallangebase64 = Base64.encode(controllerChallange);
		
		//authReturnMessage = authReturnMessage + controllerChallangebase64 + " ";
		
		
		//concat 2 byte arrays authReturnMessage + client chaööange + controllerChallange
		byte[] finalByteMessage = new byte[finalByteMessage01.length + controllerChallangebase64.length];
		//					byte 1, start byte 1  ,deszination, start dest, end byte 1
		System.arraycopy(finalByteMessage01, 0, finalByteMessage, 0, finalByteMessage01.length);
		System.arraycopy(controllerChallangebase64, 0, finalByteMessage, finalByteMessage01	.length, controllerChallangebase64.length);
		
		//concat 2 byte arrays finalByteMessagee + whiteSpace
		byte[] finalByteMessage2 = new byte[finalByteMessage.length + whiteSpaceByte.length];
		System.arraycopy(finalByteMessage, 0, finalByteMessage2, 0, finalByteMessage.length);
		System.arraycopy(whiteSpaceByte, 0, finalByteMessage2, finalByteMessage.length, whiteSpaceByte.length);
		
		
		
		
		//generates a 16byte secure random number initVector
		SecureRandom secureRandomIV = new SecureRandom();
		 byte[] initVector = new byte[16];
		secureRandomIV.nextBytes(initVector);
		this.initV = initVector;
		// encode client challenge into Base64 format
		byte[] initVectorBase64 = Base64.encode(initVector);
		
		
		
		//generate AES Key 256bit
		KeyGenerator generator = null;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		// KEYSIZE is in bits
		generator.init(256);
		SecretKey keyAES = generator.generateKey();
		this.secretKeyAES = keyAES;
		byte[] keyAESBytes = keyAES.getEncoded();
		// encode AES Key into Base64 format
		byte[] keyAESBase64 = Base64.encode(keyAESBytes);
		

		//authReturnMessage = authReturnMessage + keyAESBase64 + " " + initVectorBase64;
		
		
		//concat 2 byte arrays authReturnMessage + client +  controllerChallange + AES 
		byte[] finalByteMessage3 = new byte[finalByteMessage2.length + keyAESBase64.length];
		System.arraycopy(finalByteMessage2, 0, finalByteMessage3, 0, finalByteMessage2.length);
		System.arraycopy(keyAESBase64, 0, finalByteMessage3, finalByteMessage2.length, keyAESBase64.length);
		
		//concat 2 byte arrays finalByteMessagee + whiteSpace	
		byte[] finalByteMessage4 = new byte[finalByteMessage3.length + whiteSpaceByte.length];
		System.arraycopy(finalByteMessage3, 0, finalByteMessage4, 0, finalByteMessage3.length);
		System.arraycopy(whiteSpaceByte, 0, finalByteMessage4, finalByteMessage3.length, whiteSpaceByte.length);
				
		//concat 2 byte arrays authReturnMessage + client + controllerChallange + AES + IV 
		byte[] finalByteMessage5 = new byte[finalByteMessage4.length + initVectorBase64.length];
		System.arraycopy(finalByteMessage4, 0, finalByteMessage5, 0, finalByteMessage4.length);
		System.arraycopy(initVectorBase64, 0, finalByteMessage5, finalByteMessage4.length, initVectorBase64.length);
		
		
		//byte[] finalByteMessage5  = authReturnMessage.getBytes();
		// prepare cipher RSA
		Cipher cipher = null;
		byte[] finalByteMessageEncrypted = null;
		
		try {
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE,this.publicKeyClient);
			finalByteMessageEncrypted = cipher.doFinal(finalByteMessage5);
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
		byte[] finalByteMessageEncryptedBase64 = Base64.encode(finalByteMessageEncrypted);
		
		String message2 = null;
		try {
			message2 = new String(finalByteMessageEncryptedBase64,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
		}
	
		
		try {
			writer.println( new String(finalByteMessageEncryptedBase64,"UTF-8"));
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		}
		
		//wait for last confirmation message 3
		String input3rdMessage =null;
		try {
			input3rdMessage = reader.readLine();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		byte[] input3rdMessageBytes = input3rdMessage.getBytes();
		
		 //received 3rd message
		
		byte[] final3rdMessageDecrypted = null;
		
		try {
			cipherAESdecode = Cipher.getInstance("AES/CTR/NoPadding");
			cipherAESencode = Cipher.getInstance("AES/CTR/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			//
			e1.printStackTrace();
		}
		 // MODE is the encryption/decryption mode
		 // KEY is either a private, public or secret key
		 // IV is an init vector, needed for AES
		 IvParameterSpec ivspec = new IvParameterSpec(this.initV);
		 try {
			cipherAESdecode.init(Cipher.DECRYPT_MODE,this.secretKeyAES,ivspec);
			cipherAESencode.init(Cipher.ENCRYPT_MODE,this.secretKeyAES,ivspec);
			
			final3rdMessageDecrypted = cipherAESdecode.doFinal(Base64.decode(input3rdMessageBytes));
			
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		 
		if(Arrays.equals(controllerChallangebase64,final3rdMessageDecrypted)){
			System.out.println("Controller Challenge Accepted");
			
			collector.setOnline(clientName);
			this.uname = clientName;
		}
			
		return "Client Accepted";
} 

	
	
	private String loginCommand(String uname, String pass) {

		boolean loggedIn = collector.isLoggedIn(uname);

		if (!loggedIn) {

			boolean match = collector.login(uname, pass);

			if (match) {

				this.uname = uname;
				return "Successfully logged in.";
			}

			return "Wrong username or password.";
		}

		if (this.uname == null)
			this.uname = uname;

		return "You are already logged in!";
	}

	private String buyCommand(String arg) {

		try {

			long credits = Long.valueOf(arg);
			return "You now have " + collector.buyCredits(uname, credits)
					+ " credits.";
		} catch (NumberFormatException e) {

			return "Not well formed command!";
		}
	}

	private String computeCommand(String[] comp) {

		int first;
		int second;
		Character op;
		String rsltTmp = null;

		if (comp.length % 2 != 0)
			return "Wrong number of operations!";

		if (!collector.canCompute(uname, (comp.length / 2) - 1))
			return "Not enough credits!";

		try {

			first = Integer.valueOf(comp[1]);
			int i = 2;

			while (i < comp.length) {

				op = comp[i++].charAt(0);
				second = Integer.valueOf(comp[i++]);

				rsltTmp = compute(first, second, op);

				try {

					first = Integer.valueOf(rsltTmp);

				} catch (NumberFormatException e) {

					return rsltTmp;
				}
			}

		} catch (NumberFormatException e) {

			return "Not well formed operations!";
		} catch (IOException e) {

			return "Node not reachable!";
		}

		return String.valueOf(first);
	}

	private String compute(int first, int second, char op) throws IOException {

		String result = null;

		NodeHelper node = collector.getLowestNodeFor(op);

		if (node == null)
			return "Not supported operation!";

		Socket socket = new Socket(node.getAddress(), node.getPort());
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		PrintStream writer = new PrintStream(socket.getOutputStream(), true);

		writer.println("!compute " + first + " " + op + " " + second);
		result = reader.readLine();

		if (writer != null)
			writer.close();

		if (reader != null)
			reader.close();

		if (socket != null)
			socket.close();

		collector.arrangeCredits(node.getPort(), uname, result);

		return result;
	}
}
