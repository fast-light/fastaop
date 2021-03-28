package org.fastlight.apt.processor;

import static com.google.common.base.Charsets.UTF_8;

import java.io.*;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.tools.StandardLocation;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 从 google auto 里面 copy 过来的
 * 
 * @author ychost
 * @param <T>
 * @see com.google.auto.service.processor.AutoServiceProcessor
 */
public abstract class BaseFastSpiProcessor<T extends Annotation> extends BaseFastProcessor<T> {

    protected Multimap<String, String> providers = HashMultimap.create();

    protected Class<?> spiType;

    protected Class<? extends Annotation> spiAnnotationTypes;

    /**
     * 注入要生成 SPI 的接口类型
     */
    protected abstract Class<?> supportSpiTypes();

    protected Class<? extends Annotation> supportSpiAnnotationTypes() {
        return atClass;
    }

    @Override
    public void processExecutableElement(ExecutableElement executableElement, AnnotationMirror atm) {

    }

    @Override
    public void processTypeElement(TypeElement typeElement, AnnotationMirror atm) {
        providers.put(spiType.getName(), getBinaryName(typeElement));
    }

    public BaseFastSpiProcessor() {
        spiType = supportSpiTypes();
        spiAnnotationTypes = supportSpiAnnotationTypes();
    }

    @Override
    public void processOver() {
        try {
            generateConfigFiles();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 写入的文件路径，默认是 spi 的路径
     */
    protected String getFilePath(String type) {
        return "META-INF/services/" + type;
    }

    protected synchronized void generateConfigFiles() throws IOException {
        for (String type : providers.keySet()) {
            Filer filer = environment.getFiler();
            String filePath = filer.getResource(StandardLocation.CLASS_OUTPUT, "", getFilePath(type)).toUri().getPath();
            File serviceFile = new File(filePath);
            if (!serviceFile.exists()) {
                Files.createDirectories(Paths.get(filePath.substring(0, filePath.lastIndexOf("/"))));
                Files.write(Paths.get(filePath), new byte[0], StandardOpenOption.CREATE);
            }
            Set<String> services = ServicesFiles.readServiceFile(new FileInputStream(serviceFile));
            Set<String> newServices = new HashSet<>(providers.get(type));
            if (services.containsAll(newServices)) {
                return;
            }
            services.addAll(newServices);
            Path path = Paths.get(filePath);
            Files.write(path, Joiner.on("\n").join(services).getBytes(), StandardOpenOption.WRITE);
        }

    }

    protected String getBinaryName(TypeElement element) {
        return getBinaryNameImpl(element, element.getSimpleName().toString());
    }

    protected String getBinaryNameImpl(TypeElement element, String className) {
        Element enclosingElement = element.getEnclosingElement();

        if (enclosingElement instanceof PackageElement) {
            PackageElement pkg = (PackageElement)enclosingElement;
            if (pkg.isUnnamed()) {
                return className;
            }
            return pkg.getQualifiedName() + "." + className;
        }
        TypeElement typeElement = (TypeElement)enclosingElement;
        return getBinaryNameImpl(typeElement, typeElement.getSimpleName() + "$" + className);
    }

    static final class ServicesFiles {
        static Set<String> readServiceFile(InputStream input) throws IOException {
            HashSet<String> serviceClasses = new HashSet<String>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int commentStart = line.indexOf('#');
                    if (commentStart >= 0) {
                        line = line.substring(0, commentStart);
                    }
                    line = line.trim();
                    if (!line.isEmpty()) {
                        serviceClasses.add(line);
                    }
                }
                return serviceClasses;
            }
        }

    }
}
