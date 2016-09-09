package org.paninij.lang;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.paninij.runtime.Panini$Capsule;

public class PaniniEvent<T> {

	ConcurrentLinkedQueue<PaniniConnection<T>> list = new ConcurrentLinkedQueue<>();
	
	public PaniniEvent() {
		
	}
	
	public PaniniConnection<T> register(Panini$Capsule capsule, Consumer<T> handler) {
		PaniniConnection<T> conn = new PaniniConnection<>(this, handler);
		list.add(conn);
		return conn;
	}
	
	public void announce(T arg) {
		for (PaniniConnection<T> con : list) {
			con.handler.accept(arg);
		}
	}
}
