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
package org.paninij.examples.eventasteroids;

import java.awt.event.KeyEvent;

import org.paninij.examples.eventasteroids.TextAreaUI;
import org.paninij.examples.eventasteroids.Window;
import org.paninij.lang.Capsule;
import org.paninij.lang.Broadcast;
import org.paninij.lang.Event;

@Capsule
public class TextAreaUITemplate {
    @Broadcast Event<String> keyPressed;
    Window window;

    void design(TextAreaUI self) {
        window = new Window(self);
    }

    void init() {
        window.show();
    }
    
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        String key = KeyEvent.getKeyText(keyCode);
        keyPressed.announce(key);
    }

    public void setText(String str) {
        window.setText(str);
    }

}
