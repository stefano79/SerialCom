package com.wordpress.blogste.SerialCom;

import java.io.IOException;
import java.util.Observable;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Classe per la gestione di una porta seriale tramite la libreiria JSSC Gli
 * argomenti del costruttore sono i parametri della porta seriale e devono
 * essere presi dagli enumeratori messi a dispozione dalla classe I dati
 * ricevuti vengono bufferizzati in array di 32768 byte e possono essere letti
 * in qualsiasi momento, ogni evento DATA_AVAILABLE sulla seriale notfica al
 * Observer che sono bufferizzati dei nuovi byte, è possibile settare il tipo di
 * ogetto da restituore all' Observer tramite il metodo setSubjectObserver()
 * altrimenti verrà restitutita una stringa "DATA_AVAILABLE"
 * <P>
 * <b>Example:</b><br>
 * 
 * <pre>
 * {
 * 	&#064;code
 * 	SerialCom serial = new SerialCom(SerialCom.DataRate.DATARATE_9600,
 * 			SerialCom.DataBits.DATABITS_8, SerialCom.StopBits.STOPBITS_1,
 * 			SerialCom.Parity.NONE);
 * 	serial.openPort(&quot;/dev/tty.usbserial-A900cdA5&quot;);
 * 	serial.write(&quot;Hello World!&quot;);
 * }
 * </pre>
 * 
 * @author Stefano Aniballi
 * 
 */
public class Serial extends Observable implements SerialPortEventListener {

	/*
	 * *********** Variabili Pubbliche *************
	 */

	/**
	 * Enumaratore con la lista di baudarate accettati dalla porta seriale
	 * 
	 */
	public static enum DATA_RATE {
		DATARATE_300(SerialPort.BAUDRATE_300), DATARATE_1200(
				SerialPort.BAUDRATE_1200), DATARATE_4800(
				SerialPort.BAUDRATE_4800), DATARATE_9600(
				SerialPort.BAUDRATE_9600), DATARATE_14400(
				SerialPort.BAUDRATE_14400), DATARATE_19200(
				SerialPort.BAUDRATE_19200), DATARATE_38400(
				SerialPort.BAUDRATE_38400), DATARATE_57600(
				SerialPort.BAUDRATE_57600), DATARATE_115200(
				SerialPort.BAUDRATE_115200), DATARATE_256000(
				SerialPort.BAUDRATE_256000);

		private int value;

		private DATA_RATE(int value) {
			this.value = value;
		}
		
		public int getValue(){
			return value;
			
		}

	}

	/**
	 * Enumaratore con la lista di databits accettati dalla porta seriale
	 * 
	 */
	public static enum DATA_BITS {
		DATABITS_5(SerialPort.DATABITS_5), DATABITS_6(SerialPort.DATABITS_6), DATABITS_7(
				SerialPort.DATABITS_7), DATABITS_8(SerialPort.DATABITS_8);
		private int value;

		private DATA_BITS(int value) {
			this.value = value;
		}
	}

	/**
	 * Enumaratore con la lista dei stopbits accettati dalla porta seriale
	 * 
	 */
	public static enum STOP_BITS {
		STOPBITS_1(SerialPort.STOPBITS_1), STOPBITS_1_5(SerialPort.STOPBITS_1_5), STOPBITS_2(
				SerialPort.STOPBITS_2);
		private int value;

		private STOP_BITS(int value) {
			this.value = value;
		}
	}

	/**
	 * Enumaratore con la lista dei parametri per parity accettati dalla porta
	 * seriale
	 * 
	 */
	public static enum PARITY {
		NONE(SerialPort.PARITY_NONE), EVEN(SerialPort.PARITY_EVEN), MARK(
				SerialPort.PARITY_MARK), ODD(SerialPort.PARITY_ODD), SPACE(
				SerialPort.PARITY_SPACE);
		private int value;

		private PARITY(int value) {
			this.value = value;
		}
	}
	
	/**
	 * Enumeratore con la lista degli eventi per l' observer
	 *
	 */
	public  static enum EVENT {
		DATA_AVAILABLE
	};

	/*
	 * *********** Variabili Private *************
	 */

	private SerialPort serialPort;
	private int dataRate;
	private int dataBits;
	private int stopBits;
	private int parity;
	private byte buffer[] = new byte[32768];
	private int bufferIndex;
	private int bufferLast;

	/*
	 * *********** Costruttori *************
	 */

	/**
	 * @param dataRate
	 * @param dataBits
	 * @param stopBits
	 * @param parity
	 */
	public Serial(DATA_RATE dataRate, DATA_BITS dataBits, STOP_BITS stopBits,
			PARITY parity) {
		this.dataRate = dataRate.value;
		this.dataBits = dataBits.value;
		this.stopBits = stopBits.value;
		this.parity = parity.value;

	}

	/*
	 * *********** Metodi Publici *************
	 */

	/**
	 * @return - array di stringhe con i nomi delle porte disponibili
	 */
	public static String[] getListCommPort() {

		return SerialPortList.getPortNames();
	}

	/**
	 * @param nameSerialPort
	 *            - Nome della porta seriale da aprire (Es: "COM1")
	 * @throws SerialPortException
	 */
	public void openPort(String nameSerialPort) throws SerialPortException {
		serialPort = new SerialPort(nameSerialPort);
		serialPort.openPort();
		serialPort.setParams(dataRate, dataBits, stopBits, parity);
		serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
		serialPort.addEventListener(this);
		serialPort.setDTR(true);
	}

