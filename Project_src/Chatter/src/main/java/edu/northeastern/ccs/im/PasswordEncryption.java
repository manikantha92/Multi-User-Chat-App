package edu.northeastern.ccs.im;

import edu.northeastern.ccs.im.encryption.Encryption;

public class PasswordEncryption implements Encryption{

	@Override
	public byte[] encrypt(String data) {
		
		byte[] byteData = data.getBytes();
		byte[] encrypt = new byte[byteData.length];
		
		for(int k =0; k <byteData.length; k++)
		{
			encrypt[k] = (byte) ((k % 2 == 0) ? byteData[k] + 1 : byteData[k] - 1);
		}
		return encrypt;
	}

	@Override
	public byte[] decrypt(String data) {
		
		byte[] byteData = data.getBytes();
		byte[] decrypt = new byte[byteData.length];
		
		for(int k =0; k <byteData.length; k++)
		{
			decrypt[k] = (byte) ((k % 2 == 0) ? byteData[k] - 1 : byteData[k] + 1);
		}
		return decrypt;
	}

}
