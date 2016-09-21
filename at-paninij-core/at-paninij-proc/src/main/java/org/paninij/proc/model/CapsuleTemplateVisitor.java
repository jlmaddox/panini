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
 * 	Dr. Hridesh Rajan,
 * 	Dalton Mills,
 * 	David Johnston,
 * 	Trey Erenberger
 *******************************************************************************/

package org.paninij.proc.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;

import org.paninij.lang.Local;
import org.paninij.lang.Imports;
import org.paninij.lang.PaniniEvent;

/**
 * The Template visitor as the main visitor for all capsule templates. This class is used by
 * org.paninij.model.Capsule to convert a Capsule Template to an org.paninij.model.ElementCapsule.
 * This class is used when org.paninij.model.ElementCapsule.make(TypeElement e) is called.
 */
public class CapsuleTemplateVisitor extends SimpleElementVisitor8<CapsuleElement, CapsuleElement>
{
    @Override
    public CapsuleElement visitType(TypeElement e, CapsuleElement capsule) {
        capsule.setTypeElement(e);
        for (Element enclosed : e.getEnclosedElements()) {
            enclosed.accept(this, capsule);
        }
        return capsule;
    }

    @Override
    public CapsuleElement visitExecutable(ExecutableElement e, CapsuleElement capsule) {
        capsule.addExecutable(e);
        return capsule;
    }

    @Override
    public CapsuleElement visitVariable(VariableElement e, CapsuleElement capsule) {
        Variable variable = new Variable(e.asType(), e.getSimpleName().toString(), false);
        
        String eventName = PaniniEvent.class.getName();
        TypeMirror mirror = e.asType();
        DeclaredType dec = null;
        TypeElement type = null;
        String fullTypeName = null;
        if (mirror.getKind() == TypeKind.DECLARED) {
        	dec = (DeclaredType) e.asType();
        	type = (TypeElement) dec.asElement();
        	fullTypeName = type.getQualifiedName().toString();
        }
        
        
        boolean isSelf = e.getSimpleName().toString().equals("self");
        
        // See DesignDeclCheck for explanation
        TypeMirror self = e.asType();
        String actualType = self.toString();

        // No + "Template" -- dealing w/ capsule not capsule's template
        String expectedType = capsule.getQualifiedName(); 
        boolean isCorrectType = expectedType.endsWith(actualType);
        
        
        
        if (e.getAnnotation(Local.class) != null) {
            capsule.addLocals(variable);
        } else if (e.getAnnotation(Imports.class) != null) {
            capsule.addImportDecl(variable);
        } else if (eventName.equals(fullTypeName)) {
            capsule.addEvent(variable);
        } else if (isSelf && isCorrectType) {
            capsule.setSelfField(variable);
        } else {
        	capsule.addState(variable);
        }
        
        return capsule;
    }
}
