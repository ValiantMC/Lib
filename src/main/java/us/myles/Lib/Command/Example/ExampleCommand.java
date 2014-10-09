package us.myles.Lib.Command.Example;

import us.myles.Lib.Command.API.Command;
import us.myles.Lib.Command.API.CommandReference;
import us.myles.Lib.Command.API.ReferenceType;

public class ExampleCommand implements Command {
	public void hello(@CommandReference(type = ReferenceType.SELF) String player, Color colour, Integer i) {
		System.out.println("Saying hello to " + player + " Favourite Colour is " + colour.name() + " Fav number: " + i);
	}

	public void spoon(Spoon s) {
		System.out.println("Spoons! " + s.getFlavour());
	}
	@Op
	public void hug(Boolean person){
		System.out.println("Hugs " + person);
	}
}
