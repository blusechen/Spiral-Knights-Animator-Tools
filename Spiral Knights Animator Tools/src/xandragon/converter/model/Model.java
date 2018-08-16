package xandragon.converter.model;

import java.nio.ShortBuffer;
import java.util.ArrayList;

import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.model.config.ArticulatedConfig.MeshNode;
import com.threerings.opengl.model.config.ArticulatedConfig.Node;
import com.threerings.opengl.model.config.ArticulatedConfig.NodeTransform;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.renderer.config.ClientArrayConfig;

import xandragon.converter.CommonConfig;
import xandragon.util.AppendableArray;

public class Model {
	
	public String name = "model";
	public ArrayList<Geometry> geometry = new ArrayList<Geometry>();
	public ArrayList<BoneDepth> bonelist = new ArrayList<BoneDepth>();
	public ArrayList<String> boneNames = new ArrayList<String>();
	//public NodeTransform[] nodeTransforms;
	public AppendableArray<NodeTransform> nodeTransforms;
	public Node rootBone = null;
	public int maxBoneDepth = 0;
	
	public static Model createNewModel(CommonConfig modelConfig) {
		if (modelConfig == null) {
			return null;
		}
		return new Model(modelConfig.visibleMeshes, modelConfig.attachments, modelConfig.boneData, modelConfig.nodeTransforms);
	}
	
	protected Model(AppendableArray<VisibleMesh> meshes, AppendableArray<VisibleMesh> attachments, Node bones, AppendableArray<NodeTransform> transforms) {//(VisibleMesh[] meshes, VisibleMesh[] attachments, Node bones, NodeTransform[] nodeTrs) {
		if (meshes != null) {
			for (VisibleMesh geometryCfg : meshes) {
				handleGeometry(geometryCfg, bones);
			}
		}
		
		if (attachments != null) {
			for (VisibleMesh geometryCfg : attachments) {
				handleGeometry(geometryCfg, bones);
			}
		}
		
		if (bones != null) {
			//Note to self: "bones" is just the root bone. Not a set of bones.
			rootBone = bones;
			//The two lines used a "depth" value that was local. It was 0. And useless.
			bonelist.add(new BoneDepth(0, bones));
			search(bones, 0);
			
			//From here we should have an entire hierarchy of bones.
			//So now we'll just append node based geometry.
			for (BoneDepth boneDepth : bonelist) {
				Node bone = boneDepth.bone;
				boneNames.add(bone.name.replace(" ", "_"));
				if (bone instanceof MeshNode) {
					MeshNode meshBone = (MeshNode) bone;
					handleGeometry(meshBone.visible, meshBone);
					//Edit: We had SERIOUS issues involving cutting out bones if they didn't have a mesh, as it threw off rigging.
					//By giving geometry a reference to the parent model we can IDEALLY resolve that.
				}
			}
		}
		
		if (transforms != null) {
			nodeTransforms = transforms;
		}
	}
	
	protected void handleGeometry(VisibleMesh geometryCfg) {
		handleGeometry(geometryCfg, null);
	}
	
	/**
	 * Handle geometry for a MeshBone
	 * @param geometryCfg the geometry
	 * @param bone the bone
	 */
	protected void handleGeometry(VisibleMesh geometryCfg, Node bone) {
		if (geometryCfg == null) return;
		GeometryConfig.Stored geo = (GeometryConfig.Stored) geometryCfg.geometry;
		GeometryConfig.IndexedStored idgeo = new GeometryConfig.IndexedStored();
		GeometryConfig.SkinnedIndexedStored sidgeo = new GeometryConfig.SkinnedIndexedStored();
		if (geometryCfg.geometry instanceof GeometryConfig.IndexedStored) {
			idgeo = (GeometryConfig.IndexedStored) geometryCfg.geometry;
		}
		if (geometryCfg.geometry instanceof GeometryConfig.SkinnedIndexedStored) {
			sidgeo = (GeometryConfig.SkinnedIndexedStored) geometryCfg.geometry;
		}
		
		ClientArrayConfig vertices = geo.vertexArray;
		ClientArrayConfig normals = geo.normalArray;
		ClientArrayConfig uvs = geo.texCoordArrays != null ? geo.texCoordArrays[0] : (idgeo.texCoordArrays != null ? idgeo.texCoordArrays[0] : (sidgeo.texCoordArrays != null ? sidgeo.texCoordArrays[0] : null));
		ClientArrayConfig[] vertexAttributes = geo.vertexAttribArrays != null ? geo.vertexAttribArrays : (idgeo.vertexAttribArrays != null ? idgeo.vertexAttribArrays : (sidgeo.vertexAttribArrays != null ? sidgeo.vertexAttribArrays : null));
		ShortBuffer indices = idgeo.indices != null ? idgeo.indices : sidgeo.indices;
		
		Geometry geometryObj = new Geometry(vertices, normals, uvs, vertexAttributes, indices, sidgeo != null ? sidgeo.bones : null, bone);
		geometry.add(geometryObj);
		geometryObj.parentModel = this;
	}
	
	protected void search(Node b, int d) {
		maxBoneDepth = Math.max(maxBoneDepth, d);
		for (Node bone : b.children) {
			bonelist.add(new BoneDepth(d + 1, bone));
			search(bone, d + 1);
		}
	}
}
