package xandragon.converter.model;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import com.threerings.opengl.model.config.ArticulatedConfig.MeshNode;
import com.threerings.opengl.model.config.ArticulatedConfig.Node;
import com.threerings.opengl.renderer.config.ClientArrayConfig;

public class Geometry {
	
	public String[] boneList = null;
	public Node thisBone = null;
	public ArrayList<Float> vertices = new ArrayList<Float>();
	public ArrayList<Float> normals = new ArrayList<Float>();
	public ArrayList<Float> uvs = new ArrayList<Float>();
	public ArrayList<Float> vertexAttributes = new ArrayList<Float>();
	public ArrayList<Float> boneWeights = new ArrayList<Float>();
	public ArrayList<Integer> boneIndices = new ArrayList<Integer>();
	public ArrayList<Short> indices = new ArrayList<Short>();
	public HashMap<String, Node> bonesByName = new HashMap<String, Node>();
	public Model parentModel = null;
	public boolean isRigged = false;
	
	//Create this for a MeshNode.
	public Geometry(ClientArrayConfig _vertices, ClientArrayConfig _normals, ClientArrayConfig _uvs, ClientArrayConfig[] _vertexAttributes, ShortBuffer _indices, String[] bones, Node bone) {
		this(_vertices, _normals, _uvs, _vertexAttributes, _indices, bones);
		if (bone != null) {
			isRigged = bone instanceof MeshNode;
			thisBone = bone;
			thisBone.name = thisBone.name.replace(" ", "_");
		}
	}
	
	public Geometry(ClientArrayConfig _vertices, ClientArrayConfig _normals, ClientArrayConfig _uvs, ClientArrayConfig[] _vertexAttributes, ShortBuffer _indices, String[] bones) {
		//boneList = bones;
		if (bones != null) {
			boneList = new String[bones.length];
			for (int index = 0; index < boneList.length; index++) {
				boneList[index] = bones[index].replace(" ", "_");
			}
		}
		
		int stride, offset;
		
		stride = _vertices.stride / 4;
		offset = _vertices.offset / 4;
		for (int i = offset; i < _vertices.floatArray.limit(); i+=stride) {
			float x = _vertices.floatArray.get(i);
			float y = _vertices.floatArray.get(i + 1);
			float z = _vertices.floatArray.get(i + 2);
			vertices.add(x);
			vertices.add(y);
			vertices.add(z);
		}
		
		stride = _normals.stride / 4;
		offset = _normals.offset / 4;
		for (int i = offset; i < _normals.floatArray.limit(); i+=stride) {
			float x = _normals.floatArray.get(i);
			float y = _normals.floatArray.get(i + 1);
			float z = _normals.floatArray.get(i + 2);
			normals.add(x);
			normals.add(y);
			normals.add(z);
		}
		
		if (_uvs != null) {
			stride = _uvs.stride / 4;
			offset = _uvs.offset / 4;
			for (int i = offset; i < _uvs.floatArray.limit(); i+=stride) {
				float u = _uvs.floatArray.get(i);
				float v = _uvs.floatArray.get(i + 1);
				uvs.add(u);
				uvs.add(v);
			}
		}
		
		if (_vertexAttributes != null && bones != null) {
			//Bones are responsible for a nasty issue, so we'll do our best to resolve this.
			for (ClientArrayConfig va : _vertexAttributes) {
				String strDump = va.toString();
				String name = "null";
				int s = strDump.indexOf("name=");
				if (s != -1) {
					int e = strDump.indexOf(",", s);
					if (e == -1)
						e = strDump.indexOf("]", s);
					
					name = strDump.substring(s + 5, e);
				}
				
				stride = va.stride / 4;
				offset = va.offset / 4;
				for (int i = offset; i < va.floatArray.limit(); i+=stride) {
					float x = va.floatArray.get(i);
					float y = va.floatArray.get(i + 1);
					float z = va.floatArray.get(i + 2);
					float w = va.floatArray.get(i + 3);
					vertexAttributes.add(x);
					vertexAttributes.add(y);
					vertexAttributes.add(z);
					vertexAttributes.add(w);
					if (name.matches("boneIndices")) {
						boneIndices.add((int) x);
						boneIndices.add((int) y);
						boneIndices.add((int) z);
						boneIndices.add((int) w);
					} else if (name.matches("boneWeights")) {
						boneWeights.add(x);
						boneWeights.add(y);
						boneWeights.add(z);
						boneWeights.add(w);
					}
				}
			}
		}
		
		for (int i = 0; i < _indices.limit(); i++) {
			indices.add(_indices.get());
		}
		
		if (thisBone != null) {
			iterate(thisBone);
		}
	}
	
	private void iterate(Node node) {
		bonesByName.put(node.name.replace(" ", "_"), node);
		for (Node child : node.children) {
			iterate(child);
		}
	}
	
	public String createVertexList() {
		String list = "";
		for (Float f : vertices) {
			list = list + f.floatValue() + " ";
		}
		list = list.trim();
		return list;
	}
	
	public String createNormalList() {
		String list = "";
		for (Float f : normals) {
			list = list + f.floatValue() + " ";
		}
		list = list.trim();
		return list;
	}
	
	public String createUVList() {
		String list = "";
		for (Float f : uvs) {
			list = list + f.floatValue() + " ";
		}
		list = list.trim();
		return list;
	}
	
	public String createIndexList() {
		String list = "";
		for (Short f : indices) {
			list = list + f.shortValue() + " ";
		}
		list = list.trim();
		return list;
	}
}
