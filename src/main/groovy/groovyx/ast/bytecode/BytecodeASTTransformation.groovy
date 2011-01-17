/*
 *
 *
 *   Copyright 2011 Cédric Champeau
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  /
 * /
 */

package groovyx.ast.bytecode

import groovyjarjarasm.asm.Label
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.classgen.BytecodeInstruction
import org.codehaus.groovy.classgen.BytecodeSequence
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.Statement

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class BytecodeASTTransformation implements ASTTransformation, Opcodes {
    void visit(ASTNode[] nodes, SourceUnit source) {
        def meth = nodes[1]
        meth.code = new BytecodeSequence(new BytecodeGenerator(meth, meth.code.statements))
    }

    /**
     * Given a String of the form '.field' or 'fqn.Class.field', returns
     * a couple ('fqn.Class', 'field') where the class is always replaced with the
     * enclosing class if not specified.
     */
    private static def extractClazzAndFieldOrMethod(classExpr, meth) {
        def clazz, field
        if (classExpr[0] == '.') {
            clazz = meth.declaringClass.name
            field = classExpr[1..<classExpr.length()]
        } else {
            def index = classExpr.lastIndexOf('.')
            clazz = classExpr.substring(0, index).replaceAll(/\./, '/')
            field = classExpr.substring(index + 1)
        }
        return [clazz, field]
    }

    /**
     * Converts bytecode instructions from an AST tree into ASM bytecode generation code.
     */
    private static class BytecodeGenerator extends BytecodeInstruction {

        private def meth;
        private def instructions;

        BytecodeGenerator(method, instructions) {
            this.meth = method
            this.instructions = instructions
        }

        @Override
        void visit(MethodVisitor mv) {
            def labels = [:].withDefault { throw new IllegalArgumentException("Label [${it}] is not defined")}
            // perform first visit to collect labels
            collectLabels(labels)
            // second iteration transforms each instruction into bytecode visitor instructions
            instructions.each { stmt ->
                if (stmt.statementLabel) {
                    mv.visitLabel(labels[stmt.statementLabel])
                }
                if (stmt instanceof ReturnStatement) {
                    mv.visitInsn(Opcodes.RETURN)
                } else if (stmt instanceof ExpressionStatement) {
                    def expression = stmt.expression
                    if (expression instanceof VariableExpression) {
                        def text = expression.text.toLowerCase()
                        if (text ==~ /l[0-9]+/) {
                            mv.visitLabel(labels[text])
                        } else if (text == 'vreturn') {
                            // vreturn replaces the regular "return" bytecode statement
                            mv.visitInsn(Opcodes.RETURN)
                        } else if (Instructions.UNIT_OPS.contains(text)) {
                            mv.visitInsn(Opcodes."${text.toUpperCase()}")
                        } else if (text =~ /(load|store)_[0-4]/) {
                            def (var, cpt) = text.split("_")
                            mv.visitVarInsn(Opcodes."${var.toUpperCase()}", cpt as int)
                        } else {
                            throw new IllegalArgumentException("Bytecode operation unsupported : " + text);
                        }
                    } else if (expression instanceof MethodCallExpression) {
                        if (expression.objectExpression instanceof VariableExpression && expression.arguments instanceof ArgumentListExpression) {
                            if (expression.objectExpression.text == "this") {
                                def opcode = expression.methodAsString.toUpperCase()
                                ArgumentListExpression args = expression.arguments
                                switch (opcode) {
                                    case '_GOTO':
                                        mv.visitJumpInsn(GOTO, labels[args.expressions[0].text])
                                        break;
                                    case '_NEW':
                                        mv.visitTypeInsn(NEW, args.expressions[0].text)
                                        break;
                                    case '_INSTANCEOF':
                                        mv.visitTypeInsn(INSTANCEOF, args.expressions[0].text)
                                        break;
                                    case 'IF_ICMPGE':
                                    case 'IF_ICMPLE':
                                    case 'IF_ICMPNE':
                                    case 'IF_ICMPLT':
                                    case 'IF_ICMPGT':
                                    case 'IF_ICMPEQ':
                                    case 'IF_ACMPEQ':
                                    case 'IF_ACMPNE':
                                    case 'IFEQ':
                                    case 'IFGE':
                                    case 'IFGT':
                                    case 'IFLE':
                                    case 'IFLT':
                                    case 'IFNE':
                                    case 'IFNONNULL':
                                    case 'IFNULL':
                                        mv.visitJumpInsn(Opcodes."${opcode}", labels[args.expressions[0].text])
                                        break;
                                    case 'ALOAD':
                                    case 'ILOAD':
                                    case 'LLOAD':
                                    case 'FLOAD':
                                    case 'DLOAD':
                                    case 'ASTORE':
                                    case 'ISTORE':
                                    case 'FSTORE':
                                    case 'LSTORE':
                                    case 'DSTORE':
                                        mv.visitVarInsn(Opcodes."${opcode}", args.expressions[0].text as int)
                                        break;
                                    case 'IINC':
                                        mv.visitIincInsn(args.expressions[0].text as int, args.expressions[1].text as int)
                                        break;
                                    case 'INVOKEVIRTUAL':
                                    case 'INVOKESTATIC':
                                    case 'INVOKEINTERFACE':
                                    case 'INVOKESPECIAL':
                                        def classExpr = args.expressions[0].text
                                        def (clazz, call) = extractClazzAndFieldOrMethod(classExpr, meth)
                                        def signature = args.expressions[1].text
                                        mv.visitMethodInsn(Opcodes."${opcode}", clazz, call, signature)
                                        break;
                                    case 'FRAME':
                                        // frames only supported in JDK 1.6+
                                        break;
                                    case 'CHECKCAST':
                                        mv.visitTypeInsn(CHECKCAST, args.expressions[0].text)
                                        break;
                                    case 'LDC':
                                        mv.visitLdcInsn(args.expressions[0].value)
                                        break;
                                    case 'GETFIELD':
                                    case 'PUTFIELD':
                                    case 'GETSTATIC':
                                    case 'PUTSTATIC':
                                        def classExpr = args.expressions[0].text
                                        def (clazz, field) = extractClazzAndFieldOrMethod(classExpr, meth)
                                        mv.visitFieldInsn(Opcodes."${opcode}", clazz, field, args.expressions[1].text)
                                        break;
                                    case 'BIPUSH':
                                    case 'SIPUSH':
                                        mv.visitIntInsn(Opcodes."${opcode}", args.expressions[0].text as int)
                                        break;
                                    case 'NEWARRAY':
                                        mv.visitIntInsn(Opcodes."${opcode}", Opcodes."${args.expressions[0].text.toUpperCase()}")
                                        break;
                                    case 'ANEWARRAY':
                                        mv.visitTypeInsn(ANEWARRAY, args.expressions[0].text);
                                        break;
                                    case 'MULTIANEWARRAY':
                                        mv.visitMultiANewArrayInsn(args.expressions[0].text, args.expressions[1].text as int)
                                        break;
                                    case 'TRYCATCHBLOCK':
                                        if (args.expressions.size() != 4) throw new IllegalArgumentException("Bytecode operation unsupported [trycatchblock requires exactly 4 parameters] : " + expression);
                                        def tcargs = args.expressions
                                        mv.visitTryCatchBlock(labels[tcargs[0].text], labels[tcargs[1].text], labels[tcargs[2].text], tcargs[3].value)
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
                                }
                            } else {
                                throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
                            }
                        } else if (expression.objectExpression instanceof VariableExpression && expression.arguments instanceof TupleExpression) {
                            if (expression.method.text == 'lookupswitch') {
                                processLookupSwitch(expression, mv, labels)
                            } else if (expression.method.text == 'tableswitch') {
                                processTableSwitch(expression, mv, labels)
                            } else {
                                throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
                            }
                        } else {
                            throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
                        }
                    } else {
                        throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
                    }
                }
            }
        }

        private def collectLabels(labels) {
            instructions.each { Statement stmt ->
                if (stmt.statementLabel) {
                    labels.put(stmt.statementLabel, new Label())
                }
                if (stmt instanceof ExpressionStatement) {
                    def expression = stmt.expression
                    if (expression instanceof VariableExpression) {
                        def text = expression.text
                        if (text ==~ /l[0-9]+/) {
                            labels.put(text, new Label())
                        }
                    }
                }
            }
        }

        private def processLookupSwitch(MethodCallExpression expression, MethodVisitor mv, labels) {
            if (expression.arguments.expressions && expression.arguments.expressions[0] instanceof NamedArgumentListExpression) {
                def defaultLabel = null
                def values = []
                def targetLabels = []
                def exprs = expression.arguments.expressions[0].mapEntryExpressions
                exprs.each { MapEntryExpression mapEntryExpression ->
                    def key = mapEntryExpression.keyExpression.value
                    switch (key) {
                        case 'default':
                            defaultLabel = labels[mapEntryExpression.valueExpression.text]
                            break;
                        default:
                            values << (key as int)
                            targetLabels << labels[mapEntryExpression.valueExpression.text]
                    }
                }
                if (defaultLabel == null) throw new IllegalArgumentException("Bytecode operation unsupported [lookupswitch must provide default label]: " + expression);
                mv.visitLookupSwitchInsn(defaultLabel, values as int[], targetLabels as Label[])
            } else {
                throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
            }
        }

        private def processTableSwitch(MethodCallExpression expression, MethodVisitor mv, labels) {
            if (expression.arguments.expressions && expression.arguments.expressions[0] instanceof NamedArgumentListExpression) {
                def defaultLabel = null
                def values = []
                def targetLabels = []
                def exprs = expression.arguments.expressions[0].mapEntryExpressions
                exprs.each { MapEntryExpression mapEntryExpression ->
                    def key = mapEntryExpression.keyExpression.value
                    switch (key) {
                        case 'default':
                            defaultLabel = labels[mapEntryExpression.valueExpression.text]
                            break;
                        default:
                            values << (key as int)
                            targetLabels << labels[mapEntryExpression.valueExpression.text]
                    }
                }
                if (defaultLabel == null) throw new IllegalArgumentException("Bytecode operation unsupported [tableswitch must provide default label]: " + expression);
                values[1..<values.size()].eachWithIndex { it, i ->
                    if (it != values[i] + 1) throw new IllegalArgumentException("Bytecode operation unsupported [tableswitch must consist of sequential values]: " + expression)
                }
                mv.visitTableSwitchInsn(values.min(), values.max(), defaultLabel, targetLabels as Label[])
            } else {
                throw new IllegalArgumentException("Bytecode operation unsupported : " + expression);
            }
        }

    }
}
