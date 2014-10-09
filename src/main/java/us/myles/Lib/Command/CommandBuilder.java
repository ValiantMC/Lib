package us.myles.Lib.Command;

import us.myles.Lib.Command.Exception.CommandFailedException;
import us.myles.Lib.Command.Exception.InvalidArgumentException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.MessageFormat;
import java.util.ArrayList;
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
		});
	}

	public void addAdapter(ParamAdapter pa) {
		this.adapters.add(pa);
	}

	public void setCommand(Command command) {
		this.command = command;
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

				String t = type.getSimpleName();
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

	private List<Object> generateParameters(Method commandMethod, String[] args, String arguments, Object userObject) {
		int currentParam = 0;
		int currentArg = 0;
		List toGive = new ArrayList();
		for (Class param : commandMethod.getParameterTypes()) {
			Annotation[] paramAnnotations = commandMethod.getParameterAnnotations()[currentParam];

			if (isSpecial(paramAnnotations)) {
				CommandReference cr = getCommandReference(paramAnnotations);
				if (cr != null) {
					if (cr.type() == ReferenceType.RAWARGS) {
						if (param.equals(String[].class)) {
							toGive.add(arguments);
						} else {
							throw new InvalidArgumentException(param.getSimpleName(), "Is not String[].class");
						}
					}
					if (cr.type() == ReferenceType.ARGSTRING) {
						if (param.equals(String.class)) {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < args.length; i++) {
								if (i != 0) {
									sb.append(new StringBuilder().append(sb.length() == 0 ? "" : " ").append(args[i]).toString());
								}
							}
							toGive.add(sb.toString());
						} else {
							throw new InvalidArgumentException(param.getSimpleName(), "Is not String.class");
						}
					}
					if (cr.type() == ReferenceType.SELF)
						if (userObject.getClass().equals(param))
							toGive.add(userObject);
						else
							throw new InvalidArgumentException(param.getSimpleName(), MessageFormat.format("Does not equal {0}", param.getSimpleName()));
				} else {
					throw new InvalidArgumentException(param.getSimpleName(), "Failed, command reference missing.");
				}
			} else if (param.equals(Optional.class)) {
				Optional o;
				try {
					String argument = args[(currentArg + 1)];
					Class base = (Class) ((java.lang.reflect.ParameterizedType) commandMethod.getGenericParameterTypes()[currentParam]).getActualTypeArguments()[0];
					Object x = adaptString(argument, base);
					currentArg++;
					o = new Optional(x);
				} catch (Exception e) {
					o = new Optional(null);
				}
				toGive.add(o);
			} else {
				currentArg++;
				try {
					String argument = args[currentArg];
					toGive.add(adaptString(argument, param));
				} catch (Exception e) {
					if ((e instanceof InvalidArgumentException)) {
						throw e;
					}
					throw new InvalidArgumentException("Error", MessageFormat.format("Expecting additional argument of type {0}", param.getSimpleName()));
				}

			}

			currentParam++;
		}
		if (currentArg != args.length - 1) {
			throw new InvalidArgumentException("Error", "You supplied too many arguments");
		}
		return toGive;
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

	public String getCommandName() {
		return this.commandName;
	}
}