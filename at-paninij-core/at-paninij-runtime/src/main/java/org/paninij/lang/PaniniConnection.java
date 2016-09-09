package org.paninij.lang;

import java.util.function.Consumer;

public class PaniniConnection<T> {
	protected PaniniEvent<T> event;
	protected Consumer<T> handler;
	
	public PaniniConnection(PaniniEvent<T> event, Consumer<T> handler) {
		this.event = event;
		this.handler = handler;
	}
	
	public void unregister() {
		
	}
}
