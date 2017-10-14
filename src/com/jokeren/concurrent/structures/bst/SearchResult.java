package com.jokeren.concurrent.structures.bst;

public class SearchResult {
	public Node ppred, pred, curr;

	public SearchResult(Node ppred, Node pred, Node curr) {
		super();
		this.ppred = ppred;
		this.pred = pred;
		this.curr = curr;
	}
}
