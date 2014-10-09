package us.myles.Lib.Command.Example;

import us.myles.Lib.Command.CommandBuilder;
import us.myles.Lib.Command.Exception.AccessDeniedException;
import us.myles.Lib.Command.Exception.CommandFailedException;
import us.myles.Lib.Command.Interface.ParamAdapter;
import us.myles.Lib.Command.Interface.SecurityHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class TestRun {
	public static void main(String[] args) {
		// Creation
		CommandBuilder cm = new CommandBuilder("test").adapter(
				new ParamAdapter<Spoon>(Spoon.class) {
					@Override
					public Spoon adapt(String arg) {
						return new Spoon(arg);
					}
				}).security(
				new SecurityHandler() {
					@Override
					public void access(Method m, Object userObject) throws AccessDeniedException {
						for (Annotation a : m.getDeclaredAnnotations()) {
							if (a instanceof Op) {
								if (!userObject.equals("Jahovis")) throw new AccessDeniedException("You have to be Jahovis to do this!");
							}
						}
					}
				}).command(new ExampleCommand());
		// Execution
		try {
			cm.execute("Jahovis", "hug", "Squashy");
		} catch (CommandFailedException e) {
			for (String s : e.getMultilineException()) {
				System.out.println(s);
			}
		}
		// Completion
		String input = "hello "; //<- They have hit the tab key.
		String[] data = input.split(" ");
		List<String> s = cm.complete("Jahovis", data);
		System.out.println("Suggestions:");
		for (String l : s) {
			System.out.println(l);
		}
	}
}
