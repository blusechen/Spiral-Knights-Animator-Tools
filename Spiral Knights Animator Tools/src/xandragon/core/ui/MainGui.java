package xandragon.core.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;

import xandragon.core.ui.tree.CustomTreeCellRenderer;
import xandragon.core.ui.tree.DataTreePath;
import xandragon.core.ui.tree.TreeRenderer;
import xandragon.util.IOHelper;
import xandragon.util.Logger;
import xandragon.util.filedata.OpenFileFilter;

@SuppressWarnings("serial")
public class MainGui extends Frame implements ActionListener, WindowListener {
	
	
	// THIS CODE IS COMPLETE AIDS.
	// Maybe I should fix it...
	// I did. It's less horrid.
	public static MainGui INSTANCE;
	
	// Objects for the UI.
	protected TextArea UI_Label = null; //This is public because the logger needs access to it.
	protected Button openButton;
	protected Button saveButton;
	protected JFileChooser chooser;
	protected JTree dataTree;
	
	//Other
	protected SelectType fileMode = SelectType.NONE;
	
	// Main constructor.
	public MainGui() {
		if (IOHelper.getResourceDirectory() == null) {
			showGUIError();
		} else {
			createMainGUI();
		}
		INSTANCE = this;
	}
	
	protected JFileChooser createConfirmingChooser() {
		return new JFileChooser() {
			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (file == null) {
					return;
				}
				if (fileMode == SelectType.SAVE) {
					String ext = IOHelper.getFileExtension(file);
					//if (JOptionPane.showconfirmDialog(parent, body, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					String intendedExt = ((OpenFileFilter)getFileFilter()).fileExt.toLowerCase();
					if (!ext.toLowerCase().matches(intendedExt)) {
						int renFile = JOptionPane.showConfirmDialog(this, "This file does not have the *." + intendedExt + " extension.\nDo you want to save with the correct extension?", "Extension incorrect", JOptionPane.YES_NO_CANCEL_OPTION);
						if (renFile == JOptionPane.YES_OPTION) {
							file = IOHelper.setFileExtension(file, intendedExt); //Yes, keep this.
							setSelectedFile(file);
						} else if (renFile == JOptionPane.CANCEL_OPTION) {
							return;
						}
					}
					if (file.exists()) {
						int result = JOptionPane.showConfirmDialog(this, "A file in this directory is already using the name \"" + file.getName() + "\"\nAre you sure you want to overwrite this file?", "Warning: File Overwrite", JOptionPane.YES_NO_CANCEL_OPTION);
						if (result == JOptionPane.YES_OPTION) {
							super.approveSelection();
						} else if (result == JOptionPane.CANCEL_OPTION) {
							cancelSelection();
						}
					} else {
						super.approveSelection();
					}
				} else {
					super.approveSelection();
				}
			}
		};
	}
	
	protected void createMainGUI() {
		setLayout(null);
		setResizable(false);
		setTitle("Spiral Knights Animator Tools");
		setSize(800, 500);
		
		//chooser = new JFileChooser();
		chooser = createConfirmingChooser();
		chooser.setAcceptAllFileFilterUsed(false);
		
		UI_Label = new TextArea("Welcome to Spiral Knights Animator Tools.\nPlease select a model file from the game directory.\n", 0, 0, TextArea.SCROLLBARS_NONE);
		Logger.setLabel(UI_Label);
		dataTree = TreeRenderer.createBlankTree();
		openButton = new Button("Select a model...");
		saveButton = new Button("Save model as...");
		
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		dataTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		dataTree.setRootVisible(true);
		dataTree.setEditable(false);
		UI_Label.setEditable(false);
		
		saveButton.setEnabled(false);
		
		UI_Label.setBounds(5, 70, 415, 420);
		add(UI_Label);
		
		JScrollPane scroll = new JScrollPane(dataTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBounds(420, 65, 380, 420);
		dataTree.setBounds(0, 0, 1000, 1000);
		add(scroll);
		
		openButton.setBounds(25, 35, 180, 30);
		add(openButton);
		
		saveButton.setBounds(230, 35, 180, 30);
		add(saveButton);
		
		addWindowListener(this);
		openButton.addActionListener(this);
		saveButton.addActionListener(this);
		chooser.addActionListener(this);
		
		setVisible(true);
		Logger.setReady(true);
		chooser.setCurrentDirectory(IOHelper.getResourceDirectory());
	}
	
	protected void showGUIError() {
		JOptionPane.showMessageDialog(null, "Please put this JAR file in the Spiral Knights install folder", "Directory incorrect", JOptionPane.WARNING_MESSAGE);
	}
	
	public void resetTree() {
		try {
			dataTree.setModel(TreeRenderer.createBlankTreePath());
			dataTree.setCellRenderer(new DefaultTreeCellRenderer());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//This is public so the BinaryParser can access it
	public void updateTree(DataTreePath dataTreePath) {
		try {
			dataTree.setModel(dataTreePath);
			dataTree.setCellRenderer(new CustomTreeCellRenderer());
			dataTree.treeDidChange();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateSaveButtonState(boolean state) {
		saveButton.setEnabled(state);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == openButton) {
			//Open requested.
			chooser.setCurrentDirectory(IOHelper.getResourceDirectory()); // Requested QoL change.
			
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileMode = SelectType.OPEN;
			chooser.addChoosableFileFilter(IOHelper.DAT);
			chooser.removeChoosableFileFilter(IOHelper.DAE);
			//chooser.removeChoosableFileFilter(IOHelper.DIR);
			chooser.addChoosableFileFilter(IOHelper.XML);
			
			chooser.showDialog(this, "Select model");
		} else if (evt.getSource() == saveButton) {
			//Save selected.
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileMode = SelectType.SAVE;
			chooser.removeChoosableFileFilter(IOHelper.DAT);
			//chooser.removeChoosableFileFilter(IOHelper.DIR);
			chooser.addChoosableFileFilter(IOHelper.DAE);
			chooser.removeChoosableFileFilter(IOHelper.XML);
			
			chooser.showDialog(this, "Save model");
		} else if (evt.getSource() == chooser) {
			if (evt.getActionCommand() == JFileChooser.APPROVE_SELECTION) {
				if (fileMode == SelectType.SAVE) {
					IOHelper.handleSaveOperation(chooser.getSelectedFile(), (OpenFileFilter) chooser.getFileFilter());
				} else if (fileMode == SelectType.OPEN) {
					IOHelper.handleOpenOperation(chooser.getSelectedFile(), (OpenFileFilter) chooser.getFileFilter());
				}
			}
		}
	}
	
	protected static enum SelectType {
		NONE,
		OPEN,
		SAVE,
		SET
	}
	
	@Override
	public void windowClosing(WindowEvent evt) {
		System.exit(0);
	}
	
	// Not Used, but need to provide an empty body to compile.
	@Override public void windowOpened(WindowEvent evt) { }
	@Override public void windowClosed(WindowEvent evt) { }
	@Override public void windowIconified(WindowEvent evt) { }
	@Override public void windowDeiconified(WindowEvent evt) { }
	@Override public void windowActivated(WindowEvent evt) { }
	@Override public void windowDeactivated(WindowEvent evt) { }
}
