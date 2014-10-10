package us.myles.Lib.Module;

import us.myles.Lib.Module.API.BaseLoaderModule;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager<T extends BaseLoaderModule> {
	private final List<T> modules;
	private final Class<T> baseClass;

	public ModuleManager(Class<T> base) {
		this.modules = new ArrayList<>();
		this.baseClass = base;
	}

	public void load(File jarFile) {
		try {
			ModuleLoader ml = new ModuleLoader(this, new URL[]{jarFile.toURI().toURL()});
			JarFile jar = new JarFile(jarFile);
			Enumeration<JarEntry> jarFiles = jar.entries();
			while (jarFiles.hasMoreElements()) {
				JarEntry file = jarFiles.nextElement();
				if (file.getName().endsWith(".class") && !file.getName().startsWith("META-INF")) {
					try {
						Class clazz = ml.loadClass(file.getName().replace("/", ".").replace(".class", ""));
						if (clazz.getSuperclass().isAssignableFrom(baseClass)) {
							load((Class<T>) clazz);
						}
					} catch (ClassNotFoundException e) {
					}
				}
			}
			jar.close();
			ml.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load(Class<? extends T> moduleClass) {
		try {
			T module = moduleClass.newInstance();
			this.modules.add(module);
			module.load();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public T getModuleByName(String name) {
		for (T module : this.modules) {
			if (module.getClass().getSimpleName().equalsIgnoreCase(name))
				return module;
		}
		return null;
	}

	public List<T> getModules() {
		return this.modules;
	}

	public List<ClassLoader> getClassLoaders() {
		List<ClassLoader> cl = new ArrayList<>();
		cl.add(this.getClass().getClassLoader());
		for (T m : modules) {
			if (!cl.contains(m.getClass().getClassLoader()))
				cl.add(m.getClass().getClassLoader());
		}
		return cl;
	}

	public void unloadAll() {
		for (T module : this.modules) {
			module.dispose();
		}
		this.modules.clear();
	}
}
