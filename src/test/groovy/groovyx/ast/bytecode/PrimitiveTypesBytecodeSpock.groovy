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

    def "should sum array"() {
        def shell = new GroovyShell()
        def sum = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int sum() {
               l0
                bipush 10
                newarray t_int
                astore 1
               l1
                iconst_0
                istore 2
               l2
                iload 2
                aload 1
                arraylength
                if_icmpge l3
               l4
                aload 1
                iload 2
                iload 2
                iastore
               l5
                iinc 2,1
                _goto l2
               l3
                iconst_0
                istore 2
               l6
                aload 1
                astore 3
               l7
                aload 3
                arraylength
                istore 4
               l8
                iconst_0
                istore 5
               l9
                iload 5
                iload 4
                if_icmpge l10
                aload 3
                iload 5
                iaload
                istore 6
               l11
                iload 2
                iload 6
                iadd
                istore 2
               l12
                iinc 5,1
                _goto l9
               l10
                iload 2
                ireturn
            }
            sum()
            """)

        expect:
            sum == 45

    }

    def "sums the values of an int array with Groovified syntax and boxing"() {
        def shell = new GroovyShell()
        def sum = shell.evaluate("""
          @groovyx.ast.bytecode.Bytecode
          public Integer sum(int[] a) {
            aload 1
            ifnonnull l0
            iconst_0
            invokestatic Integer.valueOf(int) >> Integer
            areturn
           l0:
            iconst_0
            istore 2
            aload 1
            astore 3
            aload 3
            arraylength
            istore 4
            iconst_0
            istore 5
           l1:
            iload 5
            iload 4
            if_icmpge l2
            aload 3
            iload 5
            iaload
            istore 6
            iload 2
            iload 6
            iadd
            istore 2
            iinc 5,1
            _goto l1
           l2:
            iload 2
            invokestatic Integer.valueOf(int) >> Integer
            areturn
          }
          this.&sum
        """
        )

        expect:
            sum(arr as int[]) == arr.sum()

        where:
            arr << [[1,1,1],[1,2,3]]
    }

    def "sums the values of an Integer array with Groovified syntax and unboxing"() {
        def shell = new GroovyShell()
        def sum = shell.evaluate("""
          @groovyx.ast.bytecode.Bytecode
          public int sum(Integer[] a) {
                aload 1
                ifnonnull l0
                iconst_0
                ireturn
               l0:
                iconst_0
                istore 2
                aload 1
                astore 3
                aload 3
                arraylength
                istore 4
                iconst_0
                istore 5
               l1:
                iload 5
                iload 4
                if_icmpge l2
                aload 3
                iload 5
                aaload
                invokevirtual Integer.intValue() >> int
                istore 6
                iload 2
                iload 6
                iadd
                istore 2
                iinc 5,1
                _goto l1
               l2:
                iload 2
                ireturn
          }
          this.&sum
        """
        )

        expect:
            sum(arr as Integer[]) == arr.sum()

        where:
            arr << [[1,1,1],[1,2,3]]
    }
}

