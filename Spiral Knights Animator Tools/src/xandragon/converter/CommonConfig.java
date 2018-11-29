package xandragon.converter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.opengl.model.config.*;
import com.threerings.opengl.model.config.ArticulatedConfig.Node;
import com.threerings.opengl.model.config.ArticulatedConfig.NodeTransform;
import com.threerings.opengl.scene.config.*;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;

import xandragon.core.ui.MainGui;
import xandragon.core.ui.tree.*;
import xandragon.util.AppendableArray;
import xandragon.util.IOHelper;
import xandragon.util.Logger;

/**
 * A common form of ThreeRings's ModelConfig. This class will be a single class storing all necessary information to form a model from SK.
 * It will also handle the bulk of the reading.
 * @author Xan the Dragon
 */
public class CommonConfig {
	
	/** The main configuration of the model being imported.*/
	protected ModelConfig mainConfig;
	
	/** The raw class of the configuration, which can be used to find its type.*/
	protected Object baseModelClass;
	
	/** The tree renderer. */
	public static TreeRenderer treeRenderer = null;
	
	/** The name of the model class being used*/
	public String modelClassName;
	
	/** The name of the subclass (if applicable) being used*/
	public String subClassName;
	
	/** The name of the model file, which is the file path with a dot instead of a slash.*/
	public String name;
	
	/** Visible meshes*/
	//public VisibleMesh[] visibleMeshes;
	public AppendableArray<VisibleMesh> visibleMeshes;
	
	/** Attachments */
	//public VisibleMesh[] attachments;
	public AppendableArray<VisibleMesh> attachments;
	
	/** Bones */
	public Node boneData;
	
	/** Bone transformations */
	//public NodeTransform[] nodeTransforms;
	public AppendableArray<NodeTransform> nodeTransforms;
	
	/**
	 * Create a new CommonConfig from the necessary information.
	 * @param raw The Object representation of the Spiral Knights model.
	 * @param fileName The name of the file opened.
	 * @param ren The tree renderer.
	 * @throws IOException
	 */
	public CommonConfig(Object raw, String rawString, String fileName, TreeRenderer ren) throws IOException {
		treeRenderer = ren;
		visibleMeshes = new AppendableArray<VisibleMesh>();
		attachments = new AppendableArray<VisibleMesh>();
		nodeTransforms = new AppendableArray<NodeTransform>();
		name = fileName.replace("/", ".");
		handleModelMain(raw, rawString, fileName);
	}
	
	
	protected void exitTreeState(String text, ImageIcon icon) {
		TreeNode typeTreeNode = new TreeNode(text);
		typeTreeNode.displayIcon = icon;
		treeRenderer.addNodeRoot(typeTreeNode);
		MainGui.INSTANCE.updateTree(treeRenderer.getDataTreePath());
	}
	
