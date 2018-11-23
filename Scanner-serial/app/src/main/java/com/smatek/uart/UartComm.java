package com.smatek.uart;

public class UartComm {
	static {
		System.loadLibrary("SerialAPI");
	}
	 public native int uartInit(String device);
     public native int uartDestroy(int fd);
     public native int setOpt(int fd, int baud, int dataBits, int parity, int stopBits);
     public native int send(int fd, int[] val, int length);
     public native int recv(int fd, int[] val, int length);
     public native int setRS485Read(int read);
}
