package us.myles.Lib.Module.Example;

import us.myles.Lib.Module.ModuleManager;

public class TestRun {
	public static void main(String[] args) {
		ModuleManager<BaseModule> mm = new ModuleManager(BaseModule.class);
		mm.load(ExampleModule.class);
	}
}
