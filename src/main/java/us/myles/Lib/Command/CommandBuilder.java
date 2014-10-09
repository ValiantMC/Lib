package us.myles.Lib.Command;

import us.myles.Lib.Command.Exception.CommandFailedException;
import us.myles.Lib.Command.Exception.InvalidArgumentException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandBuilder {
	private Command command;
	private List<ParamAdapter> adapters;
	private String commandName;

	public CommandBuilder(String commandName) {
		this.commandName = commandName;
		this.adapters = new ArrayList();
		this.adapters.add(new ParamAdapter(String.class) {
			public String adapt(String arg) {
				return arg;
			}
		});
		this.adapters.add(new ParamAdapter(Integer.class) {
			public Integer adapt(String arg) {
				try {
					return Integer.valueOf(Integer.parseInt(arg));
				} catch (Exception e) {
				}
				throw new InvalidArgumentException(arg, "Argument is not a number.");
			}

			@Override
			public String name() {
				return "Number";
			}
		});
	}

	public CommandBuilder adapter(ParamAdapter pa) {
		this.adapters.add(pa);
		return this;
	}

	public CommandBuilder command(Command command) {
		this.command = command;
		return this;
	}

	public void execute(Object userObject, String... arguments) throws CommandFailedException {
		StringBuilder sb = new StringBuilder();
		for (String s : arguments) {
			sb.append((sb.length() == 0 ? "" : " ") + s);
		}
		execute(userObject, sb.toString());
	}

	public void execute(Object userObject, String arguments) throws CommandFailedException {
		arguments = arguments.trim();
		if (arguments.length() == 0) arguments = "_";
		String[] args = arguments.split(" ");
		List<Method> methods = getMatchingMethods(args[0]);
		if (methods.size() != 0) {
			Method commandMethod = resolveMethod(methods, args, userObject);
			if (commandMethod != null) {
				try {
					List toGive = generateParameters(commandMethod, args, arguments, userObject);
					try {
						commandMethod.invoke(this.command, toGive.toArray());
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				} catch (InvalidArgumentException e) {
					throw new CommandFailedException(new String[]{MessageFormat.format("Failed running that sub-command, try using /{0} help,", getCommandName()),
							MessageFormat.format("The argument \"{0}\" seems to be the reason: {1}", e.getArgument(), e.getReason())});
				}
			} else {
				List list = new ArrayList();
				list.add(MessageFormat.format("The parameters \"/{0} {1}\" do not look right, try some of these:", getCommandName(), arguments));
				for (Method m : methods) {
					list.add(getRepresentation(m));
				}
				throw new CommandFailedException(list);
			}
		} else {
			throw new CommandFailedException(new String[]{MessageFormat.format("Could not find that sub-command, try using /{0} help", getCommandName())});
		}
	}

	private String getRepresentation(Method m) {
		int currentParam = 0;
		StringBuilder sb = new StringBuilder();
		sb.append(new StringBuilder().append("/").append(getCommandName()).append(" ").append(m.getName()).toString());
		for (Class param : m.getParameterTypes()) {
			Annotation[] paramAnnotations = m.getParameterAnnotations()[currentParam];
			if (!isSpecial(paramAnnotations)) {
				boolean optional = false;
				Class type;
				if (param.equals(Optional.class)) {
					optional = true;
					type = (Class) ((ParameterizedType) m.getGenericParameterTypes()[currentParam]).getActualTypeArguments()[0];
				} else {
					type = param;
				}

				String t = getTypeName(type);
				if (type.isEnum()) {
					StringBuilder enumTypes = new StringBuilder();
					for (Object o : type.getEnumConstants()) {
						if ((o instanceof Enum)) {
							Enum ee = (Enum) o;
							enumTypes.append(new StringBuilder().append(enumTypes.length() == 0 ? "" : "/").append(ee.name()).toString());
						}
					}
					t = MessageFormat.format("{0}({1})", param.getSimpleName(), enumTypes);
				}
				sb.append(new StringBuilder().append(" ").append(optional ? "<" : "").append(t).append(optional ? ">" : "").toString());
			}
			currentParam++;
		}
		return sb.toString();
	}

	private String getTypeName(Class type) {
		for (ParamAdapter a : this.adapters) {
			if ((a.type().isAssignableFrom(type)) || (type.isAssignableFrom(a.type()))) {
				return a.name();
			}
		}
		return type.getSimpleName();
	}

	private List<Object> generateParameters(Method commandMethod, String[] args, String arguments, Object userObject) {
		int currentArg = 0;
		List<Object> toGive = new ArrayList();
		for (int currentParam = 0; currentParam < commandMethod.getParameterTypes().length; currentParam++) {
			currentArg = generateParameter(toGive, commandMethod, currentParam, currentArg, userObject, args, arguments);
		}
		if (currentArg != args.length - 1) {
			throw new InvalidArgumentException("Error", "You supplied too many arguments");
		}
		return toGive;
	}

	private int generateParameter(List<Object> objects, Method m, int index, int currentArg, Object userObject, String[] args, String arguments) {
		Annotation[] annotations = m.getParameterAnnotations()[index];
		Class parameter = m.getParameterTypes()[index];
		if (isSpecial(annotations)) {
			CommandReference cr = getCommandReference(annotations);
			if (cr != null) {
				if (cr.type() == ReferenceType.RAWARGS) {
					if (parameter.equals(String[].class)) {
						objects.add(arguments);
					} else {
						throw new InvalidArgumentException(parameter.getSimpleName(), "Is not String[].class");
					}
				}
				if (cr.type() == ReferenceType.ARGSTRING) {
					if (parameter.equals(String.class)) {
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < args.length; i++) {
							if (i != 0) {
								sb.append(new StringBuilder().append(sb.length() == 0 ? "" : " ").append(args[i]).toString());
							}
						}
						objects.add(sb.toString());
					} else {
						throw new InvalidArgumentException(parameter.getSimpleName(), "Is not String.class");
					}
				}
				if (cr.type() == ReferenceType.SELF)
					if (userObject.getClass().equals(parameter))
						objects.add(userObject);
					else
						throw new InvalidArgumentException(parameter.getSimpleName(), MessageFormat.format("Does not equal {0}", parameter.getSimpleName()));
			} else {
				throw new InvalidArgumentException(parameter.getSimpleName(), "Failed, command reference missing.");
			}
		} else if (parameter.equals(Optional.class)) {
			Optional o;
			try {
				String argument = args[(currentArg + 1)];
				Class base = (Class) ((java.lang.reflect.ParameterizedType) m.getGenericParameterTypes()[index]).getActualTypeArguments()[0];
				Object x = adaptString(argument, base);
				currentArg++;
				o = new Optional(x);
			} catch (Exception e) {
				o = new Optional(null);
			}
			objects.add(o);
		} else {
			currentArg++;
			try {
				String argument = args[currentArg];
				objects.add(adaptString(argument, parameter));
			} catch (Exception e) {
				if ((e instanceof InvalidArgumentException)) {
					throw e;
				}
				throw new InvalidArgumentException("Error", MessageFormat.format("Expecting additional argument of type {0}", parameter.getSimpleName()));
			}
		}
		return currentArg;
	}

	private Method resolveMethod(List<Method> methods, String[] args, Object userObject) {
		for (Method m : methods)
			try {
				generateParameters(m, args, "", userObject);
				return m;
			} catch (Exception e) {
			}
		return null;
	}

	private Method closestMethod(List<Method> methods, String[] args, Object userObject) {
		int highest = 0;
		Method high = null;
		for (Method m : methods) {
			int currentArg = 0;
			List<Object> toGive = new ArrayList();
			int pts = 0;
			for (int currentParam = 0; currentParam < m.getParameterTypes().length; currentParam++) {
				try {
					currentArg = generateParameter(toGive, m, currentParam, currentArg, userObject, args, "Another Placeholder");
					pts++;
				} catch (Exception e) {
				}
			}
			if (pts > highest) {
				high = m;
				highest = pts;
			}
		}
		return high;
	}

	public List<String> complete(Object userObject, String[] args) {
		List<Method> methods = getMatchingMethods(args[0]);
		if (methods.size() != 0) {
			Method commandMethod = closestMethod(methods, args, userObject);
			if (commandMethod != null) {
				int currentArg = 0;
				for (int currentParam = 0; currentParam < commandMethod.getParameterTypes().length; currentParam++) {
					Annotation[] annotations = commandMethod.getParameterAnnotations()[currentParam];
					Class parameter = commandMethod.getParameterTypes()[currentParam];
					if (!isSpecial(annotations)) {
						if (parameter.equals(Optional.class)) {
							Class base = (Class) ((java.lang.reflect.ParameterizedType) commandMethod.getGenericParameterTypes()[currentParam]).getActualTypeArguments()[0];
							try {
								String argument = args[(currentArg + 1)];
								adaptString(argument, base);
								currentArg++;
							} catch (Exception e) {
								if (!(e instanceof InvalidArgumentException)) {
									return getAdapterExamples(base, userObject);
								}
							}
						} else {
							currentArg++;
							try {
								String argument = args[currentArg];
								adaptString(argument, parameter);
							} catch (Exception e) {
								if (!(e instanceof InvalidArgumentException)) {
									return getAdapterExamples(parameter, userObject);
								}
							}
						}
					}
				}
			} else {
				return Arrays.asList();
			}
		} else {
			return Arrays.asList();
		}
		return Arrays.asList();
	}

	private Object adaptString(String argument, Class<?> param) {
		if (param.isEnum()) {
			Enum e = getMatchingEnum(argument, param);
			if (e != null)
				return e;
			StringBuilder enumTypes = new StringBuilder();
			for (Object o : param.getEnumConstants()) {
				if ((o instanceof Enum)) {
					Enum ee = (Enum) o;
					enumTypes.append(new StringBuilder().append(enumTypes.length() == 0 ? "" : ", ").append(ee.name()).toString());
				}
			}
			throw new InvalidArgumentException(argument, MessageFormat.format("Does not match any of the following ", enumTypes));
		}
		for (ParamAdapter a : this.adapters) {
			if ((a.type().isAssignableFrom(param)) || (param.isAssignableFrom(a.type()))) {
				return param.cast(a.adapt(argument));
			}
		}
		throw new InvalidArgumentException(argument, MessageFormat.format("Cannot find adapter to cast to {0}.class", param.getSimpleName()));
	}

	private List<String> getAdapterExamples(Class<?> param, Object userObject) {
		if (param.isEnum()) {
			List<String> s = new ArrayList<String>();
			for (Object o : param.getEnumConstants()) {
				if ((o instanceof Enum)) {
					Enum ee = (Enum) o;
					s.add(ee.name());
				}
			}
			return s;
		}
		for (ParamAdapter a : this.adapters) {
			if ((a.type().isAssignableFrom(param)) || (param.isAssignableFrom(a.type()))) {
				return a.examples(userObject);
			}
		}
		return Arrays.asList();
	}

	private Enum getMatchingEnum(String argument, Class<?> param) {
		for (Object o : param.getEnumConstants()) {
			if ((o instanceof Enum)) {
				Enum e = (Enum) o;
				if (e.name().equalsIgnoreCase(argument)) return e;
			}
		}
		return null;
	}

	private CommandReference getCommandReference(Annotation[] paramAnnotations) {
		for (Annotation x : paramAnnotations) {
			if ((x instanceof CommandReference)) return (CommandReference) x;
		}
		return null;
	}

	private boolean isSpecial(Annotation[] a) {
		for (Annotation x : a) {
			if ((x instanceof CommandReference)) return true;
		}
		return false;
	}

	private List<Method> getMatchingMethods(String arg) {
		List methods = new ArrayList();
		for (Method m : this.command.getClass().getDeclaredMethods()) {
			if (m.getName().equalsIgnoreCase(arg)) methods.add(m);
		}
		return methods;
	}

	private String getCommandName() {
		return this.commandName;
	}
}