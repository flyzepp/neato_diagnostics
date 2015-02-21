package com.sabev.events;

import java.util.LinkedHashSet;

public class Event<D> implements IEventSource<D>, IEventSignal<D> {
	private LinkedHashSet<IEventHandler<D>> handlers = new LinkedHashSet<IEventHandler<D>>();
	private HandlerRegistration wrappingHandlerRegistraton = new HandlerRegistration() {
		@Override
		public void removeHandler() {
			//do nothing this is just to escape the usage of if later on
		}
	};
	
	@Override
	public HandlerRegistration addEventHandler(final IEventHandler<D> handler) {
		handlers.add(handler);
		return new HandlerRegistration (){

			@Override
			public void removeHandler() {
				handlers.remove(handler);
			}
		};
	}
	
	public void removeEventHandler(IEventHandler<D> handler) {
		handlers.remove(handler);
	}

	@Override
	public void signal(D data) {
		//copy the handlers before delivering the event
		//this is important because handlers might themselves
		//register new handlers for the same event and cause
		//concurrent modification exception
		final LinkedHashSet<IEventHandler<D>> copy = new LinkedHashSet<IEventHandler<D>>(handlers);		
		for (IEventHandler<D> eh : copy) {
			eh.onEvent(data);
		}
	}
	
	public void wrap(IEventSource<D> other) {
		wrappingHandlerRegistraton = other.addEventHandler(new IEventHandler<D>() {

			@Override
			public void onEvent(D data) {
				signal(data);
			}
			
		});
	}
	
	public void unwrap() {
		wrappingHandlerRegistraton.removeHandler();
	}
}
