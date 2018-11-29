package xandragon.converter.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.opengl.model.config.ArticulatedConfig.Node;

import xandragon.converter.model.BoneDepth;
import xandragon.converter.model.Geometry;
import xandragon.converter.model.Model;
import xandragon.core.Main;
import xandragon.util.Logger;

@SuppressWarnings("unused")

public class DAEBuilder {
	
	private static final String IDENTITY_MATRIX = "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1";
	
	private Model processedModel;
	private File OUTPUT_FILE;
	private Document DOCUMENT;
	private Element ROOT;
	private Element lib_images;
	private Element lib_effects;
	private Element lib_materials;
	private Element lib_geometry;
	private Element lib_control;
	private Element lib_nodes;
	private Element lib_visual_scene;
	private Element base_scene;
	
	public DAEBuilder(File out, Model mdl) {
		try {
			processedModel = mdl;
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			DOCUMENT = docBuilder.newDocument();
			OUTPUT_FILE = out;
		} catch (ParserConfigurationException e) {
			Logger.AppendLn("A critical error has occurred when attempting to save the model as a DAE. Location: DAE data creation. Stack trace:", e.getStackTrace());
		}
		/////////////////////////////////////
		//PART 1: CREATE ROOT AND BASE DATA//
		/////////////////////////////////////
		
		//////Create the root element.
		ROOT = DOCUMENT.createElement("COLLADA");
		ROOT.setAttribute("xmlns", "http://www.collada.org/2005/11/COLLADASchema");
		ROOT.setAttribute("version", "1.4.1");
		DOCUMENT.appendChild(ROOT);
		
		
		//////CREATE METADATA
		Element asset = DOCUMENT.createElement("asset");
		Element contributor = DOCUMENT.createElement("contributor");
		Element authorElement = DOCUMENT.createElement("author");
		Element authoringTool = DOCUMENT.createElement("authoring_tool");
		Element appVersion = DOCUMENT.createElement("authoring_tool_version");
		Element unit = DOCUMENT.createElement("unit");
		Element upAxis = DOCUMENT.createElement("up_axis");
		
		Text author = DOCUMENT.createTextNode("Xan the Dragon");
		Text tool = DOCUMENT.createTextNode("Spiral Knights Animator Tools");
		Text version = DOCUMENT.createTextNode(Main.SK_ANIM_VER);
		Text upAxisTxt = DOCUMENT.createTextNode("Z_UP");
		
		authorElement.appendChild(author);
		authoringTool.appendChild(tool);
		appVersion.appendChild(version);
		
		contributor.appendChild(authorElement);
		contributor.appendChild(authoringTool);
		contributor.appendChild(appVersion);
				
		upAxis.appendChild(upAxisTxt);
		
		unit.setAttribute("meter", "1");
		unit.setAttribute("name", "meter");
		
		asset.appendChild(contributor);
		asset.appendChild(upAxis);
		asset.appendChild(unit);
		ROOT.appendChild(asset);
		
		//////////////////////////////////
		//PART 2: CREATE DATA CONTAINERS//
		//////////////////////////////////
		
		//Create library_images element
		lib_images = DOCUMENT.createElement("library_images");
		
		//Create library_effects element
		lib_effects = DOCUMENT.createElement("library_effects");
		
		//Create library_materials element
		lib_materials = DOCUMENT.createElement("library_materials");
		
		//Create library_geometries element
		lib_geometry = DOCUMENT.createElement("library_geometries");
		
		//Create library_nodes element
		lib_nodes = DOCUMENT.createElement("library_nodes");
		
		//Create library_controllers element
		lib_control = DOCUMENT.createElement("library_controllers");
		
		//Create library_visual_scenes element
		lib_visual_scene = DOCUMENT.createElement("library_visual_scenes");
		
		base_scene = DOCUMENT.createElement("visual_scene");
 		base_scene.setAttribute("id", "Scene");
 		base_scene.setAttribute("name", "Scene");
 		lib_visual_scene.appendChild(base_scene);
 		
 		process();
	}
	
