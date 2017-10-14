package com.jokeren.concurrent.structures.bst;

public class LazyNode extends Node {
	volatile public LazyNode left;
	volatile public LazyNode right;
	final LockAdapter lock;
	
	public LazyNode(int key, Object value, LazyNode left, LazyNode right, NodeType type, LockAdapter lock) {
		this.key = key;
		this.value = value;
		this.left = left;
		this.right = right;
		this.type = type;
		this.lock = lock;
	}

	public boolean tryLock() {
		return lock.tryLock();
	}
	
	public boolean tryLock(int version) {
		return lock.tryLock(version);
	}
	
	public void release() {
		lock.unlock();
	}
	
	public int getVersion() {
		return lock.getVersion();
	}
	
	public void revert() {
		lock.revert();
	}
//	public void revert() {
//		lock.revert();
//	}
}
