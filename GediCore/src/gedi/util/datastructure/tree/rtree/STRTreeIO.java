/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.util.datastructure.tree.rtree;

import gedi.util.FileUtils;
import gedi.util.io.randomaccess.PageFileWriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Queue;


/**
 * File format:
 * 
 * STR[int][int] (node capacity,level)
 * level by level, how many children, bfs encoded as shorts
 * leaves: 4 doubles for envelope, object serialized
 * 
 * node is bounds+children+level or data instead of children
 * class either com.vividsolutions.jts.index.strtree.STRtree$STRtreeNode oder com.vividsolutions.jts.index.strtree.ItemBoundable
 * 
 * @author erhard
 *
 */
public class STRTreeIO {

	
	/**
	 * Does not close the file!
	 * @param tree
	 * @param out
	 * @throws IOException
	 */
	public static <T extends SpatialObject> void write(STRTree<T> tree, PageFileWriter out) throws IOException {
		// header
		out.putChar('S').putChar('T').putChar('R');//"STR");
		out.putInt(tree.computeDepth());
		
		// tree structure
		Queue<SpatialObject> bfs = new LinkedList<SpatialObject>();
		bfs.add(tree.root);
		
		while (!bfs.isEmpty()) {
			SpatialObject n = bfs.poll();
			
			if (n instanceof STRInnerNode) {
				// inner node
				STRInnerNode node = (STRInnerNode) n;
				out.putShort((short) node.getChildren().size());
				for (SpatialObject ch : node.getChildren()) {
					bfs.add(ch);
				}
				
			} else {
				FileUtils.write(out,n);
			}
		}
		
	}
	
//	
//	public static <T extends SpatialObject> STRTree<T> read(BufferedRandomAccessFile in, Class<T> cls) throws IOException {
//		if (in.readChar()!='S' || in.readChar()!='T' || in.readChar()!='R') 
//			throw new IOException("Not a valid index file!");
//		
//		int rootLevel = in.readInt();
//		STRInnerNode root = new STRInnerNode();
//		
//		Queue<Object> bfs = new LinkedList<>();
//		bfs.add(root);
//		
//		HashMap<STRInnerNode, Integer> tolevel = new HashMap<>();
//		tolevel.put(root,rootLevel);
//		
//		while (!bfs.isEmpty()) {
//			Object n = bfs.poll();
//			if (n instanceof STRInnerNode) {
//				STRInnerNode node = (STRInnerNode) n;
//				int children = in.readShort();
//				for (int i=0; i<children; i++) {
//					SpatialObject child = tolevel.get(node)>0?new STRInnerNode():new ItemBoundable(null, null);
//					node.addChildBoundable(child);
//					bfs.add(child);
//				}
//			} else {
//				ItemBoundable node = (ItemBoundable) n;
//				int size = in.readByte();
//				if (size>=0) {
//					node.bounds = new Envelope(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble());
//				} else {
//					double x = in.readDouble();
//					double y = in.readDouble();
//					node.bounds = new Envelope(x,x,y,y);
//					size = -1-size;
//				}
//				long[] d = new long[size];
//				for (int i=0; i<size; i++)
//					d[i] = in.readLong();
//				node.item = d;
//			}
//			
//		}
//		
//		
//		// bottom up traversal to compute bounds
//		Stack<STRtreeNode> dfs = new Stack<STRtreeNode>();
//		dfs.push((STRtreeNode) tree.root);
//		HashMap<STRtreeNode,STRtreeNode> lastChildToParent = new HashMap<STRtreeNode, STRtreeNode>();
//		while (!dfs.isEmpty()) {
//			STRtreeNode node = dfs.pop();
//			
//			if (lastChildToParent.containsKey(node))
//				lastChildToParent.get(node).computeBounds();
//			
//			if (node.getChildBoundables().get(0) instanceof STRtreeNode) {
//				STRtreeNode last = null;
//				for (Object o : node.getChildBoundables()) {
//					last = (STRtreeNode) o;
//					dfs.push(last);
//				}
//				lastChildToParent.put(last, node);
//			} else {
//				node.computeBounds();
//			}
//			
//		}
//		
//		tree.built = true;
//		
//		return tree;
//		
//	}
	
	
}
