/*
 *
 *
 *   Copyright 2011 C�dric Champeau
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

import spock.lang.Specification
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.CompilerConfiguration

/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 15/01/11
 * Time: 22:38
 */

/**
 * A specification which simplifies determining which instructions
 * are converted by the AST Transformation.
 */
class InstructionsImplementedSpock extends Specification {
    def "all those no-arg instructions should be interpreted by the AST transform"() {
        def shell = new GroovyShell()

        when:
            try {
                shell.evaluate("""
                    @groovyx.ast.bytecode.Bytecode
                    void test() {
                        $instruction
                    }
                """)
            } catch (java.lang.VerifyError err) {
                // not a problem, that's not what we're testing
            }
        then:
            notThrown(MultipleCompilationErrorsException)
        where:
            instruction << [
                    "aaload",
                    "aastore",
                    "aconst_null",
                    "aload_0","aload_1","aload_2","aload_3",
                    "areturn",
                    "arraylength",
                    "astore_0","astore_1","astore_2","astore_3",
                    "athrow",
                    "baload","bastore",
                    "caload","castore",
                    "d2f","d2i","d2l","dadd",
                    "daload","dastore",
                    "dcmpg","dcmpl",
                    "dconst_0","dconst_1","ddiv","dload",
                    "dload_0","dload_1","dload_2","dload_3",
                    "dmul","dneg","drem","dreturn",
                    "dstore_0","dstore_1","dstore_2","dstore_3",
                    "dsub","dup","dup2","dup2_x1","dup2_x2","dup_x1","dup_x2",
                    "f2d","f2i","f2l","fadd","faload","fastore",
                    "fcmpg","fcmpl",
                    "fconst_0","fconst_1","fconst_2",
                    "fdiv","fload","fload_0","fload_1","fload_2","fload_3",
                    "fmul","fneg","frem","freturn","fstore",
                    "fstore_0","fstore_1","fstore_2","fstore_3",
                    "fsub","i2b","i2c","i2d","i2f","i2l","i2s",
                    "iadd","iaload","iand","iastore",
                    "iconst_0","iconst_1","iconst_2","iconst_3","iconst_4","iconst_5","iconst_m1",
                    "idiv",
                    "iload_0","iload_1","iload_2","iload_3",
                    "imul","ineg","ior","irem","ireturn",
                    "ishl","ishr",
                    "istore_0","istore_1","istore_2","istore_3",
                    "isub","iushr","ixor",
                    "l2d","l2f","l2i","ladd",
                    "laload",
                    "land","lastore","lcmp",
                    "lconst_0","lconst_1",
                    "ldiv",
                    "lload_0","lload_1","lload_2","lload_3",
                    "lmul","lneg","lor","lrem",
                    "lreturn",
                    "lshl","lshr",
                    "lstore_0","lstore_1","lstore_2","lstore_3",
                    "lsub","lushr","lxor",
                    "monitorenter","monitorexit",
                    "nop","pop","pop2",
                    "vreturn", // should be "return"
                    "saload","sastore",
                    "swap"
            ]
    }

    def "all those one-arg instructions should be interpreted by the AST transform"() {
        def shell = new GroovyShell()
        def errorCollector = null
        def otherError = null
        when:
            try {
                shell.evaluate("""
                    @groovyx.ast.bytecode.Bytecode
                    void test() {
                        $instruction 'arg'
                    }
                    throw new org.codehaus.groovy.control.MultipleCompilationErrorsException()
                """)
            } catch (java.lang.VerifyError err) {
                 // not a problem, that's not what we're testing
            } catch (MultipleCompilationErrorsException e) {
                errorCollector = e.errorCollector
            } catch (Throwable e) {
                otherError = e
                println "Error on [$instruction]"
                e.printStackTrace()
            }
        then:
            isSupported(instruction,errorCollector)
            otherError == null
        where:
            instruction << Instructions.UNARY_OPS
    }

    private boolean isSupported(instruction,ErrorCollector errorCollector) {
        errorCollector==null||errorCollector.errorCount==0 || !errorCollector.errors[0].cause.message.startsWith('Bytecode operation unsupported')
    }
}