	@SuppressWarnings("rawtypes")
	private Element appendGeometryData(Geometry geo, ArrayList data, String type, String name) {
		String list = "";
		if (data == geo.vertices) {
			list = geo.createVertexList();
		} else if (data == geo.normals) {
			list = geo.createNormalList();
		} else if (data == geo.uvs) {
			list = geo.createUVList();
		}
		
		int sizeVal = data.size();
		
		//Source
		Element src = DOCUMENT.createElement("source");
		src.setAttribute("id", name+"-mesh-"+type);
		
		//Array
		Element arrayHeading = DOCUMENT.createElement("float_array");
		arrayHeading.setAttribute("id", name+"-mesh-"+type+"-array");
		arrayHeading.setAttribute("count", String.valueOf(sizeVal));
		Text arrayText = DOCUMENT.createTextNode(list);
		arrayHeading.appendChild(arrayText);
		src.appendChild(arrayHeading);
		
		//Technique
		Element technique = DOCUMENT.createElement("technique_common");
		src.appendChild(technique);
		
		//Accessor
		Element accessor = DOCUMENT.createElement("accessor");
		accessor.setAttribute("source", "#"+name+"-mesh-"+type+"-array");
		accessor.setAttribute("count", String.valueOf(sizeVal / (data == geo.uvs ? 2 : 3)));
		accessor.setAttribute("stride", data == geo.uvs ? "2" : "3");
		technique.appendChild(accessor);
		
		//Parameters
		if (data == geo.uvs) {
			Element paramS = DOCUMENT.createElement("param");
			Element paramT = DOCUMENT.createElement("param");
			
			paramS.setAttribute("name", "S");
			paramT.setAttribute("name", "T");
			paramS.setAttribute("type", "float");
			paramT.setAttribute("type", "float");
			
			accessor.appendChild(paramS);
			accessor.appendChild(paramT);
		} else {
			Element paramX = DOCUMENT.createElement("param");
			Element paramY = DOCUMENT.createElement("param");
			Element paramZ = DOCUMENT.createElement("param");
			
			paramX.setAttribute("name", "X");
			paramY.setAttribute("name", "Y");
			paramZ.setAttribute("name", "Z");
			paramX.setAttribute("type", "float");
			paramY.setAttribute("type", "float");
			paramZ.setAttribute("type", "float");
			
			accessor.appendChild(paramX);
			accessor.appendChild(paramY);
			accessor.appendChild(paramZ);
		}
		
		return src;
	}
	
	public void appendNewGeometry(Geometry geo, String name, int id) {
		//PRE-WRITE: GET VALUES FROM GEOMETRY.
		int vertexSize = geo.vertices.size();
		int normalSize = geo.normals.size();
		int uvSize = geo.uvs.size();
		
		///////////////////
		//PART 1: HEADING//
		///////////////////	
		Element geometryId = DOCUMENT.createElement("geometry");
		geometryId.setAttribute("id", name + id +"-mesh");
		geometryId.setAttribute("name", name + id);
		Element mesh = DOCUMENT.createElement("mesh");
		geometryId.appendChild(mesh);
		
		////////////////////
		//PART 2: RAW DATA//
		////////////////////	
		mesh.appendChild(appendGeometryData(geo, geo.vertices, "positions", name + id));
		mesh.appendChild(appendGeometryData(geo, geo.normals, "normals", name + id));
		mesh.appendChild(appendGeometryData(geo, geo.uvs, "map-0", name + id));
		
		//////////////////////
		//PART 3: REFERENCES//
		//////////////////////
		Element vertices = DOCUMENT.createElement("vertices");
 		vertices.setAttribute("id", name + id +"-mesh-vertices");
 		mesh.appendChild(vertices);
 		
 		Element posInput = DOCUMENT.createElement("input");
 		posInput.setAttribute("semantic", "POSITION");
 		posInput.setAttribute("source", "#"+ name + id +"-mesh-positions");
 		vertices.appendChild(posInput);
 		
 		Element triangleList = DOCUMENT.createElement("triangles");
 		triangleList.setAttribute("count", String.valueOf(geo.indices.size()));
 		triangleList.setAttribute("material", name + id + "-material");
 		mesh.appendChild(triangleList);
 		
 		Element vtxInput = DOCUMENT.createElement("input");
 		vtxInput.setAttribute("semantic", "VERTEX");
 		vtxInput.setAttribute("source", "#"+ name + id +"-mesh-vertices");
 		vtxInput.setAttribute("offset", "0");
 		triangleList.appendChild(vtxInput);
 		
 		Element nrmInput = DOCUMENT.createElement("input");
 		nrmInput.setAttribute("semantic", "NORMAL");
 		nrmInput.setAttribute("source", "#"+ name + id +"-mesh-normals");
 		nrmInput.setAttribute("offset", "0");
 		triangleList.appendChild(nrmInput);
 		
 		Element uvInput = DOCUMENT.createElement("input");
 		uvInput.setAttribute("semantic", "TEXCOORD");
 		uvInput.setAttribute("source", "#"+ name + id +"-mesh-map-0");
 		uvInput.setAttribute("offset", "0");
 		uvInput.setAttribute("set", "0");
 		triangleList.appendChild(uvInput);
 		
 		////////////////////
 		//PART 4: INDEXING//
 		////////////////////
 		Element idxElement = DOCUMENT.createElement("p");
 		Text idxValue = DOCUMENT.createTextNode(geo.createIndexList());
 		idxElement.appendChild(idxValue);
 		triangleList.appendChild(idxElement);
 		
 		////////////////////////////
 		//PART 5: BONE REFERENCING//
 		////////////////////////////
 		//This is required even without bones since this method handles appending things without a skeleton.
 		//EDIT: Do not permit this to continue with this function if there IS a bone list!
 		//It will cause a double-append
 		if (geo.boneList == null) appendBoneReference(geo, name, id);
 		
 		//Complete:
 		lib_geometry.appendChild(geometryId);
	}
	
