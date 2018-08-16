package xandragon.util;

import java.awt.TextArea;
import java.util.ArrayList;

/**
 * @author Xan the Dragon
 */
public class Logger {
	
	//**A label that is used in the UI's log.*/
	//protected static TextArea UI_Label = null;
	
	/**A queue of things to write if this is called before the label is created.*/
	protected static ArrayList<String> writeQueue = new ArrayList<String>();
	
	/**A manual switch for the queue.*/
	protected static boolean isReady = true;
	
	/**The label.*/
	protected static TextArea UI_Label = null;
	
	/**
	 * Append text + a new line to both the system output and, the UI's log window.<br>
	 * @param objects A tuple of any objects, preferably strings. If objects are not strings, it will call toString() on them.
	 */
	public static void AppendLn(Object... objects) {
		//Special case: there can be no arguments. If it's a length of 0, we just append newline and that's it.
		if (objects.length == 0) {
			Append("\n");
			return;
		}
		for (int idx = 0; idx < objects.length; idx++) {
			Object obj = objects[idx];
			if (obj != null) {
				if (obj instanceof String) {
					Append((String) obj + "\n");
				} else {
					Append(obj.toString() + "\n");
				}
			} else {
				Append("null\n");
			}
		}
	}
	
	/**
	 * Append text to both the system output and, if it exists, the UI's log window.<br>
	 * @param objects A tuple of any objects, preferably strings. If objects are not strings, it will call toString() on them.
	 */
	public static void Append(Object... objects) {
		for (int idx = 0; idx < objects.length; idx++) {
			Object obj = objects[idx];
			if (obj instanceof String) {
				Append((String) obj);
			} else {
				Append(obj.toString());
			}
		}
	}
	
	public static void setLabel(TextArea label) {
		UI_Label = label;
		if (isReady) {
			writeFromQueue();
		}
	}
	
	/**
	 * Clear the log.<br>
	 * <strong>This will only work if a GUI element has been passed in.</strong>
	 */
	public static void ClearLog() {
		if (UI_Label != null) {
			UI_Label.setText(null);
		}
	}
	
	/**
	 * Set the ready state
	 * @param ready true if the logger can write to the UI from the queue, false if not.
	 */
	public static void setReady(boolean ready) {
		if (!isReady && isReady != ready) {
			//It was off, and we've turned it on.
			isReady = ready; //This is important! Without this, we have issues.
			writeFromQueue();
		}
		isReady = ready;
	}
	
	/**
	 * Get the ready state
	 * @return ready true if the logger can write to the UI from the queue, false if not.
	 */
	public static boolean getReady() {
		return isReady;
	}
	
	/**
	 * Append a list of text entries to the log. This was made for specific use cases for helping with debugging.
	 * @param texts The text to append.
	 */
	public static void AppendList(String... texts) {
		for (int idx = 0; idx < texts.length; idx++) {
			//We'll automatically put newlines.
			Append(" - " + texts[idx].replaceAll("\\n", "")  + "\n");
		}
	}
	
	/**
	 * Append text to the log with special formatting, and print.<br>
	 * <strong>Do not get this confused with the public Append function! This is the internal function that directly applies text. All append functions go here.</strong>
	 * @param text The text to append.
	 */
	protected static void Append(String text) {
		System.out.print(text); //Text will have a newline if it needs to.
		if (UI_Label != null && isReady) {
			UI_Label.setText(UI_Label.getText() + text);
		} else {
			writeQueue.add(text);
		}
	}
	
	protected static void writeFromQueue() {
		//This will forcefully write to the UI label, it's like above without the isReady check.
		if (writeQueue.size() == 0) {
			return;
		}
		String resultText = "";
		for (String item : writeQueue) {
			resultText = resultText + item;
		}
		if (UI_Label != null) {
			UI_Label.setText(UI_Label.getText() + resultText);
			writeQueue.clear();
		}
	}
}
