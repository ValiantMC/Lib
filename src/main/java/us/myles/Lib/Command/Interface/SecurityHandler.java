package us.myles.Lib.Command.Interface;

import us.myles.Lib.Command.Exception.AccessDeniedException;

import java.lang.reflect.Method;

public interface SecurityHandler {
	public void access(Method m, Object userObject) throws AccessDeniedException;
}
