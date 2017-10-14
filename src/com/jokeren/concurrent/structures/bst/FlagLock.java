package com.jokeren.concurrent.structures.bst;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;

public class FlagLock implements LockAdapter {
	final AtomicBoolean modify = new AtomicBoolean(false);
	final AtomicBoolean marked = new AtomicBoolean(false);

	@Override
	public void unlock() {
		modify.set(false);
	}

	@Override
	public void lock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Condition newCondition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean tryLock() {
		// TODO Auto-generated method stub
		return modify.compareAndSet(false, true);
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(int pversion) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void revert() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getMarked() {
		// TODO Auto-generated method stub
		return marked.get();
	}

	@Override
	public void setMarked(boolean flag) {
		// TODO Auto-generated method stub
		marked.set(flag);
	}

	@Override
	public boolean getFlag() {
		// TODO Auto-generated method stub
		return modify.get();
	}

	@Override
	public void setFlag(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getVersion() {
		// TODO Auto-generated method stub
		return 0;
	}
}
