package xandragon.core;

import xandragon.core.ui.MainGui;
import xandragon.util.Logger;

public class Main {
	
	/**The current version of the program.*/
	public static final String SK_ANIM_VER = "V1.0.5";
	
	public static void main(String[] args) throws Exception { //I'm so lazy. "throws Exception". Wow.
		Logger.setReady(false);
		new MainGui();
	}
	
}