	public void appendNewBoneGeometry(Geometry geo, String name, int id) {
		if (geo.boneList == null || geo.boneIndices.size() == 0) {
			return; //See BinaryParser
		}
		//Start off with the controller id for the bones.
		Element controller = DOCUMENT.createElement("controller");
		controller.setAttribute("id", name + id + "-skin");
		lib_control.appendChild(controller);
		
		Element skin = DOCUMENT.createElement("skin");
		skin.setAttribute("source", "#" + name + id + "-mesh");
		controller.appendChild(skin);
		
		Element bindMtx = DOCUMENT.createElement("bind_shape_matrix");
		Text bindVal = DOCUMENT.createTextNode("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");
		bindMtx.appendChild(bindVal);
		controller.appendChild(bindMtx);
		
		Element src_joints = DOCUMENT.createElement("source");
		src_joints.setAttribute("id", name + id + "-skin-joints");
		skin.appendChild(src_joints);
		
		//The real bone list.
		ArrayList<BoneDepth> bDepthList = geo.parentModel.bonelist;
		ArrayList<Node> compiledBonesList = new ArrayList<Node>(bDepthList.size());
		Node[] compiledBones = new Node[0];
		String nameListStr = "";
		
		for (BoneDepth depth : bDepthList) {
			boolean isRoot = depth.bone.name.contains("ROOT") && depth.bone.name.startsWith("%") && depth.bone.name.endsWith("%");
			if (!isRoot) {
				compiledBonesList.add(depth.bone);
				nameListStr = nameListStr + depth.bone.name.replace(" ", "_") + " ";
			}
		}
		compiledBonesList.trimToSize();
		compiledBones = compiledBonesList.toArray(compiledBones);
		
		Element nameArray = DOCUMENT.createElement("Name_array");
		nameArray.setAttribute("count", String.valueOf(compiledBones.length));
		nameArray.setAttribute("id", name + id + "-skin-joints-array");
		nameListStr = nameListStr.trim();
		Text nameList = DOCUMENT.createTextNode(nameListStr);
		nameArray.appendChild(nameList);
		src_joints.appendChild(nameArray);
		
		//Technique for joints
		Element joint_technique = DOCUMENT.createElement("technique_common");
		Element joint_technique_accessor = DOCUMENT.createElement("accessor");
		joint_technique_accessor.setAttribute("source", "#" + name + id + "-skin-joints-array");
		joint_technique_accessor.setAttribute("count", String.valueOf(compiledBones.length));
		joint_technique_accessor.setAttribute("stride", "1");
		Element joint_technique_param = DOCUMENT.createElement("param");
		joint_technique_param.setAttribute("name", "JOINT");
		joint_technique_param.setAttribute("type", "name");
		
		joint_technique_accessor.appendChild(joint_technique_param);
		joint_technique.appendChild(joint_technique_accessor);
		src_joints.appendChild(joint_technique);
		
		//Weights
		String weightsStr = "";
		//int c = 0;
		for (int idx = 0; idx < geo.indices.size(); idx++) {
			weightsStr = weightsStr + geo.boneWeights.get(geo.indices.get(idx) * 4).floatValue() + " ";
			//c++; //what a funny joke
		}
		weightsStr = weightsStr.trim();
		
		Element src_weights = DOCUMENT.createElement("source");
		src_weights.setAttribute("id", name + id + "-skin-weights");
		skin.appendChild(src_weights);
		
		Element weight_floatArray = DOCUMENT.createElement("float_array");
		weight_floatArray.setAttribute("id", name + id + "-skin-weights-array");
		weight_floatArray.setAttribute("count", String.valueOf(geo.indices.size()));
		src_weights.appendChild(weight_floatArray);
		
		Text weight_array = DOCUMENT.createTextNode(weightsStr);
		weight_floatArray.appendChild(weight_array);
		
		//Technique for weights
		Element weight_technique = DOCUMENT.createElement("technique_common");
		Element weight_technique_accessor = DOCUMENT.createElement("accessor");
		weight_technique_accessor.setAttribute("source", "#" + name + id + "-skin-weights-array");
		weight_technique_accessor.setAttribute("count", String.valueOf(geo.indices.size()));
		weight_technique_accessor.setAttribute("stride", "1");
		Element weight_technique_param = DOCUMENT.createElement("param");
		weight_technique_param.setAttribute("name", "WEIGHT");
		weight_technique_param.setAttribute("type", "float");
		
		weight_technique_accessor.appendChild(weight_technique_param);
		weight_technique.appendChild(weight_technique_accessor);
		src_weights.appendChild(weight_technique);
		
		//Transforms
		String matrixStr = "";
		
		//had to stare at my complete shit code from Spiral Knights Model Converter (the old thing) for so long to get this damn thing working.
		//Sorry I took ... Well no, I have audio for this.
		//https://clyp.it/zqombyub
		
		//jfc did I even have an idea for what good code writing is? That code makes me want to go back in time and slap myself.
		//So basically what I did was go through the indices (which each have four values, they're stored in quads), 
		//grabbing only the first one since that one seems to be the only used one. After that, I had two values which I
		//assume were "currentIndex" and "finalIndex" (they had bad names)...
		
		//Following that I got a name variable for grabbing a bone name from the current bone index, then
		//I iterated through some global variable I had named "compiled_bones" which was just all of the bones compiled into a list.
		
		//I finally compared if the names were the same. If they were, I would set this final index value to the current index of the for loop.
		//...And then I subtracted...?
		//Ok screw this I'll just rewrite it below, read that instead.
		//I mean, what the hell? It DID work....
		//I'm not gonna question my shitty-code self.
		
		String vtxStr = "";
		String vcStr = "";
		//UPDATE: Upon looking at the construction of the code, the proposed "bone_indices" variable is actually the same length as the mesh indices.
		//boneIndices -> indices
		//int bIdx = 0;

		
		
		//ALRIGHT SCREW THIS IT DOESN'T EVEN FOLLOW THE SAME RULES.
		//I HAVE NO IDEA HOW THIS WORKS, IT JUST DOES, PLEASE DON'T ASK HOW
		for (int idx = 0; idx < Math.ceil(geo.boneIndices.size() / 4); idx++) {

			//And I was LAZY about this one too. I suppose I'll just carry that over here because I'm insecure, and I need to get this out
			//I'll make it pretty in V1.0.1
			try {
				int meshIndex = geo.indices.get(idx);
				int boneIndex = geo.boneIndices.get(idx * 4);
				//Note: The reason we do *4 is because each index is in quad groups
				//In SK model converter I did a 2D array where each X index was a bone index set, and each Y index was oriented to one of the four in each set
				//The code there did boneIndices[meshIndex][0], which is the same as *4 in this array where they are all in one 1D array.
				//bIdx += 4;
				
				//Ok here's that part I was confused about with the subtraction and all.
				int currentIndex = boneIndex;
				int finalIndex = boneIndex;
				String goalName = geo.boneList[boneIndex].replace(" ", "_");
				for (int jdx = 0; jdx < compiledBones.length; jdx++) {
					String currentName = compiledBones[jdx].name.replace(" ", "_");
					if (currentName.equalsIgnoreCase(goalName)) {
						finalIndex = jdx;
						break;
					}
				}
				
				//Ok ok here it is right here
				//this is the insanity I was confused about.
				boneIndex = finalIndex;
				
				//So basically what I did was grab the indices and the ... other indices. Yes. Good. Cool.
				//Why.
				//TO--DO: Enable this if something goes apeshit.
				vtxStr = vtxStr + boneIndex + " " + meshIndex + " ";
				//vtxStr = vtxStr + boneIndex + " ";
				vcStr = vcStr + "1 ";
				
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Out of bounds " + idx);
				//this_is_fine_fire_dog.png
			}
		}
		
		String vtxamount = String.valueOf(geo.indices.size()); //was c
		vtxStr = vtxStr.trim();
		vcStr = vcStr.trim();
		
		Element vtx_weights = DOCUMENT.createElement("vertex_weights");
		vtx_weights.setAttribute("count", vtxamount);
		Element sem_joint = DOCUMENT.createElement("input");
		sem_joint.setAttribute("semantic", "JOINT");
		sem_joint.setAttribute("source", "#" + name + id + "-skin-joints");
		sem_joint.setAttribute("offset", "0");
		Element sem_weight = DOCUMENT.createElement("input");
		sem_weight.setAttribute("semantic", "WEIGHT");
		sem_weight.setAttribute("source", "#" + name + id + "-skin-weights");
		sem_weight.setAttribute("offset", "1");
		
		//INVERSE BIND MATRIX
		Element src_bindmtx = DOCUMENT.createElement("source");
		src_bindmtx.setAttribute("id", name + id + "-skin-joints-bind");
		
		//Node bone = geo.thisBone;
		
		String boneTransformationArrayText = "";
		int boneMatrixCount = 0;
		int boneRawCount = 0;
		for (Node bone : compiledBones) {
			//boneTransformationArrayText = boneTransformationArrayText + IDENTITY_MATRIX + " ";
			boneTransformationArrayText = boneTransformationArrayText + getMatrixAsString(bone) + " ";
			boneMatrixCount += 16;
			boneRawCount++;
		}
		boneTransformationArrayText = boneTransformationArrayText.trim();
		
		
		Element bindmtx_array = DOCUMENT.createElement("float_array");
		bindmtx_array.setAttribute("count", String.valueOf(boneMatrixCount));
		bindmtx_array.setAttribute("sid", name + id + "-skin-joints-bind-array");
		Text bindmtx_arraytext = DOCUMENT.createTextNode(boneTransformationArrayText);
		bindmtx_array.appendChild(bindmtx_arraytext);
		
		Element bindmtx_technique = DOCUMENT.createElement("technique_common");
		Element bindmtx_accessor = DOCUMENT.createElement("accessor");
		bindmtx_accessor.setAttribute("count", String.valueOf(boneRawCount));
		bindmtx_accessor.setAttribute("source", "#" + name + id + "-skin-joints-bind-array");
		bindmtx_accessor.setAttribute("stride", "16");
		
		
		Element bindmtx_param = DOCUMENT.createElement("param");
		bindmtx_param.setAttribute("name", "TRANSFORM");
		bindmtx_param.setAttribute("type", "float4x4");
		
		
		bindmtx_accessor.appendChild(bindmtx_param);
		bindmtx_technique.appendChild(bindmtx_accessor);
		src_bindmtx.appendChild(bindmtx_array);
		src_bindmtx.appendChild(bindmtx_technique);
		//TO--DO: Enable this if something goes apeshit
		skin.appendChild(src_bindmtx);
		
		Element jointData = DOCUMENT.createElement("joints");
		
		Element inputJoints = DOCUMENT.createElement("input");
		inputJoints.setAttribute("semantic", "JOINT");
		inputJoints.setAttribute("source", "#" + name + id + "-skin-joints");
		jointData.appendChild(inputJoints);
		
		Element inputTransform = DOCUMENT.createElement("input");
		inputTransform.setAttribute("semantic", "INV_BIND_MATRIX");
		inputTransform.setAttribute("source", "#" + name + id + "-skin-joints-bind");
		//TO--DO: Enable this if something goes apeshit.
		jointData.appendChild(inputTransform);
		
		skin.appendChild(jointData);
		
		Element vcount = DOCUMENT.createElement("vcount");
		Text vcountstr = DOCUMENT.createTextNode(vcStr);
		vcount.appendChild(vcountstr);
		
		Element v = DOCUMENT.createElement("v");
		Text vstr = DOCUMENT.createTextNode(vtxStr);
		v.appendChild(vstr);

		vtx_weights.appendChild(sem_joint);
		vtx_weights.appendChild(sem_weight);
		vtx_weights.appendChild(vcount);
		vtx_weights.appendChild(v);
		skin.appendChild(vtx_weights);
		
		appendBoneReference(geo, name, id);
	}
	
