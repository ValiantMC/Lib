package us.myles.Lib.Command.Example;

import us.myles.Lib.Command.CommandBuilder;
import us.myles.Lib.Command.Exception.CommandFailedException;
import us.myles.Lib.Command.ParamAdapter;

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
				}).command(new ExampleCommand());
		// Execution
		try {
			cm.execute("Jahovis", "hello", "blue", "5");
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
