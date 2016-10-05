/*******************************************************************************
 * This file is part of the Panini project at Iowa State University.
 *
 * @PaniniJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * @PaniniJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with @PaniniJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributors:
 *  Dr. Hridesh Rajan,
 *  Dalton Mills,
 *  David Johnston,
 *  Trey Erenberger
 *  Jackson Maddox
 *******************************************************************************/
package org.paninij.lang;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

import org.paninij.runtime.EventMode;

public class PaniniEvent<T> {
    private ConcurrentLinkedQueue<PaniniConnection<T>> list = new ConcurrentLinkedQueue<>();
    private final EventMode mode;
    
    public PaniniEvent(EventMode mode) {
        this.mode = mode;
    }
    
    public PaniniConnection<T> register(BiConsumer<PaniniEventExecution, T> handler) {
        PaniniConnection<T> conn = new PaniniConnection<>(handler);
        list.add(conn);
        return conn;
    }

    public void announce(T arg) {
        PaniniEventExecution ex = new PaniniEventExecution();
        for (PaniniConnection<T> con : list) {
            if (con.on) {
                con.handler.accept(ex, arg);
            }
        }
    }
}