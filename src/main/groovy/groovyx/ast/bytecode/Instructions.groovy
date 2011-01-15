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


/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 15/01/11
 * Time: 23:45
 */

/**
 * List of instructions supported by the AST transform
 */
class Instructions {

    /**
     * Unit operations except load_X / store_X
     */
    public static final Set<String> UNIT_OPS = [
                    "aaload",
                    "aastore",
                    "aconst_null",
                    "areturn",
                    "athrow",
                    "arraylength",
                    "baload","bastore",
                    "caload","castore",
                    "d2f","d2i","d2l","dadd",
                    "daload","dastore",
                    "dcmpg","dcmpl",
                    "dconst_0","dconst_1","ddiv","dload",
                    "dmul","dneg","drem","dreturn",
                    "dsub","dup","dup2","dup2_x1","dup2_x2","dup_x1","dup_x2",
                    "f2d","f2i","f2l","fadd","faload","fastore",
                    "fcmpg","fcmpl",
                    "fconst_0","fconst_1","fconst_2",
                    "fdiv","fload",
                    "fmul","fneg","frem","freturn","fstore",
                    "fsub","i2b","i2c","i2d","i2f","i2l","i2s",
                    "iadd","iaload","iand","iastore",
                    "iconst_0","iconst_1","iconst_2","iconst_3","iconst_4","iconst_5","iconst_m1",
                    "idiv",
                    "imul","ineg","ior","irem","ireturn",
                    "ishl","ishr",
                    "isub","iushr","ixor",
                    "l2d","l2f","l2i","ladd",
                    "laload",
                    "land","lastore","lcmp",
                    "lconst_0","lconst_1",
                    "ldiv",
                    "lmul","lneg","lor","lrem",
                    "lreturn",
                    "lshl","lshr",
                    "lsub","lushr","lxor",
                    "monitorenter","monitorexit",
                    "nop","pop","pop2",
                    "vreturn", // should be "return"
                    "saload","sastore",
                    "swap"
            ]
}
