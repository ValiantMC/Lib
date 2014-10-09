package us.myles.Lib.Command.Example;

import us.myles.Lib.Command.CommandBuilder;
import us.myles.Lib.Command.Exception.CommandFailedException;
import us.myles.Lib.Command.ParamAdapter;

public class TestRun {
	public static void main(String[] args) {
		CommandBuilder cm = new CommandBuilder("test");
		cm.addAdapter(new ParamAdapter<Spoon>(Spoon.class) {
			@Override
			public Spoon adapt(String arg) {
				return new Spoon(arg);
			}
		});
		cm.setCommand(new ExampleCommand());
		try {
			cm.execute("Jahovis", "hello orange 5");
		} catch (CommandFailedException e) {
			for (String s : e.getMultilineException()) {
				System.out.println(s);
			}
		}
		//	cm.execute("Jahovis", "spoon hi");
	}
}
