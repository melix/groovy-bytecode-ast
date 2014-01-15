/*
 *
 *  *
 *  *  Copyright 2014 C�dric Champeau
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  * /
 *
 */

package groovyx.ast.bytecode

import org.codehaus.groovy.control.CompilerConfiguration
import spock.lang.Specification

/**
 * Unit tests for invokedynamic enabled bytecode
 */

class IndyBytecodeSpock extends Specification {

    def "delegate to a Fibonacci method using invokedynamic"() {
        given:
        def config = new CompilerConfiguration()
        config.optimizationOptions.indy = true
        def shell = new GroovyShell(config)
        def fib = shell.evaluate("""import groovy.transform.CompileStatic

import java.lang.invoke.CallSite
import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import static groovyjarjarasm.asm.Opcodes.*

@CompileStatic
class Helper {
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String callType, MethodType type) {
        def mh = lookup.findStatic(Helper, 'fib', type)
        new ConstantCallSite(mh)
    }

    static int fib(int n) { n<2?n:fib(n-2)+fib(n-1) }
}

@groovyx.ast.bytecode.Bytecode
int fib(int n) {
    ILOAD_1
    invokedynamic 'experiment', '(I)I', [H_INVOKESTATIC, 'Helper', 'bootstrap', [CallSite, MethodHandles.Lookup, String, MethodType]]
    ireturn
}
this.&fib
""")
        expect:
        fib(i) == reference

        where:
        i  | reference
        0  | 0
        1  | 1
        2  | 1
        3  | 2
        4  | 3
        5  | 5
        6  | 8
        7  | 13
        8  | 21
        9  | 34
        10 | 55
    }

}
