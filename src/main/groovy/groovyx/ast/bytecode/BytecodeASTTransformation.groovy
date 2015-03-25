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

import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.Type
import groovyjarjarasm.asm.Label
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.classgen.BytecodeInstruction
import org.codehaus.groovy.classgen.BytecodeSequence
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.Variable
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ClassHelper

import java.lang.invoke.MethodType

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class BytecodeASTTransformation implements ASTTransformation, Opcodes {
    private SourceUnit sourceUnit
    private boolean showLineNumbers = false

    void visit(ASTNode[] nodes, SourceUnit source) {
        def ann = nodes[0]
        extractOptions(ann)
        def meth = nodes[1]
        meth.code = new BytecodeSequence(new BytecodeGenerator(meth, meth.code.statements))
        sourceUnit = source
    }

    private final extractOptions(AnnotationNode node) {
        def member = node.getMember('lineNumbers')
        if (member instanceof ConstantExpression) {
            showLineNumbers = member.value as boolean
        }
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
    private class BytecodeGenerator extends BytecodeInstruction {

        private final MethodNode meth
        private final Collection<Statement> instructions
        private final List<Closure> localVariables = []

        BytecodeGenerator(MethodNode method, Collection<Statement> instructions) {
            this.meth = method
            this.instructions = instructions
        }

        @Override
        void visit(MethodVisitor mv) {
            def labels = [:].withDefault { throw new IllegalArgumentException("Label [${it}] is not defined")}
            // perform first visit to collect labels
            collectLabels(labels)
            Label start = new Label()
            labels.start = start
            mv.visitLabel(start)
            // second iteration transforms each instruction into bytecode visitor instructions
            visitInstructions(mv, labels)
            Label end = new Label()
            labels.end = end
            mv.visitLabel(end)
            // declare local variables
            int idx = meth.isStatic()?0:1
            for (Parameter p : meth.parameters) {
                mv.visitLocalVariable(p.name, BytecodeHelper.getTypeDescription(p.originType), null, start, end, idx++)
            }
            localVariables.each { it.call() }
        }

        private def visitInstructions(MethodVisitor mv, Map labels) {
            instructions.each { stmt ->
                printLineNumber(stmt, mv)
                if (stmt.statementLabel) {
                    mv.visitLabel(labels[stmt.statementLabel])
                }
                if (stmt instanceof ReturnStatement) {
                    mv.visitInsn(Opcodes.RETURN)
                } else if (stmt instanceof ExpressionStatement) {
                    def expression = stmt.expression
                    if (expression instanceof VariableExpression) {
                        visitVariableExpression(expression, mv, labels)
                    } else if (expression instanceof MethodCallExpression) {
                        visitMethodCallExpression(expression, mv, labels, meth)
                    } else {
                        unsupportedBytecodeOperation(expression)
                    }
                }
            }
        }

        private void printLineNumber(ASTNode stmt, MethodVisitor mv) {
            if (showLineNumbers && stmt.lineNumber > 0) {
                def label = new Label()
                mv.visitLabel(label)
                mv.visitLineNumber(stmt.lineNumber, label)
            }
        }

        private def visitMethodCallExpression(MethodCallExpression expression, MethodVisitor mv, Map labels, meth) {
            if (expression.objectExpression instanceof VariableExpression && expression.arguments instanceof ArgumentListExpression) {
                if (expression.objectExpression.text == "this") {
                    def opcode = expression.methodAsString.toUpperCase()
                    ArgumentListExpression args = expression.arguments
                    switch (opcode) {
                        case '_GOTO':
                            mv.visitJumpInsn(GOTO, labels[args.expressions[0].text])
                            break;
                        case '_NEW':
                        case 'NEWOBJECT':
                            mv.visitTypeInsn(NEW, internalClassName(args.expressions[0]))
                            break;
                        case '_INSTANCEOF':
                            mv.visitTypeInsn(INSTANCEOF, internalClassName(args.expressions[0]))
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
                        case 'INVOKEDYNAMIC':
                            visitInvokeDynamic(mv, args)
                            break;
                        case 'INVOKEVIRTUAL':
                        case 'INVOKESTATIC':
                        case 'INVOKEINTERFACE':
                        case 'INVOKESPECIAL':
                            visitMethodInvoke(mv, opcode, meth, args)
                            break;
                        case 'FRAME':
                            // frames only supported in JDK 1.6+
                            break;
                        case 'CHECKCAST':
                            mv.visitTypeInsn(CHECKCAST, internalClassName(args.expressions[0]))
                            break;
                        case 'LDC':
                            mv.visitLdcInsn(args.expressions[0].value)
                            break;
                        case 'GETFIELD':
                        case 'PUTFIELD':
                        case 'GETSTATIC':
                        case 'PUTSTATIC':
                            visitFieldInstruction(mv, meth, opcode, args)
                            break;
                        case 'BIPUSH':
                        case 'SIPUSH':
                            mv.visitIntInsn(Opcodes."${opcode}", args.expressions[0].text as int)
                            break;
                        case 'NEWARRAY':
                            if (args.expressions[0] instanceof ConstantExpression) {
                                mv.visitIntInsn(Opcodes."${opcode}", Opcodes."${args.expressions[0].text.toUpperCase()}")
                            } else if (args.expressions[0] instanceof ClassExpression) {
                                mv.visitIntInsn(Opcodes."${opcode}", Opcodes."T_${args.expressions[0].type.nameWithoutPackage.toUpperCase()}")
                            } else if (args.expressions[0] instanceof VariableExpression) {
                                mv.visitIntInsn(Opcodes."${opcode}", Opcodes."${args.expressions[0].text.toUpperCase()}")
                            } else {
                                unsupportedBytecodeOperation(expression)
                            }
                            break;
                        case 'ANEWARRAY':
                            mv.visitTypeInsn(ANEWARRAY, internalClassName(args.expressions[0]));
                            break;
                        case 'MULTIANEWARRAY':
                            if (args.expressions.size()==2) {
                                // legacy syntax
                                mv.visitMultiANewArrayInsn(internalClassName(args.expressions[0]), args.expressions[1].text as int)
                            } else {
                                unsupportedBytecodeOperation(expression)
                            }
                            break;
                        case 'TRYCATCHBLOCK':
                            if (args.expressions.size() != 4) throw new IllegalArgumentException("Bytecode operation unsupported [trycatchblock requires exactly 4 parameters] : " + expression);
                            def tcargs = args.expressions
                            mv.visitTryCatchBlock(labels[tcargs[0].text], labels[tcargs[1].text], labels[tcargs[2].text], internalClassName(tcargs[3]))
                            break
                        case 'LOCALVARIABLE':
                            if (args.expressions.size() != 6) throw new IllegalArgumentException("Bytecode operation unsupported [localvariable requires exactly 4 parameters] : $expression.text")
                            def ag = args.expressions
                            localVariables << {
                                mv.visitLocalVariable(
                                        ag[0].text,
                                        ag[1].text,
                                        null,
                                        labels[ag[3].text],
                                        labels[ag[4].text],
                                        ag[5].value)
                            }
                            break
                        default:
                            unsupportedBytecodeOperation(expression)
                    }
                } else {
                    unsupportedBytecodeOperation(expression)
                }
            } else if (expression.objectExpression instanceof VariableExpression && expression.arguments instanceof TupleExpression) {
                if (expression.method.text == 'lookupswitch') {
                    processLookupSwitch(expression, mv, labels)
                } else if (expression.method.text == 'tableswitch') {
                    processTableSwitch(expression, mv, labels)
                } else if (expression.method.text == 'go' || expression.method.text=='instance') {
                    if (expression.arguments instanceof TupleExpression) {
                        TupleExpression args = expression.arguments
                        if (args.expressions.size()==1) {
                            def arg = args.expressions[0]
                            if (arg instanceof NamedArgumentListExpression) {
                                arg.mapEntryExpressions.each { MapEntryExpression mapEntryExpression ->
                                    if (mapEntryExpression.keyExpression.text == 'to' && expression.method.text=='go') {
                                        mv.visitJumpInsn(GOTO, labels[mapEntryExpression.valueExpression.text])
                                    } else if (mapEntryExpression.keyExpression.text == 'of' && expression.method.text=='instance') {
                                        mv.visitTypeInsn(INSTANCEOF, internalClassName(mapEntryExpression.valueExpression))
                                    } else {
                                        throw new IllegalArgumentException("Bytecode operation supported : $expression")
                                    }
                                }
                            } else {
                                unsupportedBytecodeOperation(expression);
                            }
                        } else {
                            unsupportedBytecodeOperation(expression);
                        }
                    } else {
                        unsupportedBytecodeOperation(expression)
                    }
                } else {
                    unsupportedBytecodeOperation(expression)
                }
            } else {
                unsupportedBytecodeOperation(expression)
            }
        }

        private static unsupportedBytecodeOperation(expression) {
            throw new IllegalArgumentException("Bytecode operation unsupported : " + expression)
        }

        private static String internalClassName(Expression expr) {
            if (expr instanceof ConstantExpression) {
                expr.value==null?null:expr.text
            } else if (expr instanceof ClassExpression) {
                BytecodeHelper.getClassInternalName(expr.type)
            } else {
                unsupportedBytecodeOperation(expr)
            }
        }

        private def visitFieldInstruction(MethodVisitor mv, meth, opcode, ArgumentListExpression args) {
            def clazz, field, descriptor

            // syntax of the form: getstatic name >> String
            if (args.expressions[0] instanceof BinaryExpression && args.expressions[0].operation.text == '>>') {
                BinaryExpression binExpr = args.expressions[0]
                if (binExpr.rightExpression instanceof ClassExpression) {
                    //descriptor = Type.getDescriptor(args.expressions[0].rightExpression.type.typeClass)
                    descriptor = BytecodeHelper.getTypeDescription(args.expressions[0].rightExpression.type)
                } else {
                    throw new IllegalArgumentException("Expected a class expression on the right of '>>'")
                }

                if (binExpr.leftExpression instanceof Variable) {
                    clazz = meth.declaringClass.name
                    field = binExpr.leftExpression.name
                } else if (binExpr.leftExpression instanceof PropertyExpression) {
                    PropertyExpression propExp = binExpr.leftExpression
                    if (propExp.objectExpression instanceof ClassExpression) {
                        //clazz = Type.getInternalName(propExp.objectExpression.type.typeClass)
                        clazz = BytecodeHelper.getClassInternalName(propExp.objectExpression.type)
                        field = propExp.property.text
                    } else {
                        throw new IllegalArgumentException(
                                "Expected a class expression but got a ${propExp.objectExpression.class.simpleName}")
                    }
                } else {
                    throw new IllegalArgumentException("Expected a variable or a property on the left of '>>")
                }

            } else { // usual syntax
                def classExpr = args.expressions[0].text
                (clazz, field) = extractClazzAndFieldOrMethod(classExpr, meth)
                descriptor = args.expressions[1].text
            }

            mv.visitFieldInsn(Opcodes."${opcode}", clazz, field, descriptor)
        }

        private def visitMethodInvoke(MethodVisitor mv, opcode, meth, ArgumentListExpression args) {
            def clazz, call, signature

            // syntax of the form: invokevirtual SomeClass.method(double[], String) >> int[]
            if (args.expressions[0] instanceof BinaryExpression && args.expressions[0].operation.text == '>>') {
                // return type is what's on the right of the >> binary expression
                ClassNode returnTypeClassNode = args.expressions[0].rightExpression.type
                def returnType = returnTypeClassNode == ClassHelper.VOID_TYPE ? "V" : BytecodeHelper.getTypeDescription(returnTypeClassNode)

                MethodCallExpression methCall = args.expressions[0].leftExpression

                // the callee is the subject on which the method is invoked
                def callee = methCall.objectExpression

                // either the type of the class is explicitely defined
                if (callee instanceof ClassExpression) {
                    clazz = BytecodeHelper.getClassInternalName(callee.type)
                }
                // or the call is made on this
                else if (callee instanceof VariableExpression && callee.name == "this") {
                    clazz = meth.declaringClass.name
                }
                // otherwise it's an error
                else {
                    throw new IllegalArgumentException("Expected a class expression or variable expression")
                }

                signature = '(' + methCall.arguments.expressions.collect { ClassExpression ce ->
                    Type.getDescriptor(ce.type.typeClass)
                }.join('') + ')' + returnType

                call = methCall.methodAsString
            } else { // usual syntax
                def classExpr = args.expressions[0].text
                (clazz, call) = extractClazzAndFieldOrMethod(classExpr, meth)
                signature = args.expressions[1].text
            }

            mv.visitMethodInsn(Opcodes."${opcode}", clazz, call, signature)
        }

        private String signature(ListExpression list) {
            MethodCallExpression mce = new MethodCallExpression(
                    new StaticMethodCallExpression(
                            ClassHelper.make(MethodType),
                            "methodType",
                            new ArgumentListExpression(list.expressions)
                    ),
                    "toMethodDescriptorString",
                    ArgumentListExpression.EMPTY_ARGUMENTS
            )
            ExpressionEvaluator.evaluate(mce)
        }

        private def visitInvokeDynamic(MethodVisitor mv, ArgumentListExpression args) {
            sourceUnit.configuration.targetBytecode = '1.7'
            def exprs = args.getExpressions()
            if (exprs.size()<3) {
                throw new IllegalArgumentException("Not enough arguments for an invokedynamic call")
            }
            // invokedynamic "name", "signature()I", [H_INVOKESTATIC, ...]
            String name = exprs[0].text
            String desc
            def handleCreateArgs = new ArgumentListExpression()
            def handleExpressions = exprs[2].expressions
            for (int i=0; i<handleExpressions.size()-1; i++) {
                handleCreateArgs.addExpression(handleExpressions[i])
            }
            handleCreateArgs.addExpression(new ConstantExpression(signature(handleExpressions[-1])))
            ConstructorCallExpression handleExpr = new ConstructorCallExpression(
                    ClassHelper.make(Handle),
                    handleCreateArgs
            )
            Object[] extraArgs = (exprs.size()>3?exprs.subList(3, exprs.size()).collect { ConstantExpression ce ->
                ce.value}:[]) as Object[]
            def argTypes = exprs[1]
            if (argTypes instanceof ConstantExpression) {
                desc = argTypes.text
            } else if (argTypes instanceof ListExpression) {
                desc = signature(argTypes)
            } else {
                throw new IllegalArgumentException("Second argument of invokedynamic call must be a list of classes")
            }

            Handle handle = ExpressionEvaluator.evaluate(handleExpr)
            mv.visitInvokeDynamicInsn(name, desc, handle, *extraArgs)
        }

        private def visitVariableExpression(VariableExpression expression, MethodVisitor mv, Map labels) {
            def text = expression.text.toLowerCase()
            if (text ==~ /l[0-9]+/) {
                mv.visitLabel(labels[text])
            } else if (text == 'vreturn') {
                // vreturn replaces the regular "return" bytecode statement
                mv.visitInsn(RETURN)
            } else if (Instructions.UNIT_OPS.contains(text)) {
                mv.visitInsn(Opcodes."${text.toUpperCase()}")
            } else if (text =~ /(load|store)_[0-4]/) {
                def (var, cpt) = text.split("_")
                mv.visitVarInsn(Opcodes."${var.toUpperCase()}", cpt as int)
            } else {
                throw new IllegalArgumentException("Bytecode operation unsupported : " + text);
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
                unsupportedBytecodeOperation(expression)
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
                unsupportedBytecodeOperation(expression)
            }
        }

    }
}
