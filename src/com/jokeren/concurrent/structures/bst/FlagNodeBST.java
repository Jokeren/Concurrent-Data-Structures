package com.jokeren.concurrent.structures.bst;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class FlagNodeBST<K, V> implements Map<K, V> {
	LazyNode root;

	public FlagNodeBST() {
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

			if (!pred.tryLock()) {
				continue;
			}

			if (right) {
				if (pred.right != curr) {
					pred.release();
					continue;
				}
			} else {
				if (pred.left != curr) {
					pred.release();
					continue;
				}
			}

			LazyNode newLazyLazyNode = new LazyNode(key.hashCode(), value,
					null, null, NodeType.LEAF, new FlagLock());
			LazyNode newParent = new LazyNode(0, null, null, null,
					NodeType.INTERNAL, new FlagLock());

			if (key.hashCode() < curr.key) {
				newParent.key = curr.key;
				newParent.left = newLazyLazyNode;
				newParent.right = curr;
			} else {
				newParent.key = newLazyLazyNode.key;
				newParent.left = curr;
				newParent.right = newLazyLazyNode;
			}

			if (right) {
				pred.right = newParent;
			} else {
				pred.left = newParent;
			}

			pred.release();

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

			if (!ppred.tryLock()) {
				continue;
			} else {
				if ((pRight && ppred.right != pred)
						|| (!pRight && ppred.left != pred)) {
					ppred.release();
					continue;
				}

				if (!pred.tryLock()) {
					ppred.release();
					continue;
				} else {
					if ((right && pred.right != curr)
							|| (!right && pred.left != curr)) {
						pred.release();
						ppred.release();
						continue;
					}
				}
			}

			if (pRight) {
				if (right) {
					ppred.right = pred.left;
				} else {
					ppred.right = pred.right;
				}
			} else {
				if (right) {
					ppred.left = pred.left;
				} else {
					ppred.left = pred.right;
				}
			}

			ppred.release();

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