	//Called by above.
	private void appendBoneReference(Geometry geo, String name, int id) {
		boolean usesBones = !(geo.isRigged || (geo.thisBone == null));
		Element mtlBindParent = null;
		
		//Append the base scene data.
 		Element topLevelGeoNode = DOCUMENT.createElement("node");
 		
		if (!usesBones) {
	 		topLevelGeoNode.setAttribute("id", name + id);
	 		topLevelGeoNode.setAttribute("name", name + id);
	 		topLevelGeoNode.setAttribute("type", "NODE");
			
			//Just make a default matrix for the root transform.
			Element trsMatrix = DOCUMENT.createElement("matrix");
			trsMatrix.setAttribute("sid", "transform");
			Text mtxVal = DOCUMENT.createTextNode(IDENTITY_MATRIX);
			//Text mtxVal = DOCUMENT.createTextNode(getMatrixAsString(geo.bonesByName.get(name)));
			trsMatrix.appendChild(mtxVal);
			topLevelGeoNode.appendChild(trsMatrix);
			
			Element geoInstance = DOCUMENT.createElement("instance_geometry");
			geoInstance.setAttribute("url", "#"+ name + id +"-mesh");
			geoInstance.setAttribute("name", name + id);
			topLevelGeoNode.appendChild(geoInstance);
			
			Element mainNode = DOCUMENT.createElement("node");
			mainNode.setAttribute("id", name + id);
			mainNode.setAttribute("name", name + id);
			mainNode.setAttribute("type", "NODE");
			//base_scene.appendChild(mainNode);
			//topLevelGeoNode.appendChild(mainNode);
			//Material is done at the end (due to being common to both conditions)
			
			mtlBindParent = geoInstance;
		} else {		
			//Append the bone tree.
			//assembleBoneTree(topLevelGeoNode, bone);
			Node bone = geo.thisBone;
			
			//Reference a controller.
			//controller.setAttribute("id", name + id + "-skin");
			Element controller_ref = DOCUMENT.createElement("instance_controller");
			controller_ref.setAttribute("url", "#" + name + id + "-skin");
			Element skeleton = DOCUMENT.createElement("skeleton");
			Text mainBoneSkele = DOCUMENT.createTextNode("#" + bone.name);
			skeleton.appendChild(mainBoneSkele);
			controller_ref.appendChild(skeleton);
			
			topLevelGeoNode = assembleBoneTree(null, bone, "#" + name + id + "-mesh");
			//assembleBoneTree(topLevelGeoNode, bone, "#" + name + id + "-mesh");
			//Element boneTree = assembleBoneTree(null, bone, "#" + name + id + "-mesh");
			
			mtlBindParent = controller_ref;
		}
		
		base_scene.appendChild(topLevelGeoNode);
		
		Element mtlBinding = DOCUMENT.createElement("bind_material");
		mtlBindParent.appendChild(mtlBinding);
		Element technique = DOCUMENT.createElement("technique_common");
		mtlBinding.appendChild(technique);
		
		Element mtlInstance = DOCUMENT.createElement("instance_material");
		mtlInstance.setAttribute("symbol", name + id + "-material");
		mtlInstance.setAttribute("target", "#" + name + id + "-material");
		technique.appendChild(mtlInstance);
		
		//topLevelGeoNode.appendChild(mtlBindParent); //WRONG! Turns out the line below was right, it just had to be in its own node.
		Element secondaryGeoNode = DOCUMENT.createElement("node");
		secondaryGeoNode.setAttribute("id", name + id);
		secondaryGeoNode.setAttribute("name", name + id);
		secondaryGeoNode.setAttribute("type", "NODE");
		secondaryGeoNode.appendChild(mtlBindParent);
		//base_scene.appendChild(mtlBindParent);
		base_scene.appendChild(secondaryGeoNode);
	}
	
