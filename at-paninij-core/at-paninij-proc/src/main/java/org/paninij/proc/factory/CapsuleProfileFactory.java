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
import java.util.List;

import javax.lang.model.type.TypeKind;

import org.paninij.proc.model.Procedure;
import org.paninij.proc.model.Variable;
import org.paninij.proc.util.MessageShape;
import org.paninij.proc.util.PaniniModel;
import org.paninij.proc.util.Source;


public abstract class CapsuleProfileFactory extends AbstractCapsuleFactory
{
    protected abstract String generateClassName();

    protected String generateProcedureID(Procedure p) {
        String base = "panini$proc$";
        List<String> params = new ArrayList<String>();

        for (Variable param : p.getParameters()) {
            params.add(param.encodeFull());
        }

        String paramStrings = params.size() > 0 ? "$" + String.join("$", params) : "";

        return base + p.getName() + paramStrings;
    }

    protected String generateProcedureReturn(MessageShape shape) {
        switch (shape.behavior) {
        case BLOCKED_FUTURE:
            String ret = shape.returnType.isVoid() ? "" : "return ";
            ret += "panini$message.get();";
            return ret;
        case ERROR:
            break;
        case BLOCKED_PREMADE:
            return "return panini$message.get();";
        case UNBLOCKED_DUCK:
        case UNBLOCKED_FUTURE:
        case UNBLOCKED_PREMADE:
            return "return panini$message;";
        case UNBLOCKED_SIMPLE:
            return "";
        default:
            break;
        }

        return null;
    }

    protected String generateProcedureArguments(MessageShape shape) {
        String procID = this.generateProcedureID(shape.procedure);
        List<String> argNames = new ArrayList<String>();
        argNames.add(procID);
        argNames.addAll(this.generateProcArgumentNames(shape.procedure));
        return String.join(", ", argNames);
    }

    protected List<String> generateProcedure(Procedure procedure) {
        MessageShape shape = new MessageShape(procedure);
        String encoding = PaniniModel.isPaniniCustom(shape.returnType.getMirror()) ? shape.returnType.raw() : shape.encoded;
        
        List<String> source = Source.lines(
                "#5",
                "@Override",
                "#0",
                "{",
                "    #1 panini$message = null;",
                "    panini$message = new #1(#2);",
                "    #3;",
                "    panini$push(panini$message);",
                "    #4",
                "}",
                "");
        
        return Source.formatAll(source,
                this.generateProcedureDecl(shape),
                encoding,
                this.generateProcedureArguments(shape),
                this.generateAssertSafeInvocationTransfer(),
                this.generateProcedureReturn(shape),
                shape.kindAnnotation);
    }

    protected List<String> generateProcArgumentDecls(Procedure p) {
        List<String> argDecls = new ArrayList<String>();
        for (Variable v : p.getParameters()) {
            argDecls.add(v.toString());
        }
        return argDecls;
    }

    protected List<String> generateProcArgumentNames(Procedure p) {
        List<String> argNames = new ArrayList<String>();
        for (Variable v : p.getParameters()) {
            argNames.add(v.getIdentifier());
        }
        return argNames;
    }

    protected String generateProcedureDecl(MessageShape shape) {
        List<String> argDecls = this.generateProcArgumentDecls(shape.procedure);
        String argDeclString = String.join(", ", argDecls);
        String declaration = Source.format("public #0 #1(#2)",
                shape.realReturn,
                shape.procedure.getName(),
                argDeclString);
        List<String> thrown = shape.procedure.getThrown();
        declaration += (thrown.isEmpty()) ? "" : " throws " + String.join(", ", thrown);
        return declaration;
    }

    protected List<String> generateEventMethods() {
        List<String> list = new ArrayList<String>();
        
        List<Variable> allEvents = capsule.getBroadcastEventFields();
        allEvents.addAll(capsule.getChainEventFields());
        
        for (Variable v : allEvents) {
            List<String> source = Source.lines(
                    "@Override",
                    "public #0 #1() {",
                    "    return panini$encapsulated.#1;",
                    "}",
                    "");

            list.addAll(Source.formatAll(source,
                    v.raw(),
                    v.getIdentifier()));
        }

        return list;
    }
    
    protected List<String> generateEventHandlers() {
        List<String> list = new ArrayList<String>();
        
        for (Procedure p : capsule.getEventHandlers()) {
            List<String> source = Source.lines(
                    "@Override",
                    "public void #0(PaniniEventExecution ex, #1) {",
                    "    PaniniEventMessage<#2> panini$message = null;",
                    "    panini$message = new PaniniEventMessage<>(#4, ex, #3);",
                    "    panini$push(panini$message);",
                    "}",
                    "");
            
            Variable param = p.getParameters().get(0);
            String argDeclString = param.toString();
            list.addAll(Source.formatAll(source,
                    p.getName(),
                    argDeclString,
                    param.getMirror().toString(),
                    param.getIdentifier(),
                    generateProcedureID(p)));
        }
        
        return list;
    }

    protected List<String> generateConstructor() {
        List<String> list = new ArrayList<String>();

        list.add(Source.format(
                "public #0() {",
                generateClassName()));

        for (Variable v : capsule.getBroadcastEventFields()) {
            list.add(Source.format(
                    "    panini$encapsulated.#0 = new PaniniEvent<>(org.paninij.runtime.EventMode.BROADCAST);",
                    v.getIdentifier()));
        }
        for (Variable v : capsule.getChainEventFields()) {
            list.add(Source.format(
                    "    panini$encapsulated.#0 = new PaniniEvent<>(org.paninij.runtime.EventMode.CHAIN);",
                    v.getIdentifier()));
        }

        list.add("}");
        list.add("");

        return list;
    }
    
