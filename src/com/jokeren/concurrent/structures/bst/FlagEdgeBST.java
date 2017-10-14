package com.jokeren.concurrent.structures.bst;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class FlagEdgeBST<K, V> implements Map<K, V> {
	LazyNode root;

	public FlagEdgeBST() {
		root = new LazyNode(Integer.MAX_VALUE, null, null, null,
				NodeType.INTERNAL, new FlagLock());
		LazyNode left = new LazyNode(Integer.MAX_VALUE - 1, null, null, null,
				NodeType.LEAF, new FlagLock());
		LazyNode right = new LazyNode(Integer.MAX_VALUE, null, null, null,
				NodeType.LEAF, new FlagLock());
		root.left = left;
		root.right = right;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	private LazyResult find(int key) {
		LazyNode ppred = null;
		LazyNode pred = null;
		LazyNode curr = root;
		boolean right = false;
		boolean pRight = false;

		while (curr.type != NodeType.LEAF) {
			ppred = pred;
			pRight = right;
			pred = curr;
			if (key < curr.key) {
				curr = curr.left;
				right = false;
			} else {
				curr = curr.right;
				right = true;
			}
		}
		return new LazyResult(ppred, pred, curr, pRight, right);
	}

	@Override
	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		int findKey = find(key.hashCode()).curr.key;

		if (findKey == key.hashCode()) {
			return true;
		}

		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<K> keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V put(K key, V value) {
		// TODO Auto-generated method stub
		// synchronized (this) {
		while (true) {
			LazyResult result = find(key.hashCode());
			LazyNode curr = (LazyNode) result.curr;
			LazyNode pred = (LazyNode) result.pred;
			boolean right = result.right;

			if (curr.key == key.hashCode()) {
				return null;
			}

			if (!curr.tryLock()) {
				continue;
			}

			if (pred.lock.getFlag() == true) {
				curr.release();
				continue;
			}

			if (right) {
				if (pred.right != curr) {
					curr.release();
					continue;
				}
			} else {
				if (pred.left != curr) {
					curr.release();
					continue;
				}
			}

			LazyNode newNode = new LazyNode(key.hashCode(), value, null, null,
					NodeType.LEAF, new FlagLock());
			LazyNode newParent = new LazyNode(0, null, null, null,
					NodeType.INTERNAL, new FlagLock());

			if (key.hashCode() < curr.key) {
				newParent.key = curr.key;
				newParent.left = newNode;
				newParent.right = curr;
			} else {
				newParent.key = newNode.key;
				newParent.left = curr;
				newParent.right = newNode;
			}

			// assert(curr.lock.getFlag() == true);

			if (right) {
				// assert(pred.right == curr);
				pred.right = newParent;
			} else {
				// assert(pred.left == curr);
				pred.left = newParent;
			}

			curr.release();

			return value;
		}
		// }
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub

	}

	@Override
	public V remove(Object key) {
		// synchronized (this) {
		while (true) {
			LazyResult result = find(key.hashCode());
			LazyNode curr = (LazyNode) result.curr;
			LazyNode pred = (LazyNode) result.pred;
			LazyNode ppred = (LazyNode) result.ppred;
			boolean pRight = result.pRight;
			boolean right = result.right;

			if (curr.key != key.hashCode()) {
				return null;
			}

			LazyNode node;
			if (!pred.tryLock()) {
				continue;
			} else {
				if (ppred.lock.getFlag() == true) {
					pred.release();
					continue;
				}

				if ((pRight && ppred.right != pred)
						|| (!pRight && ppred.left != pred)) {
					pred.release();
					continue;
				}

				if (!curr.tryLock()) {
					pred.release();
					continue;
				} else {
					if ((right && pred.right != curr)
							|| (!right && pred.left != curr)) {
						curr.release();
						pred.release();
						continue;
					}
					
					if (right)
						node = pred.left;
					else
						node = pred.right;

					while (true) {
							while (!node.tryLock()) {
								if (right)
									node = pred.left;
								else
									node = pred.right;
							};
							
							if ((right && pred.left != node)
									|| (!right && pred.right != node)) {
								node.release();
								if (right)
									node = pred.left;
								else
									node = pred.right;
							} else {
								break;
							}
					}
				}
			}

			if (pRight) {
				ppred.right = node;
				// assert(ppred.right == node);
			} else {
				ppred.left = node;
				// assert(ppred.left == node);
			}

			node.release();

			return (V) curr.value;
		}

		// }
	}

	private int numberOfKeys(LazyNode node) {
		if (node.type == NodeType.LEAF) {
			if (node.value == null)
				return 0;
			else
				return 1;
		} else {
			int left = numberOfKeys(node.left);
			int right = numberOfKeys(node.right);
			return left + right;
		}
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return numberOfKeys(root);
	}

	@Override
	public Collection<V> values() {
		// TODO Auto-generated method stub
		return null;
	}

}