	private Matrix4f extractMatrixFromTransform(Transform3D in) {
		Matrix4f matrix = new Matrix4f();
		
		if (in != null) {
			int mainType = in.getType();
			if (mainType == 0 || mainType == 1 || mainType == 2) {
				//Identity, Rigid (vec3/quat), Uniform (vec3/quat/scale)
				matrix = new Matrix4f().setToTransform(in.extractTranslation(), in.extractRotation());
			} else {
				//affine, general
				matrix = in.getMatrix();
			}
		}
		
		return matrix;
	}
	
	public Quaternion swapRotation(Quaternion in) {
		Vector3f angles = in.toAngles();
		return in.fromAnglesZXY(angles.y, angles.x, angles.z);
	}
	
	private String getTranslation(Node bone) {
		Transform3D in = bone.invRefTransform;
		if (in != null) {
			int mainType = in.getType();
			if (mainType == 0 || mainType == 1 || mainType == 2) {
				//Identity, Rigid (vec3/quat), Uniform (vec3/quat/scale)
				Vector3f vec = in.extractTranslation();
				return vec.encodeToString().replace(", ", " ");
			}
		}
		return "0 0 0";
	}
	
	private String getRotation(Node bone) {
		Transform3D in = bone.invRefTransform;
		if (in != null) {
			int mainType = in.getType();
			if (mainType == 0 || mainType == 1 || mainType == 2) {
				//Identity, Rigid (vec3/quat), Uniform (vec3/quat/scale)
				Quaternion rot = in.extractRotation();
				return rot.encodeToString().replace(", ", " ");
			}
		}
		return "0 0 0 1";
	}
	
