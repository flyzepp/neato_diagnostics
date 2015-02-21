package com.sabev.events;

public interface IEventSignal<D> {
	
	public void signal(D data);

}