	/**
	 * Chiusura della porta seriale
	 * 
	 * @throws IOException
	 * 
	 */
	public synchronized void closePort() throws IOException {

		if (serialPort != null) {
			try {
				if (serialPort.isOpened()) {
					serialPort.closePort();
				}
			} catch (SerialPortException e) {
				throw new IOException(e);
			} finally {
				serialPort = null;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
	 * 
	 * Questo metodo è chiamato ogni volta che ci sono byte sul buffer della
	 * seriale
	 */
	public synchronized void serialEvent(SerialPortEvent serialEvent) {
		if (serialEvent.isRXCHAR()) {
			try {
				byte[] buf = serialPort.readBytes(serialEvent.getEventValue());
				if (buf.length > 0) {
					if (bufferLast == buffer.length) {
						byte temp[] = new byte[bufferLast << 1];
						System.arraycopy(buffer, 0, temp, 0, bufferLast);
						buffer = temp;
					}
					System.arraycopy(buf, 0, buffer, bufferLast, buf.length);
					bufferLast += buf.length;
					setChanged();
					notifyObservers(EVENT.DATA_AVAILABLE);
				}
			} catch (SerialPortException e) {
				System.out.println("serialEvent" + e);
			}

		}
	}

	/**
	 * @return - Numero di byte presenti nel buffer di lettura
	 */
	public synchronized int available() {
		return (bufferLast - bufferIndex);
	}

	/**
	 * Svuota il buffer di lettura
	 */
	public synchronized void clear() {
		bufferLast = 0;
		bufferIndex = 0;
	}

	/**
	 * @return - numero compreso tra 0 e 255 del primo byte del buffer. Return
	 *         -1 se il buffer è vuoto.
	 */
	public synchronized int read() {
		if (bufferIndex == bufferLast)
			return -1;

		int outgoing = buffer[bufferIndex++] & 0xff;
		if (bufferIndex == bufferLast) { // rewind
			bufferIndex = 0;
			bufferLast = 0;
		}
		return outgoing;
	}

	/**
	 * @return - Il primo byte del buffer in formato char. Return -1, o 0xffff
	 *         se il buffer è vuoto.
	 */
	public synchronized char readChar() {
		if (bufferIndex == bufferLast)
			return (char) (-1);
		return (char) read();
	}

	/**
	 * @return - Array di byte di tutto il contenuto del buffer. Meno
	 *         performante rispetto a readBytes(byte[]). Return null se il
	 *         buffer è vuoto.
	 */
	public synchronized byte[] readBytes() {
		if (bufferIndex == bufferLast)
			return null;

		int length = bufferLast - bufferIndex;
		byte outgoing[] = new byte[length];
		System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

		bufferIndex = 0; // rewind
		bufferLast = 0;
		return outgoing;
	}

	/**
	 * @return - Copia il contenuto del buffer nel array di byte passato come
	 *         parametro e returns il numero di byte copiati. Se l' array
	 *         passato come parametro è più piccolo dei byte presenti nel buffer
	 *         i byte in eccesso vengono lasciati nel buffer per la lettura
	 *         successiva.
	 */
	public synchronized int readBytes(byte outgoing[]) {
		if (bufferIndex == bufferLast)
			return 0;

		int length = bufferLast - bufferIndex;
		if (length > outgoing.length)
			length = outgoing.length;
		System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

		bufferIndex += length;
		if (bufferIndex == bufferLast) {
			bufferIndex = 0; // rewind
			bufferLast = 0;
		}
		return length;
	}

	/**
	 * @return - Contenuto del buffer in formato String
	 */
	public synchronized String readString() {
		if (bufferIndex == bufferLast)
			return null;
		return new String(readBytes());
	}

	/**
	 * @param bytes
	 *            - array di bytes da inviare in uscita sulla porta seriale
	 * @throws IOException
	 */
	public void write(byte bytes[]) throws IOException {
		try {
			serialPort.writeBytes(bytes);
		} catch (SerialPortException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @param what
	 *            - Stringa da inviare in uscita sulla porta seriale
	 * @throws IOException
	 */
	public void write(String what) throws IOException {
		write(what.getBytes());
	}

	public int getDataRate() {
		return dataRate;
	}

	public void setDataRate(DATA_RATE dataRate) throws SerialPortException {
		this.dataRate = dataRate.value;
		if (serialPort != null)
			serialPort.setParams(this.dataRate, dataBits, stopBits, parity);
	}

	public int getDataBits() {
		return dataBits;
	}

	public void setDataBits(DATA_BITS dataBits) throws SerialPortException {
		this.dataBits = dataBits.value;
		if (serialPort != null)
			serialPort.setParams(dataRate, this.dataBits, stopBits, parity);
	}

	public int getStopBits() {
		return stopBits;
	}

	public void setStopBits(STOP_BITS stopBits) throws SerialPortException {
		this.stopBits = stopBits.value;
		if (serialPort != null)
			serialPort.setParams(dataRate, dataBits, this.stopBits, parity);
	}

	public int getParity() {
		return parity;
	}

	public void setParity(PARITY parity) throws SerialPortException {
		this.parity = parity.value;
		if (serialPort != null)
			serialPort.setParams(dataRate, dataBits, stopBits, this.parity);
	}

	/**
	 * @return - Stato connessione della porta seriale
	 */
	public boolean isOpened() {
		if (serialPort != null) {
			return serialPort.isOpened();
		} else {
			return false;
		}
	}

}
