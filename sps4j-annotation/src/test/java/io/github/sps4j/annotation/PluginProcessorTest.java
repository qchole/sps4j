package io.github.sps4j.annotation;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PluginProcessorTest {

    @InjectMocks
    private PluginProcessor processor;

    @Mock
    private ProcessingEnvironment processingEnv;
    @Mock
    private Types typeUtils;

    // Mocks for findAllInterfacesOfType
    @Mock
    private TypeElement classElement, parentInterfaceElement, grandParentInterfaceElement;
    @Mock
    private DeclaredType parentInterfaceMirror, grandParentInterfaceMirror;

    // Mocks for findPluginInterfaceFrom
    @Mock
    private AnnotationMirror pluginAnnotationMirror;
    @Mock
    private DeclaredType annotationType;
    @Mock
    private Element annotationElement;
    @Mock
    private ExecutableElement valueExecutableElement;
    @Mock
    private AnnotationValue annotationValue;


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void testFindAllInterfacesOfType() {
        // --- MOCK SETUP ---
        processor.init(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);

        // 1. Define the hierarchy:
        //    - GrandParentInterface (no super-interfaces)
        //    - ParentInterface extends GrandParentInterface
        //    - MyClass implements ParentInterface

        // Use raw List to bypass generics issue
        List parentInterfaces = Collections.singletonList(parentInterfaceMirror);
        List grandParentInterfaces = Collections.singletonList(grandParentInterfaceMirror);


        // 2. Mock behavior for MyClass
        when(classElement.getInterfaces()).thenReturn(parentInterfaces);

        // 3. Mock behavior for ParentInterface
        when(parentInterfaceMirror.toString()).thenReturn("com.example.ParentInterface");
        when(typeUtils.asElement(parentInterfaceMirror)).thenReturn(parentInterfaceElement);
        when(parentInterfaceElement.getInterfaces()).thenReturn(grandParentInterfaces);

        // 4. Mock behavior for GrandParentInterface
        when(grandParentInterfaceMirror.toString()).thenReturn("com.example.GrandParentInterface");
        when(typeUtils.asElement(grandParentInterfaceMirror)).thenReturn(grandParentInterfaceElement);
        when(grandParentInterfaceElement.getInterfaces()).thenReturn(Collections.emptyList());


        // --- EXECUTION ---
        Map<String, TypeElement> interfaces = processor.findAllInterfacesOfType(classElement);


        // --- VERIFICATION ---
        assertNotNull(interfaces);
        assertEquals(2, interfaces.size());
        assertTrue(interfaces.containsKey("com.example.ParentInterface"));
        assertTrue(interfaces.containsKey("com.example.GrandParentInterface"));
        assertEquals(parentInterfaceElement, interfaces.get("com.example.ParentInterface"));
        assertEquals(grandParentInterfaceElement, interfaces.get("com.example.GrandParentInterface"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void detectPluginInterfaceType_happyPath() {
        // --- HIERARCHY ---
        // MyPlugin -> AbstractPlugin -> Object
        // MyPlugin implements MyPluginInterface
        // MyPluginInterface extends Sps4jPlugin (base)
        // MyPluginInterface is annotated with @Sps4jPluginInterface(value="my-type")

        // --- MOCK SETUP ---
        processor.init(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);

        // 1. Mock TypeElements
        TypeElement myPluginElement = mock(TypeElement.class);
        TypeElement abstractPluginElement = mock(TypeElement.class);
        TypeElement objectElement = mock(TypeElement.class);
        TypeElement myPluginInterfaceElement = mock(TypeElement.class);
        TypeElement sps4jPluginElement = mock(TypeElement.class);

        // 2. Mock TypeMirrors
        DeclaredType abstractPluginMirror = mock(DeclaredType.class);
        DeclaredType objectMirror = mock(DeclaredType.class);
        DeclaredType myPluginInterfaceMirror = mock(DeclaredType.class);
        DeclaredType sps4jPluginMirror = mock(DeclaredType.class);

        // 3. Link hierarchy with getSuperclass() and getQualifiedName()
        when(myPluginElement.getQualifiedName()).thenReturn(new NameMock("com.example.MyPlugin"));
        when(myPluginElement.getSuperclass()).thenReturn(abstractPluginMirror);
        when(typeUtils.asElement(abstractPluginMirror)).thenReturn(abstractPluginElement);

        when(abstractPluginElement.getQualifiedName()).thenReturn(new NameMock("com.example.AbstractPlugin"));
        when(abstractPluginElement.getSuperclass()).thenReturn(objectMirror);
        when(typeUtils.asElement(objectMirror)).thenReturn(objectElement);

        when(objectElement.getQualifiedName()).thenReturn(new NameMock("java.lang.Object")); // Stop condition

        // 4. Link interfaces with getInterfaces()
        List myPluginInterfaces = Collections.singletonList(myPluginInterfaceMirror);
        when(myPluginElement.getInterfaces()).thenReturn(myPluginInterfaces);
        when(myPluginInterfaceMirror.toString()).thenReturn("com.example.MyPluginInterface");
        when(typeUtils.asElement(myPluginInterfaceMirror)).thenReturn(myPluginInterfaceElement);

        List myPluginInterfaceInterfaces = Collections.singletonList(sps4jPluginMirror);
        when(myPluginInterfaceElement.getInterfaces()).thenReturn(myPluginInterfaceInterfaces);
        when(sps4jPluginMirror.toString()).thenReturn(InterfaceAnnotationProcessor.PLUGIN_BASE_INTERFACE);
        when(typeUtils.asElement(sps4jPluginMirror)).thenReturn(sps4jPluginElement);

        when(sps4jPluginElement.getInterfaces()).thenReturn(Collections.emptyList());
        when(abstractPluginElement.getInterfaces()).thenReturn(Collections.emptyList());

        // 5. Mock the annotation on MyPluginInterface
        setupAnnotationMock("my-type");
        List annotationMirrors = Collections.singletonList(pluginAnnotationMirror);
        when(myPluginInterfaceElement.getAnnotationMirrors()).thenReturn(annotationMirrors);

        // --- EXECUTION ---
        Pair<String, String> result = processor.detectPluginInterfaceType(myPluginElement);

        // --- VERIFICATION ---
        assertNotNull(result);
        assertEquals("my-type", result.getKey());
        assertEquals("com.example.MyPluginInterface", result.getValue());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void detectPluginInterfaceType_shouldFailWhenBaseInterfaceIsMissing() {
        // --- HIERARCHY ---
        // MyPlugin -> Object
        // MyPlugin implements MyPluginInterface
        // MyPluginInterface is NOT extending Sps4jPlugin

        // --- MOCK SETUP ---
        processor.init(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);

        TypeElement myPluginElement = mock(TypeElement.class);
        TypeElement myPluginInterfaceElement = mock(TypeElement.class);
        DeclaredType myPluginInterfaceMirror = mock(DeclaredType.class);
        TypeElement objectElement = mock(TypeElement.class);
        DeclaredType objectMirror = mock(DeclaredType.class);

        // Mock hierarchy traversal
        when(myPluginElement.getQualifiedName()).thenReturn(new NameMock("com.example.MyPlugin"));
        when(myPluginElement.getSuperclass()).thenReturn(objectMirror);
        when(typeUtils.asElement(objectMirror)).thenReturn(objectElement);
        when(objectElement.getQualifiedName()).thenReturn(new NameMock("java.lang.Object"));


        List myPluginInterfaces = Collections.singletonList(myPluginInterfaceMirror);
        when(myPluginElement.getInterfaces()).thenReturn(myPluginInterfaces);
        when(myPluginInterfaceMirror.toString()).thenReturn("com.example.MyPluginInterface");
        when(typeUtils.asElement(myPluginInterfaceMirror)).thenReturn(myPluginInterfaceElement);
        when(myPluginInterfaceElement.getInterfaces()).thenReturn(Collections.emptyList()); // Does not extend base

        // --- EXECUTION & VERIFICATION ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            processor.detectPluginInterfaceType(myPluginElement);
        });
        assertTrue(exception.getMessage().contains("Plugin must implements"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void detectPluginInterfaceType_shouldFailWhenPluginInterfaceIsMissing() {
        // --- HIERARCHY ---
        // MyPlugin -> Object
        // MyPlugin implements MyInterface (which extends Sps4jPlugin, but is NOT annotated)

        // --- MOCK SETUP ---
        processor.init(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);

        TypeElement myPluginElement = mock(TypeElement.class);
        TypeElement myInterfaceElement = mock(TypeElement.class);
        TypeElement sps4jPluginElement = mock(TypeElement.class);
        DeclaredType myInterfaceMirror = mock(DeclaredType.class);
        DeclaredType sps4jPluginMirror = mock(DeclaredType.class);
        TypeElement objectElement = mock(TypeElement.class);
        DeclaredType objectMirror = mock(DeclaredType.class);

        when(myPluginElement.getQualifiedName()).thenReturn(new NameMock("com.example.MyPlugin"));
        when(myPluginElement.getSuperclass()).thenReturn(objectMirror);
        when(typeUtils.asElement(objectMirror)).thenReturn(objectElement);
        when(objectElement.getQualifiedName()).thenReturn(new NameMock("java.lang.Object"));

        List myPluginInterfaces = Collections.singletonList(myInterfaceMirror);
        when(myPluginElement.getInterfaces()).thenReturn(myPluginInterfaces);
        when(myInterfaceMirror.toString()).thenReturn("com.example.MyInterface");
        when(typeUtils.asElement(myInterfaceMirror)).thenReturn(myInterfaceElement);

        List myInterfaceInterfaces = Collections.singletonList(sps4jPluginMirror);
        when(myInterfaceElement.getInterfaces()).thenReturn(myInterfaceInterfaces); // Extends base
        when(sps4jPluginMirror.toString()).thenReturn(InterfaceAnnotationProcessor.PLUGIN_BASE_INTERFACE);
        when(typeUtils.asElement(sps4jPluginMirror)).thenReturn(sps4jPluginElement);

        when(myInterfaceElement.getAnnotationMirrors()).thenReturn(Collections.emptyList()); // But is not annotated

        // --- EXECUTION & VERIFICATION ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            processor.detectPluginInterfaceType(myPluginElement);
        });
        assertEquals("can not detect plugin type", exception.getMessage());
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void findPluginInterfaceFrom_shouldReturnValueWhenSet() {
        // --- MOCK SETUP ---
        // Mock the annotation mirror to represent @Sps4jPluginInterface(value = "my-plugin")
        setupAnnotationMock("my-plugin");

        TypeElement pluginInterfaceElement = mock(TypeElement.class);
        List annotationMirrors = Collections.singletonList(pluginAnnotationMirror);
        when(pluginInterfaceElement.getAnnotationMirrors()).thenReturn(annotationMirrors);

        Map<String, TypeElement> interfaces = Collections.singletonMap("com.example.MyPluginInterface", pluginInterfaceElement);

        // --- EXECUTION ---
        Pair<String, String> result = PluginProcessor.findPluginInterfaceFrom(interfaces);

        // --- VERIFICATION ---
        assertNotNull(result);
        assertEquals("my-plugin", result.getKey());
        assertEquals("com.example.MyPluginInterface", result.getValue());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void findPluginInterfaceFrom_shouldReturnInterfaceNameWhenValueIsBlank() {
        // --- MOCK SETUP ---
        // Mock the annotation mirror to represent @Sps4jPluginInterface(value = "")
        setupAnnotationMock("");

        TypeElement pluginInterfaceElement = mock(TypeElement.class);
        List annotationMirrors = Collections.singletonList(pluginAnnotationMirror);
        when(pluginInterfaceElement.getAnnotationMirrors()).thenReturn(annotationMirrors);

        Map<String, TypeElement> interfaces = Collections.singletonMap("com.example.MyPluginInterface", pluginInterfaceElement);

        // --- EXECUTION ---
        Pair<String, String> result = PluginProcessor.findPluginInterfaceFrom(interfaces);

        // --- VERIFICATION ---
        assertNotNull(result);
        assertEquals("com.example.MyPluginInterface", result.getKey());
        assertEquals("com.example.MyPluginInterface", result.getValue());
    }

    @Test
    void findPluginInterfaceFrom_shouldReturnNullWhenNoAnnotation() {
        // --- MOCK SETUP ---
        TypeElement nonPluginInterfaceElement = mock(TypeElement.class);
        when(nonPluginInterfaceElement.getAnnotationMirrors()).thenReturn(Collections.emptyList());
        Map<String, TypeElement> interfaces = Collections.singletonMap("com.example.MyInterface", nonPluginInterfaceElement);

        // --- EXECUTION ---
        Pair<String, String> result = PluginProcessor.findPluginInterfaceFrom(interfaces);

        // --- VERIFICATION ---
        assertNull(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void findPluginInterfaceFrom_shouldThrowExceptionOnConflict() {
        // --- MOCK SETUP ---
        setupAnnotationMock("plugin-1");
        TypeElement pluginInterface1 = mock(TypeElement.class);
        List annotationMirrors1 = Collections.singletonList(pluginAnnotationMirror);
        when(pluginInterface1.getAnnotationMirrors()).thenReturn(annotationMirrors1);

        // Create a separate set of mocks for the second annotation
        AnnotationMirror pluginAnnotationMirror2 = mock(AnnotationMirror.class);
        DeclaredType annotationType2 = mock(DeclaredType.class);
        Element annotationElement2 = mock(Element.class);
        ExecutableElement valueExecutableElement2 = mock(ExecutableElement.class);
        AnnotationValue annotationValue2 = mock(AnnotationValue.class);
        when(pluginAnnotationMirror2.getAnnotationType()).thenReturn(annotationType2);
        when(annotationType2.asElement()).thenReturn(annotationElement2);
        when(annotationElement2.asType()).thenReturn(annotationType2);
        when(annotationType2.toString()).thenReturn(Sps4jPluginInterface.class.getCanonicalName());
        Map values2 = Collections.singletonMap(valueExecutableElement2, annotationValue2);
        when(pluginAnnotationMirror2.getElementValues()).thenReturn(values2);
        when(valueExecutableElement2.getSimpleName()).thenReturn(new NameMock("value"));
        when(annotationValue2.getValue()).thenReturn("plugin-2");

        TypeElement pluginInterface2 = mock(TypeElement.class);
        List annotationMirrors2 = Collections.singletonList(pluginAnnotationMirror2);
        when(pluginInterface2.getAnnotationMirrors()).thenReturn(annotationMirrors2);

        Map<String, TypeElement> interfaces = new HashMap<String, TypeElement>() {
            {
                put("com.example.Plugin1", pluginInterface1);
                put("com.example.Plugin2", pluginInterface2);
            }
        };

        // --- EXECUTION & VERIFICATION ---
        assertThrows(IllegalStateException.class, () -> PluginProcessor.findPluginInterfaceFrom(interfaces));
    }


    @Test
    void testIsValidVersionConstraint() {
        assertTrue(PluginProcessor.isValidVersionConstraint(">=1.0.0"));
        assertTrue(PluginProcessor.isValidVersionConstraint("~1.2.3"));
        assertFalse(PluginProcessor.isValidVersionConstraint("1.0.0-invalid"));
        assertFalse(PluginProcessor.isValidVersionConstraint(""));
    }

    /**
     * Helper to set up the complex chain of mocks for an Sps4jPluginInterface annotation.
     * @param value The value for the annotation's "value" property.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupAnnotationMock(String value) {
        when(pluginAnnotationMirror.getAnnotationType()).thenReturn(annotationType);
        when(annotationType.asElement()).thenReturn(annotationElement);
        when(annotationElement.asType()).thenReturn(annotationType);
        when(annotationType.toString()).thenReturn(Sps4jPluginInterface.class.getCanonicalName());
        Map values = Collections.singletonMap(valueExecutableElement, annotationValue);
        when(pluginAnnotationMirror.getElementValues()).thenReturn(values);
        when(valueExecutableElement.getSimpleName()).thenReturn(new NameMock("value"));
        when(annotationValue.getValue()).thenReturn(value);
    }

    /**
     * Mock implementation of Name, because it's a final class and cannot be mocked by Mockito directly.
     */
    private static class NameMock implements Name {
        private final String name;
        public NameMock(String name) { this.name = name; }
        @Override public boolean contentEquals(CharSequence cs) { return name.contentEquals(cs); }
        @Override public int length() { return name.length(); }
        @Override public char charAt(int index) { return name.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { return name.subSequence(start, end); }
        @Override public String toString() { return name; }
    }
}
