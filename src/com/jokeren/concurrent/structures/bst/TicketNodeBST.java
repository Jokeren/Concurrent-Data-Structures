package com.jokeren.concurrent.structures.bst;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TicketNodeBST<K, V> implements Map<K, V> {
	LazyNode root;

	public TicketNodeBST() {
		root = new LazyNode(Integer.MAX_VALUE, null, null, null,
				NodeType.INTERNAL, new TicketLock());
		LazyNode left = new LazyNode(Integer.MAX_VALUE - 1, null, null, null,
				NodeType.LEAF, new TicketLock());
		LazyNode right = new LazyNode(Integer.MAX_VALUE, null, null, null,
				NodeType.LEAF, new TicketLock());
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
		int pversion = 0;
		int ppversion = 0;

		while (curr.type != NodeType.LEAF) {
			ppred = pred;
			ppversion = pversion;
			pRight = right;
			pversion = curr.getVersion();
			pred = curr;
			if (key < curr.key) {
				curr = curr.left;
				right = false;
			} else {
				curr = curr.right;
				right = true;
			}
		}
		return new LazyResult(ppred, pred, curr, pRight, right, ppversion, pversion);
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
			int pversion = result.pverison;

			if (curr.key == key.hashCode()) {
				return null;
			}
			
			if (!pred.tryLock(pversion)) {
				continue;
			}

			LazyNode newLazyNode = new LazyNode(key.hashCode(), value,
					null, null, NodeType.LEAF, new TicketLock());
			LazyNode newParent = new LazyNode(0, null, null, null,
					NodeType.INTERNAL, new TicketLock());

			if (key.hashCode() < curr.key) {
				newParent.key = curr.key;
				newParent.left = newLazyNode;
				newParent.right = curr;
			} else {
				newParent.key = newLazyNode.key;
				newParent.left = curr;
				newParent.right = newLazyNode;
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
			int ppversion = result.ppversion;
			int pversion = result.pverison;

			if (curr.key != key.hashCode()) {
				return null;
			}

			if (!ppred.tryLock(ppversion)) {
				continue;
			} else {
				if (!pred.tryLock(pversion)) {
					ppred.revert();
					continue;
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
