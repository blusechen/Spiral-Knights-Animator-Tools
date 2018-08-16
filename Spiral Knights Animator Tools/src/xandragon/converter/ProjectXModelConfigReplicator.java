package xandragon.converter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import xandragon.util.IOHelper;

public class ProjectXModelConfigReplicator {
	
	//com.threerings.projectx.config.ProjectXModelConfig
	
	private static Class<?> ProjectXModelConfig = null;
	
	/*
	public static final int PROJECTX = 0;
	public static final int NOT_PROJECTX = 1;
	public static final int TEST_FAILURE = 2;
	*/
	
	//TO DO: Delete this class?
	//It might be easier to just read the raw binary decompressed contents and look for the class identifier at the start.
	//This current method relies on the program being in the Spiral Knights directory and I don't like that very much.
	
	static {
		try {
			File jar = IOHelper.getProjectXPCode();
			URL url = jar.toURI().toURL();
			URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(classLoader, url);
			
			ProjectXModelConfig = classLoader.loadClass("com.threerings.projectx.config.ProjectXModelConfig");
		} catch (Exception e) {
			//Oh well.
		}
	}
	
	public static boolean isModelProjectX(Object model) {
		try {
			if (ProjectXModelConfig == null) {
				return false;
			}
			
			return model.getClass().isAssignableFrom(ProjectXModelConfig);
		} catch (Exception e) {
			
		}
		return false;
	}
	
}