	protected void handleModelMain(Object raw, String rawString, String fileName, DefaultMutableTreeNode lastTreeNode, boolean isAttachment) throws IOException {
		if (raw instanceof ModelConfig) {
			mainConfig = (ModelConfig) raw;
		} else if (raw instanceof AnimationConfig) {
			Logger.AppendLn("An error occurred! An AnimationConfig (an animation) was imported (If you didn't do this, ignore this). Import a model instead.");
			exitTreeState("Type: AnimationConfig", Icon.animation);
			
			return;
		} else {
			Logger.AppendLn("An error occurred! The input file's classtype is unknown. Try opening another file.\n(Input Type = " + raw.getClass().getSimpleName() + " -- This will probably be ModelConfig due to Java being unable to create the specific type, which won't be helpful in debugging.)");
			exitTreeState("Type: ???", Icon.unknown);
			
			return;
		}
		
		//Ok there's a bad bug when opening player-knight models involving the class for the type not even existing.
		//Because of this I created ProjectXModelConfigReplicator which uses some hacky reflection tricks to test the type.
		//Said reflection relies on being in the SK directory, just like Spiral Spy yet again!
		String name = null;
		if (mainConfig.implementation == null) {
			if (rawString.toLowerCase().contains("projectxmodelconfig")) {
				//Special case: Knight model!
				modelClassName = "ProjectXModelConfig";
				subClassName = null;
			} else {
				Logger.AppendLn("WARNING: Implementation null!");
				Logger.AppendLn("This model is a valid ThreeRings file, but is using customized logic likely designed specifically for whatever game it's from. As a result, this model cannot be loaded.");
				exitTreeState("null", Icon.error);
				
				return;
			}
			
		} else { 
			baseModelClass = mainConfig.implementation.copy(null);
			name = baseModelClass.getClass().getName();
		}
		
		//To what I know, models will either end in "." (blah.blah.blah.ModelName) so I can substring everything to that last . to take out ModelName
		//Though if it's a subclass i.e. Derived it will end in $.
		if (name != null) {
			boolean isSubclass = name.contains((CharSequence) "$");
			if (!isSubclass) {
				modelClassName = name.substring(name.lastIndexOf('.') + 1);
				subClassName = null;
			} else {
				modelClassName = name.substring(name.lastIndexOf('.') + 1, name.lastIndexOf("$"));
				subClassName = name.substring(name.lastIndexOf("$") + 1);
			}
		}
		
		TreeNode typeTreeNode = new TreeNode("Type: "+modelClassName);
		
		if (modelClassName.matches("ModelConfig") && subClassName != null) {
			typeTreeNode.displayText = "Type: "+modelClassName+"::"+subClassName;
			
			if (subClassName.matches("Imported")) {
				Logger.AppendLn("The specific classtype in its raw form is:",typeTreeNode.displayText);
				typeTreeNode.displayIcon = Icon.unknown;
				typeTreeNode.displayText = "Unknown Type";
				
				
			} else if (subClassName.matches("Derived")) {
				typeTreeNode.displayIcon = Icon.derived;
				ModelConfig.Derived cfg = (ModelConfig.Derived) mainConfig.implementation;
				
				TreeNode refTreeNode = new TreeNode("This model derives:", Icon.object);
				
				DefaultMutableTreeNode refTreeNodeTree;
				DefaultMutableTreeNode newModelTreeNodeTree;
				
				if (lastTreeNode == null) {
					//Parent to root.
					treeRenderer.addNodeRoot(typeTreeNode);
					refTreeNodeTree = treeRenderer.addNodeRoot(refTreeNode);
				} else {
					//Parent to the reference folder, aka lastTreeNode
					//Here, we need to create the modelTreeNode
					TreeNode modelTreeNode = new TreeNode(fileName, Icon.model);
					newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
					//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
					treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
					refTreeNodeTree = treeRenderer.addNode(newModelTreeNodeTree, refTreeNode);
				}
				
				openReferenceFromPath(cfg.model.getName(), refTreeNodeTree, isAttachment);
			} else if (subClassName.matches("Schemed")) {
				typeTreeNode.displayIcon = Icon.script_part;
				ModelConfig.Schemed cfg = (ModelConfig.Schemed) mainConfig.implementation;
				
				TreeNode refTreeNode = new TreeNode("Referenced assets:", Icon.object);
				
				DefaultMutableTreeNode refTreeNodeTree;
				DefaultMutableTreeNode newModelTreeNodeTree;
				
				if (lastTreeNode == null) {
					//Parent to root.
					treeRenderer.addNodeRoot(typeTreeNode);
					refTreeNodeTree = treeRenderer.addNodeRoot(refTreeNode);
				} else {
					//Parent to the reference folder, aka lastTreeNode
					//Here, we need to create the modelTreeNode
					TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
					newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
					//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
					treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
					refTreeNodeTree = treeRenderer.addNode(newModelTreeNodeTree, refTreeNode);
				}
				
				openReference(cfg, refTreeNodeTree, isAttachment);
			}
			
		} else if (modelClassName.matches("ArticulatedConfig")) {
			ArticulatedConfig cfg = (ArticulatedConfig) mainConfig.implementation;
			typeTreeNode.displayIcon = Icon.person;
			TreeNode refTreeNode = new TreeNode("Attachments", Icon.plug);
			
			//DefaultMutableTreeNode refTreeNodeTree = null;
			DefaultMutableTreeNode newModelTreeNodeTree;
			
			if (lastTreeNode == null) {
				//Parent to root.
				treeRenderer.addNodeRoot(typeTreeNode);
				treeRenderer.addNodeRoot(refTreeNode); //used refTreeNodeTree
			} else {
				//Parent to the reference folder, aka lastTreeNode
				//Here, we need to create the modelTreeNode
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.attachment); //////////////////////////////// was model
				newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
				treeRenderer.addNode(newModelTreeNodeTree, refTreeNode); //used refTreeNodeTree
			}
			
			if (!isAttachment) {
				//visibleMeshes = cfg.skin.visible;
				visibleMeshes.add(cfg.skin.visible);
			} else {
				//attachments = cfg.skin.visible;
				attachments.add(cfg.skin.visible);
			}
			boneData = cfg.root;
			//nodeTransforms = cfg.nodeTransforms;
			nodeTransforms.add(cfg.nodeTransforms);
			/*for (Attachment a : cfg.attachments) {
				if (a.model != null) {
					openReferenceFromPath(a.model.getName(), refTreeNodeTree, true);
				}
			}*/
			
		} else if (modelClassName.matches("CompoundConfig")) {
			CompoundConfig cfg = (CompoundConfig) mainConfig.implementation;
			
			//typeTreeNode.displayIcon = Icon.model2;
			typeTreeNode.displayIcon = Icon.partgroup;
			TreeNode refTreeNode = new TreeNode("Referenced assets:", Icon.object);
			
			DefaultMutableTreeNode refTreeNodeTree;
			DefaultMutableTreeNode newModelTreeNodeTree;
			
			if (lastTreeNode == null) {
				//Parent to root.
				treeRenderer.addNodeRoot(typeTreeNode);
				refTreeNodeTree = treeRenderer.addNodeRoot(refTreeNode);
			} else {
				//Parent to the reference folder, aka lastTreeNode
				//Here, we need to create the modelTreeNode
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
				newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
				refTreeNodeTree = treeRenderer.addNode(newModelTreeNodeTree, refTreeNode);
			}
			
			openReference(cfg, refTreeNodeTree, isAttachment);
			
		} else if (modelClassName.matches("GeneratedStaticConfig")) {
			//GeneratedStaticConfig cfg = (GeneratedStaticConfig) mainConfig.implementation;
			typeTreeNode.displayIcon = Icon.part_clear;
			if (lastTreeNode == null) {
				treeRenderer.addNodeRoot(typeTreeNode);
			} else {
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
				DefaultMutableTreeNode newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
			}
			//There's no way to read this. Rip.
			MainGui.INSTANCE.updateTree(treeRenderer.getDataTreePath());
			
		} else if (modelClassName.matches("MergedStaticConfig")) {
			MergedStaticConfig cfg = (MergedStaticConfig) mainConfig.implementation;

			typeTreeNode.displayIcon = Icon.partgroup;
			TreeNode refTreeNode = new TreeNode("Referenced assets:", Icon.object);
			
			DefaultMutableTreeNode refTreeNodeTree;
			DefaultMutableTreeNode newModelTreeNodeTree;
			
			if (lastTreeNode == null) {
				//Parent to root.
				treeRenderer.addNodeRoot(typeTreeNode);
				refTreeNodeTree = treeRenderer.addNodeRoot(refTreeNode);
			} else {
				//Parent to the reference folder, aka lastTreeNode
				//Here, we need to create the modelTreeNode
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
				newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
				refTreeNodeTree = treeRenderer.addNode(newModelTreeNodeTree, refTreeNode);
			}
			openReference(cfg, refTreeNodeTree, isAttachment);
			
		} else if (modelClassName.matches("StaticConfig")) {
			StaticConfig cfg = (StaticConfig) mainConfig.implementation;
			typeTreeNode.displayIcon = Icon.part;
			if (lastTreeNode == null) {
				treeRenderer.addNodeRoot(typeTreeNode);
			} else {
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
				DefaultMutableTreeNode newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
			}
			visibleMeshes.add(cfg.meshes.visible);
			//boneData = null;
			//attachments = null;
			//nodeTransforms = null;
			
		} else if (modelClassName.matches("StaticSetConfig")) {
			StaticSetConfig cfg = (StaticSetConfig) mainConfig.implementation;
			typeTreeNode.displayIcon = Icon.partgroup;
			if (lastTreeNode == null) {
				treeRenderer.addNodeRoot(typeTreeNode);
			} else {
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.reference);
				DefaultMutableTreeNode newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
			}
			
