/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.ast.bytecode;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.tools.GroovyClass;

public class ExpressionEvaluator {
    public static Object evaluate(Expression expr) {
        CompilationUnit cu = new CompilationUnit();
        SourceUnit dummy = SourceUnit.create("dummy", "");
        cu.addSource(dummy);
        cu.compile(Phases.CONVERSION);
        ClassNode classNode = dummy.getAST().getClasses().get(0);
        MethodNode run = classNode.getMethods("run").get(0);
        Statement code = new ExpressionStatement(expr);
        run.setCode(code);
        cu.compile(Phases.CLASS_GENERATION);
        GroovyClass clazz = (GroovyClass) cu.getClasses().get(0);
        Class aClass = cu.getClassLoader().defineClass(clazz.getName(), clazz.getBytes());
        try {
            return  ((GroovyObject) aClass.newInstance()).invokeMethod("run", null);
        } catch (InstantiationException e) {
            throw new GroovyBugError(e);
        } catch (IllegalAccessException e) {
            throw new GroovyBugError(e);
        }
    }
}
