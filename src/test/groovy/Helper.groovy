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
def shell = new GroovyShell()
def run = shell.evaluate("""
                    @groovyx.ast.bytecode.Bytecode
                    int[][] run(int x, int y) {
                        iload_1
                        iload_2
                        return
                        multianewarray '[[I',2
                        return
                        areturn
                    }
                    this.&run
                """)

println """
   L0
    LINENUMBER 27 L0
    ICONST_0
    ISTORE 2
   L1
    LINENUMBER 28 L1
    ICONST_0
    ISTORE 3
   L2
   FRAME APPEND [I I]
    ILOAD 3
    ILOAD 1
    IF_ICMPGE L3
   L4
    LINENUMBER 29 L4
    ILOAD 2
    ILOAD 1
    IADD
    ISTORE 2
   L5
    LINENUMBER 28 L5
    IINC 3 1
    GOTO L2
   L3
    LINENUMBER 31 L3
   FRAME CHOP 1
    ILOAD 2
    IRETURN""".toLowerCase()