			if (cfg.model != null) {
				MeshSet set = cfg.meshes.get(cfg.model);
				visibleMeshes.add(set.visible);
			} else {
				for (MeshSet set : cfg.meshes.values()) {
					if (set.visible != null) visibleMeshes.add(set.visible);
				}
			}
		} else if (modelClassName.matches("ProjectXModelConfig")) {
			typeTreeNode.displayIcon = Icon.error;
			typeTreeNode.displayText = "Type: ProjectXModelConfig";
			if (lastTreeNode == null) {
				treeRenderer.addNodeRoot(typeTreeNode);
			} else {
				treeRenderer.addNode(lastTreeNode, typeTreeNode);
			}
			Logger.AppendLn("ERROR! You attempted to open a player knight model. These can't be read. You should use '/rsrc/character/npc/crew/model.dat' instead!");
			
			
		} else if (modelClassName.matches("ViewerAffecterConfig")) {
			//Phew. This is gonna be crazy.
			//Viewer Affecters are things that... well, affect the view. This includes things like skyboxes, particle effects (like the snow on icy levels), ambient sounds, and ambient light.
			//To get the information out of this I have to create raw object of it and see if it casts to any specific subclasses, then handle accordingly.
			ViewerAffecterConfig cfg = (ViewerAffecterConfig) mainConfig.implementation;
			ViewerEffectConfig view_cfg = cfg.effect;
			Object raw_view = view_cfg.copy(null);
			
			typeTreeNode.displayText = "Type: ViewerAffecterConfig"+raw_view.getClass().getName().substring(raw_view.getClass().getName().lastIndexOf('$'));
			if (raw_view instanceof ViewerEffectConfig.Skybox) {
				//Type is skybox.
				
				ViewerEffectConfig.Skybox sky = (ViewerEffectConfig.Skybox) raw_view;
				
				if (sky.model != null) {
					typeTreeNode.displayIcon = Icon.sky;
					TreeNode refTreeNode = new TreeNode("Referenced assets:", Icon.object);
					
					DefaultMutableTreeNode refTreeNodeTree;
					DefaultMutableTreeNode newModelTreeNodeTree;
					
					if (lastTreeNode == null) {
						//Parent to root.
						treeRenderer.addNodeRoot(typeTreeNode);
						refTreeNodeTree = treeRenderer.addNodeRoot(refTreeNode);
					} else {
						//Parent to the reference folder, aka lastTreeNode
						//Here, we need to create the modelTreeNode
						TreeNode modelTreeNode = new TreeNode(fileName, Icon.model);
						newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
						//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
						treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
						refTreeNodeTree = treeRenderer.addNode(newModelTreeNodeTree, refTreeNode);
					}
					openReferenceFromPath(sky.model.getName(), refTreeNodeTree, isAttachment);
					
				} else {
					DefaultMutableTreeNode typeTreeNodeTree;
					typeTreeNode.displayIcon = Icon.unknown;
					if (lastTreeNode == null) {
						typeTreeNodeTree = treeRenderer.addNodeRoot(typeTreeNode);
					} else {
						typeTreeNodeTree = treeRenderer.addNode(lastTreeNode, typeTreeNode);
					}
					TreeNode info = new TreeNode("Unknown Type", Icon.info);
					Logger.AppendLn("The model you have imported is not a known type for the standard ThreeRings model library.",
							"The classname of this type is: " + modelClassName);
					treeRenderer.addNode(typeTreeNodeTree, info);
					
				}
			} else {
				typeTreeNode.displayIcon = Icon.unknown;
				if (lastTreeNode == null) {
					treeRenderer.addNodeRoot(typeTreeNode);
				} else {
					treeRenderer.addNode(lastTreeNode, typeTreeNode);
				}
			}
		} else {
			typeTreeNode.displayIcon = Icon.unknown;
			if (lastTreeNode == null) {
				treeRenderer.addNodeRoot(typeTreeNode);
			} else {
				TreeNode modelTreeNode = new TreeNode(fileName, Icon.model);
				DefaultMutableTreeNode newModelTreeNodeTree = treeRenderer.addNode(lastTreeNode, modelTreeNode);
				//Mode TreeNode made. Now the type + ref go into the new model TreeNode.
				treeRenderer.addNode(newModelTreeNodeTree, typeTreeNode);
			}
		}
		MainGui.INSTANCE.updateTree(treeRenderer.getDataTreePath());
	}
	
	protected void handleModelMain(Object raw, String rawString, String fileName) throws IOException {
		handleModelMain(raw, rawString, fileName, null, false);
	}
	
	protected void openReference(CompoundConfig cfg, DefaultMutableTreeNode parent, boolean isAttachment) throws IOException {
		for (int i = 0; i < cfg.models.length; i++) {
			if (cfg.models[i].model != null) {
				String path = cfg.models[i].model.getName();
				openReferenceFromPath(path, parent, isAttachment);
			}
		}
	}
	
	protected void openReference(MergedStaticConfig cfg, DefaultMutableTreeNode parent, boolean isAttachment) throws IOException {
		for (int i = 0; i < cfg.models.length; i++) {
			if (cfg.models[i].model != null) {
				String path = cfg.models[i].model.getName();
				openReferenceFromPath(path, parent, isAttachment);
			}
		}
	}
	
	protected void openReference(ModelConfig.Schemed cfg, DefaultMutableTreeNode parent, boolean isAttachment) throws IOException {
		for (int i = 0; i < cfg.models.length; i++) {
			if (cfg.models[i].model != null) {
				String path = cfg.models[i].model.getName();
				openReferenceFromPath(path, parent, isAttachment);
			}
		}
	}
	
	protected void openReferenceFromPath(String path, DefaultMutableTreeNode parent, boolean isAttachment) throws IOException {
		File ref = IOHelper.openModelReferenceTo(path);
		FileInputStream fileIn = new FileInputStream(ref);
		BinaryImporter stockImporter = new BinaryImporter(fileIn);
		
		String rawString = "";
		try {
			DataInputStream rawData = new DataInputStream(new FileInputStream(ref));
			rawData.readInt();
			rawData.readShort();
			short flags = rawData.readShort();
	        boolean compressed = (flags & BinaryExporter.COMPRESSED_FORMAT_FLAG) != 0;
	        if (compressed) {
	        	rawData = new DataInputStream(new InflaterInputStream(rawData));
	        }
	        
	        rawString = rawData.readUTF();
	        rawData.close();
		} catch (Exception e) { }
		
		handleModelMain(stockImporter.readObject(), rawString, ref.getName(), parent, isAttachment);
		stockImporter.close();
	}
}
