package com.jokeren.concurrent.structures.bst;

import java.util.concurrent.locks.Lock;

public interface LockAdapter extends Lock {
	public boolean tryLock(int pversion);
	
	public int getVersion();
	
	public void revert();
	
	public boolean getMarked();
	
	public void setMarked(boolean flag);
	
	public boolean getFlag();
	
	public void setFlag(boolean flag);
	
}
