package it.rattly.regions.utils

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import it.rattly.regions.MovecraftRegions
import kotlin.reflect.KClass

// remember to call close on this else memory leaks ._.
val cachedScan: ScanResult by lazy {
    @Suppress("UnstableApiUsage")
    ClassGraph()
        .enableClassInfo()
        .enableAnnotationInfo()
        .acceptPackages(MovecraftRegions.pluginMeta.mainClass.split(".").dropLast(1).joinToString("."))
        .scan()
}

inline fun <reified T> getAllAnnotatedWith(annotation: KClass<out Annotation>) =
    lazy {
        with(cachedScan) {
            getClassesWithAnnotation(annotation.java.canonicalName).map { info ->
                (Class.forName(info.name).kotlin.objectInstance
                    ?: throw IllegalStateException("Class ${info.name} is not an object")) as? T
                    ?: throw IllegalStateException("Class ${info.name} is not an instance of ${T::class.simpleName}")
            }
        }
    }