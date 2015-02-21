package com.sabev.events;

public interface IEventHandler<D> {
	
	public void onEvent(D data);

}
