package xandragon.util;

import java.io.File;
import java.util.regex.Pattern;

import xandragon.converter.BinaryParser;
import xandragon.converter.file.DAEBuilder;
import xandragon.converter.model.Model;
import xandragon.core.ui.MainGui;
import xandragon.util.filedata.OpenFileFilter;

//This will handle getting all of the UI stuff running involving files and all.
public class IOHelper {
	
	/** The directory on my computer for the spiral knights game folder. This is used as a dummy value in the development build.*/
	private static final String XAN_TEST_FAKE_SK_DIR = "F:/Program Files (x86)/Steam/steamapps/common/Spiral Knights/";
	
	/** Is this code in the development environment or a compiled jar file?*/
	private static boolean isDevelopmentEnvironment = false;
	
	/** If true, this will forcefully fail the finder.*/
	private static boolean forceFailInDev = false;
	
	/** The root game resource directory, which is the Spiral Knights/rsrc folder.*/
	private static File rootGameRsrcDirectory = null;
		
	/** The latest model opened. */
	private static Model latestOpenedModel = null;
	
	/** For the function to set file extension, this is the regex to ensure it has the right name.*/
	private static Pattern extensionNameGuidelines = Pattern.compile("[^a-zA-Z0-9]");
	
	public static final OpenFileFilter DAT = new OpenFileFilter("DAT", "Binary Spiral Knights asset file");
	public static final OpenFileFilter DAE = new OpenFileFilter("DAE", "Collada DAE");
	public static final OpenFileFilter XML = new OpenFileFilter("XML", "XML Spiral Knights asset file");
	public static final OpenFileFilter DIR = new OpenFileFilter(true);
	
	/**
	 * Tests if the Spiral Knights directory is the right one, and sets the {@value rootGameRsrcDirectory} property.
	 * @param rootSKDir The path to the parent folder of the jar file (or dev environment).
	 * @return <code>true</code> if the directory is the right one, <code>false</code> if it is not.
	 */
	private static boolean isDirectoryCorrect(String rootSKDir) {
		//NOTE: No, I'm not going to use File.separator.
		//Forward slash works fine and dandy on windows. Windows will take both kinds of slashes.
		//This alongside mac + linux using forward slash makes it just fine.
		if (isDevelopmentEnvironment) {
			rootSKDir = XAN_TEST_FAKE_SK_DIR;
			Logger.AppendLn("[Dev] This is running in a development environment.");
		} else {
			//I have this wrapped in an if/else because I know that string above has a / on the end.
			if (!rootSKDir.endsWith("/")) {
				//Just a sanity check here. Don't want malformed values that can pop up.
				rootSKDir = rootSKDir + "/";
			}
		}
		//Ok so all I really care about is if the rsrc folder is here.
		File rsrcFolder = new File(rootSKDir + "rsrc");
		if (rsrcFolder.exists() && rsrcFolder.isDirectory()) {
			rootGameRsrcDirectory = rsrcFolder;
			return true;
		}
		return false;
	}
	
	public static File getBaseDirectory() {
		return getResourceDirectory().getParentFile();
	}
	
	/**
	 * This method will return the rsrc folder that should be in the Spiral Knights folder.
	 * It will also set the {@link isDevelopmentEnvironment} state accordingly.
	 * @return The rsrc directory, or null if the jar is in the wrong place.
	 */
	public static File getResourceDirectory() {
		if (rootGameRsrcDirectory != null) {
			return rootGameRsrcDirectory;
		}
		String uri = IOHelper.class.getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ").substring(1);
		File locationOrJar = new File(uri);
		isDevelopmentEnvironment = locationOrJar.isDirectory() && locationOrJar.getName().matches("bin");
		if (isDevelopmentEnvironment && forceFailInDev) {
			Logger.AppendLn("[Dev] forceFailInDev = true, an error will occur!");
			isDevelopmentEnvironment = false;
		}
		if (!isDirectoryCorrect(uri)) {
			return null;
		}
		return rootGameRsrcDirectory;//locationOrJar;
	}
	
	/**
	 * This method will attempt to grab the projectx-pcode.jar file out of the Spiral Knights directory.
	 * @return
	 */
	public static File getProjectXPCode() {
		File jarFile = new File(getBaseDirectory().getPath() + "/code/projectx-pcode.jar");
		if (jarFile.exists())return jarFile;
		
		return null;
	}
	
	/**
	 * Intended to be used in CommonConfig, this will create a new file based on a reference requested by a model.
	 * This is a QOL function to make code shorter on the other end.
	 * @param modelReferencedPath The path to the reference the object wants.
	 * @return A file for the reference
	 */
	public static File openModelReferenceTo(String modelReferencedPath) {
		return new File(rootGameRsrcDirectory + File.separator + modelReferencedPath);
	}
	
	/**
	 * The save operation in the GUI.
	 * @param openedFile
	 */
	public static void handleSaveOperation(File saveFile, OpenFileFilter fileType) {
		if (latestOpenedModel == null) {
			Logger.AppendLn("You haven't opened a file yet!");
			return;
		}
		
		if (fileType == DAE) {
			DAEBuilder builder = new DAEBuilder(saveFile, latestOpenedModel);
			builder.createDAE();
			latestOpenedModel = null;
		} else {
			Logger.AppendLn("Error! Attempted to save a format that this program cannot create.");
		}
		MainGui.INSTANCE.updateSaveButtonState(latestOpenedModel != null);
	}
	
	/**
	 * The open operation in the GUI.
	 * @param openedFile
	 */
	public static void handleOpenOperation(File openedFile, OpenFileFilter fileType) {
		if (!fileType.accept(openedFile)) {
			Logger.AppendLn("Error: You opened the incorrect type of file.",
					"Expected type: " + fileType.fileExt + " || This file's type:" + getFileExtension(openedFile));
			return;
		}

		latestOpenedModel = BinaryParser.createProcessedModel(openedFile, fileType == DAT);
		if (latestOpenedModel == null) {
			//Error.
			Logger.AppendLn("This file can't be read.");
		}
	
	
		MainGui.INSTANCE.updateSaveButtonState(latestOpenedModel != null);
	}
	
	/**
	 * QOL function to take the extension off of a filename and return it. Will return an empty string for directories.
	 * @param f The file to observe.
	 * @return The extension off of a filename. Will return an empty string for directories.
	 */
	public static String getFileExtension(File f) {
		if (!f.isDirectory()) {
			String name = f.getName();
			int lastIndex = name.lastIndexOf('.');
			if (lastIndex != -1 && lastIndex > name.lastIndexOf('/') && lastIndex > name.lastIndexOf('\\')) {
				return name.substring(lastIndex+1);
			}
		}
		return "";
	}
	
	/**
	 * Overwrites the current extension of a file or appends the new extension. This creates a new file object, but does not create any actual files in the system.
	 * @param f The file to overwrite the extension for.
	 * @param extension The new extension
	 * @return A file with the new extension.
	 */
	public static File setFileExtension(File f, String extension) {
		if (extension.length() < 1) {
			return f;
		}
		if (!f.isDirectory()) {
			File newFile = null;
			String name = f.getName();
			int lastIndex = name.lastIndexOf('.');
			if (lastIndex != -1 && lastIndex > name.lastIndexOf('/') && lastIndex > name.lastIndexOf('\\')) {
				if (extension.startsWith(".")) {
					extension = extension.substring(1);
				}
				if (extensionNameGuidelines.matcher(extension).find()) return f;
				name = name.substring(0, lastIndex) + "." + extension;
				newFile = new File(f.getParent() + "/" + name);
			}
			return newFile;
		}
		return null;
	}
	
	
}
