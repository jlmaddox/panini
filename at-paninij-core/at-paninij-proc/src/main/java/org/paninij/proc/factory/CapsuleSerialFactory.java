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
 *  Jackson Maddox
 *******************************************************************************/

package org.paninij.proc.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.paninij.proc.PaniniProcessor;
import org.paninij.proc.model.Procedure;
import org.paninij.proc.model.Type;
import org.paninij.proc.model.Variable;
import org.paninij.proc.util.MessageShape;
import org.paninij.proc.util.PaniniModel;
import org.paninij.proc.util.Source;


public class CapsuleSerialFactory extends CapsuleProfileFactory
{

    public static final String CAPSULE_PROFILE_SERIAL_SUFFIX = "$Serial";

    @Override
    protected String getQualifiedName()
    {
        return this.capsule.getQualifiedName() + CAPSULE_PROFILE_SERIAL_SUFFIX;
    }

    @Override
    protected String generateContent()
    {
        String src = Source.cat(
                "package #0;",
                "",
                "##",
                "",
                "#1",
                "@SuppressWarnings(\"unused\")",  // To suppress unused import warnings.
                "public class #2 extends Capsule$Serial implements #3",
                "{",
                "    ##",
                "}");

        src = Source.format(src,
                this.capsule.getPackage(),
                PaniniProcessor.getGeneratedAnno(CapsuleSerialFactory.class),
                this.generateClassName(),
                this.capsule.getSimpleName());

        src = Source.formatAligned(src, generateImports());
        src = Source.formatAligned(src, generateCapsuleBody());

        return src;
    }

    @Override
    protected String generateClassName()
    {
        return this.capsule.getSimpleName() + CAPSULE_PROFILE_SERIAL_SUFFIX;
    }

    private List<String> generateImports()
    {
        Set<String> imports = new HashSet<String>();

        for (Procedure p : this.capsule.getProcedures()) {
            MessageShape shape = new MessageShape(p);
            imports.add(shape.fullLocation());
        }

        imports.addAll(this.capsule.getImports());
        
        imports.add("javax.annotation.Generated");
        imports.add("java.util.concurrent.Future");
        imports.add("org.paninij.lang.PaniniEventExecution");
        imports.add("org.paninij.runtime.PaniniEventMessage");
        imports.add("org.paninij.runtime.Capsule$Serial");
        imports.add("org.paninij.runtime.Panini$Capsule");
        imports.add("org.paninij.runtime.Panini$Message");
        imports.add("org.paninij.runtime.Panini$Future");
        imports.add("org.paninij.runtime.Panini$System");
        imports.add(this.capsule.getQualifiedName());

        List<String> prefixedImports = new ArrayList<String>();

        for (String i : imports) {
            prefixedImports.add("import " + i + ";");
        }

        return prefixedImports;
    }

    private String generateEncapsulatedDecl()
    {
        return Source.format(
                "private #0 panini$encapsulated = new #0();",
                this.capsule.getQualifiedName() + PaniniModel.CAPSULE_TEMPLATE_SUFFIX);
    }

    @Override
    protected List<String> generateProcedure(Procedure procedure) {
        MessageShape shape = new MessageShape(procedure);

        List<String> source = Source.lines(
                "#1",
                "@Override",
                "#0",
                "{",
                "   ##",
                "}",
                "");
        source = Source.formatAll(source,
                this.generateProcedureDecl(shape),
                shape.kindAnnotation);

        return Source.formatAlignedFirst(source, this.generateEncapsulatedMethodCall(shape));
    }

    private List<String> generateEncapsulatedMethodCall(MessageShape shape)
    {
        List<String> encap = new ArrayList<String>();
        List<String> argNames = this.generateProcArgumentNames(shape.procedure);
        String args = String.join(", ", argNames);
        String call = "panini$encapsulated." + shape.procedure.getName() + "(" + args + ")";
        switch(shape.behavior) {
        case UNBLOCKED_DUCK:
        case BLOCKED_FUTURE:
            String ret = shape.returnType.isVoid() ? "" : "return ";
            encap.add(ret + call + ";");
            return encap;
        case UNBLOCKED_PREMADE:
        case BLOCKED_PREMADE:
            encap.add("return " + call + ";");
            return encap;
        case ERROR:
            break;
        case UNBLOCKED_FUTURE:
            argNames.add(0, "-1");
            args = String.join(", ", argNames);
            encap.add(shape.encoded + " msg = new " + shape.encoded + "(" + args + ");");
            Type r = shape.procedure.getReturnType();
            if (r.isVoid()) {
                encap.add(call + ";");
                encap.add("msg.panini$resolve(null);");
            } else {
                encap.add(r.wrapped() + " result = " + call + ";");
                encap.add("msg.panini$resolve(result);");
            }
            encap.add("return msg;");
            return encap;
        case UNBLOCKED_SIMPLE:
            encap.add(call + ";");
            return encap;
        default:
            break;
        }
        return encap;
    }

