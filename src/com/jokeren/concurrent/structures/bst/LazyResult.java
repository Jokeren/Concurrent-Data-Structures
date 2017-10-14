package com.jokeren.concurrent.structures.bst;

public class LazyResult extends SearchResult {
	boolean pRight, right;
	int pverison, ppversion;

	public LazyResult(Node ppred, Node pred, Node curr) {
		super(ppred, pred, curr);
		// TODO Auto-generated constructor stub
	}

	public LazyResult(Node ppred, Node pred, Node curr, boolean pRight,
			boolean right) {
		super(ppred, pred, curr);
		// TODO Auto-generated constructor stub
		this.pRight = pRight;
		this.right = right;
	}

	public LazyResult(Node ppred, Node pred, Node curr, boolean pRight,
			boolean right, int ppversion, int pversion) {
		super(ppred, pred, curr);
		this.pRight = pRight;
		this.right = right;
		this.ppversion = ppversion;
		this.pverison = pversion;
	}

}