	private String getMatrixAsString(Node bone) {
		if (bone == null) return IDENTITY_MATRIX;
		Transform3D invTransformation = bone.invRefTransform;
		//Transform3D transformation = bone.transform;
		
		Matrix4f matrix = extractMatrixFromTransform(invTransformation);
		
		return matrix.encodeToString().replace(", ", " ");
	}
	
	public void appendNewMaterial(String name, int id) {
		////////////////////////////
		//PART 1: MATERIAL HEADING//
		////////////////////////////
		Element materialId = DOCUMENT.createElement("material");
		materialId.setAttribute("id", name + id +"-material");
		materialId.setAttribute("name", name + id);
		
		Element mtlEffect = DOCUMENT.createElement("effect");
		mtlEffect.setAttribute("id", name + id + "-effect");
		
		Element mtlProfile = DOCUMENT.createElement("profile_COMMON");
		Element mtlTechnique = DOCUMENT.createElement("technique");
		Element shadeType = DOCUMENT.createElement("phong");
		
		mtlTechnique.setAttribute("sid", "common");
		
		mtlEffect.appendChild(mtlProfile);
		mtlProfile.appendChild(mtlTechnique);
		mtlTechnique.appendChild(shadeType);
		
		/////////////////////////
		//PART 2: MATERIAL DATA//
		/////////////////////////
		
		Element emission = DOCUMENT.createElement("emission");
		Element ambient = DOCUMENT.createElement("ambient");
		Element diffuse = DOCUMENT.createElement("diffuse");
		Element specular = DOCUMENT.createElement("specular");
		Element shininess = DOCUMENT.createElement("shininess");
		Element refraction = DOCUMENT.createElement("index_of_refraction");
		shadeType.appendChild(emission);
		shadeType.appendChild(ambient);
		shadeType.appendChild(diffuse);
		shadeType.appendChild(specular);
		shadeType.appendChild(shininess);
		shadeType.appendChild(refraction);
		
		Element color_Emission = DOCUMENT.createElement("color");
		color_Emission.setAttribute("sid", "emission");
		emission.appendChild(color_Emission);
		Text value_Emission = DOCUMENT.createTextNode("0 0 0 1");
		color_Emission.appendChild(value_Emission);
		
		
		Element color_Ambient = DOCUMENT.createElement("color");
		color_Ambient.setAttribute("sid", "ambient");
		ambient.appendChild(color_Ambient);
		Text value_Ambient = DOCUMENT.createTextNode("0 0 0 1");
		color_Ambient.appendChild(value_Ambient);
		
		
		Element color_Diffuse = DOCUMENT.createElement("color");
		color_Diffuse.setAttribute("sid", "diffuse");
		diffuse.appendChild(color_Diffuse);
		Text value_Diffuse = DOCUMENT.createTextNode("1 1 1 1");
		color_Diffuse.appendChild(value_Diffuse);
		
		
		Element color_Specular = DOCUMENT.createElement("color");
		color_Specular.setAttribute("sid", "specular");
		specular.appendChild(color_Specular);
		Text Value_Specular = DOCUMENT.createTextNode("0 0 0 1");
		color_Specular.appendChild(Value_Specular);
		
		
		Element number_Shininess = DOCUMENT.createElement("color");
		number_Shininess.setAttribute("sid", "shininess");
		shininess.appendChild(number_Shininess);
		Text value_Shininess = DOCUMENT.createTextNode("0");
		number_Shininess.appendChild(value_Shininess);
		
		
		Element float_Refraction = DOCUMENT.createElement("float");
		float_Refraction.setAttribute("sid", "index_of_refraction");
		refraction.appendChild(float_Refraction);
		Text value_Refraction = DOCUMENT.createTextNode("1");
		float_Refraction.appendChild(value_Refraction);
		
		Element instanceEffect = DOCUMENT.createElement("instance_effect");
		instanceEffect.setAttribute("url", "#" + name + id + "-effect");
		materialId.appendChild(instanceEffect);
		
		//And finally append
		lib_materials.appendChild(materialId);
		lib_effects.appendChild(mtlEffect);
	}
	/*
	private Element assembleBoneTree(Element parentElement, Node node) {
		return assembleBoneTree(parentElement, node, null);
	}
	*/
	private Element assembleBoneTree(Element parentElement, Node node, String sid) {
		//System.out.println(node.toString());
		Element mdlNode = DOCUMENT.createElement("node");
		String name = node.name.replace(" ", "_");
		mdlNode.setAttribute("id", name);
		mdlNode.setAttribute("name", name);
		if (parentElement != null) mdlNode.setAttribute("sid", name);
		//mdlNode.setAttribute("sid", sid);
		if (parentElement != null) {
			mdlNode.setAttribute("type", "JOINT");
		} else {
			mdlNode.setAttribute("type", "NODE");
		}
		
		Element matrixNode = DOCUMENT.createElement("matrix");
		matrixNode.setAttribute("sid", "transform");
		Text matrixText = DOCUMENT.createTextNode(IDENTITY_MATRIX); //was transformation
		matrixNode.appendChild(matrixText);
		mdlNode.appendChild(matrixNode);
		
		/*
		Element posNode = DOCUMENT.createElement("translate");
		Element rotNode = DOCUMENT.createElement("rotate");
		Text posText = DOCUMENT.createTextNode(getTranslation(node));
		Text rotText = DOCUMENT.createTextNode(getRotation(node));
		posNode.appendChild(posText);
		rotNode.appendChild(rotText);
		mdlNode.appendChild(posNode);
		mdlNode.appendChild(rotNode);
		*/
		if (parentElement != null) parentElement.appendChild(mdlNode);
		
		//Now, go through this node's sub-objects.
		for (Node sub : node.children) {
			assembleBoneTree(mdlNode, sub, sid); //Computer: Y U DO DIS RECURSION TO ME ლ(ಠ益ಠლ)
		}
		return mdlNode;
	}
	
