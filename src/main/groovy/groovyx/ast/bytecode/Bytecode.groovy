package groovyx.ast.bytecode

/**
 * Created by IntelliJ IDEA.
 * User: cedric
 * Date: 14/01/11
 * Time: 21:51
 */

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import org.codehaus.groovy.transform.GroovyASTTransformationClass

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(["groovyx.ast.bytecode.BytecodeASTTransformation"])
public @interface Bytecode {

}
