package us.myles.Lib.Command.Exception;

public class InvalidArgumentException extends RuntimeException {
	private final String argument;
	private final String reason;

	public InvalidArgumentException(String argument, String reason) {
		this.argument = argument;
		this.reason = reason;
	}

	public String getArgument() {
		return this.argument;
	}

	public String getReason() {
		return this.reason;
	}
}
