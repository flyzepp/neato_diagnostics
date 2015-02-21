package com.sabev.events;


public interface IEventSource<D> {
	
	public HandlerRegistration addEventHandler(IEventHandler<D> handler);
		
}
