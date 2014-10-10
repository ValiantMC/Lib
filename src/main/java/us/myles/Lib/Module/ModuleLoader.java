package us.myles.Lib.Module;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class ModuleLoader extends URLClassLoader{
	private final ModuleManager manager;

	public ModuleLoader(ModuleManager manager, URL[] urls) {
		super(urls);
		this.manager = manager;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		try{
			return super.loadClass(name, resolve);
		}catch(ClassNotFoundException e){}
		List<ClassLoader> loaders = this.manager.getClassLoaders();
		for(ClassLoader cl:loaders){
			try{
				return cl.loadClass(name);
			}catch(ClassNotFoundException e){}
		}
		throw new ClassNotFoundException(name);
	}
}
