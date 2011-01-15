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

/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 14/01/11
 * Time: 22:10
 */

class PrimitiveTypesBytecodeSpock extends Specification {

    def "test double square"() {
        def shell = new GroovyShell()
        def square = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            double square(double x) {
               l0
                dload 1
                dload 1
                dmul
                dreturn
            }
            this.&square
        """)

        expect:
            square(x) == x*x

        where:
            x << (0..10)
    }

    def "test int square"() {
        def shell = new GroovyShell()
        def square = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            int square(int x) {
               l0
                iload 1
                iload 1
                imul
                ireturn
            }
            this.&square
        """)

        expect:
            square(x) == x*x

        where:
            x << (0..10)
    }

    def "test float square"() {
        def shell = new GroovyShell()
        def square = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            float square(float x) {
               l0
                fload 1
                fload 1
                fmul
                freturn
            }
            this.&square
        """)

        expect:
            square(x) == x*x

        where:
            x << (0..10)
    }

    def "test long square"() {
        def shell = new GroovyShell()
        def square = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            long square(long x) {
               l0
                lload 1
                lload 1
                lmul
                lreturn
            }
            this.&square
        """)

        expect:
            square(x) == x*x

        where:
            x << (0..10)
    }
}

