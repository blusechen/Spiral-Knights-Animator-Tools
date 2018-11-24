package xandragon.core.ui.tree;

import java.net.URL;

import javax.swing.ImageIcon;

public class Icon {
	protected static ImageIcon loadImage(String name) {
		URL url = Icon.class.getClassLoader().getResource("assets/"+name);
		return new ImageIcon(url);
	}
	
	public static ImageIcon info = loadImage("info.png");
	public static ImageIcon object = loadImage("object.png");
	public static ImageIcon sky = loadImage("sky.png");
	public static ImageIcon model = loadImage("model.png");
	public static ImageIcon value = loadImage("value.png");
	public static ImageIcon reference = loadImage("reference.png");
	public static ImageIcon tag = loadImage("tag.png");
	public static ImageIcon plug = loadImage("plug.png");
	public static ImageIcon folder_gear = loadImage("folder_gear.png");
	public static ImageIcon folder_wrench = loadImage("folder_wrench.png");
	public static ImageIcon array = loadImage("array.png");
	public static ImageIcon person = loadImage("person.png");
	public static ImageIcon people = loadImage("people.png");
	public static ImageIcon script = loadImage("script.png");
	public static ImageIcon attachment = loadImage("hat.png");
	public static ImageIcon derived = loadImage("derived.png");
	public static ImageIcon script_part = loadImage("script_part.png");
	public static ImageIcon part = loadImage("part.png");
	public static ImageIcon partgroup = loadImage("partgroup.png");
	public static ImageIcon part_clear = loadImage("part_clear.png");
	public static ImageIcon animation = loadImage("animation.png");
	public static ImageIcon unknown = loadImage("unknown.png");
	public static ImageIcon error = loadImage("error.png");
	public static ImageIcon none = loadImage("empty.png");
}
