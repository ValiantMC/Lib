package us.myles.Lib.Command;

public abstract class ParamAdapter<T> {
	private final Class<T> type;

	public ParamAdapter(Class<T> type) {
		this.type = type;
	}

	public abstract T adapt(String arg);

	public Class<T> getType() {
		return this.type;
	}
}