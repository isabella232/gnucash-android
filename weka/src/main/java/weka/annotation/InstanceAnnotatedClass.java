package weka.annotation;

import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 27.
 */

public class InstanceAnnotatedClass {
    private TypeElement annotatedClassElement;
    private String name;

    public InstanceAnnotatedClass(TypeElement classElement) throws IllegalArgumentException {
        this.annotatedClassElement = classElement;
        Instance annotation = classElement.getAnnotation(Instance.class);
        name = annotation.name();

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException(
                    String.format("name() in @%s for class %s is null or empty.",
                            Instance.class.getSimpleName(),
                            classElement.getQualifiedName().toString())
            );
        }

    }

    public String getName() {
        return name;
    }

    public TypeElement getTypeElement() {
        return annotatedClassElement;
    }

    public void generateCode(Elements elements, Filer filer) throws IOException {
        JavaFileObject jfo = filer.createSourceFile("asdf");
    }
}
