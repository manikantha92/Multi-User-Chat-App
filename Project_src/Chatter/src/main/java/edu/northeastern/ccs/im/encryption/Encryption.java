package edu.northeastern.ccs.im.encryption;

public interface Encryption {

	byte[] encrypt (String data);
	byte[] decrypt (String data);
}
