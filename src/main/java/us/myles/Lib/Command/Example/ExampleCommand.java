package us.myles.Lib.Command.Example;

import us.myles.Lib.Command.Interface.Command;
import us.myles.Lib.Command.CommandReference;
import us.myles.Lib.Command.ReferenceType;

public class ExampleCommand implements Command {
	public void hello(@CommandReference(type = ReferenceType.SELF) String player, Color colour, Integer i) {
		System.out.println("Saying hello to " + player + " Favourite Colour is " + colour.name() + " Fav number: " + i);
	}

	public void spoon(Spoon s) {
		System.out.println("Spoons! " + s.getFlavour());
	}
	@Op
	public void hug(String person){
		System.out.println("Hugs " + person);
	}
}
