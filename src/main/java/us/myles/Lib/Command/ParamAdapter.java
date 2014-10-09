package us.myles.Lib.Command;

import java.util.ArrayList;
import java.util.List;

public abstract class ParamAdapter<T> {
	private final Class<T> type;

	public ParamAdapter(Class<T> type) {
		this.type = type;
	}

	public abstract T adapt(String arg);

	public List<String> examples(Object userObject){
		return new ArrayList<String>();
	}

	public String name() {
		return type.getSimpleName();
	}

	public Class<T> type() {
		return this.type;
	}
}