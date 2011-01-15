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

    def "should cast double as int"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int cast(double x) {
                dload 1
                d2i
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (int) x

        where:
            x << [0d,1d,1.5d,1.9d, 2.1d, Double.MAX_VALUE]

    }

    def "should cast double as long"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            long cast(double x) {
                dload 1
                d2l
                lreturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (long) x

        where:
            x << [0d,1d,1.5d,1.9d, 2.1d, Double.MAX_VALUE]

    }

    def "should cast double as short"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            short cast(double x) {
                dload 1
                d2i
                i2s
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (short)x

        where:
            x << [0d,1d,1.5d,1.9d, 2.1d, Double.MAX_VALUE]

    }

    def "should cast double as byte"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            byte cast(double x) {
                dload 1
                d2i
                i2b
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (byte)x

        where:
            x << [0d,1d,1.5d,1.9d, 2.1d, Double.MAX_VALUE]
    }

    def "should cast double as char"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            char cast(double x) {
                dload 1
                d2i
                i2c
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (char)x

        where:
            x << [0d,1d,1.5d,1.9d, 2.1d, Double.MAX_VALUE]
    }

    def "should cast byte as double"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            double cast(byte x) {
                iload 1
                i2d
                dreturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (double)x

        where:
            x << [(byte)0,(byte)1,(byte)3]
    }

    def "should cast byte as int"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int cast(byte x) {
                iload 1
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (double)x

        where:
            x << [(byte)0,(byte)1,(byte)3]
    }

    def "should cast short as int"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            int cast(short x) {
                iload 1
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (short)x

        where:
            x << [(short)(short)0,(short)1i,(short)3i, Short.MAX_VALUE]
    }

    def "should cast byte as long"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            long cast(byte x) {
                iload 1
                i2l
                lreturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (long)x

        where:
            x << [(byte)0,(byte)1,(byte)3]
    }

    def "should cast long as double"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            double cast(long x) {
                lload 1
                l2d
                dreturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (double)x

        where:
            x << [0l,1l,2l,3l, Long.MAX_VALUE]
    }

    def "should cast long as char"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            char cast(long x) {
                lload 1
                l2i
                i2c
                ireturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (char)x

        where:
            x << [0l,1l,2l,3l, Long.MAX_VALUE]
    }

    def "should cast char as double"() {
        def shell = new GroovyShell()
        def cast = shell.evaluate("""
            @groovyx.ast.bytecode.Bytecode
            double cast(char x) {
                iload 1
                i2d
                dreturn
            }
            this.&cast
        """)

        expect:
            cast(x) == (double)x

        where:
            x << [(char)0,(char)1,(char)2,(char)3, Character.MAX_VALUE]
    }


    def "should cast float as double"() {
         def shell = new GroovyShell()
         def cast = shell.evaluate("""
             @groovyx.ast.bytecode.Bytecode
             double cast(float x) {
                 fload 1
                 f2d
                 dreturn
             }
             this.&cast
         """)

         expect:
             cast(x) == (double)x

         where:
             x << [0f,1f,1.5f,3f, Float.MAX_VALUE]
     }

    def "should cast double as float"() {
         def shell = new GroovyShell()
         def cast = shell.evaluate("""
             @groovyx.ast.bytecode.Bytecode
             float cast(double x) {
                 dload 1
                 d2f
                 freturn
             }
             this.&cast
         """)

         expect:
             cast(x) == (float)x

         where:
             x << [0d,1d,1.5d,3d, Double.MAX_VALUE]
     }

}
