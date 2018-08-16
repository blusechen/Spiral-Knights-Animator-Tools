package xandragon.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.threerings.export.BinaryImporter;
import com.threerings.export.Importer;
import com.threerings.export.XMLImporter;

import xandragon.converter.model.Model;
import xandragon.core.ui.MainGui;
import xandragon.core.ui.tree.Icon;
import xandragon.core.ui.tree.TreeNode;
import xandragon.core.ui.tree.TreeRenderer;
import xandragon.util.Logger;

public class BinaryParser {
	
	/** The tree node renderer for passing into the converter.*/
	public static TreeRenderer treeRen = null;
	
	private static Importer getAppropriateImporter(File input, boolean isBinary) throws FileNotFoundException {
		FileInputStream fileIn = new FileInputStream(input);
		Importer importer = null;
		if (isBinary) {
			importer = new BinaryImporter(fileIn);
		} else {
			importer = new XMLImporter(fileIn);
		}
		return importer;
	}
	
	/**
	 * Process a DAT file, storing all of the information about it.
	 * @param INPUT_FILE The file.
	 * @throws FileNotFoundException 
	 */
	public static Model createProcessedModel(File INPUT_FILE, boolean isBinary) {
		try {	
			Importer stockImporter = getAppropriateImporter(INPUT_FILE, isBinary);
			treeRen = new TreeRenderer(new TreeNode(INPUT_FILE.getName(), Icon.model));
			MainGui.INSTANCE.updateTree(new TreeRenderer(new TreeNode(INPUT_FILE.getName() + " (LOADING)", Icon.info)).getDataTreePath());
			
			Object rawModel = stockImporter.readObject();
			stockImporter.close();
			
			CommonConfig com = new CommonConfig(rawModel, INPUT_FILE.getName(), treeRen);
			//System.setErr(System.err);
			return Model.createNewModel(com);
		} catch (Exception e) {
			MainGui.INSTANCE.updateTree(new TreeRenderer(new TreeNode("An unknown error occurred.", Icon.error)).getDataTreePath());
			Logger.AppendLn("An error occurred while reading this model.");
			e.printStackTrace();
		}
		return null;
	}
}