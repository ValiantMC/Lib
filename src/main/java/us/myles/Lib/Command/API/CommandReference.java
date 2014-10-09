package us.myles.Lib.Command.API;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandReference {
	public abstract ReferenceType type();
}