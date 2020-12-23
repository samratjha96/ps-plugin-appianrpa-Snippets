package com.appian.rpa.snippet.ibm3270;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.appian.rpa.snippet.ibm3270.clients.PCOMMEmulatorCommons;
import com.appian.rpa.snippet.ibm3270.clients.PartenonEmulatorCommons;
import com.appian.rpa.snippet.ibm3270.clients.WC3270EmulatorCommons;
import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.IWaitFor;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.exceptions.JidokaException;
import com.novayre.jidoka.client.api.exceptions.JidokaFatalException;
import com.novayre.jidoka.client.api.exceptions.JidokaUnsatisfiedConditionException;
import com.novayre.jidoka.windows.api.EShowWindowState;
import com.novayre.jidoka.windows.api.IWindows;
import com.novayre.jidoka.windows.api.WindowInfo;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * Class for opening and closing the 3270 Terminal
 */
public class IBM3270AppManager {

	/** Pause in seconds */
	public static final int LONG_WAIT_SECONDS = 30;
	/** Default pause in milliseconds */
	public static final long WAIT_MILLISECONDS = 500;
	/** Server instance */
	private IJidokaServer<Serializable> server;
	/** Windows module instance */
	private IWindows windows;
	/** Waitfor module instance */
	private IWaitFor waitFor;

	IBM3270Commons ibm3270Commons;

	/**
	 * Open the 3270 terminal, a symbolic link is used to open it
	 * 
	 * @param processName
	 * @param titleExpected
	 * @throws JidokaException
	 */
	public static IBM3270AppManager openIBM3270(String path, String windowsTitle, IRobot robot, String emulType) {
		IBM3270AppManager instance = new IBM3270AppManager(path, windowsTitle, robot, emulType);
		return instance;
	}
	
	private IBM3270AppManager(String path, String windowsTitle, IRobot robot, String emulType) {
		if (emulType.toUpperCase().equals("WC3270")) {
			ibm3270Commons = new WC3270EmulatorCommons(robot, windowsTitle);
		} else if (emulType.toUpperCase().equals("PCOMM")){
			ibm3270Commons = new PCOMMEmulatorCommons(robot, windowsTitle);
		} else if (emulType.toUpperCase().equals("PARTENON")){
			ibm3270Commons = new PartenonEmulatorCommons(robot, windowsTitle);
		}else {
			throw new JidokaFatalException("Only [WC3270] , [PCOMM] or [PARTENON] emulTypes are valid");
		}
		this.server = (IJidokaServer<Serializable>) JidokaFactory.getServer();
		this.windows = IWindows.getInstance(robot);
		this.waitFor = windows.getWaitFor(robot);

		server.debug(String.format("Launching [%s] with title [%s]", path, windowsTitle));

		String[] cmdArray = new String[3];
		cmdArray[0] = "cmd";
		cmdArray[1] = "/C";
		cmdArray[2] = path;
		try {
			Runtime.getRuntime().exec(cmdArray);
		} catch (IOException e1) {
			throw new JidokaFatalException(String.format("Unable to open 3270 Terminal with path [%s] and Windows title [%s]",path, windows), e1);
		}

		AtomicReference<HWND> ieAtomic = new AtomicReference<>();
		try {
			waitFor.wait(LONG_WAIT_SECONDS, String.format("Waiting for the application [%s] to open", windowsTitle), false, () -> {
				WindowInfo window = windows.getWindow(windowsTitle);
				if (window != null) {
					ieAtomic.set(window.gethWnd());
					windows.showWindow(window.gethWnd(), EShowWindowState.SW_MAXIMIZE);
					windows.pause(WAIT_MILLISECONDS);
					return true;
				}
				return false;
			});
		} catch (JidokaUnsatisfiedConditionException e) {
			throw new JidokaFatalException(String.format("Unable to open 3270 Terminal with path [%s] and Windows title [%s]",path, windowsTitle));
		}
	}

	public void write(String label, int offsetX, int offsetY, String text, int retries) {
		ibm3270Commons.moveToCoordinates(label, offsetX, offsetY, retries);
		ibm3270Commons.write(text);
	}

	public void write( int X, int Y, String text) {
		ibm3270Commons.moveToCoordinates(X, Y);
		ibm3270Commons.write(text);
	}
	
	public void control() {
		ibm3270Commons.control();
	}

	public void enter() {
		ibm3270Commons.enter();
	}

	public String getStringLine(int line, int startX, int endX) {
		//TODO To implement
		return null;
	}

	
	public List<String> getString(int startX, int startY, int endX, int endY) {
		//TODO TO implement
		return null;
	}

	
	
	/**
	 * Close 3270 terminal.
	 * @throws IOException 
	 */
	public void close() throws IOException {
		if (ibm3270Commons!=null) {
			ibm3270Commons.close();
		}
	}

}
