package net.curxxed.dev.wintercore.annotation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("net.curxxed.dev.wintercore.annotation.NullCheck")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NullCheckProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(NullCheck.class)) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                generateNullCheckLogic(method);
            }
        }
        return true;
    }

    private void generateNullCheckLogic(ExecutableElement method) {
        //TODO: Implement generation
        System.out.println("Processing method: " + method.getSimpleName());
    }
}