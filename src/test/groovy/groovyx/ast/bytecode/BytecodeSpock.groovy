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

import spock.lang.Specification
import org.codehaus.groovy.control.MultipleCompilationErrorsException

/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 14/01/11
 * Time: 22:10
 */

class BytecodeSpock extends Specification {

    def "generate a Fibonacci method"() {
        def shell = new GroovyShell()
        def fib = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int fib(int n) {
                 l0
                    iload 1
                    iconst_2
                    if_icmpge l1
                    iload 1
                    _goto l2
                 l1
                    aload 0
                    iload 1
                    iconst_2
                    isub
                    invokevirtual '.fib','(I)I'
                    aload 0
                    iload 1
                    iconst_1
                    isub
                    invokevirtual '.fib', '(I)I'
                    iadd
                 l2
                    ireturn
            }
            this.&fib
        """)

        expect:
            fib(i) == reference

        where:
            i   | reference
            0   | 0
            1   | 1
            2   | 1
            3   | 2
            4   | 3
            5   | 5
            6   | 8
            7   | 13
            8   | 21
            9   | 34
            10  | 55
    }

    def "test for loop with int index"() {
        def shell = new GroovyShell()
        def sum = shell.evaluate("""
            /**
            * int sum = 0;
            * for (int i = 0; i < limit; i++) {
            *   sum += i;
            * }
            * return sum;
            */
            @groovyx.ast.bytecode.Bytecode
            int sum(int limit) {
               l0
                iconst_0
                istore 2
               l1
                iconst_0
                istore 3
               l2
                iload 3
                iload 1
                if_icmpge l3
               l4
                iload 2
                iload 3
                iadd
                istore 2
               l5
                iinc 3,1
                _goto l2
               l3
                iload 2
                ireturn
            }
            this.&sum
        """)

        expect:
            sum(i) == reference
        where:
            i   |   reference
            0   |   0
            1   |   0
            2   |   1
            3   |   3
            4   |   6
    }

    def "tests toString"() {
        def shell = new GroovyShell()
        def toString = shell.evaluate("""
            /**
            * return o.toString()
            */
            @groovyx.ast.bytecode.Bytecode
            String toStr(Object o) {
               l0
                aload 1
                invokevirtual 'java.lang.Object.toString','()Ljava/lang/String;'
                areturn
            }
            this.&toStr
        """)

        expect:
            toString(o) == o.toString()

        where:
            o << ["toto", 1, new GroovyShell()]
    }

    def "test static method call"() {
        def shell = new GroovyShell()
        def toString = shell.evaluate("""

            static String echo(String text) {
                text
            }

            @groovyx.ast.bytecode.Bytecode
            String echo2(String o) {
               l0
                aload 1
                invokestatic '.echo','(Ljava/lang/String;)Ljava/lang/String;'
                areturn
            }
            this.&echo2
        """)

        expect:
            toString(o) == o

        where:
            o << ["toto", "tata"]
    }

    def "test private method call"() {
        def shell = new GroovyShell()
        def toString = shell.evaluate("""

            private String echo(String text) {
                text
            }

            @groovyx.ast.bytecode.Bytecode
            String echo2(String o) {
               l0
                aload 0
                aload 1
                invokespecial '.echo','(Ljava/lang/String;)Ljava/lang/String;'
                areturn
            }
            this.&echo2
        """)

        expect:
            toString(o) == o

        where:
            o << ["toto", "tata"]
    }

    def "test jump to illegal label"() {
        def shell = new GroovyShell()

        when:
            shell.evaluate("""

            @groovyx.ast.bytecode.Bytecode
            void jump() {
                _goto l1
            }
        """)

        then:
            def e = thrown(MultipleCompilationErrorsException)
            e.errorCollector.errors.size() == 1
            e.errorCollector.errors[0].cause.class == IllegalArgumentException

    }

    def "test void return"() {
        def shell = new GroovyShell()

        when:
            shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            void Void() {
                vreturn
            }
        """)

        then:
            notThrown(Exception)
    }

    def "should instantiate and return integer with autoboxing"() {
        def shell = new GroovyShell()
        def run = shell.evaluate("""
            /**
            * return new Integer(i)
            */
            @groovyx.ast.bytecode.Bytecode
            int run(int i) {
                _new 'java/lang/Integer'
                dup
                iload 1
                invokespecial 'java/lang/Integer.<init>','(I)V'
                invokevirtual 'java/lang/Integer.intValue','()I'
                ireturn
            }
            this.&run
        """)

        expect:
            run(i) == i

        where:
            i << (0..10)
    }

    def "should pop value on stack"() {
        def shell = new GroovyShell()
        def run = shell.evaluate("""
            /**
            * 1
            * return i
            */
            @groovyx.ast.bytecode.Bytecode
            int run(int i) {
                iconst_1
                pop
                iload 1
                ireturn
            }
            this.&run
        """)

        expect:
            run(i) == i

        where:
            i << (0..10)
    }

    def "should pop double value on stack"() {
        def shell = new GroovyShell()
        def run = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int run(int i) {
                ldc 2.0d
                pop2
                iload 1
                ireturn
            }
            this.&run
        """)

        expect:
            run(i) == i

        where:
            i << (0..10)
    }

    def "test lookupswitch statement"() {
        def shell = new GroovyShell()
        def run = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int run(int i) {
                iload_1
                lookupswitch(
                   2: l2,
                   5: l1,
                   default: l3)
                l1
                    iconst_1
                    ireturn
                l2
                    iconst_2
                    ireturn
                l3
                    iconst_3
                    ireturn
            }
            this.&run
        """)

        expect:
            run(x) == y

        where:
            x   |   y
            0   |   3
            1   |   3
            2   |   2
            3   |   3
            4   |   3
            5   |   1
            6   |   3
    }

    def "test tableswitch statement"() {
        def shell = new GroovyShell()
        def run = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int run(int i) {
                iload_1
                tableswitch(
                   0: l2,
                   1: l1,
                   default: l3)
                l1
                    iconst_1
                    ireturn
                l2
                    iconst_2
                    ireturn
                l3
                    iconst_3
                    ireturn
            }
            this.&run
        """)

        expect:
            run(x) == y

        where:
            x   |   y
            0   |   2
            1   |   1
            2   |   3
            3   |   3
            4   |   3
            5   |   3
            6   |   3
    }

    def "test multidimensional array instantiation"() {
        def shell = new GroovyShell()
                def run = shell.evaluate("""
                    @groovyx.ast.bytecode.Bytecode
                    int[][] run(int x, int y) {
                        iload_1
                        iload_2
                        multianewarray '[[I',2
                        areturn
                    }
                    this.&run
                """)

                expect:
                    run(x,y).length == x
                    sizeOf2dLevel(run(x,y)) == y
                where:
                    x   |   y
                    0   |   0
                    1   |   0
                    1   |   1
                    2   |   0
                    2   |   1
                    2   |   2
    }

    private static sizeOf2dLevel(int[][] arr) {
        arr.length==0?0:arr[0].length
    }
}

