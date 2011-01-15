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
 * Date: 15/01/11
 * Time: 11:36
 */
class TypeCastingBytecodeSpock extends Specification {
    def "should cast object as string"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            CharSequence cast(Object o) {
               l0
                aload 1
                checkcast 'java/lang/CharSequence'
                areturn
            }
            this.&cast
        """)

        expect:
            cast(o) == o

        where:
            o << ['String',new StringBuffer('test'), new StringBuilder('Spock')]
    }

    def "should fail casting as char sequence"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            CharSequence cast(Object o) {
               l0
                aload 1
                checkcast 'java/lang/CharSequence'
                areturn
            }
            this.&cast
        """)
        when:
            cast(x)

        then:
            thrown(ClassCastException)

        where:
            x << [1, 1f, 1d, new Object()]
    }
}
