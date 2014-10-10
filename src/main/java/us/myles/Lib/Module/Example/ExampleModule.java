package us.myles.Lib.Module.Example;

public class ExampleModule extends BaseModule{
	@Override
	public void load() {
		System.out.println("Module Loaded");
	}

	@Override
	public void dispose() {
		System.out.println("Module Unloaded");
	}
}
