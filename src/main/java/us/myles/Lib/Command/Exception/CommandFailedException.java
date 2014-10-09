package us.myles.Lib.Command.Exception;

import java.util.List;

public class CommandFailedException extends Exception {
	private final String[] mlc;

	public CommandFailedException(String[] ex) {
		this.mlc = ex;
	}

	public CommandFailedException(List<String> ex) {
		this.mlc = ex.toArray(new String[0]);
	}

	public String[] getMultilineException() {
		return this.mlc;
	}
}