    private List<String> generateProcedures()
    {
        ArrayList<String> src = new ArrayList<String>();
        for (Procedure p : this.capsule.getProcedures()) {
            src.addAll(this.generateProcedure(p));
        }
        return src;
    }

    private List<String> generateInitLocals()
    {
        List<Variable> locals = this.capsule.getLocalFields();
        List<String> source = new ArrayList<String>();

        for (Variable local : locals) {
            if (local.isArray()) {
                List<String> lines = Source.lines(
                        "for (int i = 0; i < panini$encapsulated.#0.length; i++) {",
                        "    panini$encapsulated.#0[i] = new #1#2();",
                        "}",
                        "");
                source.addAll(Source.formatAll(
                        lines,
                        local.getIdentifier(),
                        local.getEncapsulatedType(),
                        CAPSULE_PROFILE_SERIAL_SUFFIX));
            } else {
                source.add(Source.format(
                        "panini$encapsulated.#0 = new #1#2();",
                        local.getIdentifier(),
                        local.raw(),
                        CAPSULE_PROFILE_SERIAL_SUFFIX));
            }
        }


        for (Variable local : locals) {
            if (local.isArray()) {
                List<String> lines = Source.lines(
                        "for (int i = 0; i < panini$encapsulated.#0.length; i++) {",
                        "    ((Panini$Capsule) panini$encapsulated.#0[i]).panini$openLink();",
                        "}");
                source.addAll(Source.formatAll(
                        lines,
                        local.getIdentifier()));
            } else {
                source.add(Source.format(
                        "((Panini$Capsule) panini$encapsulated.#0).panini$openLink();",
                        local.getIdentifier()));
            }
        }

        if (this.capsule.hasDesign()) {
            source.add("panini$encapsulated.design(this);");
        }

        for (Variable local : locals) {
            if (local.isArray()) {
                List<String> src = Source.lines(
                        "for (int i = 0; i < panini$encapsulated.#0.length; i++) {",
                        "    panini$encapsulated.#0[i].panini$start();",
                        "}");
                source.addAll(Source.formatAll(src, local.getIdentifier()));
            } else {
                source.add(Source.format(
                        "panini$encapsulated.#0.panini$start();",
                        local.getIdentifier()));
            }
        }

        List<String> decl = Source.lines(
                "@Override",
                "protected void panini$initLocals() {",
                "    ##",
                "}",
                "");

        return Source.formatAlignedFirst(decl, source);
    }

    private List<String> generateRun()
    {
        if (this.capsule.isActive()) {
            return Source.lines(
                    "@Override",
                    "public void run() {",
                    "    try {",
                    "        panini$checkRequiredFields();",
                    "        panini$initLocals();",
                    "        panini$initState();",
                    "        panini$encapsulated.run();",
                    "    } catch (Throwable thrown) {",
                    "        panini$errors.add(thrown);",
                    "    } finally {",
                    "        panini$onTerminate();",
                    "        try {",
                    "           Panini$System.threads.countDown();",
                    "        } catch (InterruptedException e) {",
                    "            e.printStackTrace();",
                    "        }",
                    "    }",
                    "}",
                    "");
        }

        return Source.lines(
                "@Override",
                "@SuppressWarnings(\"unchecked\")",
                "public void run() {",
                "    try {",
                "        panini$checkRequiredFields();",
                "        panini$initLocals();",
                "        panini$initState();",
                "    } catch (Throwable thrown) {",
                "        panini$errors.add(thrown);",
                "    }",
                "}",
                "");
    }

    private List<String> generateCapsuleBody()
    {
        List<String> src = new ArrayList<String>();

        src.add(this.generateEncapsulatedDecl());
        src.addAll(this.generateConstructor());
        src.addAll(this.generateProcedures());
        src.addAll(this.generateEventHandlers());
        src.addAll(this.generateEventMethods());
        src.addAll(this.generateCheckRequiredFields());
        src.addAll(this.generateExport());
        src.addAll(this.generateInitLocals());
        src.addAll(this.generateInitState());
        src.addAll(this.generateOnTerminate());
        src.addAll(this.generateGetAllState());
        src.addAll(this.generateRun());
        src.addAll(this.generateMain());

        return src;
    }

}
