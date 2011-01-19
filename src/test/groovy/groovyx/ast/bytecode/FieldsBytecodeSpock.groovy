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
 * Tests various access to fields using bytecode.
 */
class FieldsBytecodeSpock extends Specification {

    def "should set and get field value"() {
        def shell = new GroovyShell()
        def result = shell.evaluate("""
            class Test {
                private String field;

                @groovyx.ast.bytecode.Bytecode
                public String getField() {
                    aload_0
                    getfield '.field','Ljava/lang/String;'
                    areturn
                }

                @groovyx.ast.bytecode.Bytecode
                public void setField(String field) {
                    aload_0
                    aload_1
                    putfield '.field','Ljava/lang/String;'
                    return
                }
            }
            def test = new Test()
            test.field = "$value"
            test.field
        """)

        expect:
        result == value

        where:
        value << ['test','test2']
    }

    def "should set and get static field value"() {
        def shell = new GroovyShell()
        def result = shell.evaluate("""
            class Test {
                private static String field;

                @groovyx.ast.bytecode.Bytecode
                public String getField() {
                    getstatic '.field','Ljava/lang/String;'
                    areturn
                }

                @groovyx.ast.bytecode.Bytecode
                public void setField(String field) {
                    aload_1
                    putstatic '.field','Ljava/lang/String;'
                    return
                }
            }
            def test = new Test()
            test.field = "$value"
            test.field
        """)

        expect:
        result == value

        where:
        value << ['test','test2']
    }

    def "set a public field on an instance of a class with the groovyfied notation"() {
        def shell = new GroovyShell()
        def result = shell.evaluate("""
            import groovyx.ast.bytecode.*

            @Bytecode
            def getPerson() {
                _new "groovyx/ast/bytecode/SomePerson"
                dup
                invokespecial SomePerson."<init>"() >> Void
                astore 1
                aload 1
                ldc "Guillaume"
                putfield SomePerson.name >> String
                aload 1
                areturn
            }
            getPerson().name
        """)

        expect:
        result == "Guillaume"
    }

    def "get a public static field with the groovyfied notation"() {
        def shell = new GroovyShell()
        def result = shell.evaluate("""
            import groovyx.ast.bytecode.*

            class Someone {
                public static int age = 33

                @Bytecode
                int getAge() {
                    getstatic age >> int
                    ireturn
                }
            }

            new Someone().getAge()
        """)

        expect:
        result == 33
    }
}

class SomePerson {
    public String name
    public static int age = 33
}