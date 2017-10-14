package com.jokeren.concurrent.structures.bst;


public class Node {
	volatile public int key;
	volatile public Object value;
	volatile public NodeType type;
}