	private void process() {
		String name = processedModel.name;
		for (int i = 0; i < processedModel.geometry.size(); i++) {
			appendNewGeometry(processedModel.geometry.get(i), name, i);
			appendNewBoneGeometry(processedModel.geometry.get(i), name, i);
			appendNewMaterial("Material", i);
		}
	}
	
	public void createDAE() {
		try {
			//Finish up the whole thing.
			ROOT.appendChild(lib_images);
			ROOT.appendChild(lib_effects);
			ROOT.appendChild(lib_materials);
			ROOT.appendChild(lib_geometry);
			ROOT.appendChild(lib_control);
			ROOT.appendChild(lib_nodes);
			ROOT.appendChild(lib_visual_scene);
			lib_visual_scene.appendChild(base_scene);
			
			Element scene = DOCUMENT.createElement("scene");
			Element visScene = DOCUMENT.createElement("instance_visual_scene");
			visScene.setAttribute("url", "#Scene");
			scene.appendChild(visScene);
			ROOT.appendChild(scene);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			
			DOMSource source = new DOMSource(DOCUMENT);
			StreamResult result = new StreamResult(new StringWriter());
			
			transformer.transform(source, result);
			
			String xmlString = result.getWriter().toString();
	    	
	    	FileWriter fileWriter;
	    	try {
	    		fileWriter = new FileWriter(OUTPUT_FILE);
	            fileWriter.write(xmlString);
	            fileWriter.flush();
	            fileWriter.close();
	            Logger.AppendLn("Model has been successfully exported!");
	    	} catch (IOException e) {
	    		Logger.AppendLn("A critical error has occurred when attempting to save the model as a DAE. Location: File saving, writing data to file. Stack trace:", e.getStackTrace());
	        }
		} catch (TransformerException e) {
			Logger.AppendLn("A critical error has occurred when attempting to save the model as a DAE. Location: File saving, transforming data. Stack trace:", e.getStackTrace());
		}
	}
}
