package us.myles.Lib.Command;

public class Optional<T> {
	T value;

	public Optional(T t) {
		this.value = t;
	}

	public boolean isPresent() {
		return this.value != null;
	}

	public T get() {
		return this.value;
	}
}