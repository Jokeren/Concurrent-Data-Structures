package com.jokeren.concurrent.structures.bst;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

public class TicketLock implements LockAdapter {
	final AtomicInteger version = new AtomicInteger(0);
	final AtomicInteger ticket = new AtomicInteger(0);
	
	public boolean tryLock(int pversion)
	{
		int v = version.get();
		int t = ticket.get();
		if (v != t) {
			return false;
		}
		
		return ticket.compareAndSet(pversion, pversion + 1);
	}
	
	public void revert()
	{
		ticket.decrementAndGet();
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
		return false;
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unlock() {
		// TODO Auto-generated method stub
		version.incrementAndGet();
	}

	@Override
	public boolean getMarked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setMarked(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getFlag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFlag(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getVersion() {
		// TODO Auto-generated method stub
		return version.get();
	}
}