    protected String generateAssertSafeInvocationTransfer()
    {
        // TODO: Clean this up!
        /**
        return Source.format("assert DynamicOwnershipTransfer.#0.isSafeTransfer(#1, #2): #3",
                             PaniniProcessor.dynamicOwnershipTransferKind,
                             "panini$message",
                             "Panini$System.self.get().panini$getAllState()",
                             "\"Procedure invocation performed unsafe ownership transfer.\"");
        */
        return "";
    }

    protected List<String> generateCheckRequiredFields()
    {
        // Get the fields which must be non-null, i.e. all @Import fields and all arrays of locals.
        List<Variable> required = this.capsule.getImportFields();

        for (Variable local : this.capsule.getLocalFields()) {
            if (local.isArray()) required.add(local);
        }

        if (required.isEmpty()) return new ArrayList<String>();

        List<String> assertions = new ArrayList<String>(required.size());
        for (int idx = 0; idx < required.size(); idx++) {
            if (required.get(idx).isCapsule()) {
                assertions.add(Source.format(
                        "assert(panini$encapsulated.#0 != null);",
                        required.get(idx).getIdentifier()));
            }
        }

        List<String> lines = Source.lines(
                "@Override",
                "public void panini$checkRequiredFields() {",
                "    ##",
                "}",
                "");
        return Source.formatAlignedFirst(lines, assertions);
    }

    protected List<String> generateExport()
    {
        List<Variable> imported = this.capsule.getImportFields();
        List<String> refs = new ArrayList<String>();
        List<String> decls = new ArrayList<String>();

        if (imported.isEmpty()) return refs;

        for (Variable var : imported) {
            String instantiation = Source.format("panini$encapsulated.#0 = #0;", var.getIdentifier());
            refs.add(instantiation);

            if (var.isArray()) {
                if (var.getEncapsulatedType().isCapsule()) {
                    List<String> lines = Source.lines(
                            "for (int i = 0; i < panini$encapsulated.#0.length; i++) {",
                            "    ((Panini$Capsule) panini$encapsulated.#0[i]).panini$openLink();",
                            "}");
                    refs.addAll(Source.formatAll(
                            lines,
                            var.getIdentifier()));
                }
            } else {
                if (var.isCapsule()) {
                    refs.add(Source.format("((Panini$Capsule) panini$encapsulated.#0).panini$openLink();", var.getIdentifier()));
                }
            }

            decls.add(var.toString());
        }

        List<String> src = Source.lines(
                "public void imports(#0) {",
                "    ##",
                "}",
                "");

        src = Source.formatAll(src, String.join(", ", decls));
        src = Source.formatAlignedFirst(src, refs);

        return src;
    }

    protected List<String> generateGetAllState()
    {
    	List<String> states = new ArrayList<String>();
    	
    	for(Variable field : capsule.getStateFields())
    	{
    		if(field.getKind() == TypeKind.ARRAY || field.getKind() == TypeKind.DECLARED)
    		{
    			states.add("panini$encapsulated." + field.getIdentifier());
    		}
    	}
    	
        List<String> src = Source.lines("@Override",
                                        "public Object panini$getAllState()",
                                        "{",
                                        "    Object[] state = {#0};",
                                        "    return state;",
                                        "}",
                                        "");

        return Source.formatAll(src, String.join(", ", states));
    }

    protected List<String> generateInitState()
    {
        if (!this.capsule.hasInit()) return new ArrayList<String>();
        return Source.lines(
                "@Override",
                "protected void panini$initState() {",
                "    panini$encapsulated.init();",
                "}",
                "");
    }

    protected List<String> generateOnTerminate() {
        List<String> shutdowns = new ArrayList<String>();
        List<Variable> references = new ArrayList<Variable>();

        references.addAll(this.capsule.getImportFields());
        references.addAll(this.capsule.getLocalFields());

        if (references.isEmpty()) return shutdowns;

        for (Variable reference : references) {
            if (reference.isArray()) {
                if (reference.getEncapsulatedType().isCapsule()) {
                    List<String> src = Source.lines(
                            "for (int i = 0; i < panini$encapsulated.#0.length; i++) {",
                            "    ((Panini$Capsule) panini$encapsulated.#0[i]).panini$closeLink();",
                            "}");
                    shutdowns.addAll(Source.formatAll(src, reference.getIdentifier()));
                }
            } else {
                if (reference.isCapsule()) {
                    shutdowns.add(Source.format("((Panini$Capsule) panini$encapsulated.#0).panini$closeLink();", reference.getIdentifier()));
                }
            }
        }

        List<String> src = Source.lines(
                "@Override",
                "protected void panini$onTerminate() {",
                "    ##",
                "    this.panini$terminated = true;",
                "}",
                "");

        return Source.formatAlignedFirst(src, shutdowns);
    }

    protected boolean deservesMain()
    {
        return capsule.isRoot();
    }

    protected List<String> generateMain()
    {
        if (!this.deservesMain()) return new ArrayList<String>();

        List<String> src = Source.lines(
                "public static void main(String[] args) {",
                "    try {",
                "        Panini$System.threads.countUp();",
                "        #0 root = new #0();",
                "        root.run();",
                "    } catch (InterruptedException e) {",
                "       e.printStackTrace();",
                "    }",
                "}");

        return Source.formatAll(src, this.generateClassName());
    }
}
